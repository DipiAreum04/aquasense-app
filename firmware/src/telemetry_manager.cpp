#include "telemetry_manager.hpp"
#include "consts.hpp"
#include "sensor_sanitizer.hpp"

TelemetryManager::TelemetryManager(WiFiFirebase& firebase)
    : _firebase(firebase),
      _periodsTemperature("temperature"),
      _periodsWaterLevel("water_level"),
      _periodsDissolvedSolids("dissolved_solids"),
      _periodsPhLevel("ph_level") {}

void TelemetryManager::collect(Periods** out) {
    out[0] = &_periodsTemperature;
    out[1] = &_periodsWaterLevel;
    out[2] = &_periodsDissolvedSolids;
    out[3] = &_periodsPhLevel;
}

bool TelemetryManager::tick(
    unsigned long epoch,
    bool linkUp,
    float temperature,
    float waterLevel,
    float dissolvedSolids,
    float phLevel
) {
    const float readings[SENSOR_COUNT] = {
        sanitizeSensorValue(SENSOR_TEMPERATURE, temperature),
        sanitizeSensorValue(SENSOR_WATER_LEVEL, waterLevel),
        sanitizeSensorValue(SENSOR_TDS, dissolvedSolids),
        sanitizeSensorValue(SENSOR_PH, phLevel),
    };

    Periods* sensors[SENSOR_COUNT];
    collect(sensors);

    bool online = linkUp && _firebase.ensureFreshToken();
    if (online) {
        online = resumeSensors(sensors, epoch);
    }

    if (!allResumed()) {
        return false;
    }

    for (int i = 0; i < SENSOR_COUNT; i++) {
        sensors[i]->account(readings[i]);
    }

    return commit(sensors, readings, epoch, online);
}

bool TelemetryManager::resumeSensors(Periods** sensors, unsigned long currentTime) {
    for (int i = 0; i < SENSOR_COUNT; i++) {
        if (_resumed[i]) {
            continue;
        }

        _resumed[i] = sensors[i]->resume(_firebase, currentTime);
        if (!_resumed[i]) {
            return false;
        }
    }

    return true;
}

bool TelemetryManager::allResumed() const {
    for (int i = 0; i < SENSOR_COUNT; i++) {
        if (!_resumed[i]) {
            return false;
        }
    }

    return true;
}

bool TelemetryManager::commit(
    Periods** sensors,
    const float* readings,
    unsigned long commitTime,
    bool online
) {
    SensorInstant instants[SENSOR_COUNT];
    for (int i = 0; i < SENSOR_COUNT; i++) {
        instants[i].sensorId = sensors[i]->kind().c_str();
        instants[i].value = readings[i];
    }

    int count = 0;
    for (int i = 0; i < SENSOR_COUNT; i++) {
        count += sensors[i]->pendingUpdates(
            commitTime,
            _updates + count,
            MAX_PENDING_UPDATES - count
        );
    }

    if (
        online
        && _firebase.commitTick(instants, SENSOR_COUNT, _updates, count, commitTime)
    ) {
        for (int i = 0; i < SENSOR_COUNT; i++) {
            sensors[i]->commitPending(commitTime);
        }

        for (int i = 0; i < SENSOR_COUNT; i++) {
            Serial.println(
                "INSTANT["+sensors[i]->kind()+"] Committed "+String(readings[i], 7)+
                " at "+String(commitTime)
            );
        }

        return true;
    }

    for (int i = 0; i < SENSOR_COUNT; i++) {
        sensors[i]->cachePending(commitTime);
    }

    Serial.println(
        "TICK Failed to commit "+
        String(WiFiFirebase::pathCount(SENSOR_COUNT, _updates, count))+
        " paths at "+String(commitTime)
    );

    return false;
}
