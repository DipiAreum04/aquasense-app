#ifndef BLE_WIFI_SETUP_HPP
#define BLE_WIFI_SETUP_HPP

#include <Arduino.h>
#include <ArduinoBLE.h>

class BLEWifiSetup {
public:
    BLEWifiSetup();

    bool begin(const char* deviceUid);

    bool poll();

    void end();

    const String& ssid() const { return _ssid; }
    const String& password() const { return _password; }
    const String& ownerUid() const { return _ownerUid; }

private:
    static constexpr size_t OWNER_UID_LENGTH = 28;

    static constexpr unsigned long NOTIFY_DRAIN_MS = 100;

    static constexpr unsigned long RADIO_SWITCH_MS = 200;

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
