#include "BLEWifiSetup.hpp"

#include "pairing_store.hpp"

BLEWifiSetup::BLEWifiSetup() {}

bool BLEWifiSetup::begin(const char* deviceUid) {
    if (!BLE.begin()) {
        Serial.println("BLE failed to start.");
        return false;
    }

    String uid = String(deviceUid);
    String suffix = uid.length() >= UID_SUFFIX_LENGTH
            ? uid.substring(uid.length() - UID_SUFFIX_LENGTH)
            : uid;
    String advertisedName = "AQS-" + suffix;

    BLE.setLocalName(advertisedName.c_str());
    BLE.setAdvertisedService(_service);

    _service.addCharacteristic(_uidChar);
    _service.addCharacteristic(_ssidChar);
    _service.addCharacteristic(_passwordChar);
    _service.addCharacteristic(_ownerUidChar);
    _service.addCharacteristic(_commitChar);
    _service.addCharacteristic(_statusChar);
    BLE.addService(_service);

    _uidChar.writeValue(String(deviceUid));
    _statusChar.writeValue("WAITING");

    BLE.advertise();
    _active = true;

    Serial.print("Advertising for pairing as ");
    Serial.println(advertisedName);
    return true;
}

bool BLEWifiSetup::poll() {
    if (!_active) {
        return false;
    }

    BLEDevice central = BLE.central();
    if (!central) {
        return false;
    }

    if (_ssidChar.written()) {
        _ssid = _ssidChar.value();
        Serial.println("Received SSID.");
    }
    if (_passwordChar.written()) {
        _password = _passwordChar.value();
        Serial.println("Received Wi-Fi password.");
    }
    if (_ownerUidChar.written()) {
        _ownerUid = _ownerUidChar.value();
        Serial.println("Received owner UID.");
    }

    if (!_commitChar.written()) {
        return false;
    }

    if (!hasCompleteSet()) {
        Serial.println("Commit arrived with an incomplete credential set. Ignoring.");
        return false;
    }

    if (!PairingStore::save(_ssid, _password, _ownerUid)) {
        Serial.println("Could not store the pairing. Staying in pairing mode.");
        return false;
    }

    _statusChar.writeValue("RECEIVED");
    delay(NOTIFY_DRAIN_MS);

    end();

    Serial.println("Pairing stored. Switching the radio over to Wi-Fi.");
    return true;
}

void BLEWifiSetup::end() {
    if (!_active) {
        return;
    }
    _active = false;

    BLE.disconnect();
    BLE.end();
    delay(RADIO_SWITCH_MS);
}

bool BLEWifiSetup::hasCompleteSet() const {
    if (_ssid.length() == 0) {
        Serial.println("  missing: SSID");
        return false;
    }
    if (_ownerUid.length() != OWNER_UID_LENGTH) {
        Serial.print("  owner UID is ");
        Serial.print(_ownerUid.length());
        Serial.println(" characters, expected 28");
        return false;
    }
    return true;
}
