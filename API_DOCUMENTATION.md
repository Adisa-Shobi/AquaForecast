# AquaForecast API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Authentication](#authentication)
4. [API Endpoints](#api-endpoints)
5. [Data Models](#data-models)
6. [Error Handling](#error-handling)
7. [Rate Limiting](#rate-limiting)
8. [Security](#security)
9. [Implementation Guide](#implementation-guide)
10. [Testing](#testing)

---

## Overview

The AquaForecast API is a RESTful service that enables aquaculture farmers to:
- Authenticate and manage their accounts
- Sync water quality data (water parameters only) from mobile devices with user consent
- Aggregate farm data for analytics purposes, grouped by location
- Download updated ML models for on-device predictions

**Data Privacy Notice**: With user consent, only water quality parameters (temperature, pH, dissolved oxygen, ammonia, nitrate, turbidity) and location data are stored on the backend for analytics purposes. All predictions, feeding schedules, and pond configurations are managed and stored locally on the user's device and are never transmitted to or stored on backend servers.

### Base URL
```
Production: https://api.aquaforecast.com/v1
Staging: https://staging-api.aquaforecast.com/v1
Development: http://localhost:8000/v1
```

### Technology Stack Recommendations
- **Framework**: FastAPI (Python) or Express.js (Node.js) or Spring Boot (Java)
- **Database**: PostgreSQL with PostGIS extension (for location data)
- **Cache**: Redis (for sessions, rate limiting)
- **File Storage**: AWS S3 or Google Cloud Storage (for ML models)
- **Authentication**: Firebase Admin SDK + JWT
- **API Documentation**: OpenAPI/Swagger

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────┐
│         Mobile Client (Android)             │
│  ┌──────────────────────────────────────┐  │
│  │  Local Storage (Room Database)       │  │
│  │  - Pond Configurations (local only)  │  │
│  │  - Predictions (local only)          │  │
│  │  - Feeding Schedules (local only)    │  │
│  │  - Farm Data (cached)                │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │  TensorFlow Lite ML Model            │  │
│  │  (On-device predictions)             │  │
│  └──────────────────────────────────────┘  │
└────────┬────────────────────────────────────┘
         │
         │ HTTPS/JSON (Water Parameters + Location Only)
         │
┌────────▼────────────────────────────────────┐
│         API Gateway / Load Balancer         │
│         (nginx, AWS ALB, Cloud Run)        │
└────────┬────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────┐
│           Application Server                │
│  ┌──────────────────────────────────┐      │
│  │   Authentication Middleware       │      │
│  └──────────────┬───────────────────┘      │
│                 │                           │
│  ┌──────────────▼───────────────────┐      │
│  │      API Route Handlers          │      │
│  │  - Auth Routes                   │      │
│  │  - Farm Data Sync (water only)   │      │
│  │  - ML Model Distribution         │      │
│  │  - Analytics Aggregation         │      │
│  └──────────────┬───────────────────┘      │
└─────────────────┼──────────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
┌───────▼────────┐  ┌──────▼─────────┐
│   PostgreSQL   │  │     Redis      │
│   + PostGIS    │  │   (Cache)      │
│ (Water params  │  └────────────────┘
│  + location    │
│  for analytics)│
└────────────────┘
```

### Database Schema

```sql
-- Users table (synced with Firebase - minimal data only)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_sync_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE
);

-- Farm data (water quality measurements ONLY - for analytics)
-- NOTE: Only water parameters and location data are stored on the backend with user consent
-- This data is used for aggregated analytics grouped by location
CREATE TABLE farm_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Water quality parameters (with user consent)
    temperature DECIMAL(5, 2) NOT NULL,
    ph DECIMAL(4, 2) NOT NULL,
    dissolved_oxygen DECIMAL(5, 2) NOT NULL,
    ammonia DECIMAL(5, 2) NOT NULL,
    nitrate DECIMAL(5, 2) NOT NULL,
    turbidity DECIMAL(6, 2) NOT NULL,

    -- Location data for analytics grouping
    location GEOGRAPHY(POINT, 4326) NOT NULL, -- Lat/lng where reading was taken
    country_code VARCHAR(2), -- For regional analytics

    -- Metadata
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    device_id VARCHAR(255), -- For tracking which device submitted
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- NOTE: NO farms table - farm management is local only
-- NOTE: NO ponds table - pond configurations are local only
-- NOTE: NO predictions table - predictions are generated and stored locally only
-- NOTE: NO feeding_schedules table - schedules are managed locally only

-- Model versions (for ML model management)
CREATE TABLE model_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(50) UNIQUE NOT NULL,
    model_url TEXT NOT NULL,
    model_size_bytes BIGINT,
    preprocessing_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    min_app_version VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Sync log (for tracking sync operations)
CREATE TABLE sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    sync_type VARCHAR(50) NOT NULL, -- Only 'farm_data' - water parameters sync
    records_synced INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL, -- 'success', 'partial', 'failed'
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_farm_data_user_id ON farm_data(user_id);
CREATE INDEX idx_farm_data_recorded_at ON farm_data(recorded_at DESC);
CREATE INDEX idx_farm_data_location ON farm_data USING GIST(location); -- Critical for location-based analytics
CREATE INDEX idx_farm_data_country_code ON farm_data(country_code); -- For regional analytics
```

---

## Authentication

### Firebase Authentication + JWT

The API uses Firebase Authentication for user identity management and JWT tokens for API authorization.

#### Authentication Flow

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│  Client  │                 │   API    │                 │ Firebase │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │ 1. Login (Email/Google)    │                            │
     ├───────────────────────────────────────────────────────>│
     │                            │                            │
     │ 2. Firebase ID Token       │                            │
     │<───────────────────────────────────────────────────────┤
     │                            │                            │
     │ 3. API Request             │                            │
     │    + ID Token              │                            │
     ├──────────────────────────>│                            │
     │                            │                            │
     │                            │ 4. Verify Token            │
     │                            ├──────────────────────────>│
     │                            │                            │
     │                            │ 5. User Info               │
     │                            │<──────────────────────────┤
     │                            │                            │
     │                            │ 6. Check/Create User       │
     │                            │    in Database             │
     │                            │                            │
     │ 7. API Response            │                            │
     │<──────────────────────────┤                            │
     │                            │                            │
```

#### Request Headers

All authenticated requests must include:

```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
Content-Type: application/json
X-Device-Id: <DEVICE_UNIQUE_ID> (optional)
X-App-Version: <APP_VERSION> (optional)
```

#### Token Validation Middleware

```python
# Example middleware (Python/FastAPI)
from fastapi import Header, HTTPException
from firebase_admin import auth

async def verify_firebase_token(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split("Bearer ")[1]

    try:
        # Verify Firebase ID token
        decoded_token = auth.verify_id_token(token)
        user_id = decoded_token['uid']
        email = decoded_token.get('email')

        # Get or create user in database
        user = await get_or_create_user(user_id, email)

        return user
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")
```

---

## API Endpoints

### 1. Authentication Endpoints

#### POST /auth/register
Register or verify user in the system after Firebase authentication. Only email and IDs are stored.

**Request:**
```json
{
  "firebase_uid": "abc123xyz",
  "email": "farmer@example.com"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "user_id": "uuid-here",
    "firebase_uid": "abc123xyz",
    "email": "farmer@example.com",
    "created_at": "2025-01-15T10:30:00Z"
  }
}
```

#### GET /auth/me
Get current authenticated user information. Returns only essential IDs and email.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "user_id": "uuid-here",
    "firebase_uid": "abc123xyz",
    "email": "farmer@example.com",
    "last_sync_at": "2025-01-15T14:20:00Z",
    "total_readings": 342
  }
}
```

---

### 2. Farm Data (Water Quality) Endpoints

**Note**: These are the primary endpoints for the API. Water quality parameters and location data are the only user data stored on the backend, used for aggregated analytics grouped by location.

#### POST /farm-data/sync
Sync farm data from mobile device (bulk upload). Only water quality parameters and location data are transmitted with user consent.

**Request:**
```json
{
  "device_id": "device-uuid-123",
  "readings": [
    {
      "temperature": 28.5,
      "ph": 7.2,
      "dissolved_oxygen": 6.8,
      "ammonia": 0.15,
      "nitrate": 10.5,
      "turbidity": 12.3,
      "recorded_at": "2025-01-15T08:00:00Z",
      "location": {
        "latitude": -1.2921,
        "longitude": 36.8219
      },
      "country_code": "KE"
    },
    {
      "temperature": 29.0,
      "ph": 7.3,
      "dissolved_oxygen": 7.0,
      "ammonia": 0.12,
      "nitrate": 9.8,
      "turbidity": 11.5,
      "recorded_at": "2025-01-15T14:00:00Z",
      "location": {
        "latitude": -1.2921,
        "longitude": 36.8219
      },
      "country_code": "KE"
    }
  ]
}
```

**Note**: No pond_id, farm_id, or other local configuration data is transmitted. Only water parameters and location.

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "synced_count": 2,
    "failed_count": 0,
    "sync_id": "sync-uuid-123",
    "synced_at": "2025-01-15T14:30:00Z",
    "readings": [
      {
        "data_id": "uuid-data-1",
        "recorded_at": "2025-01-15T08:00:00Z",
        "status": "success"
      },
      {
        "data_id": "uuid-data-2",
        "recorded_at": "2025-01-15T14:00:00Z",
        "status": "success"
      }
    ]
  }
}
```

#### GET /farm-data
Get user's historical farm data readings.

**Query Parameters:**
- `start_date` (optional): ISO 8601 date
- `end_date` (optional): ISO 8601 date
- `limit` (optional): Max records (default: 100, max: 1000)
- `offset` (optional): Pagination offset

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "readings": [
      {
        "data_id": "uuid-here",
        "temperature": 28.5,
        "ph": 7.2,
        "dissolved_oxygen": 6.8,
        "ammonia": 0.15,
        "nitrate": 10.5,
        "turbidity": 12.3,
        "recorded_at": "2025-01-15T08:00:00Z",
        "location": {
          "latitude": -1.2921,
          "longitude": 36.8219
        },
        "country_code": "KE",
        "synced_at": "2025-01-15T14:30:00Z"
      }
    ],
    "total": 150,
    "limit": 100,
    "offset": 0
  }
}
```

#### GET /farm-data/analytics
Get aggregated analytics for water quality data grouped by location.

**Query Parameters:**
- `region` (optional): Filter by country code (e.g., "KE", "UG")
- `start_date` (optional): ISO 8601 date
- `end_date` (optional): ISO 8601 date
- `radius_km` (optional): Radius around a location for regional analytics
- `center_lat` (optional): Center latitude for radius search
- `center_lng` (optional): Center longitude for radius search

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "aggregated_by_region": [
      {
        "country_code": "KE",
        "total_readings": 1250,
        "avg_temperature": 28.3,
        "avg_ph": 7.1,
        "avg_dissolved_oxygen": 6.9,
        "avg_ammonia": 0.14,
        "avg_nitrate": 10.2,
        "avg_turbidity": 11.8,
        "date_range": {
          "start": "2025-01-01T00:00:00Z",
          "end": "2025-01-15T23:59:59Z"
        }
      }
    ]
  }
}
```

#### DELETE /farm-data/user
Delete all farm data for the authenticated user (GDPR compliance).

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "deleted_count": 342,
    "message": "All your water quality data has been permanently deleted"
  }
}
```

---

---

### 3. ML Model Management Endpoints

**Note**: ML models are distributed through the API for on-device predictions. All predictions are generated locally on the user's device.

#### GET /models/latest
Get information about the latest ML model.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "model_id": "uuid-model-1",
    "version": "v1.2.0",
    "model_url": "https://storage.example.com/models/aqua_forecast_v1.2.0.tflite",
    "model_size_bytes": 2458624,
    "preprocessing_config": {
      "temperature_mean": 28.5,
      "temperature_std": 2.1,
      "ph_mean": 7.2,
      "ph_std": 0.5
    },
    "is_active": true,
    "min_app_version": "1.0.0",
    "created_at": "2025-01-10T00:00:00Z"
  }
}
```

#### GET /models/check-update
Check if a model update is available.

**Query Parameters:**
- `current_version` (required): Current model version

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "update_available": true,
    "current_version": "v1.1.0",
    "latest_version": "v1.2.0",
    "model_url": "https://storage.example.com/models/aqua_forecast_v1.2.0.tflite",
    "model_size_bytes": 2458624,
    "release_notes": "Improved accuracy for tilapia predictions"
  }
}
```

---

## Data Models

### Request/Response Formats

All requests and responses use JSON format with consistent structure:

**Success Response:**
```json
{
  "success": true,
  "data": { /* response data */ },
  "meta": { /* optional metadata */ }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": { /* optional error details */ }
  }
}
```

### Data Validation Rules

#### Farm Data Reading
```json
{
  "temperature": {
    "type": "number",
    "min": 0,
    "max": 50,
    "precision": 2
  },
  "ph": {
    "type": "number",
    "min": 0,
    "max": 14,
    "precision": 2
  },
  "dissolved_oxygen": {
    "type": "number",
    "min": 0,
    "max": 20,
    "precision": 2
  },
  "ammonia": {
    "type": "number",
    "min": 0,
    "max": 10,
    "precision": 2
  },
  "nitrate": {
    "type": "number",
    "min": 0,
    "max": 100,
    "precision": 2
  },
  "turbidity": {
    "type": "number",
    "min": 0,
    "max": 1000,
    "precision": 2
  }
}
```

#### Location Data
```json
{
  "latitude": {
    "type": "number",
    "min": -90,
    "max": 90,
    "precision": 6
  },
  "longitude": {
    "type": "number",
    "min": -180,
    "max": 180,
    "precision": 6
  }
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Status | Description |
|------|--------|-------------|
| 200 | OK | Request succeeded |
| 201 | Created | Resource created successfully |
| 204 | No Content | Request succeeded, no content to return |
| 400 | Bad Request | Invalid request data |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate) |
| 422 | Unprocessable Entity | Validation error |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

### Error Codes

```json
{
  "AUTH_001": "Invalid or expired token",
  "AUTH_002": "User not found",
  "AUTH_003": "Insufficient permissions",

  "VALIDATION_001": "Invalid request data",
  "VALIDATION_002": "Required field missing",
  "VALIDATION_003": "Invalid data format",

  "RESOURCE_001": "Resource not found",
  "RESOURCE_002": "Resource already exists",
  "RESOURCE_003": "Cannot delete resource with dependencies",

  "SYNC_001": "Sync failed",
  "SYNC_002": "Partial sync completed",
  "SYNC_003": "Device not recognized",

  "MODEL_001": "Model version not found",
  "MODEL_002": "Model download failed",

  "RATE_LIMIT_001": "Rate limit exceeded"
}
```

### Error Response Examples

**Validation Error:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_001",
    "message": "Invalid request data",
    "details": {
      "fields": {
        "temperature": "Value must be between 0 and 50",
        "ph": "Value must be between 0 and 14"
      }
    }
  }
}
```

**Authorization Error:**
```json
{
  "success": false,
  "error": {
    "code": "AUTH_003",
    "message": "You do not have permission to access this resource",
    "details": {
      "resource": "pond",
      "resource_id": "uuid-pond-1",
      "required_permission": "write"
    }
  }
}
```

---

## Rate Limiting

### Rate Limit Rules

| Endpoint Pattern | Limit | Window |
|-----------------|-------|--------|
| /auth/* | 20 requests | per minute |
| /farm-data/sync | 100 requests | per hour |
| /farm-data/* (read) | 1000 requests | per hour |
| /predictions/generate | 50 requests | per hour |
| /models/* | 10 requests | per hour |
| Default | 500 requests | per hour |

### Rate Limit Headers

Every response includes rate limit information:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1642272000
```

### Rate Limit Exceeded Response

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_001",
    "message": "Rate limit exceeded. Please try again later.",
    "details": {
      "limit": 1000,
      "window": "1 hour",
      "reset_at": "2025-01-15T16:00:00Z"
    }
  }
}
```

---

## Security

### Security Best Practices

1. **HTTPS Only**: All API endpoints must use HTTPS in production
2. **Token Expiry**: Firebase ID tokens expire after 1 hour
3. **Input Validation**: Validate and sanitize all input data
4. **SQL Injection**: Use parameterized queries
5. **XSS Protection**: Escape output data
6. **CORS**: Configure appropriate CORS policies
7. **Rate Limiting**: Implement per-user rate limiting
8. **Audit Logging**: Log all data modifications
9. **Data Encryption**: Encrypt sensitive data at rest
10. **Regular Updates**: Keep dependencies updated

### CORS Configuration

```javascript
// Example CORS configuration
{
  "allowed_origins": [
    "https://aquaforecast.com",
    "https://app.aquaforecast.com"
  ],
  "allowed_methods": ["GET", "POST", "PUT", "DELETE"],
  "allowed_headers": ["Authorization", "Content-Type", "X-Device-Id"],
  "max_age": 3600
}
```

### Data Privacy

**Privacy-First Design**: AquaForecast is designed with user privacy as a core principle.

**What We Collect (Backend - with user consent)**:
- ✅ **User identification**: Firebase UID, email address, and internal user ID only
- ✅ **Water quality parameters only** (temperature, pH, dissolved oxygen, ammonia, nitrate, turbidity)
- ✅ Location data (latitude/longitude, country code) for analytics grouping
- ✅ Timestamps and device IDs for sync tracking

**Note**: No user profile information (name, photo, phone, etc.) is collected or stored.

**What We DON'T Collect (Local Only)**:
- ❌ **Farm names or configurations**: All farm management is local
- ❌ **Pond names, species, or stock counts**: All pond configurations are local
- ❌ **Predictions**: Generated on-device using TensorFlow Lite, never transmitted
- ❌ **Feeding Schedules**: Created and managed locally, never transmitted
- ❌ **Notification Preferences**: Managed locally via WorkManager

**Purpose of Data Collection**:
- Aggregated analytics for regional water quality trends
- Research and insights for aquaculture community
- Model improvement (with explicit consent)

**Data Access Controls**:
- User data is isolated by user_id
- Users can delete all their data at any time (GDPR compliant)
- Location data requires explicit user consent
- All data transmission over HTTPS only

---

## Implementation Guide

### Phase 1: Core Infrastructure (Week 1-2)

1. **Setup Project**
   - Choose technology stack
   - Setup development environment
   - Configure Firebase Admin SDK
   - Setup PostgreSQL with PostGIS

2. **Implement Authentication**
   - Firebase token verification middleware
   - User registration endpoint
   - User profile endpoints

3. **Database Setup**
   - Create database schema
   - Setup migrations
   - Create seed data for testing

### Phase 2: Basic Data Collection (Week 3-4)

**Note**: No farm or pond management endpoints needed - all configuration is local only.

1. **Water Quality Data Endpoints**
   - Bulk sync endpoint for water parameters
   - Data retrieval with filtering
   - Location-based indexing with PostGIS

### Phase 3: Data Sync (Week 5-6)

1. **Farm Data Endpoints**
   - Bulk sync endpoint
   - Individual CRUD operations
   - Query with filtering and pagination

2. **Feeding Schedules**
   - CRUD operations
   - Active/inactive toggle

### Phase 4: ML Integration (Week 7-8)

1. **Prediction Endpoints**
   - ML model integration
   - Prediction generation
   - Prediction history

2. **Model Management**
   - Model versioning
   - Update checking
   - Model download URLs

### Phase 5: Optimization (Week 9-10)

1. **Performance**
   - Database indexing
   - Query optimization
   - Caching with Redis

2. **Security Hardening**
   - Rate limiting
   - Input validation
   - Security audit

### Phase 6: Testing & Deployment (Week 11-12)

1. **Testing**
   - Unit tests
   - Integration tests
   - Load testing

2. **Deployment**
   - Setup CI/CD
   - Deploy to staging
   - Deploy to production
   - Setup monitoring

---

## Testing

### Test Scenarios

#### Authentication Tests
```bash
# Test user registration
curl -X POST https://api.aquaforecast.com/v1/auth/register \
  -H "Authorization: Bearer <FIREBASE_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "firebase_uid": "test123",
    "email": "test@example.com"
  }'

# Test get user info
curl -X GET https://api.aquaforecast.com/v1/auth/me \
  -H "Authorization: Bearer <FIREBASE_TOKEN>"
```

#### Farm Data Sync Test
```bash
# Test bulk sync
curl -X POST https://api.aquaforecast.com/v1/farm-data/sync \
  -H "Authorization: Bearer <FIREBASE_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "test-device",
    "location": {"latitude": -1.2921, "longitude": 36.8219},
    "readings": [
      {
        "pond_id": "uuid-pond-1",
        "temperature": 28.5,
        "ph": 7.2,
        "dissolved_oxygen": 6.8,
        "ammonia": 0.15,
        "nitrate": 10.5,
        "turbidity": 12.3,
        "recorded_at": "2025-01-15T08:00:00Z"
      }
    ]
  }'
```

#### Location-Based Query Test
```bash
# Test getting farms near a location
curl -X GET 'https://api.aquaforecast.com/v1/farms/nearby?lat=-1.2921&lng=36.8219&radius=10000' \
  -H "Authorization: Bearer <FIREBASE_TOKEN>"
```

### Load Testing

Use tools like Apache JMeter, k6, or Locust:

```javascript
// Example k6 load test script
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
};

export default function() {
  let response = http.get('https://api.aquaforecast.com/v1/ponds', {
    headers: { 'Authorization': `Bearer ${__ENV.FIREBASE_TOKEN}` },
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

---

## Monitoring & Observability

### Metrics to Track

1. **Request Metrics**
   - Request count per endpoint
   - Response time (p50, p95, p99)
   - Error rate
   - Request size

2. **Business Metrics**
   - Daily active users
   - Data sync frequency
   - Prediction generation count
   - Model download count

3. **System Metrics**
   - CPU usage
   - Memory usage
   - Database connections
   - Cache hit rate

### Logging

Implement structured logging:

```json
{
  "timestamp": "2025-01-15T14:30:00Z",
  "level": "INFO",
  "service": "aquaforecast-api",
  "endpoint": "/farm-data/sync",
  "method": "POST",
  "user_id": "uuid-user-1",
  "duration_ms": 245,
  "status_code": 201,
  "request_id": "req-uuid-123",
  "metadata": {
    "readings_count": 5,
    "device_id": "device-uuid"
  }
}
```

### Alerting

Setup alerts for:
- Error rate > 1%
- Response time p95 > 1000ms
- Database connection pool exhausted
- Rate limit violations
- Failed authentication attempts > threshold

---

## Appendix

### Sample Client Code (Kotlin/Android)

```kotlin
// API Service Interface
interface AquaForecastApiService {
    @POST("farm-data/sync")
    suspend fun syncFarmData(
        @Header("Authorization") token: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @GET("ponds")
    suspend fun getPonds(
        @Header("Authorization") token: String
    ): Response<PondsResponse>
}

// Usage in Repository
class FarmDataRepositoryImpl(
    private val apiService: AquaForecastApiService,
    private val authRepository: AuthRepository
) : FarmDataRepository {

    override suspend fun syncData(readings: List<FarmData>): Result<Unit> {
        return try {
            val token = authRepository.getIdToken() ?: return "Not authenticated".asError()

            val request = SyncRequest(
                deviceId = getDeviceId(),
                location = getCurrentLocation(),
                readings = readings.map { it.toDto() }
            )

            val response = apiService.syncFarmData(
                token = "Bearer $token",
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Unit.asSuccess()
            } else {
                "Sync failed: ${response.message()}".asError()
            }
        } catch (e: Exception) {
            "Network error: ${e.message}".asError()
        }
    }
}
```

### Database Backup Strategy

1. **Automated Backups**
   - Full backup: Daily at 2 AM UTC
   - Incremental backup: Every 6 hours
   - Retention: 30 days

2. **Backup Storage**
   - Primary: Cloud storage (S3, GCS)
   - Secondary: Different region for disaster recovery

3. **Restore Testing**
   - Monthly restore test to staging environment
   - Document restore procedures

### Scaling Considerations

1. **Horizontal Scaling**
   - Stateless API servers
   - Load balancer (nginx, AWS ALB)
   - Database read replicas

2. **Caching Strategy**
   - Redis for session data
   - Cache frequently accessed data (pond list, model info)
   - Cache invalidation on updates

3. **Database Optimization**
   - Connection pooling
   - Query optimization
   - Partitioning large tables (farm_data by date)

---

## Changelog

### Version 1.0 (Initial Release)
- Core authentication with Firebase
- Farm data sync (water parameters + location only - with user consent)
- ML model distribution and versioning
- Location-based analytics with PostGIS
- Privacy-first architecture (no farm/pond configs, predictions, or schedules stored)
- GDPR-compliant data deletion

### Future Enhancements (v1.1+)
- Advanced analytics dashboard with regional trends
- Data export (CSV, Excel) for user's own water quality data
- Weather data integration for correlation analysis
- Community insights (aggregated, anonymized data visualization)
- Opt-in anonymous data contribution for ML model improvement
- Multi-language support for API error messages


---

**Document Version**: 1.0
**Last Updated**: October 2025
**Authors**: Shobi Ola-Adisa
