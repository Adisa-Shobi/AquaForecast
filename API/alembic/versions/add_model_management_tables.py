"""Add model management tables

Revision ID: add_model_management
Revises: 001_initial_schema
Create Date: 2025-01-30

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = 'add_model_management'
down_revision = '001_initial_schema'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Drop modelstatus enum if it exists (to recreate it fresh)
    op.execute("DROP TYPE IF EXISTS modelstatus CASCADE")

    # Create enum type for model status
    op.execute("CREATE TYPE modelstatus AS ENUM ('training', 'completed', 'failed', 'deployed', 'archived')")

    # Rename old columns first
    op.alter_column('model_versions', 'model_url', new_column_name='tflite_model_url')
    op.alter_column('model_versions', 'model_size_bytes', new_column_name='tflite_size_bytes')

    # Add new columns
    op.add_column('model_versions', sa.Column('keras_model_url', sa.Text(), nullable=True, comment='Cloudinary URL for Keras model (.keras)'))
    op.add_column('model_versions', sa.Column('keras_size_bytes', sa.BigInteger(), nullable=True, comment='Keras model size'))
    op.add_column('model_versions', sa.Column('tflite_cloudinary_id', sa.String(length=255), nullable=True, comment='Cloudinary public ID for TFLite model'))
    op.add_column('model_versions', sa.Column('keras_cloudinary_id', sa.String(length=255), nullable=True, comment='Cloudinary public ID for Keras model'))
    op.add_column('model_versions', sa.Column('base_model_id', postgresql.UUID(as_uuid=True), nullable=True, comment='Parent model used for retraining'))
    op.add_column('model_versions', sa.Column('model_config', postgresql.JSONB(), nullable=True, comment='Model architecture and training config'))
    op.add_column('model_versions', sa.Column('training_data_count', sa.Integer(), nullable=True, comment='Number of samples used for training'))
    op.add_column('model_versions', sa.Column('training_duration_seconds', sa.Integer(), nullable=True))
    op.add_column('model_versions', sa.Column('trained_by', postgresql.UUID(as_uuid=True), nullable=True))
    op.add_column('model_versions', sa.Column('metrics', postgresql.JSONB(), nullable=True, comment='R2, RMSE, MAE for weight and length predictions'))

    # Add status column with proper enum type
    op.execute("ALTER TABLE model_versions ADD COLUMN status modelstatus NOT NULL DEFAULT 'training'::modelstatus")

    op.add_column('model_versions', sa.Column('is_deployed', sa.Boolean(), nullable=False, server_default='false', comment='Currently deployed model for production'))
    op.add_column('model_versions', sa.Column('deployed_at', sa.DateTime(), nullable=True))
    op.add_column('model_versions', sa.Column('notes', sa.Text(), nullable=True, comment='Release notes or training notes'))

    # Update comment on preprocessing_config
    op.alter_column('model_versions', 'preprocessing_config',
                    comment='Scaler config, feature names, biological limits')

    # Create indexes
    op.create_index(op.f('ix_model_versions_status'), 'model_versions', ['status'], unique=False)
    op.create_index(op.f('ix_model_versions_is_deployed'), 'model_versions', ['is_deployed'], unique=False)

    # Create foreign key for base_model_id
    op.create_foreign_key('fk_model_versions_base_model', 'model_versions', 'model_versions',
                         ['base_model_id'], ['id'], ondelete='SET NULL')

    # Create foreign key for trained_by
    op.create_foreign_key('fk_model_versions_trained_by', 'model_versions', 'users',
                         ['trained_by'], ['id'], ondelete='SET NULL')

    # Create training_sessions table
    op.create_table('training_sessions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column('model_version_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('farm_data_ids', postgresql.ARRAY(postgresql.UUID(as_uuid=True)), nullable=False, comment='IDs of farm data used for training'),
        sa.Column('training_samples', sa.Integer(), nullable=False),
        sa.Column('validation_samples', sa.Integer(), nullable=False),
        sa.Column('test_samples', sa.Integer(), nullable=False),
        sa.Column('epochs', sa.Integer(), nullable=True),
        sa.Column('batch_size', sa.Integer(), nullable=True),
        sa.Column('learning_rate', sa.Float(), nullable=True),
        sa.Column('final_metrics', postgresql.JSONB(), nullable=True),
        sa.Column('training_history', postgresql.JSONB(), nullable=True, comment='Loss and metrics per epoch'),
        sa.Column('started_at', sa.DateTime(), nullable=False),
        sa.Column('completed_at', sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(['model_version_id'], ['model_versions.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_training_sessions_id'), 'training_sessions', ['id'], unique=False)
    op.create_index(op.f('ix_training_sessions_model_version_id'), 'training_sessions', ['model_version_id'], unique=False)


def downgrade() -> None:
    # Drop training_sessions table
    op.drop_index(op.f('ix_training_sessions_model_version_id'), table_name='training_sessions')
    op.drop_index(op.f('ix_training_sessions_id'), table_name='training_sessions')
    op.drop_table('training_sessions')

    # Drop indexes on model_versions
    op.drop_index(op.f('ix_model_versions_is_deployed'), table_name='model_versions')
    op.drop_index(op.f('ix_model_versions_status'), table_name='model_versions')

    # Drop foreign keys
    op.drop_constraint('fk_model_versions_trained_by', 'model_versions', type_='foreignkey')
    op.drop_constraint('fk_model_versions_base_model', 'model_versions', type_='foreignkey')

    # Remove added columns
    op.drop_column('model_versions', 'notes')
    op.drop_column('model_versions', 'deployed_at')
    op.drop_column('model_versions', 'is_deployed')
    op.execute("ALTER TABLE model_versions DROP COLUMN IF EXISTS status")
    op.drop_column('model_versions', 'metrics')
    op.drop_column('model_versions', 'trained_by')
    op.drop_column('model_versions', 'training_duration_seconds')
    op.drop_column('model_versions', 'training_data_count')
    op.drop_column('model_versions', 'model_config')
    op.drop_column('model_versions', 'base_model_id')
    op.drop_column('model_versions', 'keras_cloudinary_id')
    op.drop_column('model_versions', 'tflite_cloudinary_id')
    op.drop_column('model_versions', 'keras_size_bytes')
    op.drop_column('model_versions', 'tflite_size_bytes')
    op.drop_column('model_versions', 'keras_model_url')

    # Rename columns back
    op.alter_column('model_versions', 'tflite_model_url', new_column_name='model_url')

    # Drop enum type
    op.execute("DROP TYPE IF EXISTS modelstatus")
