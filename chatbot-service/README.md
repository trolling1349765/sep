# Chatbot Service

API chatbot hỏi–đáp chính sách trợ cấp xã hội (SPEC v1.3). Hybrid semantic search chạy hoàn toàn local: sentence embedding `paraphrase-multilingual-MiniLM-L12-v2` + TF-IDF (word 1-2gram + char 2-4gram) + keyword bonus. Không gọi AI API ngoài. Test trực tiếp qua Swagger UI tại `/docs`.

## Chạy local (Windows PowerShell, không Docker)

Yêu cầu: Python 3.11+, MySQL compose của project đang chạy.

```powershell
# 1. MySQL + tao schema/user rieng (chay MOT lan)
docker compose up -d db
Get-Content .\chatbot-service\scripts\init_db.sql -Raw | docker exec -i mysql-capstone mysql -uroot -p123456

# 2. Venv + deps (torch CPU ~200MB)
cd chatbot-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt

# 3. Cau hinh
Copy-Item .env.example .env   # sua CHATBOT_ADMIN_API_KEY

# 4. Migration + seed
.\.venv\Scripts\alembic.exe upgrade head
.\.venv\Scripts\python.exe seed.py

# 5. Chay (lan dau tai model ~500MB tu HuggingFace, khoi dong 5-15s)
.\.venv\Scripts\uvicorn.exe app.main:app --port 8001
```

Swagger UI: http://localhost:8001/docs — bấm **Authorize** nhập `X-API-Key` để test các endpoint admin. Postman collection: `postman/chatbot-service.postman_collection.json`.

## Chạy bằng Docker

```powershell
# Van can buoc init_db.sql o tren (mot lan). Sau do:
docker compose up -d --build chatbot-service
# Migration + seed chay TRONG container (DATABASE_URL tro ve db:3306):
docker compose exec chatbot-service alembic upgrade head
docker compose exec chatbot-service python seed.py
docker compose restart chatbot-service   # nap index tu DB da seed
```

Model được bake sẵn vào image (`HF_HUB_OFFLINE=1`) — runtime không cần mạng ra ngoài. Image ~2GB, RAM ổn định ~1GB (`mem_limit: 2g`).

## Test

```powershell
.\.venv\Scripts\python.exe -m pytest              # fast: mock embedder, SQLite in-memory, khong can MySQL
.\.venv\Scripts\python.exe -m pytest -m slow      # model that (paraphrase, phan biet con so)
```

## API

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/chatbot/ask` | public | Hỏi chatbot → `MATCHED` / `UNSURE` / `FALLBACK` + suggestions |
| GET | `/api/v1/qa` | X-API-Key | Danh sách Q&A, filter `category`/`active`, phân trang |
| GET | `/api/v1/qa/{id}` | X-API-Key | Chi tiết |
| POST | `/api/v1/qa` | X-API-Key | Tạo mới → encode + reload index |
| PUT | `/api/v1/qa/{id}` | X-API-Key | Cập nhật (đổi question → re-encode) → reload |
| DELETE | `/api/v1/qa/{id}` | X-API-Key | Soft delete → reload |
| POST | `/api/v1/admin/reload` | X-API-Key | Nạp lại index; `?reembed=true` ép encode lại |
| GET | `/health` | public | `{status, qa_count, model, index_built_at}` |

Điểm số: `0.55·cosine(embedding) + 0.25·cosine(word TF-IDF) + 0.20·cosine(char TF-IDF) + min(0.30, 0.10·số keyword trúng)`, ngưỡng `ANSWER_THRESHOLD=0.55` / `SUGGEST_THRESHOLD=0.35` (env, cần tune theo `ask_log`).

**Khôi phục dấu cho query không dấu:** model embedding gần như mù với tiếng Việt không dấu, nên trước khi encode, các từ không dấu trong query được khôi phục dấu bằng từ điển xây từ chính corpus Q&A (ưu tiên tần suất trong question+keywords, fallback sang answer). Từ đã có dấu giữ nguyên; từ ngoài từ điển giữ nguyên. Từ điển rebuild cùng index.

## Vận hành / lưu ý

- Embedding của từng câu hỏi được cache trong cột `qa.embedding` (float32 BLOB) kèm `embedding_model`; đổi model qua env `EMBEDDING_MODEL` sẽ tự re-encode khi build index.
- Index nằm trong bộ nhớ tiến trình → chạy **1 worker uvicorn** (mặc định). Nhiều worker sẽ lệch index sau CRUD.
- Mọi câu hỏi được ghi vào `ask_log` (result_type, score) — dùng để tune ngưỡng và phát hiện câu hỏi chưa có trong bộ Q&A.
- `DATABASE_URL` phải có `?charset=utf8mb4` kẻo lỗi font tiếng Việt.

## Tích hợp về sau (ngoài phạm vi)

Spring Boot thêm `ChatbotController` proxy `POST /chatbot/ask` → `http://chatbot-service:8001/api/v1/chatbot/ask` qua `RestClient`, bọc `APIResponse<T>`, property `chatbot.service.url` khai báo tường minh, timeout 3s (chết → trả FALLBACK), bọc `RateLimiterService`. Frontend render theo `result_type`, hiện `suggestions` thành nút bấm. API contract ổn định — tích hợp chỉ bọc bên ngoài.
