package ca.team6.aquasense.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ca.team6.aquasense.R;

public class SharedPreferenceHelper {
    private static SharedPreferenceHelper instance;
    private static SharedPreferences sharedPreferences;

    private SharedPreferenceHelper(Context context) {
        Context appContext = context.getApplicationContext();
        sharedPreferences = appContext.getSharedPreferences("ENGR390-SUMMER2026-TEAM6", Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferenceHelper getInstance(Context context) {
        if (context == null) return null;
        
        if (instance == null) {
            instance = new SharedPreferenceHelper(context);
        }

        return instance;
    }

    // Method to show a Toast for features that are not implemented yet
    public static void showComingSoon(Context context) {
        if (context == null) return;
        Toast.makeText(context.getApplicationContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }

    // Applies the saved theme preference at app startup.
    public void applySavedThemeMode() {
        applyThemeMode(getThemeMode());
    }

    // Returns the saved theme mode
    public String getThemeMode() {
        String mode = getString(SettingsRepository.KEY_THEME_MODE, null);
        if (mode != null) {
            return mode;
        }
        return SettingsRepository.THEME_SYSTEM;
    }

    // Saves the preference and switches the app theme.
    public void setThemeMode(String mode) {
        setString(SettingsRepository.KEY_THEME_MODE, mode);
        applyThemeMode(mode);
    }

    private void applyThemeMode(String mode) {
        int nightMode;
        switch (mode) {
            case SettingsRepository.THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case SettingsRepository.THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case SettingsRepository.THEME_SYSTEM:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            default:
                ScopedLogger.error("Unknown theme mode " + mode + ", defaulting to follow system.");
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    /** Persists before returning so a second lifecycle callback cannot re-read a stale value. */
    public void setBooleanSync(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).commit();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void setString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public void setLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public void setFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    private void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    /** Removes a value before returning, for one-time local data migrations. */
    public void removeSync(String key) {
        sharedPreferences.edit().remove(key).commit();
    }

    public void updateField(String key, Object value) {
        if (value instanceof Boolean) {
            setBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            setString(key, (String) value);
        } else if (value instanceof Long) {
            setLong(key, (Long) value);
        } else if (value instanceof Float) {
            setFloat(key, (Float) value);
        }
    }

    // Dashboard card order / visibility.
    // Both are stored as comma-separated sensor IDs (see AquariumSensor.getId()) rather than
    // a string Set, because insertion order is what makes the order preference meaningful.

    // Returns the saved card order, then appends any sensor IDs the user has never seen
    // (e.g. a sensor added in a later build) so new cards still show up.
    public List<String> getSensorOrder(List<String> knownIds) {
        List<String> ordered = new ArrayList<>();
        for (String id : splitCsv(getString(SettingsRepository.KEY_SENSOR_ORDER, ""))) {
            if (knownIds.contains(id) && !ordered.contains(id)) {
                ordered.add(id);
            }
        }
        for (String id : knownIds) {
            if (!ordered.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    public void setSensorOrder(List<String> orderedIds) {
        setString(SettingsRepository.KEY_SENSOR_ORDER, String.join(",", orderedIds));
    }

    public boolean isSensorHidden(String sensorId) {
        return splitCsv(getString(SettingsRepository.KEY_SENSOR_HIDDEN, "")).contains(sensorId);
    }

    public void setSensorHidden(String sensorId, boolean hidden) {
        Set<String> hiddenIds =
                new LinkedHashSet<>(splitCsv(getString(SettingsRepository.KEY_SENSOR_HIDDEN, "")));
        if (hidden) {
            hiddenIds.add(sensorId);
        } else {
            hiddenIds.remove(sensorId);
        }
        setString(SettingsRepository.KEY_SENSOR_HIDDEN, String.join(",", hiddenIds));
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }
}
