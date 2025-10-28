"""Farm data service - business logic for water quality data."""

from datetime import datetime
from typing import List, Optional, Tuple
from sqlalchemy.orm import Session
from sqlalchemy import func, and_
from geoalchemy2.functions import ST_GeogFromText, ST_DWithin, ST_Distance

from app.models.farm_data import FarmData
from app.models.user import User
from app.schemas.farm_data import FarmDataReading


class FarmDataService:
    """Service for farm data operations."""

    @staticmethod
    def create_reading(
        db: Session,
        user_id: str,
        reading: FarmDataReading,
        device_id: Optional[str] = None,
    ) -> FarmData:
        """Create a single farm data reading."""
        # Create WKT point for PostGIS
        point_wkt = f"POINT({reading.location.longitude} {reading.location.latitude})"

        farm_data = FarmData(
            user_id=user_id,
            temperature=reading.temperature,
            ph=reading.ph,
            dissolved_oxygen=reading.dissolved_oxygen,
            ammonia=reading.ammonia,
            nitrate=reading.nitrate,
            turbidity=reading.turbidity,
            location=point_wkt,
            country_code=reading.country_code,
            recorded_at=reading.recorded_at,
            synced_at=datetime.utcnow(),
            device_id=device_id,
            created_at=datetime.utcnow(),
        )

        db.add(farm_data)
        db.commit()
        db.refresh(farm_data)

        return farm_data

    @staticmethod
    def bulk_create_readings(
        db: Session,
        user_id: str,
        readings: List[FarmDataReading],
        device_id: Optional[str] = None,
    ) -> Tuple[List[FarmData], int]:
        """Bulk create farm data readings."""
        created_readings = []
        failed_count = 0

        for reading in readings:
            try:
                farm_data = FarmDataService.create_reading(
                    db, user_id, reading, device_id
                )
                created_readings.append(farm_data)
            except Exception as e:
                failed_count += 1
                continue

        # Update user's last sync time
        user = db.query(User).filter(User.id == user_id).first()
        if user:
            user.last_sync_at = datetime.utcnow()
            db.commit()

        return created_readings, failed_count

    @staticmethod
    def get_user_readings(
        db: Session,
        user_id: str,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        limit: int = 100,
        offset: int = 0,
    ) -> Tuple[List[FarmData], int]:
        """Get user's farm data readings with optional filters."""
        query = db.query(FarmData).filter(FarmData.user_id == user_id)

        if start_date:
            query = query.filter(FarmData.recorded_at >= start_date)
        if end_date:
            query = query.filter(FarmData.recorded_at <= end_date)

        total = query.count()
        readings = (
            query.order_by(FarmData.recorded_at.desc())
            .limit(limit)
            .offset(offset)
            .all()
        )

        return readings, total

    @staticmethod
    def get_reading_count(db: Session, user_id: str) -> int:
        """Get total count of readings for a user."""
        return db.query(FarmData).filter(FarmData.user_id == user_id).count()

    @staticmethod
    def get_regional_analytics(
        db: Session,
        country_code: Optional[str] = None,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
    ) -> List[dict]:
        """Get aggregated analytics by region."""
        query = db.query(
            FarmData.country_code,
            func.count(FarmData.id).label("total_readings"),
            func.avg(FarmData.temperature).label("avg_temperature"),
            func.avg(FarmData.ph).label("avg_ph"),
            func.avg(FarmData.dissolved_oxygen).label("avg_dissolved_oxygen"),
            func.avg(FarmData.ammonia).label("avg_ammonia"),
            func.avg(FarmData.nitrate).label("avg_nitrate"),
            func.avg(FarmData.turbidity).label("avg_turbidity"),
            func.min(FarmData.recorded_at).label("min_date"),
            func.max(FarmData.recorded_at).label("max_date"),
        )

        if country_code:
            query = query.filter(FarmData.country_code == country_code)
        if start_date:
            query = query.filter(FarmData.recorded_at >= start_date)
        if end_date:
            query = query.filter(FarmData.recorded_at <= end_date)

        results = query.group_by(FarmData.country_code).all()

        analytics = []
        for row in results:
            if row.country_code:
                analytics.append(
                    {
                        "country_code": row.country_code,
                        "total_readings": row.total_readings,
                        "avg_temperature": round(float(row.avg_temperature), 2),
                        "avg_ph": round(float(row.avg_ph), 2),
                        "avg_dissolved_oxygen": round(float(row.avg_dissolved_oxygen), 2),
                        "avg_ammonia": round(float(row.avg_ammonia), 2),
                        "avg_nitrate": round(float(row.avg_nitrate), 2),
                        "avg_turbidity": round(float(row.avg_turbidity), 2),
                        "date_range": {
                            "start": row.min_date.isoformat() if row.min_date else None,
                            "end": row.max_date.isoformat() if row.max_date else None,
                        },
                    }
                )

        return analytics

    @staticmethod
    def delete_user_data(db: Session, user_id: str) -> int:
        """Delete all farm data for a user (GDPR compliance)."""
        count = db.query(FarmData).filter(FarmData.user_id == user_id).count()
        db.query(FarmData).filter(FarmData.user_id == user_id).delete()
        db.commit()
        return count
