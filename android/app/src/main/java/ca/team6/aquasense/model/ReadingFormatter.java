package ca.team6.aquasense.model;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.Locale;

import ca.team6.aquasense.R;

/**
 * Formats numeric sensor readings for display using the Display &amp; Units preferences.
 *
 * <p>Decimal places are chosen per sensor rather than globally since a single "2 decimals everywhere"
 * setting would render pH as {@code 7.18} but dissolved solids as {@code 342.00}, which claims
 * precision the sensor does not have.
 */
public final class ReadingFormatter {

    private ReadingFormatter() {}

    // Decimal places per sensor ID, indexed [standard, precise].
    // Water level is absent because it is a detector shown as LOW / SAFE rather than a number,
    // so format() answers it before reaching here.
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

    // Converts a Celsius temperature to the unit the user reads the app in.
    public static double toDisplayTemperature(Context context, double celsius) {
        return isCelsius(context) ? celsius : celsius * 9 / 5 + 32;
    }

    /** Whether readings are shown in Celsius, so a caller can label an axis in the same unit. */
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

    /** True when the user picked Fahrenheit in Display &amp; Units. */
    public static boolean isFahrenheit(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return false;
        }
        return !"C".equals(
                prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit));
    }

    /**
     * Converts a stored temperature into the unit the user chose. Telemetry is always Celsius in
     * the database, so the conversion belongs at the display edge and nowhere else.
     */
    public static double toDisplayTemperature(double celsius, boolean fahrenheit) {
        return fahrenheit ? celsius * 9.0 / 5.0 + 32.0 : celsius;
    }

    /**
     * Converts a temperature <em>difference</em> (a spike size, a band width) into the user's unit.
     * A difference scales but does not shift, so this deliberately omits the +32 offset that
     * {@link #toDisplayTemperature} applies.
     */
    public static double toDisplayTemperatureDelta(double celsiusDelta, boolean fahrenheit) {
        return fahrenheit ? celsiusDelta * 9.0 / 5.0 : celsiusDelta;
    }

    /**
     * Inverse of {@link #toDisplayTemperature(double, boolean)}, for a temperature the user typed
     * rather than one the board reported. Telemetry and thresholds are both stored in Celsius, so
     * a number entered in Fahrenheit has to come back through here before it is written.
     */
    public static double fromDisplayTemperature(double displayValue, boolean fahrenheit) {
        return fahrenheit ? (displayValue - 32.0) * 5.0 / 9.0 : displayValue;
    }

    /** Inverse of {@link #toDisplayTemperatureDelta}, and likewise without the 32 degree offset. */
    public static double fromDisplayTemperatureDelta(double displayDelta, boolean fahrenheit) {
        return fahrenheit ? displayDelta * 5.0 / 9.0 : displayDelta;
    }

    @StringRes
    public static int temperatureUnitResId(boolean fahrenheit) {
        return fahrenheit ? R.string.unit_fahrenheit : R.string.unit_celsius;
    }

    /** Unit label for a sensor, honouring the temperature unit preference. */
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

    /** Display name for a sensor, matching the dashboard card titles. */
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

    /**
     * Formats a raw sensor reading for the dashboard card and chart labels.
     *
     * <p>Water level comes back as a word ({@code LOW} / {@code SAFE}); every other sensor as a number.
     *
     * @param sensorId one of the {@code AquariumSensor.getId()} values
     * @param value    the raw reading as stored in Realtime Database
     */
    public static String format(Context context, String sensorId, double value) {
        // The detector reports 1 when it still senses water and 0 once the level drops below it,
        // which reads as a state rather than a quantity. Compared against a midpoint because the
        // value arrives from the database as a double.
        if (DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId)) {
            return context.getString(value >= 0.5 ? R.string.water_level_safe : R.string.water_level_low);
        }

        boolean precise = isPrecise(context);
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? toDisplayTemperature(context, value)
                : value;
        return formatValue(sensorId, displayValue, precise);
    }

    /**
     * Formats a reading when the caller already knows the precision setting, so a screen rendering
     * many values does not re-read preferences per value.
     */
    // The precision is chosen at runtime, so the format string cannot be a literal. The IDE
    // cannot evaluate it statically and reports it as malformed; "%." + 2 + "f" is just "%.2f".
    @SuppressWarnings("MalformedFormatString")
    public static String formatValue(String sensorId, double value, boolean precise) {
        int decimals = decimalsFor(sensorId, precise);
        return String.format(Locale.getDefault(), "%." + decimals + "f", value);
    }
}
