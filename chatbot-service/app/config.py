from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Bắt buộc, không default — thiếu là fail ngay lúc khởi động
    database_url: str
    chatbot_admin_api_key: str

    embedding_model: str = "paraphrase-multilingual-MiniLM-L12-v2"
    answer_threshold: float = 0.55
    suggest_threshold: float = 0.35
    log_level: str = "INFO"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    return Settings()
