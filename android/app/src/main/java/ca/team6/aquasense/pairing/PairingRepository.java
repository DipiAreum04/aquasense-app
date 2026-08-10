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

/**
 * Drives one pairing attempt from end to end: scan, hand over credentials, create the aquarium, then
 * wait for the board to come online and publish telemetry. This is a singleton to ensure that only
 * one pairing attempt can be in progress at a time.
 */
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

    // Keyed by address so a board re-reported mid-scan updates its RSSI instead of adding a row.
    private final Map<String, DiscoveredBoard> boards = new LinkedHashMap<>();

    private final Provisioner provisioner;

    private PairingState state = PairingState.IDLE;
    @Nullable
    private PairingFailure failure;

    // Arrives over BLE, and is the key the aquarium telemetry is written under.
    private String deviceUid = "";

    // Everything the add-aquarium form collected, held until the board's UID arrives to key it by.
    // Null until a pairing attempt starts, which is the only thing that can supply one.
    @Nullable
    private NewAquariumConfig config;

    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle telemetryHandle;

    private final Runnable onlineTimeout = () -> {
        ScopedLogger.error("Board never published telemetry; the Wi-Fi password is the usual cause.");
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

    /** Why the attempt stopped. Only meaningful while the state is FAILED. */
    @Nullable
    public PairingFailure getFailure() {
        return this.failure;
    }

    /** The paired board's UID, which is also the new aquarium's key in the database. */
    @NonNull
    public String getDeviceUid() {
        return this.deviceUid;
    }

    @NonNull
    public List<DiscoveredBoard> getBoards() {
        List<DiscoveredBoard> sorted = new ArrayList<>(this.boards.values());
        // Strongest connection strength first among several boards
        sorted.sort((left, right) -> Integer.compare(right.getRssi(), left.getRssi()));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * The board or boards with this address, or null once the list has been cleared by a new scan.
     */
    @Nullable
    public DiscoveredBoard getBoard(@Nullable String address) {
        return address == null ? null : this.boards.get(address);
    }

    /**
     * This observer tracks state and board updates, firing both immediately so a screen renders from the
     * callback alone.
     */
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

    /**
     * Runs the full attempt against one board.
     *
     * <p>Every piece of user input is collected before this is called: the Wi-Fi credentials on the
     * pairing screen, the name and water type on the add-aquarium form ahead of it. That leaves the
     * BLE link to be opened, used and closed in one burst, with nothing waiting on the user in the
     * middle of it.
     */
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

        // The board needs the owner's UID as well as the Wi-Fi credentials: telemetry lives at
        // /{ownerUid}/telemetry/{aqId}, so without it the board cannot build a single write path.
        this.provisioner.provision(board, ssid, password, ownerUid);
    }

    /** Abandons whatever is under way and returns to {@link PairingState#IDLE}. */
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

    /**
     * Writes {@code /{uid}/aquariums/{deviceUid}}, which is what authorises the board to publish.
     *
     * <p>The security rule on telemetry requires this node to exist, so the ordering is not
     * cosmetic: claim first, then watch. Watching before claiming would time out every time.
     */
    private void claimAquarium() {
        if (this.deviceUid.isEmpty()) {
            ScopedLogger.error("Board acknowledged the credentials without reporting a UID.");
            this.finish(PairingFailure.INCOMPATIBLE_BOARD);
            return;
        }
        if (this.config == null) {
            // Only reachable if the provisioner reported success for an attempt pair() never
            // started, which would leave nothing to name the aquarium after.
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
                        // The board is provisioned either way and will keep trying, so this is
                        // reported apart from a Wi-Fi failure: phone's connection needs to be fixed.
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

        /* One sensor is enough. The board publishes all four together on the same tick,
         * so the first temperature reading proves the whole chain: Wi-Fi, the Firebase
         * sign-in, the security rules and a real write.
         *
         * Watching the aquarium's whole telemetry node instead would also carry six
         * periods of a hundred buckets each - hundreds of KB synced to learn one bit.
         */
        this.telemetryHandle = this.database.observeLastInstant(
                uid, this.deviceUid, DatabaseSchema.TEMPERATURE_KEY,
                new FirebaseDatabaseHelper.ReadingListener() {
                    @Override
                    public void onReading(@NonNull String sensorId,
                                          @Nullable SensorReading reading) {
                        // Fires immediately with null, because the node does not exist yet.
                        // Only a real reading counts.
                        if (reading == null) {
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
