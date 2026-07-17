import secrets
from typing import Iterator

from fastapi import HTTPException, Request, Security
from fastapi.security import APIKeyHeader
from sqlalchemy.orm import Session

from app.matcher import ChatbotIndex

api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


def get_db(request: Request) -> Iterator[Session]:
    session: Session = request.app.state.session_factory()
    try:
        yield session
    finally:
        session.close()


def get_index(request: Request) -> ChatbotIndex:
    return request.app.state.index


def require_api_key(
    request: Request, api_key: str | None = Security(api_key_header)
) -> None:
    expected: str = request.app.state.settings.chatbot_admin_api_key
    if api_key is None or not secrets.compare_digest(api_key, expected):
        raise HTTPException(status_code=401, detail="Invalid or missing X-API-Key")
