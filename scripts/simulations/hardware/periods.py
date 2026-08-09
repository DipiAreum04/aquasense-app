"""Provides the `SensorPeriods` class."""

import logging
from enum import Enum

import numpy as np
from requests.exceptions import RequestException

from simulations.database.firebase_database import FirebaseDatabase
from simulations.hardware.consts import RESOLUTION, OFFLINE_VALUE


def _as_float(value: float) -> float:
    """Narrow a value to the single-precision, seven-decimal form the database holds."""
    return round(float(np.float32(value)), 7)


class _PendingRequest(Enum):
    """Which of a bucket's writes is riding on the request that is in flight."""
    STARTUP_GAP = "startup_gap"
    FLUSH = "flush"
    COMMIT = "commit"


class _PeriodBucket:
    """Represents the current bucket of a specific period."""

    def __init__(
        self,
        sensor_id: str,
        period_id: str,
        total_span_secs: int,
        current_time: int,
        current_index: int | None,
    ) -> None:
        logger = logging.getLogger(__name__)

        self._bucket_size = total_span_secs // RESOLUTION
        self._last_commit = current_time
        self._sensor_id = sensor_id
        self._period_id = period_id
        self._value_count = 0
        self._value_total = 0.0
        self._startup_committed = False
        self._cached = False
        self._cached_value = OFFLINE_VALUE

        self._pending_request: _PendingRequest | None = None
        self._pending_entries: tuple[tuple[int, int, float], ...] = ()
        self._pending_deadline = False

        self._bucket_index = (
            (current_index+1) % RESOLUTION if current_index is not None else 0
        )

        logger.info(
            "BUCKET[%s::%s] of size %d seconds resuming from index %d",
            self._sensor_id,
            self._period_id,
            self._bucket_size,
            self._bucket_index,
        )

    @property
    def owes_startup_gap(self) -> bool:
        """Whether the gap marker every period writes once on startup is still owed."""
        return not self._startup_committed

    @property
    def cache_hit(self) -> bool:
        """Whether a window that could not be committed is being held."""
        return self._cached

    def add(self, value: float) -> None:
        """Add a value to the current bucket."""
        if value != OFFLINE_VALUE:
            self._value_total += value
            self._value_count += 1

    def at_deadline(self, current_time: int) -> bool:
        """Check whether the current bucket has reached capacity."""
        return current_time-self._last_commit >= self._bucket_size

    def cache_current(self, current_time: int) -> None:
        """Pour the bucket that has just closed into the cache using EWMA and start over."""
        value = self._current_value()

        if not self._cached or self._cached_value == OFFLINE_VALUE:
            self._cached_value = value
        elif value != OFFLINE_VALUE:
            self._cached_value = (self._cached_value+value) / 2

        self._cached = True
        self._last_commit = current_time
        self._value_count = 0
        self._value_total = 0.0

    def pending_updates(self, current_time: int) -> dict[str, object]:
        """Report the write this bucket owes, as paths relative to the telemetry node.

        Reporting moves nothing. The index, the commit deadline and the cache only advance
        in `commit_pending`, once the tick's single request has landed, so a bucket retries
        its own slot rather than skipping it. That is also why nothing here needs to be
        told whether the link is up: the report is optimistic, and `cache_pending` holds
        whatever closed if the request turns out to fail.
        """
        self._pending_request = None
        self._pending_entries = ()
        self._pending_deadline = self.at_deadline(current_time)

        if self.owes_startup_gap:
            self._value_count = 0
            self._value_total = 0.0

            return self._set_pending(
                _PendingRequest.STARTUP_GAP,
                ((self._bucket_index, current_time, OFFLINE_VALUE),),
            )

        if self._cached:
            return self._set_pending(
                _PendingRequest.FLUSH,
                self._cache_entries(current_time),
            )

        if self._pending_deadline:
            return self._set_pending(
                _PendingRequest.COMMIT,
                ((self._bucket_index, current_time, self._current_value()),),
            )

        return {}

    def commit_pending(self, current_time: int) -> None:
        """Advance past the pending write, which has just landed."""
        logger = logging.getLogger(__name__)

        if self._pending_request is None:
            return

        for bucket_index, timestamp, _ in self._pending_entries:
            logger.info(
                "BUCKET[%s::%s][%d] Committed at %d",
                self._sensor_id,
                self._period_id,
                bucket_index,
                timestamp,
            )

        self._bucket_index = (self._bucket_index+len(self._pending_entries)) % RESOLUTION

        if self._pending_request is _PendingRequest.FLUSH:
            self._cached = False
            self._cached_value = OFFLINE_VALUE

            if self._pending_deadline:
                self._start_over(current_time)
        else:
            if self._pending_request is _PendingRequest.STARTUP_GAP:
                self._startup_committed = True
            self._start_over(current_time)

        self._pending_request = None
        self._pending_entries = ()

    def cache_pending(self, current_time: int) -> None:
        """Give up on the pending write, holding a closed window in the cache.

        The startup gap is the one thing not held. It is owed until it lands, and pouring
        it into the cache would turn a single marker into a marker plus a window of
        nothing, which is the same statement made twice.
        """
        if (
            self._pending_request is not None
            and self._pending_request is not _PendingRequest.STARTUP_GAP
            and self._pending_deadline
        ):
            self.cache_current(current_time)

        self._pending_request = None
        self._pending_entries = ()

    def _current_value(self) -> float:
        return (
            self._value_total/self._value_count
            if self._value_count > 0 else OFFLINE_VALUE
        )

    def _start_over(self, current_time: int) -> None:
        self._last_commit = current_time
        self._value_count = 0
        self._value_total = 0.0

    def _cache_entries(self, current_time: int) -> tuple[tuple[int, int, float], ...]:
        values = [OFFLINE_VALUE, self._cached_value]
        if self._pending_deadline:
            values.append(self._current_value())

        return tuple(
            (
                (self._bucket_index+offset) % RESOLUTION,
                max(current_time-(len(values)-1-offset), 0),
                value,
            )
            for offset, value in enumerate(values)
        )

    def _set_pending(
        self,
        request: _PendingRequest,
        entries: tuple[tuple[int, int, float], ...],
    ) -> dict[str, object]:
        """Record `request` as the pending write and return the paths it covers."""
        self._pending_request = request
        self._pending_entries = entries

        prefix = f"{self._sensor_id}/{self._period_id}"
        updates: dict[str, object] = {
            f"{prefix}/buckets/B{bucket_index:0{len(str(RESOLUTION-1))}d}": {
                "timestamp": timestamp,
                "value": _as_float(value),
            }
            for bucket_index, timestamp, value in entries
        }
        updates[f"{prefix}/index"] = entries[-1][0]

        return updates


class SensorPeriods:
    """Represents the periods of a specific sensor."""
    SPANS_SECS = {
        "last_1h": 3600,
        "last_1d": 86400,
        "last_1w": 604800,
        "last_1m": 2592000,
        "last_6m": 15768000,
        "last_1y": 31536000,
    }

    def __init__(
        self,
        database: FirebaseDatabase,
        sensor_id: str,
        current_time: int,
    ) -> None:
        indices = database.read_period_indices(sensor_id, tuple(SensorPeriods.SPANS_SECS))
        self._buckets = [
            _PeriodBucket(sensor_id, period_id, span_secs, current_time, indices[period_id])
            for period_id, span_secs in SensorPeriods.SPANS_SECS.items()
        ]

    def account(self, value: float) -> None:
        """Accounts for the current value under all periods.

        Touches no database. The reading itself, and any bucket this closes, are uploaded
        by `commit_tick`, which batches all four sensors into one request.
        """
        for buckets in self._buckets:
            buckets.add(value)

    def pending_updates(self, current_time: int) -> dict[str, object]:
        """Reports every period's due write for the current tick without performing it."""
        updates: dict[str, object] = {}
        for buckets in self._buckets:
            updates.update(buckets.pending_updates(current_time))

        return updates

    def commit_pending(self, current_time: int) -> None:
        """Advances every period past the pending write, which has just landed."""
        for buckets in self._buckets:
            buckets.commit_pending(current_time)

    def cache_pending(self, current_time: int) -> None:
        """Gives up on the pending write, holding whatever closed in each period's cache."""
        for buckets in self._buckets:
            buckets.cache_pending(current_time)


def commit_tick(
    database: FirebaseDatabase,
    periods: dict[str, SensorPeriods],
    readings: dict[str, float],
    commit_time: int,
) -> bool:
    """Commits every sensor's latest reading and every bucket due this tick in one request.

    One PATCH rooted at the telemetry node, rather than one per bucket plus one for the
    instants. An ordinary tick has no bucket due and carries nothing but the four
    instants, so it is the request it always was; the startup tick, which owes a gap
    marker in all twenty-four periods, goes from twenty-five requests to one, and so does
    a recovery that flushes several periods at once.

    Batching them also makes the tick atomic. The first instants and the startup markers
    now land together or not at all, so the database is never left holding a sensor whose
    history has been written but which has never reported a reading.
    """
    logger = logging.getLogger(__name__)

    updates: dict[str, object] = {
        f"{sensor_id}/last_instant": {
            "timestamp": commit_time,
            "value": _as_float(value),
        }
        for sensor_id, value in readings.items()
    }
    for sensor_periods in periods.values():
        updates.update(sensor_periods.pending_updates(commit_time))

    if not updates:
        return True

    try:
        database.telemetry_aquarium_node.update(updates, database.device_token)
    except RequestException as error:
        for sensor_periods in periods.values():
            sensor_periods.cache_pending(commit_time)

        logger.warning(
            "TICK Failed to commit %d paths at %d: %s",
            len(updates),
            commit_time,
            error,
        )
        return False

    for sensor_periods in periods.values():
        sensor_periods.commit_pending(commit_time)

    for sensor_id, value in readings.items():
        logger.info(
            "INSTANT[%s] Committed %f at %d",
            sensor_id,
            value,
            commit_time,
        )

    return True
