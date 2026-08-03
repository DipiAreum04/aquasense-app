#include "telemetry_manager.hpp"
#include "consts.hpp"
#include "elapsed.hpp"
#include "sensor_sanitizer.hpp"

TelemetryManager::TelemetryManager(WiFiFirebase& firebase)
    : _firebase(firebase),
      _periodsTemperature("temperature"),
      _periodsWaterLevel("water_level"),
      _periodsDissolvedSolids("dissolved_solids"),
      _periodsPhLevel("ph_level") {}

/**
 * Private helper which gathers the sensors into an array so they can be walked
 * rather than spelled out one by one. The order matches _synced.
 *
 * @param out An array of SENSOR_COUNT pointers to fill in.
 */
void TelemetryManager::collect(Periods** out) {
    out[0] = &_periodsTemperature;
    out[1] = &_periodsWaterLevel;
    out[2] = &_periodsDissolvedSolids;
    out[3] = &_periodsPhLevel;
}

/**
 * Private helper which marks every sensor as needing to bootstrap again, so that
 * the next tick compares the clock against last_instant and records the outage as
 * a gap.
 *
 * Only the flags are cleared here. Whether a period has actually missed a bucket
 * depends on last_instant, and that is Periods::sync's question to answer.
 */
void TelemetryManager::markUnsynced() {
    for (int i = 0; i < SENSOR_COUNT; i++) {
        _synced[i] = false;
    }
}

/**
 * Ticks the telemetry manager, which will account for the given sensor values and commit them to the database if necessary.
 *
 * @param epoch The current epoch time in seconds.
 * @param temperature The current temperature value.
 * @param waterLevel The current water level value.
 * @param dissolvedSolids The current dissolved solids value.
 * @param phLevel The current pH level value.
 * @return true if the operation was successful, false if the Firebase sign-in/refresh
 *   failed or if any database operation failed. Nothing is retried in place: every
 *   path here gives up on the first failure and leaves it to the next tick, so that
 *   one unreachable database cannot hold the main loop for more than a single
 *   request timeout.
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

    /* Notice an outage from how long it has been since anything actually reached the
     * database, rather than from the state of the WiFi link. The two are not the same
     * thing: a Firebase outage or a failed token refresh strands the device just as
     * thoroughly while the link stays up, and a drop shorter than a bucket is not
     * worth reacting to however loudly the radio reports it.
     *
     * Clearing _lastUploadEpoch is what stops this firing again on the next tick.
     * Without that latch it would re-fire for as long as the database stayed away,
     * and every re-sync burns a bucket in all six of a sensor's periods.
     */
    if (_lastUploadEpoch != 0 && secondsSince(epoch, _lastUploadEpoch) >= SHORTEST_BUCKET_SECS) {
        markUnsynced();
        _lastUploadEpoch = 0;
    }

    /* Each sensor remembers its own bootstrap, because re-running one is destructive.
     * sync() decides whether to write a gap marker by comparing now against
     * last_instant, and last_instant is only written once every sensor has synced -
     * so if a later sensor fails, this tick returns before writing it, and an
     * already-synced sensor asked to sync again still sees the old timestamp, still
     * concludes it was offline, and burns another bucket in each of its six periods.
     * A shared flag did exactly that, once per tick, for as long as the failing
     * sensor kept failing.
     */
    Periods* sensors[SENSOR_COUNT];
    collect(sensors);

    for (int i = 0; i < SENSOR_COUNT; i++) {
        if (_synced[i]) {
            continue;
        }
        // Give up on the first failure rather than letting the rest wait out their
        // own timeouts: the link is evidently down, and the tick is already lost.
        _synced[i] = sensors[i]->sync(_firebase, epoch);
        if (!_synced[i]) {
            return false;
        }
    }

    float temperatureValue = sanitizeSensorValue(SENSOR_TEMPERATURE, temperature);
    float waterLevelValue = sanitizeSensorValue(SENSOR_WATER_LEVEL, waterLevel);
    float dissolvedSolidsValue = sanitizeSensorValue(SENSOR_TDS, dissolvedSolids);
    float phLevelValue = sanitizeSensorValue(SENSOR_PH, phLevel);

    _periodsTemperature.accumulate(temperatureValue);
    _periodsWaterLevel.accumulate(waterLevelValue);
    _periodsDissolvedSolids.accumulate(dissolvedSolidsValue);
    _periodsPhLevel.accumulate(phLevelValue);

    // Every sensor's instant goes up in one request rather than one each: the tick
    // runs every second, and four separate uploads means four TLS handshakes.
    const SensorInstant instants[SENSOR_COUNT] = {
        { _periodsTemperature.kind().c_str(), temperatureValue },
        { _periodsWaterLevel.kind().c_str(), waterLevelValue },
        { _periodsDissolvedSolids.kind().c_str(), dissolvedSolidsValue },
        { _periodsPhLevel.kind().c_str(), phLevelValue },
    };

    if (!_firebase.commitInstants(instants, SENSOR_COUNT, epoch)) {
        return false;
    }
    _lastUploadEpoch = epoch;

    // Stop at the first failure rather than letting the rest wait out their own
    // timeouts: they all share one link, so a failure here means the next is very
    // likely to fail too, and an uncommitted bucket is retried on the next tick
    // anyway - its index does not advance until the write has landed.
    for (int i = 0; i < SENSOR_COUNT; i++) {
        if (!sensors[i]->commitBuckets(_firebase, epoch)) {
            return false;
        }
    }

    return true;
}
