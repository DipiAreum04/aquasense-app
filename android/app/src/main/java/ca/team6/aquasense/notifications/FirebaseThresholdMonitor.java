package ca.team6.aquasense.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.FirebaseDatabaseHelper;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;


public final class FirebaseThresholdMonitor {

    // The sensors NF-1.1 alerts on, matching the telemetry node names in the schema.
    private static final String[] MONITORED_SENSORS = {
            DatabaseSchema.TEMPERATURE_KEY,
            DatabaseSchema.PH_LEVEL_KEY,
            DatabaseSchema.DISSOLVED_SOLIDS_KEY,
            DatabaseSchema.WATER_LEVEL_KEY,
    };

    private final Context appContext;
    private final SettingsRepository settingsRepository;
    private final ThresholdAlertDedupeStore dedupeStore;
    private final Map<String, AquariumWatch> watches = new HashMap<>();

    @Nullable
    private DatabaseReference aquariumsRef;
    @Nullable
    private String attachedUserId;
    private boolean authListenerRegistered;

    private final FirebaseAuth.AuthStateListener authStateListener = auth -> attachToCurrentUser();

    private final ValueEventListener aquariumsListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            syncWatches(FirebaseDatabaseHelper.parseAquariums(snapshot));
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            ScopedLogger.error("Aquarium list listener cancelled: " + error.getMessage());
        }
    };

    public FirebaseThresholdMonitor(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.settingsRepository = new SettingsRepository(appContext);
        this.dedupeStore = new ThresholdAlertDedupeStore(appContext);
    }

    public void start() {
        if (authListenerRegistered) {
            attachToCurrentUser();
            return;
        }
        authListenerRegistered = true;
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    public void stop() {
        if (authListenerRegistered) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
            authListenerRegistered = false;
        }
        detach();
    }

    private void attachToCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            detach();
            return;
        }
        if (user.getUid().equals(attachedUserId) && aquariumsRef != null) {
            return;
        }

        detach();
        attachedUserId = user.getUid();
        aquariumsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(user.getUid())
                .child(DatabaseSchema.AQUARIUMS_KEY);
        aquariumsRef.addValueEventListener(aquariumsListener);
    }

    /** Releases every subscription but leaves the auth listener in place. */
    private void detach() {
        if (aquariumsRef != null) {
            aquariumsRef.removeEventListener(aquariumsListener);
        }
        aquariumsRef = null;
        attachedUserId = null;

        for (AquariumWatch watch : watches.values()) {
            watch.stop();
        }
        watches.clear();
    }

    private void syncWatches(@NonNull List<Aquarium> aquariums) {
        String uid = attachedUserId;
        if (uid == null) {
            return;
        }

        Set<String> present = new HashSet<>();
        for (Aquarium aquarium : aquariums) {
            present.add(aquarium.getId());
            AquariumWatch watch = watches.get(aquarium.getId());
            if (watch == null) {
                watch = new AquariumWatch(
                        appContext, settingsRepository, dedupeStore, uid, aquarium);
                watches.put(aquarium.getId(), watch);
                watch.start();
            } else {
                // Bands and the display name are editable, so refresh them without tearing the
                // sensor listeners down and re-downloading every reading.
                watch.updateAquarium(aquarium);
            }
        }

        List<String> removed = new ArrayList<>();
        for (String aquariumId : watches.keySet()) {
            if (!present.contains(aquariumId)) {
                removed.add(aquariumId);
            }
        }
        for (String aquariumId : removed) {
            AquariumWatch watch = watches.remove(aquariumId);
            if (watch != null) {
                watch.stop();
            }
        }
    }

    // One aquarium's set of sensor subscriptions and hub connectivity watch.
    private static final class AquariumWatch {

        private final Context appContext;
        private final SettingsRepository settingsRepository;
        private final ThresholdAlertDedupeStore dedupeStore;
        private final String uid;
        private final Handler hubCheckHandler = new Handler(Looper.getMainLooper());
        private final List<SensorWatch> sensorWatches = new ArrayList<>();

        private volatile Aquarium aquarium;
        private long latestTelemetryEpochSeconds;
        private boolean hasSeenTelemetry;

        private final Runnable hubCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkHubConnectivity();
                hubCheckHandler.postDelayed(this, SensorThresholds.HUB_CHECK_INTERVAL_MS);
            }
        };

        AquariumWatch(
                Context appContext,
                SettingsRepository settingsRepository,
                ThresholdAlertDedupeStore dedupeStore,
                String uid,
                Aquarium aquarium
        ) {
            this.appContext = appContext;
            this.settingsRepository = settingsRepository;
            this.dedupeStore = dedupeStore;
            this.uid = uid;
            this.aquarium = aquarium;
        }

        void start() {
            for (String sensorId : MONITORED_SENSORS) {
                SensorWatch watch = new SensorWatch(
                        appContext, settingsRepository, dedupeStore, this, sensorId);
                sensorWatches.add(watch);
                watch.start(uid, aquarium.getId());
            }
            hubCheckHandler.post(hubCheckRunnable);
        }

        void stop() {
            hubCheckHandler.removeCallbacks(hubCheckRunnable);
            for (SensorWatch watch : sensorWatches) {
                watch.stop();
            }
            sensorWatches.clear();
        }

        void updateAquarium(Aquarium updated) {
            this.aquarium = updated;
        }

        Aquarium aquarium() {
            return aquarium;
        }

        void reportTelemetryTimestamp(long epochSeconds) {
            hasSeenTelemetry = true;
            if (epochSeconds > latestTelemetryEpochSeconds) {
                latestTelemetryEpochSeconds = epochSeconds;
            }
        }

        private void checkHubConnectivity() {
            if (!hasSeenTelemetry) {
                return;
            }
            long nowEpochSeconds = System.currentTimeMillis() / 1000L;
            if (nowEpochSeconds - latestTelemetryEpochSeconds
                    <= SensorThresholds.HUB_SILENCE_TIMEOUT_SECONDS) {
                return;
            }
            settingsRepository.loadSettings(this::handleHubSilence);
        }

        private void handleHubSilence(AppSettings settings) {
            if (!alertDeliveryEnabled(settings, AlertType.HUB)) {
                return;
            }
            Aquarium current = aquarium;
            ThresholdViolation violation = ThresholdViolation.hubDisconnected(current.getId());
            if (!shouldDeliverAlert(settings, violation, SensorThresholds.HUB_DEDUPE_KEY)) {
                return;
            }
            // checkHubConnectivity runs once a minute for as long as the hub stays silent, so
            // without the cooldown this re-fires every tick.
            if (!dedupeStore.cooldownElapsed(violation.cooldownKey())) {
                return;
            }
            AquasenseNotificationHelper.showThresholdAlert(
                    appContext, violation, current.getName());
            dedupeStore.saveLastNotificationTime(
                    violation.cooldownKey(), System.currentTimeMillis());
        }
    }

    /** One sensor's {@code last_instant} subscription. */
    private static final class SensorWatch implements ValueEventListener {

        private final Context appContext;
        private final SettingsRepository settingsRepository;
        private final ThresholdAlertDedupeStore dedupeStore;
        private final AquariumWatch parent;
        private final String sensorId;

        private long lastProcessedTimestamp;
        @Nullable
        private Double lastProcessedValue;

        @Nullable
        private DatabaseReference ref;

        SensorWatch(
                Context appContext,
                SettingsRepository settingsRepository,
                ThresholdAlertDedupeStore dedupeStore,
                AquariumWatch parent,
                String sensorId
        ) {
            this.appContext = appContext;
            this.settingsRepository = settingsRepository;
            this.dedupeStore = dedupeStore;
            this.parent = parent;
            this.sensorId = sensorId;

            String aquariumId = parent.aquarium().getId();
            this.lastProcessedTimestamp =
                    dedupeStore.loadLastProcessedTimestamp(aquariumId, sensorId);
            this.lastProcessedValue = dedupeStore.loadLastProcessedValue(aquariumId, sensorId);
        }

        void start(String uid, String aquariumId) {
            ref = FirebaseDatabase.getInstance()
                    .getReference()
                    .child(uid)
                    .child(DatabaseSchema.TELEMETRY_KEY)
                    .child(aquariumId)
                    .child(sensorId)
                    .child(DatabaseSchema.LAST_INSTANT_KEY);
            ref.addValueEventListener(this);
        }

        void stop() {
            if (ref != null) {
                ref.removeEventListener(this);
            }
            ref = null;
        }

        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            SensorTelemetryReading reading =
                    SensorTelemetryReading.fromInstantSnapshot(snapshot);
            if (reading == null) {
                return;
            }
            // Firebase replays the current value on (re)attach, so without this a reconnect would
            // re-alert on a sample that was already handled.
            if (reading.timestamp <= lastProcessedTimestamp) {
                return;
            }
            settingsRepository.loadSettings(settings -> handleReading(settings, reading));
        }

        private void handleReading(AppSettings settings, SensorTelemetryReading reading) {
            Aquarium aquarium = parent.aquarium();
            String aquariumId = aquarium.getId();

            parent.reportTelemetryTimestamp(reading.timestamp);
            lastProcessedTimestamp = reading.timestamp;

            if (DatabaseSchema.isOffline(reading.value)) {
                handleOfflineReading(settings, aquarium, aquariumId, reading);
                return;
            }

            if (ThresholdNotificationEvaluator.isNotAMeasurement(reading.value)) {
                dedupeStore.saveLastProcessed(
                        aquariumId,
                        sensorId,
                        reading.timestamp,
                        lastProcessedValue == null ? reading.value : lastProcessedValue);
                return;
            }

            if (!paramAlertsEnabled(settings) && !spikeAlertsEnabled(settings)) {
                // Still recorded, so re-enabling alerts compares against the real previous sample
                // instead of reading the first value after the gap as a spike.
                rememberSample(aquariumId, reading);
                return;
            }

            double spikeDelta = spikeAlertsEnabled(settings)
                    ? SensorThresholds.resolveSpikeDelta(aquarium, sensorId)
                    : Double.NaN;

            List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                    aquariumId,
                    sensorId,
                    resolveBand(aquarium, sensorId),
                    spikeDelta,
                    reading.value,
                    lastProcessedValue);

            deliverViolations(settings, aquarium, aquariumId, violations, reading);
        }

        private void handleOfflineReading(
                AppSettings settings,
                Aquarium aquarium,
                String aquariumId,
                SensorTelemetryReading reading
        ) {
            dedupeStore.saveLastProcessed(
                    aquariumId,
                    sensorId,
                    reading.timestamp,
                    lastProcessedValue == null ? reading.value : lastProcessedValue);

            if (!sensorOfflineAlertsEnabled(settings)) {
                return;
            }

            ThresholdViolation offline = ThresholdViolation.sensorOffline(aquariumId, sensorId);
            deliverViolations(
                    settings,
                    aquarium,
                    aquariumId,
                    Collections.singletonList(offline),
                    reading);
        }

        private void deliverViolations(
                AppSettings settings,
                Aquarium aquarium,
                String aquariumId,
                List<ThresholdViolation> violations,
                SensorTelemetryReading reading
        ) {
            // Every violation that clears its own window, not just the one that gets shown. The
            // shade only has room for the most serious, but a spike the band breach outranked was
            // still reported to the user by that notification, so it opens its window too --
            // otherwise it fires unsuppressed on the very next sample.
            List<ThresholdViolation> notifiable = new ArrayList<>(violations.size());
            ThresholdViolation notifyViolation = null;
            for (ThresholdViolation violation : violations) {
                if (violation.kind == ThresholdViolation.ViolationKind.THRESHOLD
                        && !paramAlertsEnabled(settings)) {
                    continue;
                }
                if (violation.kind == ThresholdViolation.ViolationKind.SPIKE
                        && !spikeAlertsEnabled(settings)) {
                    continue;
                }
                if (!shouldDeliverAlert(settings, violation, sensorId)) {
                    continue;
                }
                // Cooldowns are per condition, so they are applied before the winner is picked:
                // a muted band breach must not swallow a spike that is still allowed to speak.
                if (dedupeStore.cooldownElapsed(violation.cooldownKey())) {
                    notifiable.add(violation);
                    notifyViolation = pickHigherPriorityViolation(notifyViolation, violation);
                }
            }

            if (notifyViolation != null) {
                AquasenseNotificationHelper.showThresholdAlert(
                        appContext, notifyViolation, aquarium.getName());
                long notifiedAt = System.currentTimeMillis();
                for (ThresholdViolation notified : notifiable) {
                    dedupeStore.saveLastNotificationTime(notified.cooldownKey(), notifiedAt);
                }
            }

            if (!DatabaseSchema.isOffline(reading.value)) {
                rememberSample(aquariumId, reading);
            }
        }

        @Nullable
        private static ThresholdViolation pickHigherPriorityViolation(
                @Nullable ThresholdViolation current,
                @NonNull ThresholdViolation candidate
        ) {
            if (current == null) {
                return candidate;
            }
            if (candidate.isCritical() && !current.isCritical()) {
                return candidate;
            }
            if (candidate.kind == ThresholdViolation.ViolationKind.THRESHOLD
                    && current.kind == ThresholdViolation.ViolationKind.SPIKE) {
                return candidate;
            }
            return current;
        }

        private void rememberSample(String aquariumId, SensorTelemetryReading reading) {
            lastProcessedValue = reading.value;
            dedupeStore.saveLastProcessed(
                    aquariumId, sensorId, reading.timestamp, reading.value);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            ScopedLogger.error("Telemetry listener cancelled for " + sensorId + ": "
                    + error.getMessage());
        }
    }

    private enum AlertType {
        PARAM,
        SPIKE,
        SENSOR_OFFLINE,
        HUB
    }

    private static boolean alertDeliveryEnabled(AppSettings settings, AlertType type) {
        if (!settings.pushNotifications || settings.feedingModeSilence) {
            return false;
        }
        switch (type) {
            case PARAM:
                return settings.notifyParamOutOfRange;
            case SPIKE:
                return settings.notifyAbnormalJumps;
            case SENSOR_OFFLINE:
                return settings.notifySensorDisconnected;
            case HUB:
                return settings.notifyHubDisconnected;
            default:
                return false;
        }
    }

    private static boolean paramAlertsEnabled(AppSettings settings) {
        return alertDeliveryEnabled(settings, AlertType.PARAM);
    }

    private static boolean spikeAlertsEnabled(AppSettings settings) {
        return alertDeliveryEnabled(settings, AlertType.SPIKE);
    }

    private static boolean sensorOfflineAlertsEnabled(AppSettings settings) {
        return alertDeliveryEnabled(settings, AlertType.SENSOR_OFFLINE);
    }

    private static boolean shouldDeliverAlert(
            AppSettings settings,
            ThresholdViolation violation,
            String sensorId
    ) {
        if (violation.isCritical()) {
            return true;
        }
        if (!isSensorAlertsEnabled(settings, sensorId)) {
            return false;
        }
        if (settings.criticalAlertsOnly) {
            return false;
        }
        return !settings.quietHours
                || !QuietHours.isActiveNow(settings.quietHoursStart, settings.quietHoursEnd);
    }

    private static boolean isSensorAlertsEnabled(AppSettings settings, String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return settings.sensorAlertsTemperature;
            case DatabaseSchema.WATER_LEVEL_KEY:
                return settings.sensorAlertsWaterLevel;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return settings.sensorAlertsDissolvedSolids;
            case DatabaseSchema.PH_LEVEL_KEY:
                return settings.sensorAlertsPhLevel;
            default:
                return true;
        }
    }

    @Nullable
    private static ThresholdBand resolveBand(Aquarium aquarium, String sensorId) {
        ThresholdBand configured = aquarium.thresholdFor(sensorId);
        if (configured != null) {
            return configured;
        }
        AquariumTemplate template =
                BuiltInTemplates.forWaterType(WaterType.fromKey(aquarium.getWaterType()));
        // A sensor the water type cannot measure stays unmonitored rather than alerting on a band
        // borrowed from a different kind of tank.
        if (!template.isSensorApplicable(sensorId)) {
            return null;
        }
        return template.getThresholds(sensorId);
    }

}
