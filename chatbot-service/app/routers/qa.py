import numpy as np
from fastapi import APIRouter, Depends, HTTPException, Query, Request
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.deps import get_db, require_api_key
from app.matcher import rebuild_index
from app.models import QA
from app.schemas import QACreate, QAOut, QAPage, QAUpdate

router = APIRouter(
    prefix="/api/v1/qa", tags=["qa"], dependencies=[Depends(require_api_key)]
)


def _encode_into(request: Request, row: QA) -> None:
    embedder = request.app.state.embedder
    vec = np.asarray(embedder.encode([row.question])[0], dtype=np.float32)
    row.embedding = vec.tobytes()
    row.embedding_model = embedder.model_name


def _get_or_404(db: Session, qa_id: int) -> QA:
    row = db.get(QA, qa_id)
    if row is None:
        raise HTTPException(status_code=404, detail=f"QA {qa_id} khong ton tai")
    return row


@router.get("", response_model=QAPage)
def list_qa(
    category: str | None = None,
    active: bool | None = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> QAPage:
    stmt = select(QA)
    if category is not None:
        stmt = stmt.where(QA.category == category)
    if active is not None:
        stmt = stmt.where(QA.active.is_(active))
    total = db.scalar(select(func.count()).select_from(stmt.subquery())) or 0
    rows = db.scalars(
        stmt.order_by(QA.id).offset((page - 1) * size).limit(size)
    ).all()
    return QAPage(
        items=[QAOut.model_validate(r) for r in rows],
        total=total,
        page=page,
        size=size,
    )


@router.get("/{qa_id}", response_model=QAOut)
def get_qa(qa_id: int, db: Session = Depends(get_db)) -> QAOut:
    return QAOut.model_validate(_get_or_404(db, qa_id))


@router.post("", response_model=QAOut, status_code=201)
def create_qa(
    body: QACreate, request: Request, db: Session = Depends(get_db)
) -> QAOut:
    row = QA(**body.model_dump())
    _encode_into(request, row)
    db.add(row)
    db.commit()
    db.refresh(row)
    rebuild_index(request.app)
    return QAOut.model_validate(row)


@router.put("/{qa_id}", response_model=QAOut)
def update_qa(
    qa_id: int, body: QAUpdate, request: Request, db: Session = Depends(get_db)
) -> QAOut:
    row = _get_or_404(db, qa_id)
    changes = body.model_dump(exclude_unset=True)
    question_changed = (
        "question" in changes and changes["question"] != row.question
    )
    for field_name, value in changes.items():
        setattr(row, field_name, value)
    if question_changed:
        _encode_into(request, row)
    db.commit()
    db.refresh(row)
    rebuild_index(request.app)
    return QAOut.model_validate(row)


@router.delete("/{qa_id}", status_code=204)
def delete_qa(
    qa_id: int, request: Request, db: Session = Depends(get_db)
) -> None:
    row = _get_or_404(db, qa_id)
    row.active = False
    db.commit()
    rebuild_index(request.app)
