package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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
    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle handle;

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

        this.handle = this.database.observeTelemetry(uid, aquariumId,
                new FirebaseDatabaseHelper.TelemetryListener() {
                    @Override
                    public void onTelemetry(@NonNull Map<String, SensorReading> readingsBySensorId) {
                        publish(readingsBySensorId);
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        ScopedLogger.error("Telemetry subscription cancelled: " + error.getMessage());
                        detach();
                        watchedUid = null;
                        watchedAquariumId = null;
                        publish(Collections.emptyMap());
                    }
                });
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
        if (this.handle != null) {
            this.handle.remove();
            this.handle = null;
        }
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
