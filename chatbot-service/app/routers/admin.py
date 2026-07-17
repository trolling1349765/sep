from fastapi import APIRouter, Depends, Request

from app.deps import require_api_key
from app.matcher import rebuild_index
from app.schemas import HealthOut, ReloadOut

router = APIRouter(tags=["admin"])


@router.post(
    "/api/v1/admin/reload",
    response_model=ReloadOut,
    dependencies=[Depends(require_api_key)],
)
def reload(request: Request, reembed: bool = False) -> ReloadOut:
    index, reembedded = rebuild_index(request.app, reembed=reembed)
    return ReloadOut(reloaded=True, qa_count=index.qa_count, reembedded=reembedded)


@router.get("/health", response_model=HealthOut)
def health(request: Request) -> HealthOut:
    index = request.app.state.index
    return HealthOut(
        status="ok",
        qa_count=index.qa_count,
        model=request.app.state.settings.embedding_model,
        index_built_at=index.built_at,
    )
