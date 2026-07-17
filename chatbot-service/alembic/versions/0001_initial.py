"""Tao bang qa va ask_log

Revision ID: 0001
Revises:
Create Date: 2026-07-18

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import mysql

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "qa",
        sa.Column("id", sa.Integer(), autoincrement=True, primary_key=True),
        sa.Column("question", sa.Text(), nullable=False),
        sa.Column("answer", sa.Text(), nullable=False),
        sa.Column("keywords", sa.Text(), nullable=True),
        sa.Column("category", sa.String(length=64), nullable=True),
        sa.Column(
            "active", sa.Boolean(), nullable=False, server_default=sa.text("1")
        ),
        sa.Column(
            "embedding",
            sa.LargeBinary().with_variant(mysql.LONGBLOB(), "mysql"),
            nullable=True,
        ),
        sa.Column("embedding_model", sa.String(length=128), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
        mysql_charset="utf8mb4",
        mysql_collate="utf8mb4_unicode_ci",
    )
    op.create_index("ix_qa_active", "qa", ["active"])

    op.create_table(
        "ask_log",
        sa.Column("id", sa.Integer(), autoincrement=True, primary_key=True),
        sa.Column("question", sa.Text(), nullable=False),
        sa.Column("matched_qa_id", sa.Integer(), nullable=True),
        sa.Column("score", sa.Float(), nullable=True),
        sa.Column("result_type", sa.String(length=16), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
        mysql_charset="utf8mb4",
        mysql_collate="utf8mb4_unicode_ci",
    )
    op.create_index("ix_ask_log_created_at", "ask_log", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_ask_log_created_at", table_name="ask_log")
    op.drop_table("ask_log")
    op.drop_index("ix_qa_active", table_name="qa")
    op.drop_table("qa")
