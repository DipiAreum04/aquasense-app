package ca.team6.aquasense.aquarium;

import ca.team6.aquasense.firebase.FirebaseDatabaseHelper;
import ca.team6.aquasense.logging.ScopedLogger;
import ca.team6.aquasense.settings.SharedPreferenceHelper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AquariumRepository {

    public interface AquariumsObserver {
        void onAquariumsChanged(@NonNull List<Aquarium> aquariums);
    }

    public interface WriteCallback {
        void onSuccess();

        void onError();
    }

    public static final int MAX_AQUARIUMS_LIMIT = 50;

    private static final String KEY_ACTIVE_AQUARIUM_ID = "activeAquariumId";

    private static AquariumRepository instance;

    private final SharedPreferenceHelper prefs;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CopyOnWriteArrayList<AquariumsObserver> observers = new CopyOnWriteArrayList<>();

    private List<Aquarium> aquariums = Collections.emptyList();
    private boolean loaded;

    @Nullable
    private String subscribedUid;
    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle handle;

    private AquariumRepository(Context context) {
        this.prefs = SharedPreferenceHelper.getInstance(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabaseHelper.getInstance();
        this.firebaseAuth.addAuthStateListener(auth -> onUserChanged(auth.getCurrentUser()));
    }

    public static AquariumRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AquariumRepository(context);
        }
        return instance;
    }

    @NonNull
    public List<Aquarium> getAquariums() {
        return this.aquariums;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    @Nullable
    public Aquarium getAquarium(@Nullable String aquariumId) {
        if (aquariumId == null || aquariumId.isEmpty()) {
            return null;
        }
        for (Aquarium aquarium : this.aquariums) {
            if (aquarium.getId().equals(aquariumId)) {
                return aquarium;
            }
        }
        return null;
    }

    @Nullable
    public Aquarium getActiveAquarium() {
        Aquarium selected = this.getAquarium(this.getActiveAquariumId());
        if (selected != null) {
            return selected;
        }
        return this.aquariums.isEmpty() ? null : this.aquariums.get(0);
    }

    @NonNull
    public String getActiveAquariumId() {
        if (this.prefs == null) {
            return "";
        }
        return this.prefs.getString(KEY_ACTIVE_AQUARIUM_ID, "");
    }

    public void setActiveAquariumId(@NonNull String aquariumId) {
        if (this.prefs == null) {
            return;
        }
        this.prefs.setString(KEY_ACTIVE_AQUARIUM_ID, aquariumId);
    }

    public void addObserver(@NonNull AquariumsObserver observer) {
        this.observers.addIfAbsent(observer);
        observer.onAquariumsChanged(this.aquariums);
    }

    public void removeObserver(@NonNull AquariumsObserver observer) {
        this.observers.remove(observer);
    }

    public void addPairedAquarium(@NonNull String deviceUid,
                                  @NonNull NewAquariumConfig config,
                                  @NonNull WriteCallback callback) {
        String uid = this.currentUid();
        if (uid == null) {
            ScopedLogger.error("Cannot pair an aquarium while signed out.");
            callback.onError();
            return;
        }
        if (deviceUid.isEmpty()) {
            ScopedLogger.error("Cannot pair because the device is missing credentials.");
            callback.onError();
            return;
        }

        this.write(uid, deviceUid, config, callback);
    }

    private void write(@NonNull String uid,
                       @NonNull String aquariumId,
                       @NonNull NewAquariumConfig config,
                       @NonNull WriteCallback callback) {
        this.database.writeAquarium(uid, aquariumId, config.getName(),
                config.getWaterType().getKey(), config.getThresholds(), config.getSpikeDeltas(),
                new FirebaseDatabaseHelper.DbCallback() {
                    @Override
                    public void onSuccess() {
                        setActiveAquariumId(aquariumId);
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        ScopedLogger.error("Failed to create aquarium: " + exception);
                        callback.onError();
                    }
                });
    }

    public void saveSensorThresholds(@NonNull Aquarium aquarium,
                                     @NonNull Map<String, ThresholdBand> thresholds,
                                     @NonNull Map<String, Double> spikeDeltas,
                                     @NonNull WriteCallback callback) {
        String uid = this.currentUid();
        if (uid == null) {
            ScopedLogger.error("Cannot save thresholds while signed out.");
            callback.onError();
            return;
        }

        this.database.writeSensorThresholds(uid, aquarium.getId(), thresholds, spikeDeltas,
                new FirebaseDatabaseHelper.DbCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        ScopedLogger.error("Failed to save thresholds: " + exception);
                        callback.onError();
                    }
                });
    }

    public boolean canRemoveAquarium() {
        return this.aquariums.size() > 1;
    }

    public void removeAquarium(@NonNull Aquarium aquarium, @NonNull WriteCallback callback) {
        String uid = this.currentUid();
        if (uid == null) {
            ScopedLogger.error("Cannot remove an aquarium while signed out.");
            callback.onError();
            return;
        }
        if (!this.canRemoveAquarium()) {
            ScopedLogger.error("Refused to remove the user's last aquarium.");
            callback.onError();
            return;
        }

        this.database.deleteAquarium(uid, aquarium.getId(), new FirebaseDatabaseHelper.DbCallback() {
            @Override
            public void onSuccess() {
                if (aquarium.getId().equals(getActiveAquariumId())) {
                    setActiveAquariumId("");
                }
                callback.onSuccess();
            }

            @Override
            public void onError(@Nullable Exception exception) {
                ScopedLogger.error("Failed to remove aquarium: " + exception);
                callback.onError();
            }
        });
    }

    private void onUserChanged(@Nullable FirebaseUser user) {
        String uid = user != null ? user.getUid() : null;
        if (Objects.equals(uid, this.subscribedUid)) {
            return;
        }

        this.detach();
        this.subscribedUid = uid;

        if (uid == null) {
            this.publish(Collections.emptyList(), false);
            return;
        }
        this.attach(uid);
    }

    private void attach(@NonNull String uid) {
        this.handle = this.database.observeAquariums(uid,
                new FirebaseDatabaseHelper.AquariumsListener() {
                    @Override
                    public void onAquariums(@NonNull List<Aquarium> aquariums) {
                        publish(aquariums, true);
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        ScopedLogger.error("Aquariums subscription cancelled: " + error.getMessage());
                        detach();
                        subscribedUid = null;
                        publish(Collections.emptyList(), false);
                    }
                });
    }

    private void detach() {
        if (this.handle != null) {
            this.handle.remove();
            this.handle = null;
        }
    }

    private void publish(@NonNull List<Aquarium> aquariums, boolean loaded) {
        this.aquariums = aquariums;
        this.loaded = loaded;
        for (AquariumsObserver observer : this.observers) {
            observer.onAquariumsChanged(aquariums);
        }
    }

    @Nullable
    private String currentUid() {
        FirebaseUser user = this.firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
