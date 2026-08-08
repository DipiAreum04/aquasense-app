#ifndef BLE_WIFI_SETUP_HPP
#define BLE_WIFI_SETUP_HPP

#include <Arduino.h>
#include <ArduinoBLE.h>

/**
 * The provisioning GATT service the AquaSense app drives (BLE-02).
 *
 * Replaces the BLE-01 flow, which was two characteristics poked by hand through
 * LightBlue. Three things are new and the flow does not work without any of them:
 *
 *  - deviceUid, read by the app. This board's Firebase UID doubles as the aquarium's
 *    database key, so reading it is how the app learns which tank it is claiming. It
 *    is served from a compiled-in constant rather than from Firebase, because at this
 *    point the board is advertising and has no network to sign in over.
 *  - ownerUid, written by the app. Telemetry lives under the human user's subtree at
 *    /{ownerUid}/telemetry/{deviceUid}, so without it the board cannot build a single
 *    write path. This is the field that used to be hardcoded in wifi_firebase.cpp.
 *  - commit, written last. See poll() for why.
 *
 * Every UUID here must match PairingContract.java in the app, character for
 * character. A mismatch produces no error on either side: the app's scan filters on
 * the service UUID, so a wrong one simply means the board is never reported, which
 * looks exactly like hardware that is switched off.
 *
 * The UNO R4's BLE and Wi-Fi share one ESP32-S3 and one antenna and cannot run
 * together, so this class tears BLE down before the caller brings Wi-Fi up. That is
 * also why the board cannot report whether the credentials actually worked - by the
 * time it finds out, the radio it would have answered on is gone. The app confirms
 * success through Firebase instead.
 */
class BLEWifiSetup {
public:
    BLEWifiSetup();

    /**
     * Brings BLE up and starts advertising. Returns false if the radio would not
     * start, in which case the board cannot be paired at all this boot.
     *
     * deviceUid is copied into the read characteristic, so it need only outlive the
     * call itself.
     */
    bool begin(const char* deviceUid);

    /**
     * Services the BLE stack. Returns true exactly once, when a complete credential
     * set has been committed - at which point BLE has already been torn down and the
     * caller should read ssid()/password()/ownerUid() and join Wi-Fi.
     *
     * Returns false on every other call, including the ones where individual fields
     * arrive.
     */
    bool poll();

    /** Tears BLE down. Safe to call when it was never started. */
    void end();

    const String& ssid() const { return _ssid; }
    const String& password() const { return _password; }
    const String& ownerUid() const { return _ownerUid; }

private:
    // Firebase UIDs are always exactly this long. Checked before committing, because
    // a truncated owner UID would send every later write to a path the security rules
    // reject, with nothing anywhere reporting why.
    static constexpr size_t OWNER_UID_LENGTH = 28;

    // Long enough for the notification to leave before the link is dropped. The app
    // tolerates losing it - it treats a disconnect after commit as acceptance - but
    // there is no reason to make it rely on that.
    static constexpr unsigned long NOTIFY_DRAIN_MS = 100;

    // The coprocessor needs a moment to hand the antenna over after BLE.end() before
    // Wi-Fi will come up on it.
    static constexpr unsigned long RADIO_SWITCH_MS = 200;

    // Characters of the device UID borrowed for the advertised name. Four keeps
    // "AQS-xxxx" at eight characters, which is the entire name budget left in a
    // 31-byte advertising packet once the flags and the 128-bit service UUID are in.
    static constexpr unsigned int UID_SUFFIX_LENGTH = 4;

    BLEService _service{ "62450001-a803-4091-b1a0-ec1531056f0e" };

    BLEStringCharacteristic _uidChar     { "62450002-a803-4091-b1a0-ec1531056f0e", BLERead,             32 };
    BLEStringCharacteristic _ssidChar    { "62450003-a803-4091-b1a0-ec1531056f0e", BLEWrite,            64 };
    BLEStringCharacteristic _passwordChar{ "62450004-a803-4091-b1a0-ec1531056f0e", BLEWrite,            64 };
    BLEStringCharacteristic _ownerUidChar{ "62450005-a803-4091-b1a0-ec1531056f0e", BLEWrite,            32 };
    BLEStringCharacteristic _commitChar  { "62450006-a803-4091-b1a0-ec1531056f0e", BLEWrite,             4 };
    BLEStringCharacteristic _statusChar  { "62450007-a803-4091-b1a0-ec1531056f0e", BLERead | BLENotify, 16 };

    String _ssid;
    String _password;
    String _ownerUid;

    bool _active = false;

    bool hasCompleteSet() const;
};

#endif
