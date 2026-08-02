"""Provides the `Buckets` class."""

import logging

from simulations.database.real_database import RealDatabase
from simulations.hardware.consts import RESOLUTION, OFFLINE_VALUE

class Buckets:
    """Represents a collection of telemetry buckets."""

    def __init__(
        self,
        database: RealDatabase,
        sensor_kind: str,
        kind: str,
        total_span_secs: int,
        current_time: int,
    ) -> None:
        logger = logging.getLogger(__name__)

        self._bucket_size = int(total_span_secs / RESOLUTION)
        self._number_of_buckets = RESOLUTION
        self._last_commit = current_time
        self._kind = kind
        self._value_count = 0
        self._value_total = 0

        current_index = (database.telemetry_aquarium_node
            .child(sensor_kind)
            .child(self._kind)
            .child("index")
            .get().val()
        )
        self._bucket_index = (
            (current_index+1) % self._number_of_buckets if current_index is not None else 0
        )

        last_online = current_time
        if current_index is not None:
            last_online = (database.telemetry_aquarium_node
                .child(sensor_kind)
                .child(self._kind)
                .child("buckets")
                .child(f"B{current_index:0{len(str(RESOLUTION-1))}d}")
                .child("timestamp").get().val())

        logger.info(
            "Initialized %d %s::%s buckets of size %d seconds starting at index %d",
            self._number_of_buckets,
            sensor_kind,
            self._kind,
            self._bucket_size,
            self._bucket_index,
        )

        self.try_commit(
            database, sensor_kind, current_time, current_time-last_online >= self._bucket_size
        )

    def add(self, value: float) -> None:
        """Add a value to the current bucket."""
        if value != OFFLINE_VALUE:
            self._value_total += value
            self._value_count += 1

    def try_commit(
        self, database: RealDatabase, sensor_kind: str, current_time: int, force: bool = False
    ) -> bool:
        """Check if the current bucket should be committed and do so."""
        logger = logging.getLogger(__name__)

        if not force and current_time-self._last_commit < self._bucket_size:
            return False

        (database.telemetry_aquarium_node
            .child(sensor_kind)
            .child(self._kind)
            .child("buckets")
            .child(f"B{self._bucket_index:0{len(str(RESOLUTION-1))}d}")
            .set({
                "timestamp": current_time,
                "value": (
                    self._value_total / self._value_count
                    if self._value_count > 0 else OFFLINE_VALUE
                ),
            }))
        (database.telemetry_aquarium_node
            .child(sensor_kind)
            .child(self._kind)
            .child("index")
            .set(self._bucket_index))

        logger.info(
            "Committed %s::%s::B[%d] at %d",
            sensor_kind,
            self._kind,
            self._bucket_index,
            current_time,
        )

        self._bucket_index = (self._bucket_index + 1) % self._number_of_buckets
        self._last_commit = current_time
        self._value_count = 0
        self._value_total = 0

        return True
