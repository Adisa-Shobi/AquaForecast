"""Initial schema

Revision ID: 001_initial_schema
Revises:
Create Date: 2025-01-28 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql
import geoalchemy2

# revision identifiers, used by Alembic.
revision: str = '001_initial_schema'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create users table
    if not op.get_bind().dialect.has_table(op.get_bind(), "users"):
        op.create_table(
            'users',
            sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
            sa.Column('firebase_uid', sa.String(128), nullable=False, unique=True),
            sa.Column('email', sa.String(255), nullable=False, unique=True),
            sa.Column('created_at', sa.DateTime(), nullable=False),
            sa.Column('last_sync_at', sa.DateTime(), nullable=True),
            sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        )
        op.create_index('idx_users_firebase_uid', 'users', ['firebase_uid'])
        op.create_index('idx_users_email', 'users', ['email'])

    if not op.get_bind().dialect.has_table(op.get_bind(), "farm_data"):
        # Create farm_data table
        op.create_table(
            'farm_data',
            sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
            sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
            # Water quality parameters
            sa.Column('temperature', sa.Numeric(5, 2), nullable=False),
            sa.Column('ph', sa.Numeric(4, 2), nullable=False),
            sa.Column('dissolved_oxygen', sa.Numeric(5, 2), nullable=False),
            sa.Column('ammonia', sa.Numeric(5, 2), nullable=False),
            sa.Column('nitrate', sa.Numeric(5, 2), nullable=False),
            sa.Column('turbidity', sa.Numeric(6, 2), nullable=False),
            # Fish measurements
            sa.Column('fish_weight', sa.Numeric(8, 3), nullable=True, comment='Fish weight in kg'),
            sa.Column('fish_length', sa.Numeric(6, 2), nullable=True, comment='Fish length in cm'),
            sa.Column('verified', sa.Boolean(), nullable=False, server_default='false', comment='User confirmed measurements'),
            sa.Column('start_date', sa.Date(), nullable=True, comment='Pond cycle start date'),
            # Location
            sa.Column('location', geoalchemy2.Geography(geometry_type='POINT', srid=4326), nullable=False),
            sa.Column('country_code', sa.String(2), nullable=True),
            # Metadata
            sa.Column('recorded_at', sa.DateTime(), nullable=False),
            sa.Column('synced_at', sa.DateTime(), nullable=False),
            sa.Column('device_id', sa.String(255), nullable=True),
            sa.Column('created_at', sa.DateTime(), nullable=False),
            sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        )
        op.create_index('idx_farm_data_user_id', 'farm_data', ['user_id'])
        op.create_index('idx_farm_data_recorded_at', 'farm_data', ['recorded_at'])
        op.create_index('idx_farm_data_start_date', 'farm_data', ['start_date'])
        op.create_index('idx_farm_data_country_code', 'farm_data', ['country_code'])
        op.create_index('idx_farm_data_location', 'farm_data', ['location'], postgresql_using='gist')

    # Create model_versions table
    if not op.get_bind().dialect.has_table(op.get_bind(), "model_versions"):
        op.create_table(
            'model_versions',
            sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
            sa.Column('version', sa.String(50), nullable=False, unique=True),
            sa.Column('model_url', sa.Text(), nullable=False),
            sa.Column('model_size_bytes', sa.BigInteger(), nullable=True),
            sa.Column('preprocessing_config', postgresql.JSONB(), nullable=True),
            sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
            sa.Column('min_app_version', sa.String(20), nullable=True),
            sa.Column('created_at', sa.DateTime(), nullable=False),
        )
        op.create_index('idx_model_versions_version', 'model_versions', ['version'])


def downgrade() -> None:
    op.drop_table('model_versions')
    op.drop_table('farm_data')
    op.drop_table('users')
