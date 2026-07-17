from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool


def make_engine(url: str) -> Engine:
    if url.startswith("sqlite"):
        # Test dùng SQLite in-memory: 1 connection chia sẻ cho mọi thread
        return create_engine(
            url,
            poolclass=StaticPool,
            connect_args={"check_same_thread": False},
        )
    return create_engine(url, pool_pre_ping=True, pool_recycle=3600)


def make_session_factory(engine: Engine) -> sessionmaker[Session]:
    return sessionmaker(bind=engine, expire_on_commit=False)
