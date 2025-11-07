# Build stage
FROM python:3.11-slim AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Runtime stage
FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

COPY alembic.ini .
COPY alembic ./alembic
COPY app ./app

# Copy firebase credentials
# NOTE: For production, mount this as a secret volume instead of baking into image
COPY firebase-credentials.json ./firebase-credentials.json

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
