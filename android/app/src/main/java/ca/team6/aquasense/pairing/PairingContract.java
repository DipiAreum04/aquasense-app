package ca.team6.aquasense.pairing;

import java.util.UUID;

/**
 * The BLE provisioning contract shared with the firmware.
 *
 * <p>Every constant here must match the board's GATT definitions byte for byte.
 *
 * <p>Provisioning flow:
 * <pre>
 * board advertises   -> app scans by service UUID
 * app connects       -> reads deviceUid, which is the aquarium key in the database
 * app writes         wifi ssid, wifi password, ownerUid
 * app writes         commit  ("all fields are filled up, you can proceed")
 * board notifies     status = RECEIVED, saves the credentials, tears BLE down, joins Wi-Fi
 * </pre>
 *
 * <p>The board cannot report Wi-Fi success back over BLE as the UNO R4's Bluetooth and Wi-Fi share
 * one antenna and cannot run together, so BLE is gone by the time Wi-Fi comes up. {@code RECEIVED}
 * therefore only means the credentials were <em>stored</em>. Real success is confirmed through
 * Firebase; see {@link PairingRepository}.
 */
public final class PairingContract {

    private PairingContract() {}

    // Random UUIDs for our service known between the client and server to allow communication
    // These are derived from one randomly generated v4 UUID, 624533be-a803-4091-b1a0-ec1531056f0e, keeping
    // 6245 as the product prefix, the next four hex digits as the slot number, and the remaining as a fixed base
    
    private static final String BASE = "-a803-4091-b1a0-ec1531056f0e";

    // Advertised service (board). The scan filters on this to find the board
    public static final UUID SERVICE_UUID = uuid("62450001");

    // Read. The board's Firebase Auth UID, which is used as the aquarium ID in db
    public static final UUID DEVICE_UID_UUID = uuid("62450002");

    // Write. The home Wi-Fi network the board should connect to
    public static final UUID SSID_UUID = uuid("62450003");

    // Write. The password for the home Wi-Fi network
    public static final UUID PASSWORD_UUID = uuid("62450004");

    // Write. The signed-in user's Firebase UID
    public static final UUID OWNER_UID_UUID = uuid("62450005");

    // Write. The explicit "commit" trigger to signal that all fields are filled up
    public static final UUID COMMIT_UUID = uuid("62450006");

    // Read & Notify. Carries the status "WAITING" then "RECEIVED" to indicate the board's state
    public static final UUID STATUS_UUID = uuid("62450007");

    /**
     * Client Characteristic Configuration Descriptor. The CCCD has a universally standardized
     * SIG-adopted 16-bit UUID of 0x2902 present on every notifiable characteristic. This is used
     * to enable notifications for the STATUS_UUID characteristic.
     */
    public static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Advertised local name. Shown in the board picker to identify the board
    public static final String DEVICE_LOCAL_NAME = "AquaSense Hub";

    // The board is advertising and has not been given credentials yet
    public static final String STATUS_WAITING = "WAITING";

    // The board has stored the credentials
    public static final String STATUS_RECEIVED = "RECEIVED";

    /**
     * MTU is the maximum length of an Attribute Protocol (ATT) packet that can be sent between 
     * a BLE client and a server.
     *
     * <p>The default is 23-byte but our packets include a 28-character UID and a 63-
     * character Wi-Fi password, which exceeds the default. So, an MTU size of 185 is chosen as it
     * covers every field with room to spare and is widely supported conventionally.
     */
    public static final int MTU_REQUEST = 185;

    // How long to scan before telling the user no hub was found (10 seconds)
    public static final long SCAN_TIMEOUT_MS = 10_000L;

    // Timeout limit for the whole connect/discover/read/write exchange once a board is selected (20 seconds)
    public static final long GATT_TIMEOUT_MS = 20_000L;

    /**
     * How long to wait for the board's first telemetry write after it leaves BLE (30 seconds)
     *
     * <p> This timeout covers a Wi-Fi join, a Firebase sign-in and an NTP fetch. The board 
     * publishes every second once it is up, so anything past this is a genuine failure. 
     * Nearly always a wrong Wi-Fi password.
     */
    public static final long ONLINE_TIMEOUT_MS = 30_000L;

    // Helper method to generate the full UUID from the first block
    private static UUID uuid(String firstBlock) {
        return UUID.fromString(firstBlock + BASE);
    }
}
