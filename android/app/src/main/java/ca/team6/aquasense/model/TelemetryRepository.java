package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    private static volatile TelemetryRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CopyOnWriteArrayList<TelemetryObserver> observers = new CopyOnWriteArrayList<>();

    private Map<String, SensorReading> readings = Collections.emptyMap();

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

    // Sensors whose last_instant has delivered at least one snapshot for the current subscription.
    // The four listeners resolve independently, so the first few publishes carry only the sensors
    // read so far; a caller aggregating one verdict across all of them has to wait for the set to
    // fill or it reports a state built mostly from sensors that simply have not been read yet.
    private final Set<String> readSensorIds = new HashSet<>();

    private TelemetryRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabaseHelper.getInstance();
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

        Map<String, SensorReading> merged = new LinkedHashMap<>(this.readings);
        if (reading != null) {
            merged.put(sensorId, reading);
        } else {
            merged.remove(sensorId);
        }
        this.publish(Collections.unmodifiableMap(merged));
    }

    public void unwatch() {
        if (this.watchedUid == null && this.watchedAquariumId == null && this.readings.isEmpty()) {
            return;
        }
        this.detach();
        this.watchedUid = null;
        this.watchedAquariumId = null;
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
        }
    }

    private void detach() {
        for (FirebaseDatabaseHelper.ListenerHandle handle : this.handles) {
            handle.remove();
        }
        this.handles.clear();
        this.readSensorIds.clear();
    }

    private void publish(@NonNull Map<String, SensorReading> readings) {
        this.readings = readings;
        for (TelemetryObserver observer : this.observers) {
            observer.onTelemetryChanged(readings);
        }
    }

    @Nullable
    private String currentUid() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
