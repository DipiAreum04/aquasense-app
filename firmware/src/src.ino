#include <Arduino.h>
#include <WiFiS3.h>

#include "BLEWifiSetup.hpp"
#include "temp_sensor.hpp"
#include "ph_sensor.hpp"
#include "tds_sensor.hpp"
#include "water_level.hpp"
#include "wifi_firebase.hpp"
#include "ntp_time.hpp"
#include "pairing_data.hpp"
#include "telemetry_manager.hpp"

void run() {
    TempSensor tempSensor(4);
    PhSensor   phSensor(A0, 2.535, -1.70, 0.03);
    TdsSensor  tdsSensor(A1);
    WaterLevel waterSensor(7);

    BLEWifiSetup bleWifi;
    bool provisioningMode = false;

    PairingData pairingData;
    WiFiFirebase cloud(
        pairingData.getDbUrl(),
        pairingData.getWebApiKey(),
        pairingData.getDeviceEmail(),
        pairingData.getDevicePassword()
    );
    TelemetryManager telemetry(cloud);

    NTPTime ntp;

    unsigned long lastTickMillis = millis();
    const unsigned long TICK_INTERVAL_MS = 1000;

    Serial.begin(115200);
    delay(500); // cant lower it more than this

    Serial.println("Starting MULTI-SENSOR + Epoch + Firebase telemetry (BLE WiFi provisioning)...");

    bleWifi.begin();

    if (bleWifi.tryConnectStored()) {
        Serial.println("WiFi already configured via BLE.");
        provisioningMode = false;
    } else {
        Serial.println("Waiting for BLE WiFi credentials (AquaSense_Setup in LightBlue)...");
        provisioningMode = true;
    }

    tempSensor.begin();
    waterSensor.begin();
    analogReadResolution(14);

    ntp.begin();

    while (true) {
        bleWifi.poll();

        if (provisioningMode) {
            delay(200); // cant lower it more than this
            continue;
        }

        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("WiFi lost. Trying reconnect via stored BLE credentials...");

            if (bleWifi.tryConnectStored()) {
                Serial.println("WiFi reconnected.");
            } else {
                Serial.println("WiFi reconnect failed. Still waiting for valid credentials.");
                delay(1000); // give time before retrying the reconnect
                continue;
            }
        }

        unsigned long nowMillis = millis();
        if (nowMillis - lastTickMillis < TICK_INTERVAL_MS) {
            continue;
        }
        lastTickMillis = nowMillis;

        unsigned long epoch = ntp.getEpoch();
        if (epoch == 0) {
            Serial.println("NTP time not yet available. Retrying...");
            continue;
        }

        float tempC           = tempSensor.readTemperatureC();
        float phValue         = phSensor.readPH(tempC);
        float tdsPpm          = tdsSensor.readTdsPpm();
        float waterLevelValue = waterSensor.isDetected() ? 1.0f : 0.0f;

        Serial.print("Epoch: ");
        Serial.print(epoch);
        Serial.print(" | Temp: ");
        Serial.print(tempC);
        Serial.print(" C");
        Serial.print(" | pH: ");
        Serial.print(phValue);
        Serial.print(" | TDS: ");
        Serial.print(tdsPpm);
        Serial.print(" | Water Level: ");
        Serial.println(waterLevelValue);

        if (!telemetry.tick(epoch, tempC, waterLevelValue, tdsPpm, phValue)) {
            Serial.println("Could not reach the database. Will retry on the next tick.");
        }
    }
}

void setup() {
    run();
}

void loop() {}
