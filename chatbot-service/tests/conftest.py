import hashlib
import os

import numpy as np
import pytest

# Phai set truoc khi import app.main (module-level `app = create_app()` doc env)
os.environ.setdefault("DATABASE_URL", "sqlite://")
os.environ.setdefault("CHATBOT_ADMIN_API_KEY", "test-key")

from fastapi.testclient import TestClient  # noqa: E402
from sqlalchemy.orm import Session  # noqa: E402

from app.config import Settings  # noqa: E402
from app.db import make_engine  # noqa: E402
from app.main import create_app  # noqa: E402
from app.matcher import normalize  # noqa: E402
from app.models import QA, Base  # noqa: E402

API_KEY = "test-key"


class FakeEmbedder:
    """Embedder gia, deterministic: vector = RNG seed tu md5(normalize(text)).

    - Hai cau normalize giong nhau (co dau / khong dau) -> cung vector, cosine 1.0.
    - `aliases` (normalized -> normalized) gia lap "ngu nghia gan nhau":
      typo/paraphrase map ve cau goc de nhanh embedding cho diem cao.
    - Hai cau khac nhau -> cosine ~ 0 (nhieu ±0.1 o dim 64).
    """

    def __init__(self, aliases: dict[str, str] | None = None, dim: int = 64):
        self.model_name = "fake-model"
        self.dim = dim
        self.aliases = aliases or {}
        self.encode_calls: list[list[str]] = []

    def add_alias(self, text: str, canonical: str) -> None:
        self.aliases[normalize(text)] = normalize(canonical)

    def encode(self, texts: list[str]) -> np.ndarray:
        self.encode_calls.append(list(texts))
        out = []
        for t in texts:
            key = normalize(t)
            key = self.aliases.get(key, key)
            seed = int(hashlib.md5(key.encode("utf-8")).hexdigest()[:8], 16)
            rng = np.random.default_rng(seed)
            v = rng.standard_normal(self.dim).astype(np.float32)
            v /= np.linalg.norm(v)
            out.append(v)
        return np.vstack(out)


SEED_QA = [
    {
        "question": "Người cao tuổi bao nhiêu tuổi thì được hưởng trợ cấp xã hội hàng tháng?",
        "answer": "Theo Nghị định 01/2024/NĐ-CP, người từ đủ 80 tuổi trở lên không có lương hưu, thuộc hộ nghèo hoặc cận nghèo được hưởng trợ cấp hàng tháng bằng 1.500.000đ x 1,2.",
        "keywords": "người cao tuổi, 80 tuổi, trợ cấp, lương hưu",
        "category": "NGUOI_CAO_TUOI",
    },
    {
        "question": "Hồ sơ xin trợ cấp xã hội gồm những giấy tờ gì?",
        "answer": "Hồ sơ gồm: đơn đề nghị, bản sao CCCD, giấy xác nhận của UBND cấp xã và giấy tờ chứng minh thuộc diện hưởng.",
        "keywords": "hồ sơ, giấy tờ, đơn đề nghị",
        "category": "THU_TUC",
    },
    {
        "question": "Mức hỗ trợ mai táng phí cho người thuộc hộ nghèo là bao nhiêu?",
        "answer": "Hỗ trợ chi phí mai táng bằng 20 lần mức chuẩn trợ giúp xã hội, tức 10.000.000đ theo Thông tư 08/2024/TT-BLĐTBXH.",
        "keywords": "mai táng, hộ nghèo, 10 triệu",
        "category": "MAI_TANG",
    },
    {
        "question": "Trẻ em mồ côi được nhà nước hỗ trợ như thế nào?",
        "answer": "Trẻ dưới 16 tuổi mồ côi cả cha mẹ được trợ cấp bằng 2,5 lần mức chuẩn 500.000đ/tháng theo Nghị định 02/2024/NĐ-CP.",
        "keywords": "trẻ em, mồ côi, hỗ trợ",
        "category": "TRE_MO_COI",
    },
]


def make_settings(**overrides) -> Settings:
    values = dict(
        database_url="sqlite://",
        chatbot_admin_api_key=API_KEY,
        embedding_model="fake-model",
    )
    values.update(overrides)
    return Settings(**values)


@pytest.fixture
def fake_embedder() -> FakeEmbedder:
    return FakeEmbedder()


@pytest.fixture
def sqlite_engine():
    engine = make_engine("sqlite://")
    Base.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def db_session(sqlite_engine):
    with Session(sqlite_engine) as session:
        yield session


def insert_seed(engine, rows=SEED_QA) -> None:
    with Session(engine) as session:
        for row in rows:
            session.add(QA(**row))
        session.commit()


@pytest.fixture
def make_client(sqlite_engine, fake_embedder):
    """Factory: tao TestClient voi seed data + threshold tuy chinh."""
    clients = []

    def _make(seed=True, **settings_overrides) -> TestClient:
        if seed:
            insert_seed(sqlite_engine)
        app = create_app(
            settings=make_settings(**settings_overrides),
            embedder=fake_embedder,
            engine=sqlite_engine,
        )
        client = TestClient(app)
        client.__enter__()  # chay lifespan -> build index
        clients.append(client)
        return client

    yield _make
    for c in clients:
        c.__exit__(None, None, None)


@pytest.fixture
def client(make_client) -> TestClient:
    return make_client()
