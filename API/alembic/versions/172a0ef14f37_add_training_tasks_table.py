"""add_training_tasks_table

Revision ID: 172a0ef14f37
Revises: add_model_management
Create Date: 2025-11-01 13:02:41.691091

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


# revision identifiers, used by Alembic.
revision: str = '172a0ef14f37'
down_revision: Union[str, None] = 'add_model_management'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    from sqlalchemy import text
    connection = op.get_bind()

    result = connection.execute(text(
        "SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trainingtaskstatus')"
    ))
    enum_exists = result.scalar()

    if not enum_exists:
        op.execute("CREATE TYPE trainingtaskstatus AS ENUM ('pending', 'running', 'completed', 'failed')")

    op.create_table(
        'training_tasks',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column('base_model_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('new_version', sa.String(length=50), nullable=False),
        sa.Column('initiated_by', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('status', postgresql.ENUM('pending', 'running', 'completed', 'failed', name='trainingtaskstatus', create_type=False), nullable=False),
        sa.Column('progress_percentage', sa.Float(), nullable=True, server_default='0.0'),
        sa.Column('current_epoch', sa.Integer(), nullable=True),
        sa.Column('total_epochs', sa.Integer(), nullable=True),
        sa.Column('current_stage', sa.String(length=100), nullable=True),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('result_model_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('training_params', postgresql.JSONB(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.Column('started_at', sa.DateTime(), nullable=True),
        sa.Column('completed_at', sa.DateTime(), nullable=True),
        sa.PrimaryKeyConstraint('id')
    )

    op.create_index(op.f('ix_training_tasks_id'), 'training_tasks', ['id'], unique=False)
    op.create_index(op.f('ix_training_tasks_status'), 'training_tasks', ['status'], unique=False)
    op.create_index(op.f('ix_training_tasks_created_at'), 'training_tasks', ['created_at'], unique=False)

    op.create_foreign_key('fk_training_tasks_base_model', 'training_tasks', 'model_versions', ['base_model_id'], ['id'], ondelete='SET NULL')
    op.create_foreign_key('fk_training_tasks_result_model', 'training_tasks', 'model_versions', ['result_model_id'], ['id'], ondelete='SET NULL')
    op.create_foreign_key('fk_training_tasks_initiated_by', 'training_tasks', 'users', ['initiated_by'], ['id'], ondelete='CASCADE')


def downgrade() -> None:
    op.drop_constraint('fk_training_tasks_initiated_by', 'training_tasks', type_='foreignkey')
    op.drop_constraint('fk_training_tasks_result_model', 'training_tasks', type_='foreignkey')
    op.drop_constraint('fk_training_tasks_base_model', 'training_tasks', type_='foreignkey')

    op.drop_index(op.f('ix_training_tasks_created_at'), table_name='training_tasks')
    op.drop_index(op.f('ix_training_tasks_status'), table_name='training_tasks')
    op.drop_index(op.f('ix_training_tasks_id'), table_name='training_tasks')

    op.drop_table('training_tasks')

    op.execute("DROP TYPE IF EXISTS trainingtaskstatus")
