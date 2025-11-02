"""Farm data model - water quality measurements only."""

from datetime import datetime, date
from sqlalchemy import Column, String, DateTime, Date, Numeric, ForeignKey, Boolean
from sqlalchemy.dialects.postgresql import UUID
from geoalchemy2 import Geography
import uuid

from app.core.database import Base


class FarmData(Base):
    """
    Farm data model - stores water quality parameters and fish measurements.

    Privacy: Only water parameters, location, and fish measurements stored (with user consent).
    No pond configurations or predictions.
    """

    __tablename__ = "farm_data"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)

    # Water quality parameters
    temperature = Column(Numeric(5, 2), nullable=False)
    ph = Column(Numeric(4, 2), nullable=False)
    dissolved_oxygen = Column(Numeric(5, 2), nullable=False)
    ammonia = Column(Numeric(5, 2), nullable=False)
    nitrate = Column(Numeric(5, 2), nullable=False)
    turbidity = Column(Numeric(6, 2), nullable=False)

    # Fish measurements
    fish_weight = Column(Numeric(8, 3), nullable=True, comment="Fish weight in kg")
    fish_length = Column(Numeric(6, 2), nullable=True, comment="Fish length in cm")
    verified = Column(Boolean, nullable=False, default=False, comment="User confirmed measurements are accurate")

    # Pond cycle tracking
    start_date = Column(Date, nullable=True, index=True, comment="When the current pond cycle started")

    # Location data for analytics
    location = Column(Geography(geometry_type="POINT", srid=4326), nullable=False)
    country_code = Column(String(2), nullable=True, index=True)

    # Metadata
    recorded_at = Column(DateTime, nullable=False, index=True)
    synced_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    device_id = Column(String(255), nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def __repr__(self) -> str:
        return f"<FarmData(id={self.id}, user_id={self.user_id}, recorded_at={self.recorded_at})>"

    def to_dict(self) -> dict:
        """Convert farm data to dictionary."""
        return {
            "data_id": str(self.id),
            "temperature": float(self.temperature),
            "ph": float(self.ph),
            "dissolved_oxygen": float(self.dissolved_oxygen),
            "ammonia": float(self.ammonia),
            "nitrate": float(self.nitrate),
            "turbidity": float(self.turbidity),
            "fish_weight": float(self.fish_weight) if self.fish_weight else None,
            "fish_length": float(self.fish_length) if self.fish_length else None,
            "verified": self.verified,
            "start_date": self.start_date.isoformat() if self.start_date else None,
            "country_code": self.country_code,
            "recorded_at": self.recorded_at.isoformat() if self.recorded_at else None,
            "synced_at": self.synced_at.isoformat() if self.synced_at else None,
        }
