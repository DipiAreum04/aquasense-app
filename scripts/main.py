"""__main__"""

import logging
from time import time

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

    sensor_temperature = Sensor(16, 26)
    sensor_water_level = Sensor(1, 1)
    sensor_dissolved_solids = Sensor(50, 300)
    sensor_ph_level = Sensor(7, 10.5)

    last_token_refresh = int(time())
    periods_temperature = Periods(database, "temperature", last_token_refresh)
    periods_water_level = Periods(database, "water_level", last_token_refresh)
    periods_dissolved_solids = Periods(database, "dissolved_solids", last_token_refresh)
    periods_ph_level = Periods(database, "ph_level", last_token_refresh)

    while True:
        current_time = int(time())
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
