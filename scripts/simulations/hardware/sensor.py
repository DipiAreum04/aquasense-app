"""Provides the `Sensor` class."""

from abc import ABC
from time import time
import numpy as np

from simulations.hardware.consts import OFFLINE_VALUE

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
        self._alpha = 0.1
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
        return self._alpha == 1.0

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
        """Begin a spike test with maximum alpha."""
        self.stop_tests()
        self._alpha = 1.0

    def begin_offline_test(self) -> None:
        """Begin an offline test."""
        self.stop_tests()
        self._disabled = True

    def stop_tests(self) -> None:
        """Stop any ongoing tests."""
        self._sweep_period_secs = 0
        self._sweep_start_time = 0
        self._disabled = False
        self._alpha = 0.1
        if self._last_output is not None:
            self._previous = self._last_output
            self._last_output = None

    def measure(self) -> float:
        """Return sensor measurement at this instant."""
        if self._disabled:
            return OFFLINE_VALUE

        measurement = self._rng.normal(loc=self._mean, scale=self._std_dev)
        if self._previous is None:
            self._previous = measurement

        self._previous = self._alpha*measurement + (1-self._alpha)*self._previous
        if self._sweep_period_secs > 0:
            self._last_output = self._previous + self._sweep_offset(
                np.sin(2*np.pi * (time()-self._sweep_start_time)/self._sweep_period_secs)
            )
            return self._last_output
        return self._previous

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
