import re
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.embedder import Embedder
from app.models import QA

UNSURE_PREFIX = (
    "Có phải bạn muốn hỏi một trong những câu dưới đây không? "
    "Hãy chọn hoặc diễn đạt lại câu hỏi rõ hơn giúp mình nhé."
)
FALLBACK_ANSWER = (
    "Xin lỗi, mình chưa có thông tin để trả lời câu hỏi này. "
    "Bạn thử diễn đạt lại câu hỏi, hoặc liên hệ bộ phận hỗ trợ của cổng "
    "trợ cấp xã hội (UBND cấp xã nơi cư trú) để được giải đáp trực tiếp."
)

_EMB_WEIGHT = 0.55
_WORD_WEIGHT = 0.25
_CHAR_WEIGHT = 0.20
_KEYWORD_BONUS_PER_HIT = 0.10
_KEYWORD_BONUS_CAP = 0.30
_MAX_SUGGESTIONS = 3


def normalize(text: str) -> str:
    """Chuan hoa cho nhanh tu vung: bo dau, d->d thuong, chi giu chu/so."""
    text = unicodedata.normalize("NFC", text).lower()
    # đ la ky tu rieng, NFD khong tach dau — thay truoc khi strip
    text = text.replace("đ", "d")  # đ
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    text = re.sub(r"[^a-z0-9\s]", " ", text)
    return re.sub(r"\s+", " ", text).strip()


_WORD_RE = re.compile(r"[^\W\d_]+", re.UNICODE)


def _has_marks(word: str) -> bool:
    if "đ" in word or "Đ" in word:
        return True
    return any(
        unicodedata.category(c) == "Mn"
        for c in unicodedata.normalize("NFD", word)
    )


def build_restore_map(rows) -> dict[str, str]:
    """Tu dien khoi phuc dau tu corpus: tu-khong-dau -> dang co dau pho bien nhat.

    Model embedding gan nhu mu voi tieng Viet khong dau, nen query khong dau
    duoc khoi phuc dau bang chinh tu vung cua bo Q&A truoc khi encode.
    Uu tien tan suat trong question+keywords; chi dung answer cho tu khong
    xuat hien o do (answer dai, de keo sai kieu "toi" -> "tối" vi "tối đa").
    """
    primary: dict[str, Counter] = defaultdict(Counter)
    secondary: dict[str, Counter] = defaultdict(Counter)
    for row in rows:
        qk = row.question + " " + (row.keywords or "")
        for word in _WORD_RE.findall(qk.lower()):
            primary[normalize(word)][word] += 1
        for word in _WORD_RE.findall(row.answer.lower()):
            secondary[normalize(word)][word] += 1

    mapping: dict[str, str] = {}
    for key in set(primary) | set(secondary):
        counter = primary.get(key) or secondary[key]
        best = counter.most_common(1)[0][0]
        if best != key:
            mapping[key] = best
    return mapping


def restore_diacritics(text: str, mapping: dict[str, str]) -> str:
    """Them dau cho cac tu chua co dau theo tu dien corpus; tu co dau giu nguyen."""

    def replace(match: re.Match) -> str:
        word = match.group(0)
        lower = word.lower()
        if _has_marks(lower):
            return word
        return mapping.get(lower, word)

    return _WORD_RE.sub(replace, text)


@dataclass(frozen=True)
class Suggestion:
    id: int
    question: str
    score: float


@dataclass(frozen=True)
class MatchResult:
    result_type: str  # MATCHED | UNSURE | FALLBACK
    qa_id: int | None
    answer: str
    matched_question: str | None
    score: float
    suggestions: list[Suggestion] = field(default_factory=list)


class ChatbotIndex:
    """Snapshot bat bien cua bo Q&A: ma tran embedding + 2 ma tran TF-IDF.

    Rebuild = build instance moi roi swap reference — khong khoa request dang chay.
    """

    def __init__(
        self,
        embedder: Embedder,
        answer_threshold: float,
        suggest_threshold: float,
    ):
        self._embedder = embedder
        self._answer_threshold = answer_threshold
        self._suggest_threshold = suggest_threshold
        self._ids: list[int] = []
        self._questions: list[str] = []
        self._answers: list[str] = []
        self._keyword_sets: list[list[str]] = []
        self._restore_map: dict[str, str] = {}
        self._emb_matrix: np.ndarray | None = None
        self._word_vec: TfidfVectorizer | None = None
        self._char_vec: TfidfVectorizer | None = None
        self._word_matrix = None
        self._char_matrix = None
        self.built_at: datetime = datetime.utcnow()

    @property
    def qa_count(self) -> int:
        return len(self._ids)

    @property
    def _empty(self) -> bool:
        return not self._ids

    @classmethod
    def build(
        cls,
        session: Session,
        embedder: Embedder,
        answer_threshold: float,
        suggest_threshold: float,
        *,
        reembed: bool = False,
    ) -> tuple["ChatbotIndex", int]:
        """Nap Q&A active tu DB, dung index. Tra ve (index, so cau phai encode lai).

        Uu tien doc embedding cache trong DB (dung model); cau thieu/lech model
        thi encode va ghi nguoc vao DB.
        """
        index = cls(embedder, answer_threshold, suggest_threshold)
        rows = session.scalars(
            select(QA).where(QA.active.is_(True)).order_by(QA.id)
        ).all()
        if not rows:
            return index, 0

        vectors: list[np.ndarray | None] = []
        to_encode: list[int] = []  # vi tri trong rows can encode
        for i, row in enumerate(rows):
            cached = None
            if (
                not reembed
                and row.embedding is not None
                and row.embedding_model == embedder.model_name
            ):
                cached = np.frombuffer(row.embedding, dtype=np.float32)
            vectors.append(cached)
            if cached is None:
                to_encode.append(i)

        if to_encode:
            encoded = embedder.encode([rows[i].question for i in to_encode])
            for pos, i in enumerate(to_encode):
                vec = np.asarray(encoded[pos], dtype=np.float32)
                vectors[i] = vec
                rows[i].embedding = vec.tobytes()
                rows[i].embedding_model = embedder.model_name
            session.commit()

        index._ids = [r.id for r in rows]
        index._questions = [r.question for r in rows]
        index._answers = [r.answer for r in rows]
        index._keyword_sets = [
            [
                kw
                for kw in (normalize(part) for part in (r.keywords or "").split(","))
                if kw
            ]
            for r in rows
        ]
        index._restore_map = build_restore_map(rows)
        index._emb_matrix = np.vstack(vectors)

        corpus = [
            normalize(r.question + " " + (r.keywords or "")) for r in rows
        ]
        index._word_vec = TfidfVectorizer(ngram_range=(1, 2))
        index._word_matrix = index._word_vec.fit_transform(corpus)
        index._char_vec = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 4))
        index._char_matrix = index._char_vec.fit_transform(corpus)
        index.built_at = datetime.utcnow()
        return index, len(to_encode)

    def query(self, raw_question: str) -> MatchResult:
        if self._empty:
            return MatchResult("FALLBACK", None, FALLBACK_ANSWER, None, 0.0)

        raw = raw_question.strip()
        # Query khong dau -> khoi phuc dau theo tu vung corpus truoc khi encode
        # (model embedding hieu tieng Viet co dau tot hon han)
        query_vec = self._embedder.encode(
            [restore_diacritics(raw, self._restore_map)]
        )[0]
        emb_sims = self._emb_matrix @ query_vec

        norm_q = normalize(raw)
        # TF-IDF cua sklearn da L2-normalize -> dot = cosine
        word_sims = np.asarray(
            (self._word_matrix @ self._word_vec.transform([norm_q]).T).todense()
        ).ravel()
        char_sims = np.asarray(
            (self._char_matrix @ self._char_vec.transform([norm_q]).T).todense()
        ).ravel()

        padded = f" {norm_q} "
        bonus = np.array(
            [
                min(
                    _KEYWORD_BONUS_CAP,
                    _KEYWORD_BONUS_PER_HIT
                    * sum(1 for kw in kws if f" {kw} " in padded or kw in norm_q),
                )
                for kws in self._keyword_sets
            ]
        )

        scores = np.clip(
            _EMB_WEIGHT * emb_sims
            + _WORD_WEIGHT * word_sims
            + _CHAR_WEIGHT * char_sims
            + bonus,
            0.0,
            1.0,
        )
        order = np.argsort(scores)[::-1]
        top = int(order[0])
        top_score = float(scores[top])

        def _suggest(ranks: np.ndarray) -> list[Suggestion]:
            return [
                Suggestion(self._ids[int(i)], self._questions[int(i)], float(scores[int(i)]))
                for i in ranks[:_MAX_SUGGESTIONS]
            ]

        if top_score >= self._answer_threshold:
            return MatchResult(
                "MATCHED",
                self._ids[top],
                self._answers[top],
                self._questions[top],
                round(top_score, 4),
                _suggest(order[1:]),
            )
        if top_score >= self._suggest_threshold:
            return MatchResult(
                "UNSURE",
                None,
                UNSURE_PREFIX,
                None,
                round(top_score, 4),
                _suggest(order),
            )
        return MatchResult(
            "FALLBACK", None, FALLBACK_ANSWER, None, round(top_score, 4)
        )


def rebuild_index(app, *, reembed: bool = False) -> tuple["ChatbotIndex", int]:
    """Build index moi tu DB roi swap vao app.state.index (atomic assignment).

    Lock chi bao ve build (tranh 2 rebuild dong thoi ghi cache cheo nhau);
    request doc index khong bao gio bi chan.
    """
    state = app.state
    with state.index_lock:
        with state.session_factory() as session:
            new_index, reembedded = ChatbotIndex.build(
                session,
                state.embedder,
                state.settings.answer_threshold,
                state.settings.suggest_threshold,
                reembed=reembed,
            )
        state.index = new_index
    return new_index, reembedded
