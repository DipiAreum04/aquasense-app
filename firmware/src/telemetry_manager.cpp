#include "telemetry_manager.hpp"
#include "sensor_sanitizer.hpp"

TelemetryManager::TelemetryManager(WiFiFirebase& firebase)
    : _firebase(firebase),
      _periodsTemperature("temperature"),
      _periodsWaterLevel("water_level"),
      _periodsDissolvedSolids("dissolved_solids"),
      _periodsPhLevel("ph_level") {}

/**
 * Ticks the telemetry manager, which will account for the given sensor values and commit them to the database if necessary.
 *
 * @param epoch The current epoch time in seconds.
 * @param temperature The current temperature value.
 * @param waterLevel The current water level value.
 * @param dissolvedSolids The current dissolved solids value.
 * @param phLevel The current pH level value.
 * @return true if the operation was successful, false if the Firebase sign-in/refresh
 *   failed or if any database operation failed all 3 attempts.
 */
bool TelemetryManager::tick(
    unsigned long epoch,
    float temperature,
    float waterLevel,
    float dissolvedSolids,
    float phLevel
) {
    if (!_firebase.ensureFreshToken()) {
        return false;
    }

    if (!_synced) {
        _synced = _periodsTemperature.sync(_firebase, epoch)
            && _periodsWaterLevel.sync(_firebase, epoch)
            && _periodsDissolvedSolids.sync(_firebase, epoch)
            && _periodsPhLevel.sync(_firebase, epoch);

        if (!_synced) {
            return false;
        }
    }

    bool ok = _periodsTemperature.account(
        _firebase, sanitizeSensorValue(SENSOR_TEMPERATURE, temperature), epoch
    );
    ok = _periodsWaterLevel.account(
        _firebase, sanitizeSensorValue(SENSOR_WATER_LEVEL, waterLevel), epoch
    ) && ok;
    ok = _periodsDissolvedSolids.account(
        _firebase, sanitizeSensorValue(SENSOR_TDS, dissolvedSolids), epoch
    ) && ok;
    ok = _periodsPhLevel.account(
        _firebase, sanitizeSensorValue(SENSOR_PH, phLevel), epoch
    ) && ok;

    return ok;
}
