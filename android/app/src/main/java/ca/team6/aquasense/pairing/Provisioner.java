package ca.team6.aquasense.pairing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Finds hubs and hands them the necessary credentials.
 */
public interface Provisioner {

    /** Reports the progress of one pairing attempt */
    interface Listener {

        void onBoardFound(@NonNull DiscoveredBoard board);

        void onScanFinished(int boardCount);

        void onBoardIdentified(@NonNull String deviceUid);

        void onCredentialsStored();

        void onFailed(@NonNull PairingFailure failure);
    }

    void setListener(@Nullable Listener listener);

    void startScan();

    void stopScan();

    /**
     * Runs the whole exchange against one board.
     * Every field is taken up front so that no user input is needed once the link is open.
     */
    void provision(@NonNull DiscoveredBoard board,
                   @NonNull String ssid,
                   @NonNull String password,
                   @NonNull String ownerUid);

    void close();
}
