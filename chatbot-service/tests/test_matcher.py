import numpy as np
from sqlalchemy import select

from app.matcher import (
    ChatbotIndex,
    build_restore_map,
    normalize,
    restore_diacritics,
)
from app.models import QA
from tests.conftest import SEED_QA, FakeEmbedder, insert_seed


class TestNormalize:
    def test_normalize_strips_diacritics(self):
        assert normalize("Người cao tuổi") == "nguoi cao tuoi"
        assert (
            normalize("Trợ cấp Người cao tuổi, đúng không?")
            == "tro cap nguoi cao tuoi dung khong"
        )

    def test_normalize_handles_d_stroke(self):
        assert normalize("Nghị định 20/2021/NĐ-CP") == "nghi dinh 20 2021 nd cp"

    def test_normalize_collapses_whitespace_and_symbols(self):
        assert normalize("  hồ   sơ!!!  (giấy tờ)  ") == "ho so giay to"


class TestRestoreDiacritics:
    def _rows(self):
        class Row:
            def __init__(self, question, keywords, answer):
                self.question = question
                self.keywords = keywords
                self.answer = answer

        return [
            Row(
                "Người cao tuổi được trợ cấp không?",
                "người già, tiền",
                "Có, tối đa 6 tháng theo quy định.",
            )
        ]

    def test_restores_unaccented_words_from_corpus(self):
        mapping = build_restore_map(self._rows())
        assert restore_diacritics("nguoi cao tuoi", mapping) == "người cao tuổi"

    def test_keeps_accented_words_untouched(self):
        mapping = build_restore_map(self._rows())
        assert (
            restore_diacritics("người cao tuổi", mapping) == "người cao tuổi"
        )

    def test_question_vocab_beats_answer_vocab(self):
        # "toi" co trong answer ("tối đa") nhung khong co trong question/keywords
        # -> van lay tu answer; con tu co trong question/keywords thi uu tien
        mapping = build_restore_map(self._rows())
        assert mapping["nguoi"] == "người"
        assert mapping["toi"] == "tối"  # chi answer co -> dung answer

    def test_unknown_words_pass_through(self):
        mapping = build_restore_map(self._rows())
        assert restore_diacritics("bitcoin xyz", mapping) == "bitcoin xyz"


def build_index(session, embedder, answer=0.55, suggest=0.35, reembed=False):
    return ChatbotIndex.build(
        session, embedder, answer, suggest, reembed=reembed
    )


class TestEmbeddingCache:
    def test_embedding_cache_roundtrip(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        embedder = FakeEmbedder()

        _, reembedded = build_index(db_session, embedder)
        assert reembedded == len(SEED_QA)
        rows = db_session.scalars(select(QA)).all()
        for row in rows:
            assert row.embedding is not None
            assert len(row.embedding) == embedder.dim * 4  # float32
            assert row.embedding_model == "fake-model"

        # Build lai: doc cache, khong encode cau nao trong corpus
        embedder.encode_calls.clear()
        _, reembedded = build_index(db_session, embedder)
        assert reembedded == 0
        assert embedder.encode_calls == []

        # Vector doc tu cache phai giong het vector encode
        cached = np.frombuffer(rows[0].embedding, dtype=np.float32)
        fresh = embedder.encode([rows[0].question])[0]
        assert np.allclose(cached, fresh)

    def test_cache_invalidated_when_model_changes(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        build_index(db_session, FakeEmbedder())

        other = FakeEmbedder()
        other.model_name = "other-model"
        _, reembedded = build_index(db_session, other)
        assert reembedded == len(SEED_QA)
        assert all(
            r.embedding_model == "other-model"
            for r in db_session.scalars(select(QA)).all()
        )

    def test_reembed_forces_reencode(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        embedder = FakeEmbedder()
        build_index(db_session, embedder)
        _, reembedded = build_index(db_session, embedder, reembed=True)
        assert reembedded == len(SEED_QA)

    def test_inactive_rows_excluded(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        row = db_session.scalars(select(QA)).first()
        row.active = False
        db_session.commit()
        index, _ = build_index(db_session, FakeEmbedder())
        assert index.qa_count == len(SEED_QA) - 1


class TestEmptyIndex:
    def test_empty_db_builds_and_falls_back(self, db_session):
        index, reembedded = build_index(db_session, FakeEmbedder())
        assert index.qa_count == 0
        assert reembedded == 0
        result = index.query("người cao tuổi được trợ cấp không")
        assert result.result_type == "FALLBACK"
        assert result.suggestions == []


class TestScoring:
    def test_exact_question_scores_near_one(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        index, _ = build_index(db_session, FakeEmbedder())
        result = index.query(SEED_QA[0]["question"])
        assert result.result_type == "MATCHED"
        assert result.score > 0.9
        assert result.matched_question == SEED_QA[0]["question"]
        assert result.answer == SEED_QA[0]["answer"]

    def test_suggestions_exclude_matched_and_capped_at_three(
        self, sqlite_engine, db_session
    ):
        insert_seed(sqlite_engine)
        index, _ = build_index(db_session, FakeEmbedder())
        result = index.query(SEED_QA[0]["question"])
        assert len(result.suggestions) <= 3
        assert all(
            s.question != result.matched_question for s in result.suggestions
        )

    def test_keyword_bonus_applied(self, sqlite_engine, db_session):
        insert_seed(sqlite_engine)
        embedder = FakeEmbedder()
        index, _ = build_index(db_session, embedder)
        # Cau chua keyword "mai tang" + "ho ngheo" cua ban ghi mai tang
        with_kw = index.query("mai táng hộ nghèo")
        assert with_kw.score > index.query("xyz abc").score
