#ifndef PAIRING_DATA_HPP
#define PAIRING_DATA_HPP

#include <Arduino.h>

/**
 * This board's own identity, compiled in rather than stored.
 *
 * All of it is fixed for the life of a given board, so unlike the Wi-Fi credentials
 * and the owner UID - which change per user and live in PairingStore - there is
 * nothing here worth making writable.
 *
 * Every board needs its own values. Create a Firebase Auth account per board in the
 * console, then flash that board with the matching email, password and UID.
 */
class PairingData {
public:
    PairingData();
    const char* getDbUrl() const;
    const char* getWebApiKey() const;
    const char* getDeviceEmail() const;
    const char* getDevicePassword() const;
    const char* getDeviceUid() const;

private:
    const char* dbUrl = "https://aams-c2c68-default-rtdb.firebaseio.com";

    // TODO: placeholders until per-device Firebase credentials are wired up.
    // These must be replaced before shipping - they will not authenticate against the
    // real project.
    const char* webApiKey = "AIzaSyA3pu6pUkaqZldvXNvD5_Mh8ehcFsCz2jU";
    const char* deviceEmail = "dev-0000000001@aquasense.ca";
    const char* devicePassword = "Patty-Fuchka01#";

    /* This board's Firebase Auth UID, which doubles as the aquarium's database key.
     *
     * Compiled in rather than read from Firebase because the app reads it over BLE,
     * while the board is still advertising and has no network - there is no way to
     * have signed in yet, so FirebaseAuth::localId() is not available at that point.
     *
     * It must be exactly the UID that the deviceEmail account above resolves to. If
     * the two ever disagree, the app claims the aquarium under one key while the board
     * writes telemetry under another, every write is rejected by the security rules,
     * and nothing anywhere says why. WiFiFirebase re-checks this after sign-in and
     * warns on the Serial console.
     */
    const char* deviceUid = "8DiVHQV9CNTkffiT4u9JDHObVrE2";
};

#endif
