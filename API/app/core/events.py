"""Event notification system for training progress updates."""

import asyncio
import logging
from typing import Set
from datetime import datetime

logger = logging.getLogger(__name__)


class TrainingEventBus:
    """
    Event bus for training task notifications.

    Uses asyncio.Event to notify SSE streams when training state changes,
    avoiding unnecessary database polling.
    """

    def __init__(self):
        self._event = asyncio.Event()
        self._active_tasks: Set[str] = set()
        self._lock = asyncio.Lock()

    async def notify_training_started(self, task_id: str):
        """Notify that a training task has started."""
        async with self._lock:
            self._active_tasks.add(task_id)
        self._event.set()
        logger.info(f"Training started notification: {task_id}")

    async def notify_training_updated(self, task_id: str):
        """Notify that a training task has been updated (progress change)."""
        if task_id in self._active_tasks:
            self._event.set()

    async def notify_training_completed(self, task_id: str):
        """Notify that a training task has completed or failed."""
        async with self._lock:
            self._active_tasks.discard(task_id)
        self._event.set()
        logger.info(f"Training completed notification: {task_id}")

    async def wait_for_update(self, timeout: float = 30.0) -> bool:
        """
        Wait for a training update event.

        Returns:
            True if event was triggered, False if timeout occurred
        """
        try:
            await asyncio.wait_for(self._event.wait(), timeout=timeout)
            self._event.clear()
            return True
        except asyncio.TimeoutError:
            return False

    def has_active_tasks(self) -> bool:
        """Check if there are any active training tasks."""
        return len(self._active_tasks) > 0

    def get_active_task_count(self) -> int:
        """Get count of active training tasks."""
        return len(self._active_tasks)


# Global event bus instance
training_events = TrainingEventBus()
