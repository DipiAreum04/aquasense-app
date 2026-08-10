"""Provides the `Sensor` class."""

from abc import ABC
from time import time
import numpy as np

from simulations.hardware.consts import OFFLINE_VALUE

_SMOOTHING_ALPHA = 0.1


class Sensor(ABC):
    """Simulates a sensor."""

    def __init__(self, name: str, low: float, high: float) -> None:
        self._name = name
        self._rng = np.random.default_rng()
        self._previous = None
        self._last_output = None
        self._sweep_period_secs = 0
        self._sweep_start_time = 0
        self._disabled = False
        self._spiking = False
        self._spike_stage = 0
        self.retune(low, high)

    @property
    def name(self) -> str:
        """Return the name of the sensor."""
        return self._name

    @property
    def tuning(self) -> tuple[float, float, float, float]:
        """Return the bounds readings sit between and the ones a sweep travels past."""
        return self._low, self._high, self._warn_low, self._warn_high

    @property
    def sweep_test_active(self) -> bool:
        """Return whether a sweep test is active."""
        return self._sweep_period_secs > 0

    @property
    def spike_test_active(self) -> bool:
        """Return whether a spike test is active."""
        return self._spiking

    def retune(
        self,
        low: float,
        high: float,
        warn_low: float | None = None,
        warn_high: float | None = None,
    ) -> None:
        """Draw future readings from a new range, sweeping out past new warning bounds.

        Readings are drawn to sit between `low` and `high` almost always, so a tank left
        alone reads normal, while a sweep test travels out past `warn_low` and `warn_high`
        so that it climbs through warning into critical at each end. The warning bounds
        default to the range itself, which is what a sensor with none configured sweeps
        between.

        Only the distribution moves. The smoothed value is left where it is, so a range
        that changes under a running sensor walks its readings across over the next few
        ticks instead of teleporting them, which is what a tank being brought to a new
        setpoint looks like, and a test already under way keeps running through it.
        """
        self._low = low
        self._high = high
        self._warn_low = low if warn_low is None else warn_low
        self._warn_high = high if warn_high is None else warn_high
        self._std_dev = (high-low)/6
        self._mean = (high+low)/2

    def begin_sweep_test(self, period_secs: int) -> None:
        """Begin a sweep test with the given period."""
        self.stop_tests()
        self._sweep_period_secs = period_secs
        self._sweep_start_time = time()

    def begin_spike_test(self) -> None:
        """Begin a spike test, stepping through its cycle one reading at a time."""
        self.stop_tests()
        self._spiking = True
        self._spike_stage = 0

    def begin_offline_test(self) -> None:
        """Begin an offline test."""
        self.stop_tests()
        self._disabled = True

    def stop_tests(self) -> None:
        """Stop any ongoing tests."""
        self._sweep_period_secs = 0
        self._sweep_start_time = 0
        self._disabled = False
        self._spiking = False
        if self._last_output is not None:
            self._previous = self._last_output
            self._last_output = None

    def measure(self) -> float:
        """Return sensor measurement at this instant."""
        if self._disabled:
            return OFFLINE_VALUE

        if self._spiking:
            return self._spike_measurement()

        measurement = self._rng.normal(loc=self._mean, scale=self._std_dev)
        if self._previous is None:
            self._previous = measurement

        self._previous = _SMOOTHING_ALPHA*measurement + (1-_SMOOTHING_ALPHA)*self._previous
        if self._sweep_period_secs > 0:
            self._last_output = self._previous + self._sweep_offset(
                np.sin(2*np.pi * (time()-self._sweep_start_time)/self._sweep_period_secs)
            )
            return self._last_output
        return self._previous

    def _spike_measurement(self) -> float:
        """Return the spike test's reading for this tick and step the cycle on.

        One level per reading rather than one held over a stretch of time, because a spike
        is a jump from one reading to the next: a level repeated across several ticks would
        be a single jump followed by readings that settle next to it and clear it again.

        Deliberately not drawn from the distribution the ordinary readings come from.
        Widening that draw until consecutive ones reliably clear the delta puts a tail
        outside the safe bounds, so the levels would not hold the status each is chosen for.
        """
        levels = self._spike_levels()
        self._previous = levels[self._spike_stage % len(levels)]
        self._spike_stage += 1
        return self._previous

    def _spike_levels(self) -> tuple[float, ...]:
        """Return the level each reading of the spike test takes, in cycle order.

        The cycle steps out into the high warning zone, back into normal, holds normal, then
        does the same downwards through the low warning zone: six readings, then round
        again.

        It is built around what the app makes of each step. A step out into a warning zone
        moves the reading out of range, so the card reads warning on the strength of the
        reading itself; the app no longer counts a jump that lands out of range as a spike.
        The step back is the spike: it lands in normal having moved further than any spike
        delta, so the card holds warning for that one reading. Repeating that same level
        then moves nothing, which returns the card to normal and shows the spike clearing.

        Each return goes to the end of normal furthest from the zone it came from, so the
        jump is the whole width of normal plus half a warning zone, which is as much as the
        band has to give. The reading after it sits at that same level exactly, so no jump
        can be read into it, and leaves the cycle at the end of normal nearest the zone it
        heads out to next.

        The warning levels sit in the middle of their zone rather than on a bound. A reading
        reaches the app through a single-precision field, and one sitting on a bound can
        round to the far side of it and read as critical, or as normal, instead.

        Read fresh on every tick rather than fixed when the test starts, so a band the
        thresholds poller changes mid-test moves the cycle with it.
        """
        return (
            (self._high+self._warn_high) / 2,
            self._low,
            self._low,
            (self._low+self._warn_low) / 2,
            self._high,
            self._high,
        )

    def _sweep_offset(self, phase: float) -> float:
        """Return how far from the smoothed value a sweep sits at this point in its cycle.

        Each half of the cycle is scaled to clear the warning bound it is heading for by a
        standard deviation, so the sweep reads critical at both extremes rather than
        hovering on a boundary, and clears each one by the same margin, so a band that sits
        much further out on one side does not drag the other side off the scale. A sensor
        with no warning bounds of its own therefore sweeps four standard deviations, being
        the three its range spans plus the margin.
        """
        bound = self._warn_high-self._mean if phase > 0 else self._mean-self._warn_low
        return (bound+self._std_dev) * phase
