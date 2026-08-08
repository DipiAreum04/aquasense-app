#include "BLEWifiSetup.hpp"

#include "pairing_store.hpp"

BLEWifiSetup::BLEWifiSetup() {}

bool BLEWifiSetup::begin(const char* deviceUid) {
    if (!BLE.begin()) {
        Serial.println("BLE failed to start.");
        return false;
    }

    /* The advertising packet is capped at 31 bytes and this one is nearly full: 3 for
     * the flags and 18 for the 128-bit service UUID leave 10, of which 2 go on the
     * name's own header. Eight characters is the whole budget.
     *
     * Overrunning it does not fail loudly - the stack drops whatever no longer fits,
     * and if that is the service UUID then the app's scan filter stops matching and
     * the board becomes invisible. Deriving the name from the tail of the UID keeps it
     * inside the budget and unique per board, so several hubs on a bench are still
     * distinguishable in the app's picker.
     */
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

    // Both readable the moment the app connects: it reads the UID before writing
    // anything, and subscribes to status before that.
    _uidChar.writeValue(String(deviceUid));
    _statusChar.writeValue("WAITING");

    BLE.advertise();
    _active = true;

    Serial.print("Advertising for pairing as ");
    Serial.println(advertisedName);
    return true;
}

/**
 * Buffers each field as it arrives and acts only on commit.
 *
 * BLE writes are independent operations: each one lands separately and any one of
 * them can fail on its own. BLE-01 treated the password write as the trigger, which
 * worked when the password was the last of two fields. With three it breaks badly -
 * if the owner UID write fails but the password succeeds, the board would save an
 * incomplete set, join Wi-Fi with nobody to write under, and have no way to say so
 * because BLE is gone by then. That failure is silent, permanent, and looks exactly
 * like a wrong Wi-Fi password.
 *
 * The commit characteristic is the app saying "all of it is in, act now". Its value
 * is meaningless - the app sends "1" - only the write itself matters.
 */
bool BLEWifiSetup::poll() {
    if (!_active) {
        return false;
    }

    // Servicing the stack. Returns a null device when nobody is connected, which is
    // the normal case while advertising.
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
        // Stay advertising rather than half-provisioning. The app's exchange will time
        // out and it will offer the user a retry, which is recoverable; a board that
        // acted on a partial set would not be.
        Serial.println("Commit arrived with an incomplete credential set. Ignoring.");
        return false;
    }

    if (!PairingStore::save(_ssid, _password, _ownerUid)) {
        Serial.println("Could not store the pairing. Staying in pairing mode.");
        return false;
    }

    _statusChar.writeValue("RECEIVED");
    delay(NOTIFY_DRAIN_MS);

    // Down before the caller touches Wi-Fi - the two cannot share the antenna.
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

/**
 * Whether all three fields arrived and are usable.
 *
 * The owner UID is length-checked rather than merely tested for emptiness: Firebase
 * UIDs are always 28 characters, so anything else means a truncated or corrupted
 * write. Storing one would build a telemetry path the security rules reject on every
 * write, and nothing in the system would report why.
 *
 * The Wi-Fi password is deliberately not checked. Open networks have none, and this
 * is not the place to decide the user typed the wrong one - only the router can say
 * that, and the recovery for it already exists.
 */
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
