"""Farm data model - water quality measurements only."""

from datetime import datetime
from sqlalchemy import Column, String, DateTime, Numeric, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from geoalchemy2 import Geography
import uuid

from app.core.database import Base


class FarmData(Base):
    """
    Farm data model - stores only water quality parameters.

    Privacy: Only water parameters and location stored (with user consent).
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
            "country_code": self.country_code,
            "recorded_at": self.recorded_at.isoformat() if self.recorded_at else None,
            "synced_at": self.synced_at.isoformat() if self.synced_at else None,
        }
