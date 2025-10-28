"""User service - business logic for user management."""

from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from app.models.user import User
from app.schemas.user import UserCreate


class UserService:
    """Service for user-related business logic."""

    @staticmethod
    def get_user_by_firebase_uid(db: Session, firebase_uid: str) -> Optional[User]:
        """
        Get user by Firebase UID.

        Args:
            db: Database session
            firebase_uid: Firebase user ID

        Returns:
            User if found, None otherwise
        """
        return db.query(User).filter(User.firebase_uid == firebase_uid).first()

    @staticmethod
    def get_user_by_email(db: Session, email: str) -> Optional[User]:
        """
        Get user by email address.

        Args:
            db: Database session
            email: User email address

        Returns:
            User if found, None otherwise
        """
        return db.query(User).filter(User.email == email).first()

    @staticmethod
    def get_user_by_id(db: Session, user_id: str) -> Optional[User]:
        """
        Get user by internal ID.

        Args:
            db: Database session
            user_id: Internal user ID (UUID)

        Returns:
            User if found, None otherwise
        """
        return db.query(User).filter(User.id == user_id).first()

    @staticmethod
    def create_user(db: Session, user_data: UserCreate) -> User:
        """
        Create a new user.

        Args:
            db: Database session
            user_data: User creation data

        Returns:
            Created user

        Raises:
            IntegrityError: If user with same Firebase UID or email exists
        """
        user = User(
            firebase_uid=user_data.firebase_uid,
            email=user_data.email,
            created_at=datetime.utcnow(),
            is_active=True,
        )

        db.add(user)
        db.commit()
        db.refresh(user)

        return user

    @staticmethod
    def get_or_create_user(db: Session, user_data: UserCreate) -> tuple[User, bool]:
        """
        Get existing user or create new one.

        Args:
            db: Database session
            user_data: User creation data

        Returns:
            Tuple of (User, created) where created is True if user was newly created
        """
        # Try to find existing user
        user = UserService.get_user_by_firebase_uid(db, user_data.firebase_uid)

        if user:
            # User exists - update email if changed
            if user.email != user_data.email:
                user.email = user_data.email
                db.commit()
                db.refresh(user)
            return user, False

        # Create new user
        try:
            user = UserService.create_user(db, user_data)
            return user, True
        except IntegrityError:
            # Race condition - another request created the user
            db.rollback()
            user = UserService.get_user_by_firebase_uid(db, user_data.firebase_uid)
            if user:
                return user, False
            raise

    @staticmethod
    def update_last_sync(db: Session, user_id: str) -> None:
        """
        Update user's last sync timestamp.

        Args:
            db: Database session
            user_id: Internal user ID
        """
        user = UserService.get_user_by_id(db, user_id)
        if user:
            user.last_sync_at = datetime.utcnow()
            db.commit()

    @staticmethod
    def get_user_reading_count(db: Session, user_id: str) -> int:
        """
        Get count of water quality readings for a user.

        Args:
            db: Database session
            user_id: Internal user ID

        Returns:
            Number of readings
        """
        from app.services.farm_data_service import FarmDataService
        return FarmDataService.get_reading_count(db, user_id)

    @staticmethod
    def deactivate_user(db: Session, user_id: str) -> bool:
        """
        Deactivate a user account (soft delete).

        Args:
            db: Database session
            user_id: Internal user ID

        Returns:
            True if user was deactivated, False if not found
        """
        user = UserService.get_user_by_id(db, user_id)
        if user:
            user.is_active = False
            db.commit()
            return True
        return False
