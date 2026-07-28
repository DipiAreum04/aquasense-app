#include "telemetry_manager.hpp"

// TODO: The user ID and aquarium ID are hardcoded to one test account for the demo, so every
// board uploads into that same node. Wire these to the real signed-in user and the paired
// aquarium (through PairingData / BLE provisioning) in sprint 3. Note the aquariumId
// parameter below is ignored right now and should be used once this is hooked up properly.
TelemetryManager::TelemetryManager(WiFiFirebase& firebase, const char* aquariumId)
    : _firebase(firebase), uid("R7q2RW7bIoZccuFezvYYgrcBwrU2"), aqId("8DiVHQV9CNTkffiT4u9JDHObVrE2"), elapsedSeconds(0) {}

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

        // First-time initialization
        if (lastBucketEpoch[r] == 0) {
            lastBucketEpoch[r] = epoch;
        }

        // Check if enough real time has passed
        if (epoch - lastBucketEpoch[r] >= interval) {

            temperatureSeries.pushToResolution(r, temperature, epoch);
            uploadResolution("temperature", RESOLUTIONS[r], temperatureSeries.getBucketSeries(r));

            waterLevelSeries.pushToResolution(r, waterLevel, epoch);
            uploadResolution("water_level", RESOLUTIONS[r], waterLevelSeries.getBucketSeries(r));

            dissolvedSolidsSeries.pushToResolution(r, dissolvedSolids, epoch);
            uploadResolution("dissolved_solids", RESOLUTIONS[r], dissolvedSolidsSeries.getBucketSeries(r));

            phLevelSeries.pushToResolution(r, phLevel, epoch);
            uploadResolution("ph_level", RESOLUTIONS[r], phLevelSeries.getBucketSeries(r));

            lastBucketEpoch[r] += interval;
        }
    }
}

void TelemetryManager::uploadInstant(const char* sensorName, const String& instantJson) {
    String path =  String(uid) + "/" "telemetry" + "/" + String(aqId) + "/" + sensorName + "/last_instant";
    _firebase.sendJSON(path.c_str(), instantJson);
}

void TelemetryManager::uploadResolution(const char* sensorName, const Resolution& res, const BucketSeries& bucket) {
    String path = String(uid) + "/" "telemetry" + "/" + String(aqId) + "/" + sensorName + "/" + res.name;
    _firebase.sendJSON(path.c_str(), bucket.toJsonObjectConst());
}
