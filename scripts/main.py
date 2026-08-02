"""__main__"""

import logging
from time import time
from random import choice

from simulations import Periods, Sensor, RealDatabase


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.DEBUG,
        format="[%(asctime)s.%(msecs)03d][%(filename)s::%(funcName)s][%(levelname)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    logging.getLogger("urllib3").setLevel(logging.WARNING)

    logger = logging.getLogger(__name__)

    database = RealDatabase()

    sensor_temperature = Sensor("temperature", 16, 26)
    sensor_water_level = Sensor("water_level", 1, 1)
    sensor_dissolved_solids = Sensor("dissolved_solids", 50, 300)
    sensor_ph_level = Sensor("ph_level", 7, 10.5)

    last_token_refresh = int(time())
    periods_temperature = Periods(database, "temperature", last_token_refresh)
    periods_water_level = Periods(database, "water_level", last_token_refresh)
    periods_dissolved_solids = Periods(database, "dissolved_solids", last_token_refresh)
    periods_ph_level = Periods(database, "ph_level", last_token_refresh)

    testable_sensors = [sensor_temperature, sensor_dissolved_solids, sensor_ph_level]
    test_start_time = last_token_refresh
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

        if current_time-last_token_refresh > 900:
            database.reauthenticate()
            last_token_refresh = current_time

        periods_temperature.account(
            database,
            sensor_temperature.measure(),
            current_time,
        )
        periods_water_level.account(
            database,
            sensor_water_level.measure(),
            current_time,
        )
        periods_dissolved_solids.account(
            database,
            sensor_dissolved_solids.measure(),
            current_time,
        )
        periods_ph_level.account(
            database,
            sensor_ph_level.measure(),
            current_time,
        )
