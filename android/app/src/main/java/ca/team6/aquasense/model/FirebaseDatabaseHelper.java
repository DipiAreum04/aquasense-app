package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FirebaseDatabaseHelper {

    public interface DbCallback {
        void onSuccess();

        void onError(@Nullable Exception exception);
    }

    public interface ListenerHandle {
        void remove();
    }

    public interface ReadingListener {
        void onReading(@NonNull String sensorId, @Nullable SensorReading reading);

        void onError(@NonNull DatabaseError error);
    }

    public interface AquariumsListener {
        void onAquariums(@NonNull List<Aquarium> aquariums);

        void onError(@NonNull DatabaseError error);
    }

    public interface BucketsListener {
        void onBuckets(@NonNull List<SensorReading> buckets);

        void onError(@NonNull DatabaseError error);
    }

    public interface PeriodJsonListener {
        void onJson(@NonNull String json);

        void onEmpty();

        void onError(@Nullable Exception exception);
    }

    private static final int JSON_INDENT_SPACES = 2;

    private static volatile FirebaseDatabaseHelper instance;

    private final FirebaseDatabase database;

    private FirebaseDatabaseHelper() {
        this.database = FirebaseDatabase.getInstance();
    }

    public static FirebaseDatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (FirebaseDatabaseHelper.class) {
                if (instance == null) {
                    instance = new FirebaseDatabaseHelper();
                }
            }
        }
        return instance;
    }

    public void writeAccount(@NonNull String uid,
                             @NonNull String name,
                             @NonNull String email,
                             @NonNull DbCallback callback) {
        Map<String, Object> account = new HashMap<>();
        account.put(DatabaseSchema.NAME_KEY, name);
        account.put(DatabaseSchema.EMAIL_KEY, email);

        accountRef(uid)
                .setValue(account)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    public void updateAccountName(@NonNull String uid,
                                  @NonNull String name,
                                  @NonNull DbCallback callback) {
        accountRef(uid)
                .child(DatabaseSchema.NAME_KEY)
                .setValue(name)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    @SuppressWarnings("unused")
    @Nullable
    public String newAquariumId(@NonNull String uid) {
        return aquariumsRef(uid).push().getKey();
    }

    public void writeAquarium(@NonNull String uid,
                              @NonNull String aquariumId,
                              @NonNull String name,
                              @NonNull String waterType,
                              @NonNull Map<String, ThresholdBand> thresholds,
                              @NonNull Map<String, Double> spikeDeltas,
                              @NonNull DbCallback callback) {
        Map<String, Object> aquarium = new HashMap<>();
        aquarium.put(DatabaseSchema.NAME_KEY, name);
        aquarium.put(DatabaseSchema.WATER_TYPE_KEY, waterType);
        addSensorPaths(aquarium, thresholds, spikeDeltas);

        aquariumsRef(uid)
                .child(aquariumId)
                .updateChildren(aquarium)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    public void writeSensorThresholds(@NonNull String uid,
                                      @NonNull String aquariumId,
                                      @NonNull Map<String, ThresholdBand> thresholds,
                                      @NonNull Map<String, Double> spikeDeltas,
                                      @NonNull DbCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        addSensorPaths(updates, thresholds, spikeDeltas);
        if (updates.isEmpty()) {
            callback.onSuccess();
            return;
        }

        aquariumsRef(uid)
                .child(aquariumId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    private static void addSensorPaths(@NonNull Map<String, Object> updates,
                                       @NonNull Map<String, ThresholdBand> thresholds,
                                       @NonNull Map<String, Double> spikeDeltas) {
        for (Map.Entry<String, ThresholdBand> entry : thresholds.entrySet()) {
            updates.put(
                    DatabaseSchema.THRESHOLDS_KEY + "/" + entry.getKey(),
                    boundsOf(entry.getValue()));
        }
        for (Map.Entry<String, Double> entry : spikeDeltas.entrySet()) {
            updates.put(DatabaseSchema.SPIKE_DELTAS_KEY + "/" + entry.getKey(), entry.getValue());
        }
    }

    @NonNull
    private static Map<String, Object> boundsOf(@NonNull ThresholdBand band) {
        Map<String, Object> bounds = new HashMap<>();
        bounds.put(DatabaseSchema.WARN_LOW_KEY, band.getWarnLow());
        bounds.put(DatabaseSchema.SAFE_LOW_KEY, band.getSafeLow());
        bounds.put(DatabaseSchema.SAFE_HIGH_KEY, band.getSafeHigh());
        bounds.put(DatabaseSchema.WARN_HIGH_KEY, band.getWarnHigh());
        return bounds;
    }


    public void deleteAquarium(@NonNull String uid,
                               @NonNull String aquariumId,
                               @NonNull DbCallback callback) {
        Map<String, Object> removals = new HashMap<>();
        removals.put(DatabaseSchema.AQUARIUMS_KEY + "/" + aquariumId, null);
        removals.put(DatabaseSchema.TELEMETRY_KEY + "/" + aquariumId, null);

        userRef(uid)
                .updateChildren(removals)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    public void deleteUserNode(@NonNull String uid, @NonNull DbCallback callback) {
        userRef(uid)
                .removeValue()
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    @NonNull
    public ListenerHandle observeAquariums(@NonNull String uid,
                                           @NonNull AquariumsListener listener) {
        return attach(aquariumsRef(uid), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onAquariums(parseAquariums(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    @NonNull
    public static List<Aquarium> parseAquariums(@NonNull DataSnapshot aquariumsSnapshot) {
        List<Aquarium> aquariums = new ArrayList<>();
        for (DataSnapshot child : aquariumsSnapshot.getChildren()) {
            Aquarium aquarium = parseAquarium(child);
            if (aquarium != null) {
                aquariums.add(aquarium);
            }
        }
        return Collections.unmodifiableList(aquariums);
    }

    @NonNull
    public ListenerHandle observeLastInstant(@NonNull String uid,
                                             @NonNull String aquariumId,
                                             @NonNull String sensorId,
                                             @NonNull ReadingListener listener) {
        DatabaseReference ref = telemetryRef(uid, aquariumId)
                .child(sensorId)
                .child(DatabaseSchema.LAST_INSTANT_KEY);

        return attach(ref, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onReading(sensorId, parseSample(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    @NonNull
    public ListenerHandle observePeriod(@NonNull String uid,
                                        @NonNull String aquariumId,
                                        @NonNull String sensorId,
                                        @NonNull String periodKey,
                                        @NonNull BucketsListener listener) {
        return attach(periodRef(uid, aquariumId, sensorId, periodKey), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onBuckets(parsePeriod(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    public void readPeriodJson(@NonNull String uid,
                               @NonNull String aquariumId,
                               @NonNull String sensorId,
                               @NonNull String periodKey,
                               @NonNull PeriodJsonListener listener) {
        periodRef(uid, aquariumId, sensorId, periodKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            listener.onEmpty();
                            return;
                        }
                        try {
                            listener.onJson(toJsonText(snapshot));
                        } catch (JSONException exception) {
                            ScopedLogger.error("Could not render " + snapshot.getRef()
                                    + " as JSON: " + exception.getMessage());
                            listener.onError(exception);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        ScopedLogger.error("Period read cancelled: " + error.getMessage());
                        listener.onError(error.toException());
                    }
                });
    }

    public void deletePeriod(@NonNull String uid,
                             @NonNull String aquariumId,
                             @NonNull String sensorId,
                             @NonNull String periodKey,
                             @NonNull DbCallback callback) {
        periodRef(uid, aquariumId, sensorId, periodKey)
                .removeValue()
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    @NonNull
    private static String toJsonText(@NonNull DataSnapshot snapshot) throws JSONException {
        Object json = toJson(snapshot);
        return json instanceof JSONObject
                ? ((JSONObject) json).toString(JSON_INDENT_SPACES)
                : String.valueOf(json);
    }

    @NonNull
    private static Object toJson(@NonNull DataSnapshot snapshot) throws JSONException {
        if (!snapshot.hasChildren()) {
            Object value = snapshot.getValue();
            return value == null ? JSONObject.NULL : value;
        }

        JSONObject object = new JSONObject();
        for (DataSnapshot child : snapshot.getChildren()) {
            String key = child.getKey();
            if (key != null) {
                object.put(key, toJson(child));
            }
        }
        return object;
    }

    @NonNull
    private static List<SensorReading> parsePeriod(@NonNull DataSnapshot periodSnapshot) {
        Integer index = periodSnapshot.child(DatabaseSchema.INDEX_KEY).getValue(Integer.class);
        if (index == null) {
            return Collections.emptyList();
        }

        DataSnapshot bucketsSnapshot = periodSnapshot.child(DatabaseSchema.BUCKETS_KEY);
        List<SensorReading> newestFirst = new ArrayList<>();
        long newerTimestamp = Long.MAX_VALUE;

        for (int step = 0; step < DatabaseSchema.BUCKET_COUNT; step++) {
            int slot = Math.floorMod(index - step, DatabaseSchema.BUCKET_COUNT);
            SensorReading bucket =
                    parseSample(bucketsSnapshot.child(DatabaseSchema.bucketKey(slot)));
            if (bucket == null || bucket.getTimestampSeconds() > newerTimestamp) {
                break;
            }
            newestFirst.add(bucket);
            newerTimestamp = bucket.getTimestampSeconds();
        }

        Collections.reverse(newestFirst);
        return Collections.unmodifiableList(newestFirst);
    }

    @Nullable
    private static Aquarium parseAquarium(@NonNull DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String name = snapshot.child(DatabaseSchema.NAME_KEY).getValue(String.class);
        String waterType = snapshot.child(DatabaseSchema.WATER_TYPE_KEY).getValue(String.class);
        if (id == null || name == null || waterType == null) {
            ScopedLogger.error("Malformed aquarium at " + snapshot.getRef());
            return null;
        }
        return new Aquarium(
                id,
                name,
                waterType,
                parseThresholds(snapshot.child(DatabaseSchema.THRESHOLDS_KEY)),
                parseSpikeDeltas(snapshot.child(DatabaseSchema.SPIKE_DELTAS_KEY)));
    }

    @NonNull
    private static Map<String, ThresholdBand> parseThresholds(@NonNull DataSnapshot snapshot) {
        Map<String, ThresholdBand> bands = new LinkedHashMap<>();
        for (DataSnapshot sensorSnapshot : snapshot.getChildren()) {
            String sensorId = sensorSnapshot.getKey();
            Double warnLow = sensorSnapshot.child(DatabaseSchema.WARN_LOW_KEY).getValue(Double.class);
            Double safeLow = sensorSnapshot.child(DatabaseSchema.SAFE_LOW_KEY).getValue(Double.class);
            Double safeHigh = sensorSnapshot.child(DatabaseSchema.SAFE_HIGH_KEY).getValue(Double.class);
            Double warnHigh = sensorSnapshot.child(DatabaseSchema.WARN_HIGH_KEY).getValue(Double.class);
            if (sensorId == null || warnLow == null || safeLow == null
                    || safeHigh == null || warnHigh == null) {
                ScopedLogger.error("Incomplete threshold band at " + sensorSnapshot.getRef());
                continue;
            }

            ThresholdBand band = ThresholdBand.fromValues(warnLow, safeLow, safeHigh, warnHigh);
            if (band == null) {
                ScopedLogger.error("Out-of-order threshold band at " + sensorSnapshot.getRef());
                continue;
            }
            bands.put(sensorId, band);
        }
        return Collections.unmodifiableMap(bands);
    }

    @NonNull
    private static Map<String, Double> parseSpikeDeltas(@NonNull DataSnapshot snapshot) {
        Map<String, Double> deltas = new LinkedHashMap<>();
        for (DataSnapshot sensorSnapshot : snapshot.getChildren()) {
            String sensorId = sensorSnapshot.getKey();
            Double delta = sensorSnapshot.getValue(Double.class);
            if (sensorId != null && delta != null) {
                deltas.put(sensorId, delta);
            }
        }
        return Collections.unmodifiableMap(deltas);
    }

    @Nullable
    private static SensorReading parseSample(@NonNull DataSnapshot sampleSnapshot) {
        if (!sampleSnapshot.exists()) {
            return null;
        }
        Double value = sampleSnapshot.child(DatabaseSchema.VALUE_KEY).getValue(Double.class);
        Long timestamp = sampleSnapshot.child(DatabaseSchema.TIMESTAMP_KEY).getValue(Long.class);
        if (value == null || timestamp == null) {
            ScopedLogger.error("Malformed telemetry sample at " + sampleSnapshot.getRef());
            return null;
        }
        return new SensorReading(value, timestamp);
    }

    @NonNull
    private static ListenerHandle attach(@NonNull DatabaseReference ref,
                                         @NonNull ValueEventListener listener) {
        ref.addValueEventListener(listener);
        return new ValueListenerHandle(ref, listener);
    }

    private static final class ValueListenerHandle implements ListenerHandle {
        private final DatabaseReference ref;
        private final ValueEventListener listener;
        private boolean removed;

        ValueListenerHandle(@NonNull DatabaseReference ref, @NonNull ValueEventListener listener) {
            this.ref = ref;
            this.listener = listener;
        }

        @Override
        public void remove() {
            if (removed) {
                return;
            }
            removed = true;
            ref.removeEventListener(listener);
        }
    }

    @NonNull
    private DatabaseReference userRef(@NonNull String uid) {
        return database.getReference().child(uid);
    }

    @NonNull
    private DatabaseReference accountRef(@NonNull String uid) {
        return userRef(uid).child(DatabaseSchema.ACCOUNT_KEY);
    }

    @NonNull
    private DatabaseReference aquariumsRef(@NonNull String uid) {
        return userRef(uid).child(DatabaseSchema.AQUARIUMS_KEY);
    }

    @NonNull
    private DatabaseReference telemetryRef(@NonNull String uid, @NonNull String aquariumId) {
        return userRef(uid).child(DatabaseSchema.TELEMETRY_KEY).child(aquariumId);
    }

    @NonNull
    private DatabaseReference periodRef(@NonNull String uid,
                                        @NonNull String aquariumId,
                                        @NonNull String sensorId,
                                        @NonNull String periodKey) {
        return telemetryRef(uid, aquariumId).child(sensorId).child(periodKey);
    }

    private static void report(boolean successful,
                               @Nullable Exception exception,
                               @NonNull DbCallback callback) {
        if (successful) {
            callback.onSuccess();
            return;
        }
        callback.onError(exception);
    }
}
