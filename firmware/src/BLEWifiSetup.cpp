#include "BLEWifiSetup.h"

BLEWifiSetup::BLEWifiSetup() {}

void BLEWifiSetup::begin() {
    EEPROM.begin();

    if (!BLE.begin()) {
        Serial.println("BLE failed to start");
        return;
    }

    BLE.setLocalName("AquaSense_Setup");   // ← KEEP THIS NAME
    BLE.setAdvertisedService(wifiService);

    wifiService.addCharacteristic(ssidChar);
    wifiService.addCharacteristic(passChar);

    BLE.addService(wifiService);
    BLE.advertise();

    Serial.println("BLE WiFi Setup Ready");
    Serial.println("Use LightBlue:");
    Serial.println("1. Tap SSID → Value → Send");
    Serial.println("2. Tap Password → Value → Send");
}

void BLEWifiSetup::poll() {
    BLEDevice central = BLE.central();
    if (!central) return;

    if (ssidChar.written()) {
        Serial.print("SSID received: ");
        Serial.println(ssidChar.value());
    }

    if (passChar.written()) {
        Serial.print("Password received: ");
        Serial.println(passChar.value());

        saveCredentials(ssidChar.value(), passChar.value());

        Serial.println("Rebooting to connect...");
        delay(1500);
        NVIC_SystemReset();
    }
}

void BLEWifiSetup::saveCredentials(const String& ssid, const String& pass) {
    for (int i = 0; i < 32; i++) EEPROM.write(SSID_ADDR + i, 0);
    for (int i = 0; i < 64; i++) EEPROM.write(PASS_ADDR + i, 0);

    for (int i = 0; i < ssid.length() && i < 32; i++) EEPROM.write(SSID_ADDR + i, ssid[i]);
    for (int i = 0; i < pass.length() && i < 64; i++) EEPROM.write(PASS_ADDR + i, pass[i]);
}

void BLEWifiSetup::loadCredentials(String& ssid, String& pass) {
    char ssidBuf[33];
    char passBuf[65];

    for (int i = 0; i < 32; i++) ssidBuf[i] = EEPROM.read(SSID_ADDR + i);
    ssidBuf[32] = '\0';

    for (int i = 0; i < 64; i++) passBuf[i] = EEPROM.read(PASS_ADDR + i);
    passBuf[64] = '\0';

    ssid = String(ssidBuf);
    pass = String(passBuf);

    ssid.trim();
    pass.trim();
}

bool BLEWifiSetup::tryConnectStored() {
    String ssid, pass;
    loadCredentials(ssid, pass);

    if (ssid.length() == 0) return false;

    Serial.print("Trying WiFi: ");
    Serial.println(ssid);

    WiFi.begin(ssid.c_str(), pass.c_str());

    unsigned long start = millis();
    while (millis() - start < 8000) {
        if (WiFi.status() == WL_CONNECTED) {
            Serial.println("Connected!");
            Serial.print("IP: ");
            Serial.println(WiFi.localIP());
            return true;
        }
        delay(200);
    }

    Serial.println("Failed to connect.");
    return false;
}
