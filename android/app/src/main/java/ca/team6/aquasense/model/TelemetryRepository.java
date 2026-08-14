package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TelemetryRepository {
    public interface TelemetryObserver {
        void onTelemetryChanged(@NonNull Map<String, SensorReading> readingsBySensorId);
    }

    /** Running result of {@link #watchPeriod}, called again on every change until unwatched. */
    public interface PeriodCallback {
        /**
         * The period's buckets, oldest first, with the one the period's cursor points at last.
         * Empty when the board has committed none yet.
         */
        void onBuckets(@NonNull List<SensorReading> buckets);

        void onError();
    }

    // Matches the indent FirebaseDatabaseHelper renders a period with, so re-writing a corrected
    // copy leaves a file that still reads the way the uncorrected one did.
    private static final int EXPORT_JSON_INDENT_SPACES = 2;

    private static volatile TelemetryRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CalibrationOffsetStore calibrationOffsets;
    private final CopyOnWriteArrayList<TelemetryObserver> observers = new CopyOnWriteArrayList<>();

    // What observers see: every value with the aquarium's calibration correction already on it.
    private Map<String, SensorReading> readings = Collections.emptyMap();

    // What the board actually published, kept beside the above so a new offset can be applied to
    // the samples already in hand, and so the calibration flow has something uncorrected to work
    // its next offset out from.
    private Map<String, SensorReading> rawReadings = Collections.emptyMap();

    // Diff between the Firebase server clock and this device's clock, so staleness checks compare
    // the board's timestamp against server time instead of a phone clock that may be skewed.
    private volatile long serverTimeOffsetMillis;

    @Nullable
    private String watchedUid;
    @Nullable
    private String watchedAquariumId;

    // One subscription per sensor, each on that sensor's last_instant rather than one on their
    // shared parent. The parent also holds six periods of a hundred buckets each, so watching it
    // means syncing and caching hundreds of KB to read four numbers, and being woken every time a
    // bucket closes - none of which this repository ever looks at.
    private final List<FirebaseDatabaseHelper.ListenerHandle> handles = new ArrayList<>();

    // The one period being plotted, which is a subscription of a wholly different size and lifetime
    // from the four above: hundreds of KB, held only while a screen is drawing a graph from it.
    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle periodHandle;

    // Sensors whose last_instant has delivered at least one snapshot for the current subscription.
    // The four listeners resolve independently, so the first few publishes carry only the sensors
    // read so far; a caller aggregating one verdict across all of them has to wait for the set to
    // fill or it reports a state built mostly from sensors that simply have not been read yet.
    private final Set<String> readSensorIds = new HashSet<>();

    private TelemetryRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabaseHelper.getInstance();
        this.calibrationOffsets = new CalibrationOffsetStore(
                FirebaseApp.getInstance().getApplicationContext());
        this.firebaseAuth.addAuthStateListener(auth -> onAuthChanged(auth.getCurrentUser()));

        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long offset = snapshot.getValue(Long.class);
                        serverTimeOffsetMillis = offset != null ? offset : 0L;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        ScopedLogger.error("serverTimeOffset subscription cancelled: " + error.getMessage());
                    }
                });
    }

    /** Current time per the Firebase server clock, immune to this device's clock being wrong. */
    public long nowMillis() {
        return System.currentTimeMillis() + this.serverTimeOffsetMillis;
    }

    public static TelemetryRepository getInstance() {
        if (instance == null) {
            synchronized (TelemetryRepository.class) {
                if (instance == null) {
                    instance = new TelemetryRepository();
                }
            }
        }
        return instance;
    }

    @NonNull
    public Map<String, SensorReading> getReadings() {
        return this.readings;
    }

    /**
     * The board's latest sample for a sensor as it was published, for calibration only; every other
     * consumer wants {@link #getReadings()}, which is the same sample corrected.
     *
     * <p>An offset is worked out as the reference minus what the hardware said, so the flow that
     * derives one has to read past the correction already in force. Deriving it from a corrected
     * value would fold the old offset into the new one and stack them.
     */
    @Nullable
    public SensorReading getRawReading(@NonNull String sensorId) {
        return this.rawReadings.get(sensorId);
    }

    /** Re-publishes cached raw samples after a newly calculated offset is saved. */
    public void refreshCalibration() {
        this.publishCorrected();
    }

    /**
     * Whether every sensor's last_instant has been read at least once since the current aquarium
     * was watched. Until then the published readings are an incomplete view of the database, not a
     * report that the missing sensors are offline.
     */
    public boolean hasReadAllSensors() {
        return this.readSensorIds.size() == DatabaseSchema.SENSOR_IDS.size();
    }

    public void watchAquarium(@NonNull String aquariumId) {
        String uid = this.currentUid();
        if (uid == null || aquariumId.isEmpty()) {
            this.unwatch();
            return;
        }
        if (Objects.equals(uid, this.watchedUid) && aquariumId.equals(this.watchedAquariumId)) {
            return;
        }

        this.detach();
        this.watchedUid = uid;
        this.watchedAquariumId = aquariumId;

        this.rawReadings = Collections.emptyMap();
        this.publish(Collections.emptyMap());

        for (String sensorId : DatabaseSchema.SENSOR_IDS) {
            this.handles.add(this.database.observeLastInstant(uid, aquariumId, sensorId,
                    new FirebaseDatabaseHelper.ReadingListener() {
                        @Override
                        public void onReading(@NonNull String id, @Nullable SensorReading reading) {
                            merge(id, reading);
                        }

                        @Override
                        public void onError(@NonNull DatabaseError error) {
                            ScopedLogger.error("Telemetry subscription cancelled: " + error.getMessage());
                            detach();
                            watchedUid = null;
                            watchedAquariumId = null;
                            publish(Collections.emptyMap());
                        }
                    }));
        }
    }

    /**
     * Folds one sensor's latest reading into the published map. The readings now arrive on four
     * separate callbacks rather than one, so each publish carries the newest of every sensor seen
     * so far instead of a whole snapshot.
     *
     * <p>A null reading removes the key rather than storing a null, which keeps the previous
     * behaviour of a sensor the board has not published yet being absent from the map.
     */
    private void merge(@NonNull String sensorId, @Nullable SensorReading reading) {
        // A null reading still counts as read: the listener resolved, and "this sensor has never
        // published" is an answer about the sensor rather than a gap in what we have fetched.
        this.readSensorIds.add(sensorId);

        Map<String, SensorReading> merged = new LinkedHashMap<>(this.rawReadings);
        if (reading != null) {
            merged.put(sensorId, reading);
        } else {
            merged.remove(sensorId);
        }
        this.rawReadings = Collections.unmodifiableMap(merged);
        this.publishCorrected();
    }

    /**
     * Watches one period's buckets for the signed-in user, reporting them again on every change so
     * a graph drawn from them follows the board.
     *
     * <p>Deliberately not folded into the subscriptions above. Those watch four last_instant nodes
     * precisely so the app never syncs the periods hanging beside them; a period is a hundred
     * buckets, and only the screen plotting one has any use for it.
     *
     * <p>One at a time, like the aquarium above it: a screen plots one aquarium's one sensor over
     * one window, so asking for another is always asking to stop watching this one. That makes the
     * detach on every change of selection the same call as the attach, rather than something a
     * caller has to remember to pair. Callers must still {@link #unwatchPeriod()} on the way out.
     */
    public void watchPeriod(@NonNull String aquariumId,
                            @NonNull String sensorId,
                            @NonNull String periodKey,
                            @NonNull PeriodCallback callback) {
        this.unwatchPeriod();

        String uid = this.currentUid();
        if (uid == null || aquariumId.isEmpty()) {
            callback.onError();
            return;
        }

        this.periodHandle = this.database.observePeriod(uid, aquariumId, sensorId, periodKey,
                new FirebaseDatabaseHelper.BucketsListener() {
                    @Override
                    public void onBuckets(@NonNull List<SensorReading> buckets) {
                        // Corrected here for the same reason last_instant is: a sensor's history and
                        // its current reading are the same probe, and a graph plotted against an
                        // uncorrected past would disagree with the card above it.
                        callback.onBuckets(calibrationOffsets.correctAll(
                                aquariumId, sensorId, buckets));
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        ScopedLogger.error("Period subscription cancelled: " + error.getMessage());
                        // Firebase does not revive a cancelled listener, so the handle is dead;
                        // dropping it lets the next selection subscribe again.
                        unwatchPeriod();
                        callback.onError();
                    }
                });
    }

    /**
     * Reads one period node once, as the JSON the database stores it as, for saving a copy of it.
     *
     * <p>Unrelated to {@link #watchPeriod}: it takes its own selection and leaves whatever is being
     * watched alone, so a screen can copy the window it is plotting without giving up the
     * subscription drawing it.
     */
    public void readPeriodJson(@NonNull String aquariumId,
                               @NonNull String sensorId,
                               @NonNull String periodKey,
                               @NonNull FirebaseDatabaseHelper.PeriodJsonListener listener) {
        String uid = this.currentUid();
        if (uid == null || aquariumId.isEmpty()) {
            listener.onError(null);
            return;
        }
        this.database.readPeriodJson(uid, aquariumId, sensorId, periodKey,
                new FirebaseDatabaseHelper.PeriodJsonListener() {
                    @Override
                    public void onJson(@NonNull String json) {
                        try {
                            listener.onJson(correctPeriodJson(aquariumId, sensorId, json));
                        } catch (JSONException exception) {
                            // The node parsed as a period but could not be written back out, so
                            // what is in hand is a file of uncorrected numbers. Failing the export
                            // is better than saving one that disagrees with every other screen.
                            ScopedLogger.error("Could not calibrate the exported period: "
                                    + exception.getMessage());
                            listener.onError(exception);
                        }
                    }

                    @Override
                    public void onEmpty() {
                        listener.onEmpty();
                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        listener.onError(exception);
                    }
                });
    }

    /**
     * Puts the sensor's correction on every bucket of an exported period, leaving the node's shape -
     * its cursor, its slots, and the ones the board has not come back round to - as it was.
     *
     * <p>The export is the one copy of a reading that leaves the app, so it carries the same numbers
     * the dashboard and the graphs do. It is no longer byte-identical to the database tree because
     * of it: what the hardware reported is still in the database, and this file is what the app
     * makes of it.
     */
    @NonNull
    private String correctPeriodJson(@NonNull String aquariumId,
                                     @NonNull String sensorId,
                                     @NonNull String json) throws JSONException {
        double offset = this.calibrationOffsets.get(aquariumId, sensorId);
        if (offset == 0d) {
            return json;
        }

        JSONObject node;
        try {
            node = new JSONObject(json);
        } catch (JSONException notAnObject) {
            // No object means no buckets node, and so no values an offset could apply to.
            return json;
        }

        JSONObject buckets = node.optJSONObject(DatabaseSchema.BUCKETS_KEY);
        if (buckets == null) {
            return json;
        }
        for (Iterator<String> slots = buckets.keys(); slots.hasNext(); ) {
            JSONObject bucket = buckets.optJSONObject(slots.next());
            if (bucket == null) {
                continue;
            }
            double value = bucket.optDouble(DatabaseSchema.VALUE_KEY, Double.NaN);
            // Gap markers and the offline sentinel are protocol values rather than measurements,
            // and are left exactly as they are for the same reason they are on screen.
            if (Double.isNaN(value) || DatabaseSchema.isOffline(value)) {
                continue;
            }
            bucket.put(DatabaseSchema.VALUE_KEY, CalibrationMath.correct(value, offset));
        }
        return node.toString(EXPORT_JSON_INDENT_SPACES);
    }

    /**
     * Deletes one period node for the signed-in user.
     *
     * <p>Any subscription on that node is left in place and is how the deletion is reported: the
     * removal comes back as a snapshot with nothing in it, so a screen plotting the window it just
     * cleared empties itself on the same path every other change to it arrives by.
     */
    public void deletePeriod(@NonNull String aquariumId,
                             @NonNull String sensorId,
                             @NonNull String periodKey,
                             @NonNull FirebaseDatabaseHelper.DbCallback callback) {
        String uid = this.currentUid();
        if (uid == null || aquariumId.isEmpty()) {
            callback.onError(null);
            return;
        }
        this.database.deletePeriod(uid, aquariumId, sensorId, periodKey, callback);
    }

    /** Releases the period subscription, if there is one. Safe to call when there is not. */
    public void unwatchPeriod() {
        if (this.periodHandle != null) {
            this.periodHandle.remove();
            this.periodHandle = null;
        }
    }

    public void unwatch() {
        if (this.watchedUid == null && this.watchedAquariumId == null && this.readings.isEmpty()) {
            return;
        }
        this.detach();
        this.watchedUid = null;
        this.watchedAquariumId = null;
        this.rawReadings = Collections.emptyMap();
        this.publish(Collections.emptyMap());
    }

    public void addObserver(@NonNull TelemetryObserver observer) {
        this.observers.addIfAbsent(observer);
        observer.onTelemetryChanged(this.readings);
    }

    public void removeObserver(@NonNull TelemetryObserver observer) {
        this.observers.remove(observer);
    }

    private void onAuthChanged(@Nullable FirebaseUser user) {
        String uid = user != null ? user.getUid() : null;
        if (!Objects.equals(uid, this.watchedUid)) {
            this.unwatch();
            // The period is keyed on the user too, and the screen holding it has no way of hearing
            // about a sign-out before its next read comes back under the wrong account.
            this.unwatchPeriod();
        }
    }

    private void detach() {
        for (FirebaseDatabaseHelper.ListenerHandle handle : this.handles) {
            handle.remove();
        }
        this.handles.clear();
        this.readSensorIds.clear();
        this.rawReadings = Collections.emptyMap();
    }

    private void publish(@NonNull Map<String, SensorReading> readings) {
        this.readings = readings;
        for (TelemetryObserver observer : this.observers) {
            observer.onTelemetryChanged(readings);
        }
    }

    /** Puts the watched aquarium's offsets on every cached raw sample and publishes the result. */
    private void publishCorrected() {
        if (this.watchedAquariumId == null) {
            this.publish(Collections.emptyMap());
            return;
        }
        Map<String, SensorReading> corrected = new LinkedHashMap<>();
        for (Map.Entry<String, SensorReading> entry : this.rawReadings.entrySet()) {
            corrected.put(entry.getKey(),
                    this.correct(this.watchedAquariumId, entry.getKey(), entry.getValue()));
        }
        this.publish(Collections.unmodifiableMap(corrected));
    }

    @NonNull
    private SensorReading correct(@NonNull String aquariumId,
                                  @NonNull String sensorId,
                                  @NonNull SensorReading reading) {
        return new SensorReading(
                this.calibrationOffsets.correct(aquariumId, sensorId, reading.getValue()),
                reading.getTimestampSeconds());
    }

    @Nullable
    private String currentUid() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
