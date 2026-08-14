package ca.team6.aquasense.aquarium;

import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.firebase.FirebaseDatabaseHelper;
import ca.team6.aquasense.logging.ScopedLogger;
import ca.team6.aquasense.settings.CalibrationMath;
import ca.team6.aquasense.settings.CalibrationOffsetStore;

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

    public interface PeriodCallback {
        void onBuckets(@NonNull List<SensorReading> buckets);

        void onError();
    }

    private static final int EXPORT_JSON_INDENT_SPACES = 2;

    private static volatile TelemetryRepository instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CalibrationOffsetStore calibrationOffsets;
    private final CopyOnWriteArrayList<TelemetryObserver> observers = new CopyOnWriteArrayList<>();

    private Map<String, SensorReading> readings = Collections.emptyMap();

    private Map<String, SensorReading> rawReadings = Collections.emptyMap();

    private volatile long serverTimeOffsetMillis;

    @Nullable
    private String watchedUid;
    @Nullable
    private String watchedAquariumId;

    private final List<FirebaseDatabaseHelper.ListenerHandle> handles = new ArrayList<>();

    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle periodHandle;

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

    @Nullable
    public SensorReading getRawReading(@NonNull String sensorId) {
        return this.rawReadings.get(sensorId);
    }

    public void refreshCalibration() {
        this.publishCorrected();
    }

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

    private void merge(@NonNull String sensorId, @Nullable SensorReading reading) {
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
                        callback.onBuckets(calibrationOffsets.correctAll(
                                aquariumId, sensorId, buckets));
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        ScopedLogger.error("Period subscription cancelled: " + error.getMessage());
                        unwatchPeriod();
                        callback.onError();
                    }
                });
    }

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
            if (Double.isNaN(value) || DatabaseSchema.isOffline(value)) {
                continue;
            }
            bucket.put(DatabaseSchema.VALUE_KEY, CalibrationMath.correct(value, offset));
        }
        return node.toString(EXPORT_JSON_INDENT_SPACES);
    }

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
