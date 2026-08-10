package ca.team6.aquasense.model;

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

import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;

/**
 * Live view of the signed-in user's /{uid}/aquariums node.
 *
 * <p>The list is never fetched on demand. The repository subscribes once, when the user signs in,
 * and Realtime Database pushes every later change into the cache behind {@link #getAquariums()},
 * which is therefore a plain in-memory read that never blocks and never hits the network.
 *
 * <p>Because the first snapshot arrives asynchronously, a screen built immediately after login can
 * see an empty list that fills in moments later. Screens that must redraw when that happens
 * register an {@link AquariumsObserver}; {@link #isLoaded()} distinguishes "this user has no
 * aquariums" from "the first snapshot has not arrived yet".
 *
 * <p>Firebase delivers both auth and database callbacks on the main thread, and every caller is UI
 * code, so the cache is main-thread confined and needs no locking.
 */
public class AquariumRepository {

    /** Notified with the full list on every change, including the first snapshot after login. */
    public interface AquariumsObserver {
        void onAquariumsChanged(@NonNull List<Aquarium> aquariums);
    }

    /** Result of a write. The cache is updated by the subscription, not by the callback. */
    public interface WriteCallback {
        void onSuccess();

        void onError();
    }

    // Professional hobbyists / content creators may run large numbers of aquariums.
    public static final int MAX_AQUARIUMS_LIMIT = 50;

    // Holds the database key of the aquarium the dashboard is showing. Deliberately a different
    // key from the old "activeDeviceId": that stored a mock hardware address, which matches
    // nothing in the database and would resolve to no aquarium at all.
    private static final String KEY_ACTIVE_AQUARIUM_ID = "activeAquariumId";

    private static AquariumRepository instance;

    private final SharedPreferenceHelper prefs;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabaseHelper database;
    private final CopyOnWriteArrayList<AquariumsObserver> observers = new CopyOnWriteArrayList<>();

    private List<Aquarium> aquariums = Collections.emptyList();
    private boolean loaded;

    // The UID the live subscription belongs to, or null when nobody is signed in. Kept so a token
    // refresh, which re-fires the auth listener for the same user, does not tear down and rebuild
    // a subscription that is already correct.
    @Nullable
    private String subscribedUid;
    @Nullable
    private FirebaseDatabaseHelper.ListenerHandle handle;

    private AquariumRepository(Context context) {
        this.prefs = SharedPreferenceHelper.getInstance(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabaseHelper.getInstance();
        // Fires immediately with the current user and again on every login and sign-out, so the
        // subscription follows the session on its own instead of depending on each entry point
        // into the app remembering to start it.
        this.firebaseAuth.addAuthStateListener(auth -> onUserChanged(auth.getCurrentUser()));
    }

    public static AquariumRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AquariumRepository(context);
        }
        return instance;
    }

    /**
     * The signed-in user's aquariums, in database key order. Empty when the user has none and
     * also before the first snapshot arrives, so check {@link #isLoaded()} to tell those apart.
     *
     * <p>The returned list is the cache itself and is immutable.
     */
    @NonNull
    public List<Aquarium> getAquariums() {
        return this.aquariums;
    }

    /** True once a snapshot has arrived for the current user. Resets to false on sign-out. */
    public boolean isLoaded() {
        return this.loaded;
    }

    /** The aquarium with this database key, or null when it is absent or not yet loaded. */
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

    /**
     * The aquarium the dashboard should show: the one the user last selected, falling back to the
     * first one when that selection is gone (deleted, or made on a different device). The fallback
     * is not written back to preferences, so the original choice survives if the aquarium returns.
     *
     * @return null when the user has no aquariums, or before the first snapshot arrives.
     */
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

    /**
     * Registers for the current list and every later change. Fires once immediately, so a screen
     * renders from this callback alone and cannot forget to draw its initial state. That first
     * call carries an empty list when the snapshot has not arrived yet, which {@link #isLoaded()}
     * tells apart from a user who genuinely has no aquariums.
     *
     * <p>Callers must pair this with {@link #removeObserver} when their view goes away, or the
     * observer keeps the view alive for the life of the process.
     */
    public void addObserver(@NonNull AquariumsObserver observer) {
        this.observers.addIfAbsent(observer);
        observer.onAquariumsChanged(this.aquariums);
    }

    public void removeObserver(@NonNull AquariumsObserver observer) {
        this.observers.remove(observer);
    }

    /**
     * Creates the aquarium for a device that has just been paired, keyed by the device's own FirebaseUID.
     */
    public void addPairedAquarium(@NonNull String deviceUid,
                                  @NonNull String name,
                                  @NonNull WaterType waterType,
                                  @Nullable AquariumTemplate template,
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

        this.write(uid, deviceUid, name, waterType, template, callback);
    }

    private void write(@NonNull String uid,
                       @NonNull String aquariumId,
                       @NonNull String name,
                       @NonNull WaterType waterType,
                       @Nullable AquariumTemplate template,
                       @NonNull WriteCallback callback) {
        this.database.writeAquarium(uid, aquariumId, name, waterType.getKey(),
                template != null ? template.getAllThresholds() : Collections.emptyMap(),
                new FirebaseDatabaseHelper.DbCallback() {
                    @Override
                    public void onSuccess() {
                        // Auto-switch to the tank the user just created.
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

    /**
     * Saves the bands and spike deltas the user configured for one aquarium (SETTINGS-07).
     *
     * <p>Only the sensors named in the maps are written, so a screen that edits one sensor leaves
     * the others as they are. Nothing is updated locally: the write lands in the same node the live
     * subscription is watching, so the cache — and with it the dashboard's grading and the
     * background monitor's alerting — refreshes itself moments later.
     *
     * <p>Every band must be strictly ordered and every delta positive, which the caller is expected
     * to have validated; the database rules enforce the same and reject the write otherwise.
     */
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

    /**
     * False when removing an aquarium would leave the user with none, which the app does not
     * allow. Screens check this first so they can explain why, rather than surfacing a failure.
     */
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
            // Backstop for the check the calling screen is expected to have already made.
            ScopedLogger.error("Refused to remove the user's last aquarium.");
            callback.onError();
            return;
        }

        this.database.deleteAquarium(uid, aquarium.getId(), new FirebaseDatabaseHelper.DbCallback() {
            @Override
            public void onSuccess() {
                if (aquarium.getId().equals(getActiveAquariumId())) {
                    // Clearing the selection lets getActiveAquarium() fall back to the first
                    // aquarium, rather than pinning a second choice the user never made.
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
            // Signed out: the previous user's aquariums must not stay readable.
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
                        // Firebase does not revive a cancelled listener. Forgetting the UID as
                        // well as the handle lets the next auth callback, at the latest the next
                        // hourly token refresh, subscribe again.
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
