"""Provides the `ThresholdPoller` class."""

import logging

from requests.exceptions import RequestException

from simulations.database.firebase_database import FirebaseDatabase
from simulations.hardware.sensor import Sensor


_POLL_PERIOD_SECS = 30


def _band_tuning(band: object) -> tuple[float, float, float, float] | None:
    """Read the tuning a sensor should take out of one threshold band.

    A sensor sits inside the safe bounds, so a tank left alone reads normal, and sweeps out
    past the warning ones, so a sweep test climbs through warning into critical at each end
    instead of stopping where safe does.

    A band is whatever the app last wrote, so it is treated as untrusted: anything missing,
    non-numeric or out of order yields nothing rather than a tuning that would leave the
    sensor emitting `nan` or sweeping inwards. Its caller falls back to the sensor's
    built-in range in that case.
    """
    if not isinstance(band, dict):
        return None

    try:
        warn_low = float(band["warn_low"])
        safe_low = float(band["safe_low"])
        safe_high = float(band["safe_high"])
        warn_high = float(band["warn_high"])
    except (KeyError, TypeError, ValueError):
        return None

    if not warn_low <= safe_low <= safe_high <= warn_high:
        return None

    return safe_low, safe_high, warn_low, warn_high


class ThresholdPoller:
    """Keeps sensors tuned to the thresholds their aquarium is configured with.

    The bands belong to the app and can change at any moment, so they are re-read on a
    period rather than sampled once at startup. Firebase can push the changes over a
    stream instead, but pyrebase bakes the token into the stream URL and never refreshes
    it, so the connection would have to be rebuilt around every re-authentication, and its
    callbacks arrive on a thread of their own that the loop would then have to synchronise
    against. Four numbers fetched twice a minute cost far less than the tick already
    spends, so the poll stays on the loop's own thread.
    """

    def __init__(self, database: FirebaseDatabase, sensors: tuple[Sensor, ...]) -> None:
        self._database = database
        self._sensors = sensors
        self._defaults = {sensor.name: sensor.tuning for sensor in sensors}
        self._poll_at = 0

    def poll(self, current_time: int) -> None:
        """Re-reads the thresholds once the poll period is up and retunes every sensor.

        A no-op on almost every tick, so the caller can hand it the loop's clock
        unconditionally rather than keeping a schedule of its own. A read that fails leaves
        every sensor tuned as it is and is retried on the next period, since a momentary
        network fault says nothing about what the thresholds are.
        """
        logger = logging.getLogger(__name__)

        if current_time < self._poll_at:
            return
        self._poll_at = current_time + _POLL_PERIOD_SECS

        try:
            thresholds = self._database.read_thresholds()
        except RequestException as error:
            logger.warning("THRESHOLDS Failed to read at %d: %s", current_time, error)
            return

        for sensor in self._sensors:
            self._retune(sensor, _band_tuning(thresholds.get(sensor.name)))

    def _retune(
        self,
        sensor: Sensor,
        tuning: tuple[float, float, float, float] | None,
    ) -> None:
        """Moves one sensor onto `tuning`, or back onto its built-in range without one.

        Falling back rather than holding the last band means a threshold the app deletes is
        undone here too, the same way the app itself returns to its own defaults.
        """
        logger = logging.getLogger(__name__)

        applied = self._defaults[sensor.name] if tuning is None else tuning
        if applied == sensor.tuning:
            return

        previous_low, previous_high, _, _ = sensor.tuning
        low, high, warn_low, warn_high = applied
        sensor.retune(*applied)
        logger.info(
            "THRESHOLDS[%s] Retuned from %g..%g to %g..%g, sweeping past %g..%g",
            sensor.name,
            previous_low,
            previous_high,
            low,
            high,
            warn_low,
            warn_high,
        )
