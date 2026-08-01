package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single entry point for every read and write against the Realtime Database.
 */
public final class FirebaseDatabaseHelper {

    public interface DbCallback {
        void onSuccess();

        void onError(@Nullable Exception exception);
    }

    /** Releases a live subscription. Safe to call more than once. */
    public interface ListenerHandle {
        void remove();
    }

    public interface ReadingListener {
        void onReading(@NonNull String sensorId, @Nullable SensorReading reading);

        void onError(@NonNull DatabaseError error);
    }

    public interface TelemetryListener {
        void onTelemetry(@NonNull Map<String, SensorReading> readingsBySensorId);

        void onError(@NonNull DatabaseError error);
    }

    public interface AquariumsListener {
        void onAquariums(@NonNull List<Aquarium> aquariums);

        void onError(@NonNull DatabaseError error);
    }

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

    // Writes /{uid}/account/{name,email}. Both keys are required by the schema, so callers that
    // have no display name should pass an empty string rather than omitting it.
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

    // Writes /{uid}/account/name without touching the sibling email key.
    public void updateAccountName(@NonNull String uid,
                                  @NonNull String name,
                                  @NonNull DbCallback callback) {
        accountRef(uid)
                .child(DatabaseSchema.NAME_KEY)
                .setValue(name)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    // Removes the whole /{uid} subtree: account, aquariums and telemetry.
    public void deleteUserNode(@NonNull String uid, @NonNull DbCallback callback) {
        userRef(uid)
                .removeValue()
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    // Watches /{uid}/aquariums, which maps each aquarium ID to its display name. The dashboard
    // needs this before it can watch telemetry, since the ID is part of every telemetry path.
    @NonNull
    public ListenerHandle observeAquariums(@NonNull String uid,
                                           @NonNull AquariumsListener listener) {
        return attach(aquariumsRef(uid), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Aquarium> aquariums = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Aquarium aquarium = parseAquarium(child);
                    if (aquarium != null) {
                        aquariums.add(aquarium);
                    }
                }
                listener.onAquariums(Collections.unmodifiableList(aquariums));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    // Watches every sensor of one aquarium through a single subscription on
    // /{uid}/telemetry/{aquariumId}. Preferred over four observeLastInstant() calls when a screen
    // shows the whole set, since the four sensor nodes arrive in one snapshot.
    @NonNull
    public ListenerHandle observeTelemetry(@NonNull String uid,
                                           @NonNull String aquariumId,
                                           @NonNull TelemetryListener listener) {
        return attach(telemetryRef(uid, aquariumId), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, SensorReading> readings = new LinkedHashMap<>();
                // Iterate what is present rather than the four known sensor keys, so a sensor the
                // board has not published yet is simply absent instead of a null entry.
                for (DataSnapshot sensorSnapshot : snapshot.getChildren()) {
                    String sensorId = sensorSnapshot.getKey();
                    if (sensorId == null) {
                        continue;
                    }
                    SensorReading reading =
                            parseInstant(sensorSnapshot.child(DatabaseSchema.LAST_INSTANT_KEY));
                    if (reading != null) {
                        readings.put(sensorId, reading);
                    }
                }
                listener.onTelemetry(Collections.unmodifiableMap(readings));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    // Watches a single sensor's latest sample. For one card or detail sheet; use observeTelemetry()
    // when the screen needs the whole aquarium.
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
                listener.onReading(sensorId, parseInstant(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        });
    }

    // Reads one /{uid}/aquariums/{aquariumId} entry. Name and water type are required by the
    // schema, so an entry missing either is skipped rather than surfaced half-built.
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

    // Reads the optional thresholds map, keyed by sensor ID. An absent node yields an empty map,
    // which is indistinguishable from an empty one in Realtime Database: a node with no children
    // does not exist, so "no bands configured" and "thresholds key missing" are the same state.
    @NonNull
    private static Map<String, ThresholdBand> parseThresholds(@NonNull DataSnapshot snapshot) {
        Map<String, ThresholdBand> bands = new LinkedHashMap<>();
        for (DataSnapshot sensorSnapshot : snapshot.getChildren()) {
            String sensorId = sensorSnapshot.getKey();
            Double warnLow = sensorSnapshot.child(DatabaseSchema.WARN_LOW_KEY).getValue(Double.class);
            Double safeLow = sensorSnapshot.child(DatabaseSchema.SAFE_LOW_KEY).getValue(Double.class);
            Double safeHigh = sensorSnapshot.child(DatabaseSchema.SAFE_HIGH_KEY).getValue(Double.class);
            Double warnHigh = sensorSnapshot.child(DatabaseSchema.WARN_HIGH_KEY).getValue(Double.class);
            // The schema requires all four together, so a partial band is dropped whole rather
            // than defaulted, which would silently classify readings against a made-up range.
            if (sensorId == null || warnLow == null || safeLow == null
                    || safeHigh == null || warnHigh == null) {
                ScopedLogger.error("Incomplete threshold band at " + sensorSnapshot.getRef());
                continue;
            }
            bands.put(sensorId, new ThresholdBand(warnLow, safeLow, safeHigh, warnHigh));
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

    // Reads {timestamp, value} from a last_instant node. Returns null when the node is absent or
    // malformed; a partially written node is treated as no reading rather than a zero one.
    @Nullable
    private static SensorReading parseInstant(@NonNull DataSnapshot instantSnapshot) {
        if (!instantSnapshot.exists()) {
            return null;
        }
        Double value = instantSnapshot.child(DatabaseSchema.VALUE_KEY).getValue(Double.class);
        Long timestamp = instantSnapshot.child(DatabaseSchema.TIMESTAMP_KEY).getValue(Long.class);
        if (value == null || timestamp == null) {
            ScopedLogger.error("Malformed last_instant at " + instantSnapshot.getRef());
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
            // Idempotent: a screen may release its handles from both onStop() and onDestroyView().
            if (removed) {
                return;
            }
            removed = true;
            ref.removeEventListener(listener);
        }
    }

    // The UID is the root key, so every path starts here.
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
