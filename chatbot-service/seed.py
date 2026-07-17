"""Seed bo Q&A ban dau tu seed_data.json vao MySQL.

Chay tu thu muc chatbot-service/ (can .env voi DATABASE_URL):
    python seed.py            # them cau chua co (so theo question)
    python seed.py --truncate # xoa sach bang qa roi seed lai

Khong can model embedding — index build luc service khoi dong se encode
va ghi cache vao DB.
"""
import argparse
import json
from pathlib import Path

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.config import get_settings
from app.db import make_engine
from app.models import QA


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--truncate", action="store_true")
    args = parser.parse_args()

    data = json.loads(
        (Path(__file__).parent / "seed_data.json").read_text(encoding="utf-8")
    )
    engine = make_engine(get_settings().database_url)
    with Session(engine) as session:
        if args.truncate:
            session.execute(delete(QA))
            session.commit()
        existing = set(session.scalars(select(QA.question)).all())
        added = 0
        for row in data:
            if row["question"] in existing:
                continue
            session.add(QA(**row))
            added += 1
        session.commit()
        total = len(session.scalars(select(QA.id)).all())
    print(f"Seed xong: them {added} cau, tong {total} cau trong bang qa.")
    print("Luu y: goi POST /api/v1/admin/reload (hoac restart service) de nap index.")


if __name__ == "__main__":
    main()
