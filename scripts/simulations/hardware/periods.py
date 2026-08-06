"""Provides the `Periods` class."""

import logging
from time import sleep

from simulations.database.real_database import RealDatabase
from simulations.hardware.buckets import Buckets


class Periods:
    """Manages the buckets of several periods."""

    def __init__(self, database: RealDatabase, kind: str, current_time: int) -> None:
        self._kind = kind
        self._buckets_1h = Buckets(database, kind, "last_1h", 3600, current_time)
        self._buckets_1d = Buckets(database, kind, "last_1d", 86400, current_time)
        self._buckets_1w = Buckets(database, kind, "last_1w", 604800, current_time)
        self._buckets_1m = Buckets(database, kind, "last_1m", 2592000, current_time)
        self._buckets_6m = Buckets(database, kind, "last_6m", 15768000, current_time)
        self._buckets_1y = Buckets(database, kind, "last_1y", 31536000, current_time)

    def account(self, database: RealDatabase, value: float, commit_time: int) -> None:
        """Accounts for the current value under all periods."""
        logger = logging.getLogger(__name__)
        sleep(1)

        self._buckets_1h.add(value)
        self._buckets_1d.add(value)
        self._buckets_1w.add(value)
        self._buckets_1m.add(value)
        self._buckets_6m.add(value)
        self._buckets_1y.add(value)

        database.telemetry_aquarium_node.child(self._kind).child("last_instant").set({
            "timestamp": commit_time,
            "value": value,
        }, database.device_token)
        logger.info(
            "Committed %s %f at %d",
            self._kind,
            value,
            commit_time,
        )

        self._buckets_1h.try_commit(database, self._kind, commit_time)
        self._buckets_1d.try_commit(database, self._kind, commit_time)
        self._buckets_1w.try_commit(database, self._kind, commit_time)
        self._buckets_1m.try_commit(database, self._kind, commit_time)
        self._buckets_6m.try_commit(database, self._kind, commit_time)
        self._buckets_1y.try_commit(database, self._kind, commit_time)
