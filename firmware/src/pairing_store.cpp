#include "pairing_store.hpp"

#include <EEPROM.h>

namespace {
// Not a string literal: this is compared byte for byte and must not carry a
// terminating NUL into the record.
const uint8_t MAGIC[] = { 'A', 'Q', 'S', 'N' };
constexpr size_t MAGIC_LENGTH = sizeof(MAGIC);
}

/**
 * Reads the stored pairing, rejecting anything that does not pass every check.
 */
bool PairingStore::load(String& ssid, String& password, String& ownerUid) {
    for (size_t i = 0; i < MAGIC_LENGTH; i++) {
        if (EEPROM.read(MAGIC_ADDR + i) != MAGIC[i]) {
            return false;
        }
    }

    // A record written by an older layout would parse into the wrong offsets, which
    // is worse than not reading it at all.
    if (EEPROM.read(VERSION_ADDR) != VERSION) {
        return false;
    }

    uint16_t stored = ((uint16_t)EEPROM.read(CRC_ADDR) << 8) | EEPROM.read(CRC_ADDR + 1);
    if (stored != computeCrc()) {
        Serial.println("Stored pairing failed its CRC. Treating the board as unpaired.");
        return false;
    }

    ssid = readField(SSID_ADDR, SSID_SIZE);
    password = readField(PASSWORD_ADDR, PASSWORD_SIZE);
    ownerUid = readField(OWNER_UID_ADDR, OWNER_UID_SIZE);

    // The CRC proves the bytes survived flash; these prove they were sane going in.
    // An empty SSID or a mis-sized UID means the record is unusable even though it is
    // intact, and pairing again is the only way out.
    return ssid.length() > 0 && ownerUid.length() == OWNER_UID_LENGTH;
}

bool PairingStore::save(const String& ssid, const String& password, const String& ownerUid) {
    // Checked before any write, so a rejected set leaves whatever was already stored
    // untouched rather than half-overwriting it.
    if (ssid.length() == 0 || ssid.length() >= SSID_SIZE) {
        Serial.println("Refusing to store a missing or oversized SSID.");
        return false;
    }
    if (password.length() >= PASSWORD_SIZE) {
        Serial.println("Refusing to store an oversized Wi-Fi password.");
        return false;
    }
    if (ownerUid.length() != OWNER_UID_LENGTH) {
        Serial.println("Refusing to store an owner UID that is not 28 characters.");
        return false;
    }

    for (size_t i = 0; i < MAGIC_LENGTH; i++) {
        EEPROM.write(MAGIC_ADDR + i, MAGIC[i]);
    }
    EEPROM.write(VERSION_ADDR, VERSION);

    writeField(SSID_ADDR, SSID_SIZE, ssid);
    writeField(PASSWORD_ADDR, PASSWORD_SIZE, password);
    writeField(OWNER_UID_ADDR, OWNER_UID_SIZE, ownerUid);

    // Last, and computed by reading the fields back, so it can only ever match a
    // record that is fully on flash.
    uint16_t crc = computeCrc();
    EEPROM.write(CRC_ADDR, (uint8_t)(crc >> 8));
    EEPROM.write(CRC_ADDR + 1, (uint8_t)(crc & 0xFF));

    return true;
}

/**
 * Wipes the whole record rather than just the magic. Clearing four bytes would be
 * enough to make load() reject it, but it would leave the user's Wi-Fi password
 * sitting readable in flash after they thought they had unpaired the hub.
 */
void PairingStore::clear() {
    for (int addr = 0; addr < RECORD_SIZE; addr++) {
        EEPROM.write(addr, 0);
    }
}

uint16_t PairingStore::computeCrc() {
    uint16_t crc = 0xFFFF;
    for (int addr = 0; addr < CRC_ADDR; addr++) {
        crc ^= (uint16_t)EEPROM.read(addr) << 8;
        for (uint8_t bit = 0; bit < 8; bit++) {
            crc = (crc & 0x8000) ? (uint16_t)((crc << 1) ^ 0x1021) : (uint16_t)(crc << 1);
        }
    }
    return crc;
}

// NUL-pads to the full field width so the CRC covers a deterministic record: leaving
// the tail of a shortened field at whatever was there before would change the CRC
// without changing the credentials.
void PairingStore::writeField(int addr, size_t size, const String& value) {
    for (size_t i = 0; i < size; i++) {
        EEPROM.write(addr + i, i < value.length() ? (uint8_t)value[i] : 0);
    }
}

String PairingStore::readField(int addr, size_t size) {
    // Sized to the widest field. Fixed rather than variable-length so this cannot
    // overflow if a caller ever passes a larger size.
    char buffer[SSID_SIZE];
    if (size > sizeof(buffer)) {
        size = sizeof(buffer);
    }

    size_t length = 0;
    while (length < size - 1) {
        char character = (char)EEPROM.read(addr + length);
        if (character == '\0') {
            break;
        }
        buffer[length++] = character;
    }
    buffer[length] = '\0';

    return String(buffer);
}
