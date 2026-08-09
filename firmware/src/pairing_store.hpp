#ifndef PAIRING_STORE_HPP
#define PAIRING_STORE_HPP

#include <Arduino.h>

class PairingStore {
public:
    static constexpr size_t SSID_SIZE = 64;
    static constexpr size_t PASSWORD_SIZE = 64;
    static constexpr size_t OWNER_UID_SIZE = 32;

    static constexpr size_t OWNER_UID_LENGTH = 28;

    static bool load(String& ssid, String& password, String& ownerUid);

    static bool save(const String& ssid, const String& password, const String& ownerUid);

    static void clear();

private:
    static constexpr int MAGIC_ADDR = 0;
    static constexpr int VERSION_ADDR = 4;
    static constexpr int SSID_ADDR = 5;
    static constexpr int PASSWORD_ADDR = 69;
    static constexpr int OWNER_UID_ADDR = 133;
    static constexpr int CRC_ADDR = 165;
    static constexpr int RECORD_SIZE = 167;

    static constexpr uint8_t VERSION = 1;

    static uint16_t computeCrc();

    static void writeField(int addr, size_t size, const String& value);
    static String readField(int addr, size_t size);
};

#endif
