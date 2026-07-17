"""Test voi model sentence-transformers THAT — cham, can tai model ~500MB.

Chay rieng: pytest -m slow

Dung nguyen bo seed_data.json (38 cau) lam corpus de giong moi truong that:
restore map (khoi phuc dau) can tu vung du rong moi hoat dong dung.
"""
import json
from pathlib import Path

import pytest
from sqlalchemy.orm import Session

from app.embedder import SentenceTransformerEmbedder
from app.matcher import ChatbotIndex
from app.models import QA

pytestmark = pytest.mark.slow

SEED_DATA = json.loads(
    (Path(__file__).resolve().parents[1] / "seed_data.json").read_text(
        encoding="utf-8"
    )
)

EXTRA_QA = [
    {
        "question": "Người 75 tuổi có được hưởng trợ cấp xã hội hàng tháng không?",
        "answer": "Chưa. Trợ cấp hàng tháng cho người cao tuổi áp dụng từ đủ 80 tuổi trở lên (Nghị định 01/2024/NĐ-CP).",
        "keywords": "75 tuổi, người cao tuổi",
        "category": "NGUOI_CAO_TUOI",
    },
]

ELDERLY_QUESTIONS = {
    d["question"] for d in SEED_DATA + EXTRA_QA if d["category"] == "NGUOI_CAO_TUOI"
}


@pytest.fixture(scope="module")
def real_embedder():
    return SentenceTransformerEmbedder("paraphrase-multilingual-MiniLM-L12-v2")


@pytest.fixture
def real_index(sqlite_engine, real_embedder):
    with Session(sqlite_engine) as session:
        for row in SEED_DATA + EXTRA_QA:
            session.add(QA(**row))
        session.commit()
        index, _ = ChatbotIndex.build(session, real_embedder, 0.55, 0.35)
    return index


def _top_question(result):
    if result.matched_question:
        return result.matched_question
    return result.suggestions[0].question if result.suggestions else None


def test_ask_matches_paraphrase(real_index):
    # Dien dat khac han tu ngu — nhanh embedding phai cuu duoc,
    # top phai la cau ve nguoi cao tuoi
    result = real_index.query("bà tôi già rồi có được nhà nước cho tiền không")
    assert result.result_type in ("MATCHED", "UNSURE")
    assert _top_question(result) in ELDERLY_QUESTIONS


def test_ask_matches_paraphrase_unaccented(real_index):
    # Khong dau: nho khoi phuc dau tu corpus ma khong roi xuong FALLBACK
    result = real_index.query("ba toi gia roi nha nuoc co cho tien khong")
    assert result.result_type in ("MATCHED", "UNSURE")


def test_ask_unaccented_slang_matches(real_index):
    result = real_index.query("nguoi gia 80 tuoi co dc tro cap ko")
    assert result.result_type == "MATCHED"
    assert _top_question(result) in ELDERLY_QUESTIONS


def test_ask_distinguishes_numbers(real_index):
    # "75 tuoi" khong duoc match nham sang cau "80 tuoi"
    result = real_index.query("trợ cấp cho người 75 tuổi")
    assert _top_question(result) == EXTRA_QA[0]["question"]


def test_irrelevant_falls_back(real_index):
    for q in ("hôm nay ăn gì", "hom nay an gi", "gia vang hom nay bao nhieu"):
        result = real_index.query(q)
        assert result.result_type == "FALLBACK", (q, result.score)
