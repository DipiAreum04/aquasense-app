package ca.team6.aquasense.pairing;

import ca.team6.aquasense.R;

public enum PairingFailure {

    UNSUPPORTED(R.string.pairing_error_unsupported, false),

    BLUETOOTH_OFF(R.string.pairing_error_bluetooth_off, true),

    PERMISSION_DENIED(R.string.pairing_error_permission, true),

    NO_BOARD_FOUND(R.string.pairing_error_not_found, true),

    CONNECTION_LOST(R.string.pairing_error_connection_lost, true),

    INCOMPATIBLE_BOARD(R.string.pairing_error_incompatible, false),

    TRANSFER_FAILED(R.string.pairing_error_transfer, true),

    CLAIM_FAILED(R.string.pairing_error_claim, true),

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
