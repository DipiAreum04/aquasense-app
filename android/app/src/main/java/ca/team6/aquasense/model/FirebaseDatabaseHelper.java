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

    public interface AquariumsListener {
        void onAquariums(@NonNull List<Aquarium> aquariums);

        void onError(@NonNull DatabaseError error);
    }

    public interface BucketsListener {
        /**
         * A period's buckets, oldest first. Empty when the board has committed none. Delivered
         * again on every change, so a screen holding one of these redraws rather than refetches.
         */
        void onBuckets(@NonNull List<SensorReading> buckets);

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

    // TODO: Remove this and add a new method to allocate received deviceUID as the aquarium key
    // Unused while AquariumRepository hardcodes the mock board's UID as the key; kept because an
    // aquarium may yet need a key of its own before a board is paired to it.
    @SuppressWarnings("unused")
    @Nullable
    public String newAquariumId(@NonNull String uid) {
        return aquariumsRef(uid).push().getKey();
    }

    /**
     * Writes /{uid}/aquariums/{aquariumId}, whose key is the paired board's UID.
     * Merges rather than replaces,so even if the user changes the aquarium name,
     * the thresholds and spike deltas will still be there.
     */
    // TODO: editing one sensor's band must not wipe the whole thresholds node. 
    // Update the deeper "thresholds/{sensor}" path, which Firebase merges per path segment.
    public void writeAquarium(@NonNull String uid,
                              @NonNull String aquariumId,
                              @NonNull String name,
                              @NonNull String waterType,
                              @NonNull Map<String, ThresholdBand> thresholds,
                              @NonNull DbCallback callback) {
        Map<String, Object> aquarium = new HashMap<>();
        aquarium.put(DatabaseSchema.NAME_KEY, name);
        aquarium.put(DatabaseSchema.WATER_TYPE_KEY, waterType);
        if (!thresholds.isEmpty()) {
            aquarium.put(DatabaseSchema.THRESHOLDS_KEY, thresholdsToMap(thresholds));
        }

        aquariumsRef(uid)
                .child(aquariumId)
                .updateChildren(aquarium)
                .addOnCompleteListener(task -> report(task.isSuccessful(), task.getException(), callback));
    }

    // Turns bands into the four-key nodes the schema stores them as. All four go in together,
    // since parseThresholds drops any band that is missing one of them.
    @NonNull
    private static Map<String, Object> thresholdsToMap(@NonNull Map<String, ThresholdBand> thresholds) {
        Map<String, Object> bySensor = new HashMap<>();
        for (Map.Entry<String, ThresholdBand> entry : thresholds.entrySet()) {
            ThresholdBand band = entry.getValue();
            Map<String, Object> bounds = new HashMap<>();
            bounds.put(DatabaseSchema.WARN_LOW_KEY, band.getWarnLow());
            bounds.put(DatabaseSchema.SAFE_LOW_KEY, band.getSafeLow());
            bounds.put(DatabaseSchema.SAFE_HIGH_KEY, band.getSafeHigh());
            bounds.put(DatabaseSchema.WARN_HIGH_KEY, band.getWarnHigh());
            bySensor.put(entry.getKey(), bounds);
        }
        return bySensor;
    }

    // Removes an aquarium along with the telemetry recorded under it. Both paths go in one
    // update so the aquarium can never vanish from the picker while its telemetry subtree
    // survives with no owner to ever delete it.
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

    /**
     * Watches a single sensor's latest sample, at
     * /{uid}/telemetry/{aquariumId}/{sensorId}/last_instant.
     *
     * <p>Subscribe once per sensor rather than once on their shared parent. The parent also holds
     * six periods of a hundred buckets each, so a listener there syncs and caches hundreds of KB
     * to reach four numbers, and re-fires every time a bucket closes.
     */
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

    /**
     * Watches one period node - /{uid}/telemetry/{aquariumId}/{sensorId}/{period} - and reports its
     * buckets in chronological order, again on every change.
     *
     * <p>This is the payload {@link #observeLastInstant} is deliberately shaped to avoid: a period
     * is a hundred buckets, and watching one means being woken every time the board closes another.
     * That is the cost of a graph that follows the board rather than showing the moment it was
     * opened, and it is only paid while a screen is actually plotting the period - hence the handle,
     * which the caller is expected to release the moment it plots something else.
     */
    @NonNull
    public ListenerHandle observePeriod(@NonNull String uid,
                                        @NonNull String aquariumId,
                                        @NonNull String sensorId,
                                        @NonNull String periodKey,
                                        @NonNull BucketsListener listener) {
        DatabaseReference ref = telemetryRef(uid, aquariumId)
                .child(sensorId)
                .child(periodKey);

        return attach(ref, new ValueEventListener() {
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

    /**
     * Unrolls a period's ring buffer into chronological order.
     *
     * <p>{@code index} names the slot holding the newest bucket, so the walk runs backwards from
     * there and reverses at the end. It stops on the first slot the ring has not reached yet, and
     * on one whose timestamp is newer than the bucket ahead of it: a board that restarted its
     * cursor leaves later readings sitting in slots the walk is about to call older, and those are
     * stale rather than history.
     */
    @NonNull
    private static List<SensorReading> parsePeriod(@NonNull DataSnapshot periodSnapshot) {
        Integer index = periodSnapshot.child(DatabaseSchema.INDEX_KEY).getValue(Integer.class);
        if (index == null) {
            // No cursor means nothing has ever been committed here, which is not a malformed node.
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

    // Reads {timestamp, value}, the shape both last_instant and every bucket in a period share.
    // Returns null when the node is absent or malformed; a partially written node is treated as no
    // reading rather than a zero one.
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
