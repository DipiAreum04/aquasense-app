package ca.team6.aquasense.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class MaintenanceModeStore {

    public static final long DURATION_15_MIN = 15 * 60 * 1000L;
    public static final long DURATION_30_MIN = 30 * 60 * 1000L;
    public static final long DURATION_1_HOUR = 60 * 60 * 1000L;
    public static final long DURATION_2_HOURS = 2 * 60 * 60 * 1000L;

    public static final long[] DURATION_OPTIONS = {
            DURATION_15_MIN,
            DURATION_30_MIN,
            DURATION_1_HOUR,
            DURATION_2_HOURS,
    };

    private static final String PREFS_NAME = "ENGR390-SUMMER2026-TEAM6";
    private static final String KEY_PREFIX = "maintenanceMode_";
    private static final String EXPIRES_PREFIX = "maintenanceModeUntil_";

    private final SharedPreferences prefs;

    public MaintenanceModeStore(@NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isActive(@NonNull String aquariumId) {
        if (!prefs.getBoolean(keyFor(aquariumId), false)) {
            return false;
        }
        long expiresAt = getExpiresAt(aquariumId);
        if (expiresAt > 0 && System.currentTimeMillis() >= expiresAt) {
            clear(aquariumId);
            return false;
        }
        return true;
    }

    public void activateForDuration(@NonNull String aquariumId, long durationMs) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        prefs.edit()
                .putBoolean(keyFor(aquariumId), true)
                .putLong(expiresKeyFor(aquariumId), expiresAt)
                .apply();
    }

    public void setActive(@NonNull String aquariumId, boolean active) {
        if (active) {
            prefs.edit().putBoolean(keyFor(aquariumId), true).apply();
        } else {
            clear(aquariumId);
        }
    }

    public void clear(@NonNull String aquariumId) {
        prefs.edit()
                .remove(keyFor(aquariumId))
                .remove(expiresKeyFor(aquariumId))
                .apply();
    }

    public long getExpiresAt(@NonNull String aquariumId) {
        return prefs.getLong(expiresKeyFor(aquariumId), 0L);
    }

    public long getRemainingMs(@NonNull String aquariumId) {
        long expiresAt = getExpiresAt(aquariumId);
        if (expiresAt <= 0) {
            return 0L;
        }
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    @NonNull
    public static String formatRemainingDuration(long remainingMs) {
        long totalMinutes = (remainingMs + 59_999L) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0L) return totalMinutes + " min";
        if (minutes == 0L) return hours == 1L ? "1 hr" : hours + " hr";
        return hours + " hr " + minutes + " min";
    }

    private static String keyFor(@NonNull String aquariumId) {
        return KEY_PREFIX + aquariumId;
    }

    private static String expiresKeyFor(@NonNull String aquariumId) {
        return EXPIRES_PREFIX + aquariumId;
    }
}
