#include "telemetry_manager.h"

TelemetryManager::TelemetryManager(WiFiFirebase& firebase, const char* aquariumId)
    : _firebase(firebase), _aquariumId(aquariumId), elapsedSeconds(0) {}

void TelemetryManager::tick(unsigned long epoch,
    float temperature,
    float waterLevel,
    float dissolvedSolids,
    float phLevel) {

    SensorSpec sensors[] = {
        { "temperature",      &temperatureSeries },
        { "water_level",      &waterLevelSeries },
        { "dissolved_solids", &dissolvedSolidsSeries },
        { "ph_level",         &phLevelSeries }
    };
    float values[] = { temperature, waterLevel, dissolvedSolids, phLevel };
    const int sensorCount = sizeof(sensors) / sizeof(sensors[0]);

    for (int s = 0; s < sensorCount; s++) {
        sensors[s].series->updateInstant(values[s], epoch);
        uploadInstant(sensors[s].name, sensors[s].series->getInstantJson());
    }

    elapsedSeconds++;

    for (int r = 0; r < RESOLUTION_COUNT; r++) {
        unsigned long interval = RESOLUTIONS[r].bucketIntervalSeconds();
        if (interval == 0) continue;

        if (elapsedSeconds % interval == 0) {
            for (int s = 0; s < sensorCount; s++) {
                sensors[s].series->pushToResolution(r, values[s], epoch);
                uploadResolution(sensors[s].name, RESOLUTIONS[r], sensors[s].series->getBucketSeries(r));
            }
        }
    }
}

void TelemetryManager::uploadInstant(const char* sensorName, const String& instantJson) {
    String path = "AquariumId:" + String(_aquariumId) + "/" +"telemetry" +  "/" + sensorName + "/last_instant";
    _firebase.sendJSON(path.c_str(), instantJson);
}

void TelemetryManager::uploadResolution(const char* sensorName, const Resolution& res, const BucketSeries& bucket) {
    String path = "AquariumId:" + String(_aquariumId) + "/" +"telemetry" + "/" + sensorName + "/" + res.name;
    _firebase.sendJSON(path.c_str(), bucket.toJsonObject());
}