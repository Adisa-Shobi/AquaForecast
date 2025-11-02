"""Farm data schemas for request/response validation."""

from datetime import datetime, date
from typing import List, Optional
from pydantic import BaseModel, Field, validator


class LocationData(BaseModel):
    """Location coordinates."""
    latitude: float = Field(..., ge=-90, le=90)
    longitude: float = Field(..., ge=-180, le=180)


class FarmDataReading(BaseModel):
    """Single water quality reading with fish measurements."""
    temperature: float = Field(..., ge=0, le=50, description="Temperature in °C")
    ph: float = Field(..., ge=0, le=14, description="pH level")
    dissolved_oxygen: float = Field(..., ge=0, le=20, description="Dissolved oxygen in mg/L")
    ammonia: float = Field(..., ge=0, le=10, description="Ammonia in mg/L")
    nitrate: float = Field(..., ge=0, le=100, description="Nitrate in mg/L")
    turbidity: float = Field(..., ge=0, le=1000, description="Turbidity in NTU")
    fish_weight: Optional[float] = Field(None, ge=0, le=100, description="Fish weight in kilograms (kg)")
    fish_length: Optional[float] = Field(None, ge=0, le=500, description="Fish length in cm")
    verified: bool = Field(default=False, description="User confirmed fish measurements are accurate")
    start_date: Optional[str] = Field(None, description="Pond cycle start date (YYYY-MM-DD)")
    location: LocationData
    country_code: Optional[str] = Field(None, min_length=2, max_length=2)
    recorded_at: datetime


class FarmDataSyncRequest(BaseModel):
    """Bulk farm data sync request."""
    device_id: Optional[str] = Field(None, max_length=255)
    readings: List[FarmDataReading] = Field(..., min_items=1, max_items=100)


class SyncedReading(BaseModel):
    """Synced reading response."""
    data_id: str
    recorded_at: datetime
    status: str = "success"


class FarmDataSyncResponse(BaseModel):
    """Farm data sync response."""
    synced_count: int
    failed_count: int
    sync_id: str
    synced_at: datetime
    readings: List[SyncedReading]


class FarmDataResponse(BaseModel):
    """Single farm data response."""
    data_id: str
    temperature: float
    ph: float
    dissolved_oxygen: float
    ammonia: float
    nitrate: float
    turbidity: float
    fish_weight: Optional[float]
    fish_length: Optional[float]
    verified: bool
    start_date: Optional[str]
    country_code: Optional[str]
    recorded_at: datetime
    synced_at: datetime


class FarmDataListResponse(BaseModel):
    """List of farm data readings."""
    readings: List[FarmDataResponse]
    total: int
    limit: int
    offset: int


class RegionalAnalytics(BaseModel):
    """Regional aggregated analytics."""
    country_code: str
    total_readings: int
    avg_temperature: float
    avg_ph: float
    avg_dissolved_oxygen: float
    avg_ammonia: float
    avg_nitrate: float
    avg_turbidity: float
    date_range: dict


class AnalyticsResponse(BaseModel):
    """Analytics response."""
    aggregated_by_region: List[RegionalAnalytics]
