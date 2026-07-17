import logging
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.responses import RedirectResponse
from sqlalchemy import Engine

from app.config import Settings, get_settings
from app.db import make_engine, make_session_factory
from app.embedder import Embedder, SentenceTransformerEmbedder
from app.matcher import ChatbotIndex
from app.routers import admin, chatbot, qa


def create_app(
    settings: Settings | None = None,
    embedder: Embedder | None = None,
    engine: Engine | None = None,
) -> FastAPI:
    settings = settings or get_settings()
    embedder = embedder or SentenceTransformerEmbedder(settings.embedding_model)
    engine = engine or make_engine(settings.database_url)

    logging.basicConfig(level=settings.log_level.upper())

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.settings = settings
        app.state.engine = engine
        app.state.session_factory = make_session_factory(engine)
        app.state.embedder = embedder
        app.state.index_lock = threading.Lock()
        # Warm model + build index ngay luc khoi dong (5-15s voi model that)
        with app.state.session_factory() as session:
            app.state.index, _ = ChatbotIndex.build(
                session,
                embedder,
                settings.answer_threshold,
                settings.suggest_threshold,
            )
        # Luon warm model luc khoi dong: khi embedding da cache het trong DB,
        # build index khong encode gi -> khong warm thi request dau tien
        # phai cho load model ~10s
        embedder.encode(["khởi động"])
        yield
        engine.dispose()

    app = FastAPI(
        title="Chatbot Service",
        description=(
            "API chatbot hoi-dap chinh sach tro cap xa hoi (hybrid semantic "
            "search: sentence embedding + TF-IDF, chay hoan toan local). "
            "Test truc tiep tai /docs."
        ),
        version="1.3",
        lifespan=lifespan,
    )
    app.include_router(chatbot.router)
    app.include_router(qa.router)
    app.include_router(admin.router)

    @app.get("/", include_in_schema=False)
    def root():
        return RedirectResponse("/docs")

    return app


app = create_app()
