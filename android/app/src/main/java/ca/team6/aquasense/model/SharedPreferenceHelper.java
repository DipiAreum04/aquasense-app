package ca.team6.aquasense.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

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

    // Applies the saved dark mode preference at app startup.
    public void applySavedDarkMode() {
        applyDarkMode(getBoolean(SettingsRepository.KEY_DARK_MODE, new AppSettings().darkMode));
    }

    // Saves the preference and switches the app between light and dark theme.
    public void setDarkModeEnabled(boolean enabled) {
        setBoolean(SettingsRepository.KEY_DARK_MODE, enabled);
        applyDarkMode(enabled);
    }

    private void applyDarkMode(boolean enabled) {
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
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

    // TODO: REMOVE IF ACTUALLY UNUSED BY END OF SPRINT 2
    @SuppressWarnings("unused")
    private void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public void updateField(String key, Object value) {
        if (value instanceof Boolean) {
            setBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            setString(key, (String) value);
        } else if (value instanceof Long) {
            setLong(key, (Long) value);
        }
    }
}
