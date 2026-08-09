"""Provides the `Sensor` class."""

from abc import ABC
from time import time
import numpy as np

from simulations.hardware.consts import OFFLINE_VALUE

_SMOOTHING_ALPHA = 0.1

_SPIKE_STAGE_COUNT = 8


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
        self._spike_period_secs = 0
        self._spike_start_time = 0
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
        return self._spike_period_secs > 0

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

    def begin_spike_test(self, period_secs: int) -> None:
        """Begin a spike test with the given period."""
        self.stop_tests()
        self._spike_period_secs = period_secs
        self._spike_start_time = time()

    def begin_offline_test(self) -> None:
        """Begin an offline test."""
        self.stop_tests()
        self._disabled = True

    def stop_tests(self) -> None:
        """Stop any ongoing tests."""
        self._sweep_period_secs = 0
        self._sweep_start_time = 0
        self._disabled = False
        self._spike_period_secs = 0
        self._spike_start_time = 0
        if self._last_output is not None:
            self._previous = self._last_output
            self._last_output = None

    def measure(self) -> float:
        """Return sensor measurement at this instant."""
        if self._disabled:
            return OFFLINE_VALUE

        if self._spike_period_secs > 0:
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
        """Return where the spike test's staircase stands at this point in its cycle.

        The cycle is divided into equal stages, each jumped to and then held for the rest of
        its stage, so every step reads as a jump followed by a plateau rather than a single
        reading that could be mistaken for noise.

        Deliberately not drawn from the distribution the ordinary readings come from. A
        draw wide enough to jump between bands has a tail outside whichever one it is
        meant to be sitting in, so the plateaus would not hold a single status.
        """
        stage_secs = self._spike_period_secs / _SPIKE_STAGE_COUNT
        elapsed = time() - self._spike_start_time
        stage = min(max(int(elapsed/stage_secs), 0), _SPIKE_STAGE_COUNT-1)
        self._previous = self._spike_levels()[stage]
        return self._previous

    def _spike_levels(self) -> tuple[float, ...]:
        """Return the level each stage of the spike test holds, in order.

        The first half climbs: the top of normal, the top of warning, past the warning bound
        into critical, then back to normal. The second half repeats that downwards. Each
        level is held for a stage, so the app sees the status settle at every step instead
        of catching it mid-move.

        Every step is stretched as far as the band allows, since a step the app does not
        read as a jump is a step it never sees. Critical clears the warning bound by the
        whole width of normal rather than by a standard deviation, which is a sixth of it.
        The two returns to normal go to the end of the range furthest from the level that
        follows, so the return and the step after it are both as long as they can be, rather
        than to the middle, which halves both.

        Three of the steps are the band's own to give and cannot be stretched: normal to
        warning at each end, whose length is the width of the warning zone, and the crossing
        from the top of normal to the bottom, whose length is the width of normal. A band
        narrower there than the app's spike delta cannot produce a jump on those steps, only
        a change of status.

        Read fresh on every tick rather than fixed when the test starts, so a band the
        thresholds poller changes mid-test moves the staircase with it.
        """
        overshoot = self._high-self._low
        return (
            self._high,
            self._warn_high,
            self._warn_high + overshoot,
            self._high,
            self._low,
            self._warn_low,
            self._warn_low - overshoot,
            self._low,
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
