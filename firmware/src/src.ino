#include <Arduino.h>
#include <WiFiS3.h>
#include <EEPROM.h>

#include "BLEWifiSetup.hpp"
#include "temp_sensor.hpp"
#include "ph_sensor.hpp"
#include "tds_sensor.hpp"
#include "water_level.hpp"
#include "wifi_firebase.hpp"
#include "ntp_time.hpp"
#include "pairing_data.hpp"
#include "pairing_store.hpp"
#include "boot_counter.hpp"
#include "telemetry_manager.hpp"

static const unsigned long WIFI_CONNECT_TIMEOUT_MS = 15000;
static const unsigned long WIFI_RECONNECT_TIMEOUT_MS = 8000;
static const unsigned long RECONNECT_INTERVAL_MS = 30000;

static const int MAX_WIFI_FAILURES = 5;

static const unsigned long TICK_INTERVAL_MS = 1000;

static bool connectWifi(const String& ssid, const String& password, unsigned long timeoutMs) {
    Serial.print("Joining Wi-Fi network: ");
    Serial.println(ssid);

    WiFi.setTimeout(timeoutMs);
    WiFi.begin(ssid.c_str(), password.c_str());

    if (WiFi.status() == WL_CONNECTED) {
        Serial.print("Connected. IP: ");
        Serial.println(WiFi.localIP());
        return true;
    }

    Serial.println("Wi-Fi connection failed.");
    return false;
}

static void signalFactoryReset() {
    pinMode(LED_BUILTIN, OUTPUT);
    for (int i = 0; i < 6; i++) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(120);
        digitalWrite(LED_BUILTIN, LOW);
        delay(120);
    }
}

static void returnToPairing(const char* reason) {
    Serial.println(reason);
    Serial.println("Clearing the stored pairing and restarting to advertise.");
    PairingStore::clear();
    delay(200);
    NVIC_SystemReset();
}

void run() {
    TempSensor tempSensor(4);
    PhSensor   phSensor(A0, 1.82, 7.33, 0.001);
    TdsSensor  tdsSensor(A1);
    WaterLevel waterSensor(7);

    PairingData pairingData;
    WiFiFirebase cloud(
        pairingData.getDbUrl(),
        pairingData.getWebApiKey(),
        pairingData.getDeviceEmail(),
        pairingData.getDevicePassword()
    );
    TelemetryManager telemetry(cloud);

    BLEWifiSetup bleWifi;
    NTPTime ntp;

    Serial.begin(115200);
    delay(500);
    EEPROM.begin();

    Serial.println("AquaSense hub starting...");

    BootCounter::begin();
    if (BootCounter::factoryResetRequested()) {
        Serial.println("Power cycled "+String((int) BootCounter::TRIGGER_COUNT)+
                       " times in a row. Forgetting the stored pairing.");
        PairingStore::clear();
        BootCounter::clear();
        signalFactoryReset();
    }

    cloud.setExpectedDeviceUid(String(pairingData.getDeviceUid()));

    bool online = false;
    String ssid;
    String wifiPassword;
    String ownerUid;

    if (PairingStore::load(ssid, wifiPassword, ownerUid)) {
        Serial.println("Found a stored pairing.");
        if (connectWifi(ssid, wifiPassword, WIFI_CONNECT_TIMEOUT_MS)) {
            cloud.setOwnerUid(ownerUid);
            online = true;
        } else {
            returnToPairing("Stored Wi-Fi credentials did not work.");
        }
    } else {
        Serial.println("No usable pairing stored.");
        if (!bleWifi.begin(pairingData.getDeviceUid())) {
            Serial.println("Cannot advertise, so this board cannot be paired. Restarting.");
            delay(2000);
            NVIC_SystemReset();
        }
    }

    tempSensor.begin();
    waterSensor.begin();
    analogReadResolution(14);

    if (online) {
        ntp.begin();
    }

    unsigned long lastTickMillis = millis() - TICK_INTERVAL_MS;
    unsigned long lastReconnectMillis = millis() - RECONNECT_INTERVAL_MS;
    int wifiFailures = 0;

    while (true) {
        BootCounter::settle();

        if (!online) {
            if (!bleWifi.poll()) {
                delay(50);
                continue;
            }

            ssid = bleWifi.ssid();
            wifiPassword = bleWifi.password();
            ownerUid = bleWifi.ownerUid();

            if (!connectWifi(ssid, wifiPassword, WIFI_CONNECT_TIMEOUT_MS)) {
                returnToPairing("The Wi-Fi credentials just received did not work.");
            }

            cloud.setOwnerUid(ownerUid);
            ntp.begin();
            online = true;
            lastTickMillis = millis() - TICK_INTERVAL_MS;
            Serial.println("Paired and online. Publishing telemetry.");
            continue;
        }

        unsigned long nowMillis = millis();
        if (nowMillis - lastTickMillis < TICK_INTERVAL_MS) {
            continue;
        }
        lastTickMillis = nowMillis;

        bool linkUp = (WiFi.status() == WL_CONNECTED);
        if (!linkUp && millis() - lastReconnectMillis >= RECONNECT_INTERVAL_MS) {
            lastReconnectMillis = millis();

            Serial.println("Wi-Fi lost. Reconnecting...");
            linkUp = connectWifi(ssid, wifiPassword, WIFI_RECONNECT_TIMEOUT_MS);
            if (linkUp) {
                wifiFailures = 0;
            } else if (++wifiFailures >= MAX_WIFI_FAILURES) {
                returnToPairing("Wi-Fi has been unreachable for several attempts.");
            }

            lastTickMillis = millis();
        }

        unsigned long epoch = (linkUp || ntp.hasSynced()) ? ntp.getEpoch() : 0;
        if (epoch == 0) {
            Serial.println("NTP time not yet available. Retrying...");
            continue;
        }

        float tempC           = tempSensor.readTemperatureC();
        float phValue         = phSensor.readPH(tempC);
        float tdsPpm          = tdsSensor.readTdsPpm();
        float waterLevelValue = waterSensor.isDetected() ? 1.0f : 0.0f;

        telemetry.tick(epoch, linkUp, tempC, waterLevelValue, tdsPpm, phValue);

        if (cloud.permissionRevoked()) {
            returnToPairing("This hub has been unpaired: the database no longer accepts it.");
        }
    }
}

void setup() {
    run();
}

void loop() {}
