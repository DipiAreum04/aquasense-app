package ca.team6.aquasense.pairing;

import ca.team6.aquasense.R;

/**
 * This enum is used to describe why a pairing attempt stopped, in terms the result screen can explain.
 */
public enum PairingFailure {

    /** No BLE radio. The only terminal case that no amount of retrying can clear. */
    UNSUPPORTED(R.string.pairing_error_unsupported, false),

    /** Bluetooth is switched off. The screen sends the user to Bluetooth settings. */
    BLUETOOTH_OFF(R.string.pairing_error_bluetooth_off, true),

    /** Scan or connect permission was refused. */
    PERMISSION_DENIED(R.string.pairing_error_permission, true),

    /** The scan ran its course without seeing a board advertising the provisioning service. */
    NO_BOARD_FOUND(R.string.pairing_error_not_found, true),

    /** The link dropped part way through. Usually range issue, or the board rebooting. */
    CONNECTION_LOST(R.string.pairing_error_connection_lost, true),

    /**
     * Connected, but the expected service or characteristics were missing. Almost always due to firmware
     * GATT not matching this app's {@link PairingContract}.
     */
    INCOMPATIBLE_BOARD(R.string.pairing_error_incompatible, false),

    /** A read or write was rejected, or the exchange timed out. */
    TRANSFER_FAILED(R.string.pairing_error_transfer, true),

    /**
     * The board stored the credentials, but creating the aquarium in the database failed. The
     * board is left provisioned and will keep retrying. The fix is a working connection on the phone,
     * not another round of BLE.
     */
    CLAIM_FAILED(R.string.pairing_error_claim, true),

    /**
     * The board took the credentials and then published no <em>new</em> telemetry.
     *
     * <p>The most likely cause is a wrong Wi-Fi network name or password: the board cannot tell the
     * app so, because BLE is already torn down by the time it finds out. It clears the bad
     * credentials and returns to advertising on its own, so the fix is retrying the pairing process.
     */
    BOARD_NEVER_CAME_ONLINE(R.string.pairing_error_offline, true);

    private final int messageResId;
    private final boolean retryable;

    PairingFailure(int messageResId, boolean retryable) {
        this.messageResId = messageResId;
        this.retryable = retryable;
    }

    public int getMessageResId() {
        return this.messageResId;
    }

    public boolean isRetryable() {
        return this.retryable;
    }
}
