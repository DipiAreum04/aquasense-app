package ca.team6.aquasense.aquarium;

import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.logging.ScopedLogger;
import ca.team6.aquasense.settings.AppSettings;
import ca.team6.aquasense.settings.SettingsRepository;
import ca.team6.aquasense.settings.SharedPreferenceHelper;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.aquarium.sensors.WaterLevelSensor;

public final class ReadingFormatter {

    private ReadingFormatter() {}

    private static int decimalsFor(String sensorId, boolean precise) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
            case DatabaseSchema.PH_LEVEL_KEY:
                return precise ? 2 : 1;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return precise ? 1 : 0;
            default:
                ScopedLogger.error("Unknown sensor ID " + sensorId + ", defaulting to 1 decimal.");
                return 1;
        }
    }

    public static double toDisplayTemperature(Context context, double celsius) {
        return isCelsius(context) ? celsius : celsius * 9 / 5 + 32;
    }

    public static boolean isCelsius(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return "C".equals(new AppSettings().tempUnit);
        }
        return "C".equals(prefs.getString(
                SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit));
    }

    public static boolean isPrecise(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return false;
        }
        return SettingsRepository.PRECISION_PRECISE.equals(
                prefs.getString(SettingsRepository.KEY_READING_PRECISION,
                        SettingsRepository.PRECISION_STANDARD));
    }

    public static boolean isFahrenheit(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return false;
        }
        return !"C".equals(
                prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit));
    }

    public static double toDisplayTemperature(double celsius, boolean fahrenheit) {
        return fahrenheit ? celsius * 9.0 / 5.0 + 32.0 : celsius;
    }

    public static double toDisplayTemperatureDelta(double celsiusDelta, boolean fahrenheit) {
        return fahrenheit ? celsiusDelta * 9.0 / 5.0 : celsiusDelta;
    }

    public static double fromDisplayTemperature(double displayValue, boolean fahrenheit) {
        return fahrenheit ? (displayValue - 32.0) * 5.0 / 9.0 : displayValue;
    }

    public static double fromDisplayTemperatureDelta(double displayDelta, boolean fahrenheit) {
        return fahrenheit ? displayDelta * 5.0 / 9.0 : displayDelta;
    }

    @StringRes
    public static int temperatureUnitResId(boolean fahrenheit) {
        return fahrenheit ? R.string.unit_fahrenheit : R.string.unit_celsius;
    }

    @StringRes
    public static int unitResIdFor(String sensorId, boolean fahrenheit) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return temperatureUnitResId(fahrenheit);
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return R.string.unit_parts_per_million;
            case DatabaseSchema.PH_LEVEL_KEY:
            case DatabaseSchema.WATER_LEVEL_KEY:
                return R.string.unit_dimensionless;
            default:
                ScopedLogger.error("Unknown sensor ID " + sensorId + ", using no unit.");
                return R.string.unit_dimensionless;
        }
    }

    @StringRes
    public static int nameResIdFor(String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return R.string.temperature;
            case DatabaseSchema.PH_LEVEL_KEY:
                return R.string.ph_level;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return R.string.dissolved_solids;
            case DatabaseSchema.WATER_LEVEL_KEY:
                return R.string.water_level;
            default:
                ScopedLogger.error("Unknown sensor ID " + sensorId + ", using app name.");
                return R.string.app_name;
        }
    }

    public static String format(Context context, String sensorId, double value) {
        if (DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId)) {
            return context.getString(value >= WaterLevelSensor.HIGH_THRESHOLD
                    ? R.string.water_level_safe
                    : R.string.water_level_low);
        }

        boolean precise = isPrecise(context);
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? toDisplayTemperature(context, value)
                : value;
        return formatValue(sensorId, displayValue, precise);
    }

    @SuppressWarnings("MalformedFormatString")
    public static String formatValue(String sensorId, double value, boolean precise) {
        int decimals = decimalsFor(sensorId, precise);
        return String.format(Locale.getDefault(), "%." + decimals + "f", value);
    }
}
