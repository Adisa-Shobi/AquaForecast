# AquaForecast API - Setup Guide

This guide will help you set up the AquaForecast API for development.

## Prerequisites

- Python 3.11 or higher
- PostgreSQL 14+ with PostGIS extension
- Firebase project with Admin SDK credentials
- Git

## Step-by-Step Setup

### 1. Clone the Repository

```bash
cd AquaForecast/API
```

### 2. Create Virtual Environment

```bash
# Create virtual environment
python3 -m venv venv

# Activate virtual environment
# On macOS/Linux:
source venv/bin/activate

# On Windows:
# venv\Scripts\activate
```

### 3. Install Dependencies

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 4. Set Up PostgreSQL Database

#### Option A: Local PostgreSQL

```bash
# Install PostgreSQL (if not already installed)
# macOS:
brew install postgresql postgis

# Start PostgreSQL
brew services start postgresql

# Create database
createdb aquaforecast_dev

# Enable PostGIS extension
psql aquaforecast_dev -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

#### Option B: Using Docker

```bash
# Run PostgreSQL with PostGIS
docker run -d \
  --name aquaforecast_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=aquaforecast_dev \
  -p 5432:5432 \
  postgis/postgis:14-3.3
```

### 5. Configure Environment Variables

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your configuration
nano .env
```

Update the following variables:

```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/aquaforecast_dev
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
ENVIRONMENT=development
```

### 6. Set Up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the JSON file as `firebase-credentials.json` in the API directory

**IMPORTANT**: Never commit `firebase-credentials.json` to version control!

### 7. Run Database Migrations

```bash
# Generate initial migration (if not exists)
alembic revision --autogenerate -m "Initial migration"

# Run migrations
alembic upgrade head
```

### 8. Start the Development Server

```bash
# Run with auto-reload
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at:
- API: http://localhost:8000
- Interactive docs: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### 9. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8000/health

# Expected response:
# {"status":"healthy","version":"1.0.0","environment":"development"}
```

## Using Docker Compose (Recommended)

For a complete setup with database:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Running Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app tests/

# Run specific test file
pytest tests/test_auth.py -v

# Run with verbose output
pytest -vv
```

## Development Workflow

### Making Database Changes

1. Modify models in `app/models/`
2. Generate migration:
   ```bash
   alembic revision --autogenerate -m "Description of changes"
   ```
3. Review the generated migration in `alembic/versions/`
4. Apply migration:
   ```bash
   alembic upgrade head
   ```

### Code Formatting

```bash
# Format code with black
black app tests

# Check code style
flake8 app tests

# Type checking
mypy app
```

### Adding New Endpoints

1. Create endpoint module in `app/api/v1/endpoints/`
2. Add schemas in `app/schemas/`
3. Add business logic in `app/services/`
4. Register router in `app/api/v1/router.py`
5. Write tests in `tests/`

## Troubleshooting

### Database Connection Issues

**Error**: `could not connect to server`

**Solution**:
```bash
# Check if PostgreSQL is running
psql -U postgres -c "SELECT 1"

# Check DATABASE_URL in .env
# Ensure host, port, username, and password are correct
```

### Firebase Authentication Issues

**Error**: `firebase_admin.exceptions.InvalidArgumentError`

**Solution**:
- Verify `firebase-credentials.json` exists
- Check file path in `.env`
- Ensure JSON file is valid

### PostGIS Extension Not Found

**Error**: `type "geography" does not exist`

**Solution**:
```bash
# Enable PostGIS extension
psql aquaforecast_dev -c "CREATE EXTENSION postgis;"
```

### Port Already in Use

**Error**: `Address already in use`

**Solution**:
```bash
# Find process using port 8000
lsof -i :8000

# Kill process
kill -9 <PID>

# Or use different port
uvicorn app.main:app --reload --port 8001
```

## Next Steps

- Read the [API Documentation](../API_DOCUMENTATION.md)
- Check the [README](README.md) for API usage
- Review the [FastAPI Documentation](https://fastapi.tiangolo.com/)

## Support

For issues and questions:
- Check existing GitHub issues
- Create a new issue with:
  - Python version
  - OS version
  - Error message
  - Steps to reproduce
