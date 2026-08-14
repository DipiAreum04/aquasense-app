package ca.team6.aquasense.pairing;

import java.util.UUID;

public final class PairingContract {

    private PairingContract() {}


    private static final String BASE = "-a803-4091-b1a0-ec1531056f0e";

    public static final UUID SERVICE_UUID = uuid("62450001");

    public static final UUID DEVICE_UID_UUID = uuid("62450002");

    public static final UUID SSID_UUID = uuid("62450003");

    public static final UUID PASSWORD_UUID = uuid("62450004");

    public static final UUID OWNER_UID_UUID = uuid("62450005");

    public static final UUID COMMIT_UUID = uuid("62450006");

    public static final UUID STATUS_UUID = uuid("62450007");

    public static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final String DEVICE_LOCAL_NAME = "AquaSense Hub";

    public static final String STATUS_WAITING = "WAITING";

    public static final String STATUS_RECEIVED = "RECEIVED";

    public static final int MTU_REQUEST = 185;

    public static final long SCAN_TIMEOUT_MS = 10_000L;

    public static final long GATT_TIMEOUT_MS = 20_000L;

    public static final long ONLINE_TIMEOUT_MS = 40_000L;

    private static UUID uuid(String firstBlock) {
        return UUID.fromString(firstBlock + BASE);
    }
}
