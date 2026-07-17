from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import QA, AskLog
from tests.conftest import API_KEY, SEED_QA


def ask(client, question: str):
    resp = client.post("/api/v1/chatbot/ask", json={"question": question})
    assert resp.status_code == 200
    return resp.json()


class TestAsk:
    def test_ask_matches_exact_question(self, client):
        body = ask(client, SEED_QA[0]["question"])
        assert body["result_type"] == "MATCHED"
        assert body["answer"] == SEED_QA[0]["answer"]
        assert body["matched_question"] == SEED_QA[0]["question"]
        assert len(body["suggestions"]) <= 3

    def test_ask_matches_unaccented_input(self, client):
        body = ask(client, "nguoi cao tuoi bao nhieu tuoi thi duoc huong tro cap xa hoi hang thang")
        assert body["result_type"] == "MATCHED"
        assert body["answer"] == SEED_QA[0]["answer"]

    def test_ask_matches_typo(self, fake_embedder, make_client):
        # Char n-gram chiu loi chinh ta; alias gia lap do ben embedding
        typo = "tro cap nguoi gia 80 tuoy co duoc khong"
        fake_embedder.add_alias(typo, SEED_QA[0]["question"])
        client = make_client()
        body = ask(client, typo)
        assert body["result_type"] == "MATCHED"
        assert body["answer"] == SEED_QA[0]["answer"]

    def test_ask_returns_unsure_in_gray_zone(self, fake_embedder, make_client):
        # Nang ANSWER_THRESHOLD de diem roi vao vung xam:
        # embedding trung (alias) nhung tu vung lech -> ~0.55-0.8 < 0.95
        query = "cho hoi ve tro cap danh cho nguoi lon tuoi"
        fake_embedder.add_alias(query, SEED_QA[0]["question"])
        client = make_client(answer_threshold=0.95, suggest_threshold=0.35)
        body = ask(client, query)
        assert body["result_type"] == "UNSURE"
        assert "Có phải bạn muốn hỏi" in body["answer"]
        assert 1 <= len(body["suggestions"]) <= 3
        assert body["suggestions"][0]["question"] == SEED_QA[0]["question"]

    def test_ask_returns_fallback_when_irrelevant(self, client):
        body = ask(client, "hom nay an gi")
        assert body["result_type"] == "FALLBACK"
        assert body["suggestions"] == []
        assert "Xin lỗi" in body["answer"]

    def test_ask_fallback_on_empty_db(self, make_client):
        client = make_client(seed=False)
        body = ask(client, "người cao tuổi được trợ cấp không")
        assert body["result_type"] == "FALLBACK"

    def test_ask_validates_question_length(self, client):
        for bad in ["", "   ", "x" * 501]:
            resp = client.post("/api/v1/chatbot/ask", json={"question": bad})
            assert resp.status_code == 422

    def test_ask_writes_ask_log(self, client, sqlite_engine):
        ask(client, SEED_QA[0]["question"])
        ask(client, "hom nay an gi")
        with Session(sqlite_engine) as session:
            logs = session.scalars(
                select(AskLog).order_by(AskLog.id)
            ).all()
        assert len(logs) == 2
        assert logs[0].result_type == "MATCHED"
        assert logs[0].matched_qa_id is not None
        assert logs[0].score is not None
        assert logs[1].result_type == "FALLBACK"
        assert logs[1].matched_qa_id is None


class TestApiKey:
    def test_crud_requires_api_key(self, client):
        cases = [
            ("GET", "/api/v1/qa", None),
            ("GET", "/api/v1/qa/1", None),
            ("POST", "/api/v1/qa", {"question": "q", "answer": "a"}),
            ("PUT", "/api/v1/qa/1", {"answer": "a"}),
            ("DELETE", "/api/v1/qa/1", None),
            ("POST", "/api/v1/admin/reload", None),
        ]
        for method, url, body in cases:
            resp = client.request(method, url, json=body)
            assert resp.status_code == 401, f"{method} {url} thieu key phai 401"
            resp = client.request(
                method, url, json=body, headers={"X-API-Key": "sai-key"}
            )
            assert resp.status_code == 401, f"{method} {url} sai key phai 401"

    def test_public_endpoints_need_no_key(self, client):
        assert client.get("/health").status_code == 200
        assert (
            client.post(
                "/api/v1/chatbot/ask", json={"question": "xin chào"}
            ).status_code
            == 200
        )


AUTH = {"X-API-Key": API_KEY}


class TestQaCrud:
    def test_list_paginated_without_embedding(self, client):
        resp = client.get("/api/v1/qa?page=1&size=2", headers=AUTH)
        assert resp.status_code == 200
        body = resp.json()
        assert body["total"] == len(SEED_QA)
        assert len(body["items"]) == 2
        assert "embedding" not in body["items"][0]

    def test_list_filters(self, client):
        resp = client.get("/api/v1/qa?category=THU_TUC", headers=AUTH)
        assert resp.json()["total"] == 1
        resp = client.get("/api/v1/qa?active=false", headers=AUTH)
        assert resp.json()["total"] == 0

    def test_get_by_id_and_404(self, client):
        assert client.get("/api/v1/qa/1", headers=AUTH).status_code == 200
        assert client.get("/api/v1/qa/9999", headers=AUTH).status_code == 404

    def test_crud_triggers_index_reload(self, client):
        new_q = "Làm mất quyết định hưởng trợ cấp thì xin cấp lại ở đâu?"
        resp = client.post(
            "/api/v1/qa",
            headers=AUTH,
            json={
                "question": new_q,
                "answer": "Liên hệ UBND cấp xã nơi ra quyết định để được cấp lại.",
                "category": "HO_SO",
            },
        )
        assert resp.status_code == 201
        created = resp.json()
        assert "embedding" not in created

        # Hoi ngay cau vua them -> index da reload, phai MATCHED
        body = ask(client, new_q)
        assert body["result_type"] == "MATCHED"
        assert body["matched_question"] == new_q

        # health phan anh so luong moi
        assert client.get("/health").json()["qa_count"] == len(SEED_QA) + 1

        # Soft delete -> khong con match ve cau do nua
        resp = client.delete(f"/api/v1/qa/{created['id']}", headers=AUTH)
        assert resp.status_code == 204
        assert client.get("/health").json()["qa_count"] == len(SEED_QA)
        body = ask(client, new_q)
        assert body.get("matched_question") != new_q

    def test_update_reencodes_when_question_changes(self, client, sqlite_engine):
        new_question = "Điều kiện hưởng trợ cấp người cao tuổi là gì?"
        resp = client.put(
            "/api/v1/qa/1", headers=AUTH, json={"question": new_question}
        )
        assert resp.status_code == 200
        assert resp.json()["question"] == new_question
        body = ask(client, new_question)
        assert body["result_type"] == "MATCHED"
        assert body["matched_question"] == new_question

    def test_delete_is_soft(self, client, sqlite_engine):
        client.delete("/api/v1/qa/1", headers=AUTH)
        with Session(sqlite_engine) as session:
            row = session.get(QA, 1)
        assert row is not None
        assert row.active is False


class TestAdmin:
    def test_health(self, client):
        body = client.get("/health").json()
        assert body["status"] == "ok"
        assert body["qa_count"] == len(SEED_QA)
        assert body["model"] == "fake-model"
        assert body["index_built_at"] is not None

    def test_reload(self, client):
        resp = client.post("/api/v1/admin/reload", headers=AUTH)
        assert resp.status_code == 200
        body = resp.json()
        assert body["reloaded"] is True
        assert body["qa_count"] == len(SEED_QA)
        assert body["reembedded"] == 0  # cache con nguyen

    def test_reload_reembed(self, client):
        resp = client.post(
            "/api/v1/admin/reload?reembed=true", headers=AUTH
        )
        assert resp.json()["reembedded"] == len(SEED_QA)
