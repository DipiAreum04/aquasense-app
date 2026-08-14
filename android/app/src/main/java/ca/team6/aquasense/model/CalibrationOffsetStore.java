package ca.team6.aquasense.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CalibrationOffsetStore {
    private static final String PREFIX = "calibrationOffset.";
    private static final String CALIBRATED_AT_PREFIX = "calibratedAt.";

    private final Context context;
    private final SharedPreferenceHelper preferences;

    public CalibrationOffsetStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.preferences = SharedPreferenceHelper.getInstance(this.context);
    }

    public static boolean isCalibratable(@NonNull String sensorId) {
        return !DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId);
    }

    public double get(@NonNull String aquariumId, @NonNull String sensorId) {
        if (!isCalibratable(sensorId)) return 0d;
        String stored = preferences.getString(key(aquariumId, sensorId), null);
        if (stored == null) return 0d;
        try {
            double value = Double.parseDouble(stored);
            return Double.isFinite(value) ? value : 0d;
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }

    public void set(@NonNull String aquariumId, @NonNull String sensorId, double offset) {
        if (!Double.isFinite(offset)) throw new IllegalArgumentException("Offset must be finite");
        if (!isCalibratable(sensorId)) return;
        preferences.setString(key(aquariumId, sensorId), Double.toString(offset));
        preferences.setLong(CALIBRATED_AT_PREFIX + aquariumId + "." + sensorId,
                System.currentTimeMillis());
    }

    public long calibratedAt(@NonNull String aquariumId, @NonNull String sensorId) {
        return preferences.getLong(
                CALIBRATED_AT_PREFIX + aquariumId + "." + sensorId, 0L);
    }

    public double correct(@NonNull String aquariumId, @NonNull String sensorId, double rawValue) {
        return DatabaseSchema.isOffline(rawValue) ? rawValue
                : CalibrationMath.correct(rawValue, get(aquariumId, sensorId));
    }

    @NonNull
    public List<SensorReading> correctAll(@NonNull String aquariumId,
                                          @NonNull String sensorId,
                                          @NonNull List<SensorReading> buckets) {
        double offset = get(aquariumId, sensorId);
        if (offset == 0d) {
            return buckets;
        }

        List<SensorReading> corrected = new ArrayList<>(buckets.size());
        for (SensorReading bucket : buckets) {
            corrected.add(bucket.isOffline()
                    ? bucket
                    : new SensorReading(CalibrationMath.correct(bucket.getValue(), offset),
                            bucket.getTimestampSeconds()));
        }
        return corrected;
    }

    @Nullable
    public String describe(@NonNull String aquariumId, @NonNull String sensorId) {
        double offset = get(aquariumId, sensorId);
        if (offset == 0d) {
            return null;
        }

        boolean fahrenheit = ReadingFormatter.isFahrenheit(context);
        double displayOffset = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? ReadingFormatter.toDisplayTemperatureDelta(offset, fahrenheit)
                : offset;
        String unit = context.getString(
                ReadingFormatter.unitResIdFor(sensorId, fahrenheit)).trim();
        String number = (displayOffset > 0 ? "+" : "")
                + ReadingFormatter.formatValue(
                        sensorId, displayOffset, ReadingFormatter.isPrecise(context));
        return unit.isEmpty() ? number : number + " " + unit;
    }

    @NonNull
    private static String key(@NonNull String aquariumId, @NonNull String sensorId) {
        return PREFIX + aquariumId + "." + sensorId;
    }
}
