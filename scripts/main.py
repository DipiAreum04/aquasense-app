"""__main__"""

import logging
from time import time, sleep
from random import choice

from simulations import (
    SensorPeriods,
    Sensor,
    FirebaseDatabase,
    ThresholdPoller,
    commit_tick,
)


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.DEBUG,
        format="[%(asctime)s.%(msecs)03d][%(filename)s::%(funcName)s][%(levelname)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    logger = logging.getLogger(__name__)

    database = FirebaseDatabase()

    sensor_temperature = Sensor("temperature", 16, 26)
    sensor_water_level = Sensor("water_level", 1, 1)
    sensor_dissolved_solids = Sensor("dissolved_solids", 50, 300)
    sensor_ph_level = Sensor("ph_level", 7, 10.5)
    sensors = (
        sensor_temperature,
        sensor_water_level,
        sensor_dissolved_solids,
        sensor_ph_level,
    )

    start_time = int(time())
    thresholds = ThresholdPoller(database, sensors)
    periods = {
        sensor.name: SensorPeriods(database, sensor.name, start_time)
        for sensor in sensors
    }

    testable_sensors = [sensor_temperature, sensor_dissolved_solids, sensor_ph_level]
    test_start_time = start_time
    sensor_under_test = None
    TEST_PERIOD_SECS = 60

    while True:
        current_time = int(time())
        if current_time-test_start_time > TEST_PERIOD_SECS:
            test_start_time = current_time
            if sensor_under_test is None:
                sensor_under_test = choice(testable_sensors)
                sensor_under_test.begin_sweep_test(TEST_PERIOD_SECS)
                logger.info("Beginning sweep test for %s", sensor_under_test.name)
            elif sensor_under_test.sweep_test_active:
                sensor_under_test.stop_tests()
                sensor_under_test = choice(testable_sensors)
                sensor_under_test.begin_spike_test()
                logger.info("Beginning spike test for %s", sensor_under_test.name)
            elif sensor_under_test.spike_test_active:
                sensor_under_test.stop_tests()
                sensor_under_test = choice(testable_sensors)
                sensor_under_test.begin_offline_test()
                logger.info("Beginning offline test for %s", sensor_under_test.name)
            else:
                sensor_under_test.stop_tests()
                sensor_under_test = None
                logger.info("Test epoch ended, returning to normal operation.")

        database.ensure_fresh_token(current_time)
        thresholds.poll(current_time)

        readings = {sensor.name: sensor.measure() for sensor in sensors}
        for sensor_id, sensor_periods in periods.items():
            sensor_periods.account(readings[sensor_id])

        commit_tick(database, periods, readings, current_time)

        sleep(1)
