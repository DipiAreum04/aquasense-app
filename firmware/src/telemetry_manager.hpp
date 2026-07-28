#ifndef TELEMETRY_MANAGER_HPP
#define TELEMETRY_MANAGER_HPP

#include <Arduino.h>
#include "sensor_series.hpp"
#include "resolutions.hpp"
#include "wifi_firebase.hpp"

class TelemetryManager {
public:
    TelemetryManager(WiFiFirebase& firebase, const char* aquariumId);

    void tick(unsigned long epoch,
        float temperature,
        float waterLevel,
        float dissolvedSolids,
        float phLevel);

private:
    struct SensorSpec {
        const char* name;
        SensorSeries* series;
    };

    WiFiFirebase& _firebase;
    const char* aqId;
    const char* uid;

    SensorSeries temperatureSeries{SENSOR_TEMPERATURE};
    SensorSeries waterLevelSeries{SENSOR_WATER_LEVEL};
    SensorSeries dissolvedSolidsSeries{SENSOR_TDS};
    SensorSeries phLevelSeries{SENSOR_PH};

    unsigned long elapsedSeconds;
    unsigned long lastBucketEpoch[RESOLUTION_COUNT] = {0};

    void uploadInstant(const char* sensorName, const String& instantJson);
    void uploadResolution(const char* sensorName, const Resolution& res, const BucketSeries& bucket);
};

#endif
