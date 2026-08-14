package ca.team6.aquasense.pairing;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.FirebaseDatabaseHelper;
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.SensorReading;

public final class PairingRepository {

    public interface PairingObserver {
        void onPairingStateChanged(@NonNull PairingState state);

        void onBoardsChanged(@NonNull List<DiscoveredBoard> boards);
    }

    private static PairingRepository instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context appContext;
    private final AquariumRepository aquariumRepository;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CopyOnWriteArrayList<PairingObserver> observers = new CopyOnWriteArrayList<>();

    private final Map<String, DiscoveredBoard> boards = new LinkedHashMap<>();

    private final Provisioner provisioner;

    private PairingState state = PairingState.IDLE;
    @Nullable
    private PairingFailure failure;

    private String deviceUid = "";

    @Nullable
    private NewAquariumConfig config;

    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle telemetryHandle;

    private static final long NO_SAMPLE = Long.MIN_VALUE;

    private long baselineTimestampSeconds = NO_SAMPLE;
    private boolean baselineCaptured;

    private final Runnable onlineTimeout = () -> {
        ScopedLogger.error("Board published no new telemetry; wrong Wi-Fi credentials are the usual cause.");
        this.stopWatchingTelemetry();
        this.finish(PairingFailure.BOARD_NEVER_CAME_ONLINE);
    };

    private PairingRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.aquariumRepository = AquariumRepository.getInstance(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabaseHelper.getInstance();
        this.provisioner = new BleProvisioner(this.appContext);
        this.provisioner.setListener(this.provisioningListener);
    }

    public static synchronized PairingRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new PairingRepository(context);
        }
        return instance;
    }

    @NonNull
    public PairingState getState() {
        return this.state;
    }

    @Nullable
    public PairingFailure getFailure() {
        return this.failure;
    }

    @NonNull
    public String getDeviceUid() {
        return this.deviceUid;
    }

    @NonNull
    public List<DiscoveredBoard> getBoards() {
        List<DiscoveredBoard> sorted = new ArrayList<>(this.boards.values());
        sorted.sort((left, right) -> Integer.compare(right.getRssi(), left.getRssi()));
        return Collections.unmodifiableList(sorted);
    }

    @Nullable
    public DiscoveredBoard getBoard(@Nullable String address) {
        return address == null ? null : this.boards.get(address);
    }

    public void addObserver(@NonNull PairingObserver observer) {
        this.observers.addIfAbsent(observer);
        observer.onPairingStateChanged(this.state);
        observer.onBoardsChanged(this.getBoards());
    }

    public void removeObserver(@NonNull PairingObserver observer) {
        this.observers.remove(observer);
    }

    public void startScan() {
        this.boards.clear();
        this.failure = null;
        this.publishBoards();
        this.publishState(PairingState.SCANNING);
        this.provisioner.startScan();
    }

    public void stopScan() {
        this.provisioner.stopScan();
        if (this.state == PairingState.SCANNING) {
            this.publishState(PairingState.SCAN_COMPLETE);
        }
    }

    public void pair(@NonNull DiscoveredBoard board,
                     @NonNull String ssid,
                     @NonNull String password,
                     @NonNull NewAquariumConfig config) {
        String ownerUid = this.currentUid();
        if (ownerUid == null) {
            ScopedLogger.error("Cannot pair while signed out.");
            this.finish(PairingFailure.CLAIM_FAILED);
            return;
        }

        this.deviceUid = "";
        this.failure = null;
        this.config = config;
        this.publishState(PairingState.PROVISIONING);

        this.provisioner.provision(board, ssid, password, ownerUid);
    }

    public void reset() {
        this.main.removeCallbacks(this.onlineTimeout);
        this.stopWatchingTelemetry();
        this.provisioner.stopScan();
        this.boards.clear();
        this.deviceUid = "";
        this.failure = null;
        this.publishBoards();
        this.publishState(PairingState.IDLE);
    }

    private final Provisioner.Listener provisioningListener = new Provisioner.Listener() {

        @Override
        public void onBoardFound(@NonNull DiscoveredBoard board) {
            boards.put(board.getAddress(), board);
            publishBoards();
        }

        @Override
        public void onScanFinished(int boardCount) {
            if (boardCount == 0) {
                finish(PairingFailure.NO_BOARD_FOUND);
                return;
            }
            publishState(PairingState.SCAN_COMPLETE);
        }

        @Override
        public void onBoardIdentified(@NonNull String uid) {
            deviceUid = uid;
        }

        @Override
        public void onCredentialsStored() {
            claimAquarium();
        }

        @Override
        public void onFailed(@NonNull PairingFailure reason) {
            finish(reason);
        }
    };

    private void claimAquarium() {
        if (this.deviceUid.isEmpty()) {
            ScopedLogger.error("Board acknowledged the credentials without reporting a UID.");
            this.finish(PairingFailure.INCOMPATIBLE_BOARD);
            return;
        }
        if (this.config == null) {
            ScopedLogger.error("Provisioned a board with no aquarium configuration to claim it.");
            this.finish(PairingFailure.CLAIM_FAILED);
            return;
        }

        this.publishState(PairingState.CLAIMING);
        this.aquariumRepository.addPairedAquarium(this.deviceUid, this.config,
                new AquariumRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        awaitBoard();
                    }

                    @Override
                    public void onError() {
                        ScopedLogger.error("Claimed no aquarium for the newly paired board.");
                        finish(PairingFailure.CLAIM_FAILED);
                    }
                });
    }

    private void awaitBoard() {
        String uid = this.currentUid();
        if (uid == null) {
            this.finish(PairingFailure.CLAIM_FAILED);
            return;
        }

        this.publishState(PairingState.AWAITING_BOARD);
        this.baselineCaptured = false;
        this.baselineTimestampSeconds = NO_SAMPLE;

        this.telemetryHandle = this.database.observeLastInstant(
                uid, this.deviceUid, DatabaseSchema.TEMPERATURE_KEY,
                new FirebaseDatabaseHelper.ReadingListener() {
                    @Override
                    public void onReading(@NonNull String sensorId,
                                          @Nullable SensorReading reading) {
                        if (!isNewWrite(reading)) {
                            return;
                        }
                        main.removeCallbacks(onlineTimeout);
                        stopWatchingTelemetry();
                        failure = null;
                        publishState(PairingState.SUCCESS);
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        ScopedLogger.error("Telemetry watch cancelled while pairing: "
                                + error.getMessage());
                        main.removeCallbacks(onlineTimeout);
                        stopWatchingTelemetry();
                        finish(PairingFailure.BOARD_NEVER_CAME_ONLINE);
                    }
                });

        this.main.postDelayed(this.onlineTimeout, PairingContract.ONLINE_TIMEOUT_MS);
    }

    private boolean isNewWrite(@Nullable SensorReading reading) {
        if (!this.baselineCaptured) {
            this.baselineCaptured = true;
            this.baselineTimestampSeconds =
                    reading == null ? NO_SAMPLE : reading.getTimestampSeconds();
            return false;
        }
        return reading != null && reading.getTimestampSeconds() > this.baselineTimestampSeconds;
    }

    private void stopWatchingTelemetry() {
        if (this.telemetryHandle != null) {
            this.telemetryHandle.remove();
            this.telemetryHandle = null;
        }
    }

    private void finish(@NonNull PairingFailure reason) {
        this.main.removeCallbacks(this.onlineTimeout);
        this.stopWatchingTelemetry();
        this.failure = reason;
        this.publishState(PairingState.FAILED);
    }

    private void publishState(@NonNull PairingState next) {
        this.state = next;
        for (PairingObserver observer : this.observers) {
            observer.onPairingStateChanged(next);
        }
    }

    private void publishBoards() {
        List<DiscoveredBoard> snapshot = this.getBoards();
        for (PairingObserver observer : this.observers) {
            observer.onBoardsChanged(snapshot);
        }
    }

    @Nullable
    private String currentUid() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
