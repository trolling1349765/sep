from typing import Protocol

import numpy as np


class Embedder(Protocol):
    model_name: str

    def encode(self, texts: list[str]) -> np.ndarray:
        """Tra ve ma tran (n, dim) float32, moi hang da L2-normalize."""
        ...


class SentenceTransformerEmbedder:
    def __init__(self, model_name: str):
        self.model_name = model_name
        self._model = None

    def _load(self):
        if self._model is None:
            from sentence_transformers import SentenceTransformer

            self._model = SentenceTransformer(self.model_name)
        return self._model

    def encode(self, texts: list[str]) -> np.ndarray:
        model = self._load()
        vectors = model.encode(
            texts, normalize_embeddings=True, convert_to_numpy=True
        )
        return np.asarray(vectors, dtype=np.float32)
