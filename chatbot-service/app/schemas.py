from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, field_validator


class AskRequest(BaseModel):
    question: str

    @field_validator("question")
    @classmethod
    def trim_and_check_length(cls, v: str) -> str:
        v = v.strip()
        if not 1 <= len(v) <= 500:
            raise ValueError("question phai tu 1 den 500 ky tu sau khi trim")
        return v


class SuggestionOut(BaseModel):
    id: int
    question: str
    score: float


class AskResponse(BaseModel):
    result_type: Literal["MATCHED", "UNSURE", "FALLBACK"]
    answer: str
    matched_question: str | None = None
    score: float
    suggestions: list[SuggestionOut] = []


class QACreate(BaseModel):
    question: str
    answer: str
    keywords: str | None = None
    category: str | None = None
    active: bool = True

    @field_validator("question", "answer")
    @classmethod
    def not_blank(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("khong duoc de trong")
        return v


class QAUpdate(BaseModel):
    question: str | None = None
    answer: str | None = None
    keywords: str | None = None
    category: str | None = None
    active: bool | None = None


class QAOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    question: str
    answer: str
    keywords: str | None
    category: str | None
    active: bool
    embedding_model: str | None
    created_at: datetime
    updated_at: datetime


class QAPage(BaseModel):
    items: list[QAOut]
    total: int
    page: int
    size: int


class HealthOut(BaseModel):
    status: str
    qa_count: int
    model: str
    index_built_at: datetime | None


class ReloadOut(BaseModel):
    reloaded: bool
    qa_count: int
    reembedded: int
