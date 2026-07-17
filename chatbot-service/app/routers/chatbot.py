import logging

from fastapi import APIRouter, BackgroundTasks, Depends, Request

from app.deps import get_index
from app.matcher import ChatbotIndex, MatchResult
from app.models import AskLog
from app.schemas import AskRequest, AskResponse, SuggestionOut

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/chatbot", tags=["chatbot"])


def log_ask(session_factory, question: str, result: MatchResult) -> None:
    # Chay sau khi da tra response; loi log khong duoc lam fail request
    try:
        with session_factory() as session:
            session.add(
                AskLog(
                    question=question,
                    matched_qa_id=result.qa_id,
                    score=result.score,
                    result_type=result.result_type,
                )
            )
            session.commit()
    except Exception:
        logger.warning("Khong ghi duoc ask_log", exc_info=True)


@router.post("/ask", response_model=AskResponse)
def ask(
    body: AskRequest,
    background: BackgroundTasks,
    request: Request,
    index: ChatbotIndex = Depends(get_index),
) -> AskResponse:
    result = index.query(body.question)
    background.add_task(
        log_ask, request.app.state.session_factory, body.question, result
    )
    return AskResponse(
        result_type=result.result_type,
        answer=result.answer,
        matched_question=result.matched_question,
        score=result.score,
        suggestions=[
            SuggestionOut(id=s.id, question=s.question, score=round(s.score, 4))
            for s in result.suggestions
        ],
    )
