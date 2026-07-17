# Multi-stage: builder cai deps + tai model; runtime khong can mang ra ngoai.
FROM python:3.11-slim AS builder

WORKDIR /build
RUN python -m venv /venv
ENV PATH="/venv/bin:$PATH"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Bake model vao image — HF_HOME phai giong het stage runtime
ENV HF_HOME=/opt/hf
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')"

FROM python:3.11-slim

WORKDIR /app
COPY --from=builder /venv /venv
COPY --from=builder /opt/hf /opt/hf
ENV PATH="/venv/bin:$PATH" \
    HF_HOME=/opt/hf \
    HF_HUB_OFFLINE=1 \
    TRANSFORMERS_OFFLINE=1

COPY app/ app/
COPY alembic/ alembic/
COPY alembic.ini seed.py seed_data.json ./

EXPOSE 8001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
