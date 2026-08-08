package ca.team6.aquasense.pairing;

/**
 * This enum represents the current state of a pairing attempt.
 *
 * <p>The order below is the order they occur in. Screens key their copy off this rather than off
 * the individual callbacks. This ensures that the UI is updated correctly.
 */
public enum PairingState {

    // This is the initial state when no pairing attempt is in progress.
    IDLE,

    // This state is entered when the pairing attempt is in progress and the user is scanning for boards.
    SCANNING,

    // This state is entered when the scanning window has closed and the board list is finalized.
    SCAN_COMPLETE,

    // This state is entered when the pairing attempt is in progress and the user is connecting to the board.
    PROVISIONING,

    // This state is entered when the user is writing the aquarium ID (device UID) to the database.
    CLAIMING,

    // This state is entered when the user is waiting for the board's first telemetry write to verify the Wi-fi connection.
    AWAITING_BOARD,

    // This state is entered when the board has published telemetry and the Wi-Fi connection has been verified.
    SUCCESS,

    // This state is entered when the pairing attempt has failed for some reason.
    FAILED,
}
