#ifndef PAIRING_STORE_HPP
#define PAIRING_STORE_HPP

#include <Arduino.h>

/**
 * The pairing data the app hands over BLE, persisted across power cycles.
 *
 * Holds only what changes per user: the Wi-Fi credentials and the owner's Firebase
 * UID. The device's own identity (UID, email, password) stays compiled in, in
 * PairingData, because it never changes for a given board.
 *
 * The record is guarded by a magic number and a CRC. That is not paranoia about
 * cosmic rays - the realistic corruption here is someone pulling the power part way
 * through a write. What makes it worth the two bytes is which field gets damaged:
 *
 *  - A damaged SSID is self-correcting. Wi-Fi fails to connect, the board clears the
 *    record and goes back to advertising, and the user simply pairs again.
 *  - A damaged owner UID is not. Wi-Fi connects, Firebase sign-in succeeds, and the
 *    board writes to /{garbage}/telemetry/... forever. Every write is rejected by the
 *    security rules, nothing reaches the dashboard, and the board never falls back to
 *    pairing because from its own point of view everything is working. That state
 *    needs a reflash to escape.
 *
 * Failing the CRC drops the board into the same path as a factory-fresh one, so the
 * recovery costs no extra code.
 *
 * Layout, 167 bytes of the 8 KB available:
 *
 *   0    4   magic 'A','Q','S','N'
 *   4    1   layout version
 *   5    64  Wi-Fi SSID, NUL-padded
 *   69   64  Wi-Fi password, NUL-padded
 *   133  32  owner UID, NUL-padded
 *   165  2   CRC-16/CCITT over bytes 0..164, big-endian
 */
class PairingStore {
public:
    static constexpr size_t SSID_SIZE = 64;
    static constexpr size_t PASSWORD_SIZE = 64;
    static constexpr size_t OWNER_UID_SIZE = 32;

    // Firebase UIDs are always exactly this long, which makes it a free sanity check
    // on a field whose corruption is otherwise invisible.
    static constexpr size_t OWNER_UID_LENGTH = 28;

    /**
     * Reads the stored pairing. Returns false when the board has never been paired,
     * when the record predates this layout, or when it fails its CRC - all of which
     * the caller should treat identically: advertise and wait for the app.
     *
     * The out parameters are only meaningful when this returns true.
     */
    static bool load(String& ssid, String& password, String& ownerUid);

    /**
     * Writes the pairing and its CRC. Returns false without touching flash if any
     * field is missing or too long, so a partial set from a malformed commit cannot
     * half-provision the board.
     */
    static bool save(const String& ssid, const String& password, const String& ownerUid);

    /** Wipes the record, returning the board to its unpaired state. */
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

    // CRC-16/CCITT over everything ahead of the CRC field itself, read back out of
    // EEPROM rather than from the caller's strings, so it covers what was actually
    // committed to flash rather than what was meant to be.
    static uint16_t computeCrc();

    static void writeField(int addr, size_t size, const String& value);
    static String readField(int addr, size_t size);
};

#endif
