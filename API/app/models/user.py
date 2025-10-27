"""User model - minimal data storage per privacy requirements."""

from datetime import datetime
from sqlalchemy import Column, String, DateTime, Boolean
from sqlalchemy.dialects.postgresql import UUID
import uuid

from app.core.database import Base


class User(Base):
    """
    User model - stores only essential authentication data.

    Privacy Note: Only Firebase UID, email, and internal ID are stored.
    No profile information (name, photo, phone) is collected.
    """

    __tablename__ = "users"

    # Primary key
    id = Column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        index=True,
        comment="Internal user ID",
    )

    # Firebase authentication
    firebase_uid = Column(
        String(128),
        unique=True,
        nullable=False,
        index=True,
        comment="Firebase user ID",
    )

    # User identification
    email = Column(
        String(255),
        unique=True,
        nullable=False,
        index=True,
        comment="User email address",
    )

    # Timestamps
    created_at = Column(
        DateTime,
        nullable=False,
        default=datetime.utcnow,
        comment="Account creation timestamp",
    )

    last_sync_at = Column(
        DateTime,
        nullable=True,
        comment="Last data sync timestamp",
    )

    # Account status
    is_active = Column(
        Boolean,
        nullable=False,
        default=True,
        comment="Whether account is active",
    )

    def __repr__(self) -> str:
        """String representation of User."""
        return f"<User(id={self.id}, email={self.email}, firebase_uid={self.firebase_uid})>"

    def to_dict(self) -> dict:
        """Convert user to dictionary."""
        return {
            "user_id": str(self.id),
            "firebase_uid": self.firebase_uid,
            "email": self.email,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "last_sync_at": (
                self.last_sync_at.isoformat() if self.last_sync_at else None
            ),
            "is_active": self.is_active,
        }
