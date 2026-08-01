"""Provides the `Sensor` class."""

from abc import ABC
import numpy as np

class Sensor(ABC):
    """Simulates a sensor."""

    def __init__(self, low: float, high: float) -> None:
        self._std_dev = (high-low)/6
        self._mean = (high+low)/2
        self._rng = np.random.default_rng()
        self._previous = None

    def measure(self, alpha: float = 0.1, offset: float = 0.0, enabled: bool = True) -> float:
        """Return sensor measurement at this instant."""
        if not enabled:
            return -2147483648

        measurement = self._rng.normal(loc=self._mean+offset, scale=self._std_dev)
        if self._previous is None:
            self._previous = measurement

        self._previous = alpha*measurement + (1-alpha)*self._previous
        return self._previous
