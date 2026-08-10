package ca.team6.aquasense.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ThresholdAlertDedupeStore {

    private static final String PREFS_NAME = "ENGR390-SUMMER2026-TEAM6";
    private static final String KEY_TIMESTAMP_PREFIX = "thresholdLastProcessedTs_";
    private static final String KEY_VALUE_PREFIX = "thresholdLastProcessedValue_";
    private static final String KEY_LAST_NOTIFIED_PREFIX = "thresholdLastNotifiedAt_";

    private final SharedPreferences prefs;

    ThresholdAlertDedupeStore(@NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    long loadLastProcessedTimestamp(@NonNull String aquariumId, @NonNull String sensorId) {
        return prefs.getLong(timestampKey(aquariumId, sensorId), -1L);
    }

    @Nullable
    Double loadLastProcessedValue(@NonNull String aquariumId, @NonNull String sensorId) {
        String key = valueKey(aquariumId, sensorId);
        if (!prefs.contains(key)) {
            return null;
        }
        // Stored as raw bits so the double round-trips exactly; SharedPreferences has no putDouble.
        return Double.longBitsToDouble(prefs.getLong(key, 0L));
    }

    void saveLastProcessed(
            @NonNull String aquariumId,
            @NonNull String sensorId,
            long timestamp,
            double value
    ) {
        prefs.edit()
                .putLong(timestampKey(aquariumId, sensorId), timestamp)
                .putLong(valueKey(aquariumId, sensorId), Double.doubleToRawLongBits(value))
                .apply();
    }

    /**
     * True when {@code cooldownKey} has been quiet for at least
     * {@link SensorThresholds#NOTIFICATION_COOLDOWN_MS}.
     *
     * <p>The window is never cleared early. An earlier version reset it as soon as a reading came
     * back in range, which let a value flapping across a band edge re-alert on the very next
     * sample.
     */
    boolean cooldownElapsed(@NonNull String cooldownKey) {
        long lastNotified = prefs.getLong(lastNotifiedKey(cooldownKey), 0L);
        if (lastNotified <= 0L) {
            return true;
        }
        long sinceLast = System.currentTimeMillis() - lastNotified;
        // A clock moved backwards would otherwise mute this condition until wall time caught up.
        return sinceLast < 0L || sinceLast >= SensorThresholds.NOTIFICATION_COOLDOWN_MS;
    }

    void saveLastNotificationTime(@NonNull String cooldownKey, long wallClockMillis) {
        prefs.edit()
                .putLong(lastNotifiedKey(cooldownKey), wallClockMillis)
                .apply();
    }

    private static String timestampKey(String aquariumId, String sensorId) {
        return KEY_TIMESTAMP_PREFIX + aquariumId + "_" + sensorId;
    }

    private static String valueKey(String aquariumId, String sensorId) {
        return KEY_VALUE_PREFIX + aquariumId + "_" + sensorId;
    }

    private static String lastNotifiedKey(String cooldownKey) {
        return KEY_LAST_NOTIFIED_PREFIX + cooldownKey;
    }
}
