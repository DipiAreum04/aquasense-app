package ca.team6.aquasense.pairing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Provisioner {

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

    void provision(@NonNull DiscoveredBoard board,
                   @NonNull String ssid,
                   @NonNull String password,
                   @NonNull String ownerUid);

    void close();
}
