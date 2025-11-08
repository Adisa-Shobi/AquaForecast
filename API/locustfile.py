"""
Locust load testing for AquaForecast API.

Tests only GET endpoints that don't require authentication or request bodies
to ensure reliable load testing without failures.

Usage:
    # Local testing
    locust -f locustfile.py --host=http://localhost:8000

    # Production testing
    locust -f locustfile.py --host=https://aquaforecast.duckdns.org
"""

import random
from locust import HttpUser, task, between


class APIUser(HttpUser):
    """Simulates typical API user testing public endpoints."""

    wait_time = between(1, 5)

    @task(10)
    def health_check(self):
        """Health check endpoint - most frequently accessed."""
        self.client.get("/health")

    @task(15)
    def get_latest_model(self):
        """Get latest model - primary endpoint for mobile apps."""
        self.client.get("/api/v1/models/latest")

    @task(12)
    def get_deployed_model(self):
        """Get deployed model information."""
        self.client.get("/api/v1/models/deployed")

    @task(10)
    def check_model_update_v1_0_0(self):
        """Check for updates from version 1.0.0."""
        self.client.get("/api/v1/models/check-update?current_version=1.0.0")

    @task(8)
    def check_model_update_v1_1_0(self):
        """Check for updates from version 1.1.0."""
        self.client.get("/api/v1/models/check-update?current_version=1.1.0")

    @task(6)
    def check_model_update_v1_2_0(self):
        """Check for updates from version 1.2.0."""
        self.client.get("/api/v1/models/check-update?current_version=1.2.0")

    @task(5)
    def check_model_update_with_app_version(self):
        """Check for updates with app version compatibility."""
        version = random.choice(["1.0.0", "1.1.0", "1.2.0"])
        app_version = random.choice(["1.0.0", "1.5.0", "2.0.0"])
        self.client.get(f"/api/v1/models/check-update?current_version={version}&app_version={app_version}")
