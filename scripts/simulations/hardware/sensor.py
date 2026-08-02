"""Provides the `Sensor` class."""

from abc import ABC
from time import time
import numpy as np

from simulations.hardware.consts import OFFLINE_VALUE

class Sensor(ABC):
    """Simulates a sensor."""

    def __init__(self, name: str, low: float, high: float) -> None:
        self._name = name
        self._std_dev = (high-low)/6
        self._mean = (high+low)/2
        self._rng = np.random.default_rng()
        self._previous = None
        self._last_output = None
        self._sweep_period_secs = 0
        self._sweep_start_time = 0
        self._disabled = False
        self._alpha = 0.1

    @property
    def name(self) -> str:
        """Return the name of the sensor."""
        return self._name

    @property
    def sweep_test_active(self) -> bool:
        """Return whether a sweep test is active."""
        return self._sweep_period_secs > 0

    @property
    def spike_test_active(self) -> bool:
        """Return whether a spike test is active."""
        return self._alpha == 1.0

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
            self._last_output = self._previous + (
                4 * self._std_dev * np.sin(
                    2*np.pi * (time()-self._sweep_start_time)/self._sweep_period_secs
                )
            )
            return self._last_output
        return self._previous
