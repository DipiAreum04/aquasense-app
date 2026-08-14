package ca.team6.aquasense.notifications;

import android.content.Context;

import androidx.annotation.NonNull;

import ca.team6.aquasense.R;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ReadingFormatter;
import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

final class ThresholdAlertFormatter {

    private final Context context;
    private final boolean fahrenheit;
    private final boolean precise;

    ThresholdAlertFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.fahrenheit = ReadingFormatter.isFahrenheit(this.context);
        this.precise = ReadingFormatter.isPrecise(this.context);
    }

    @NonNull
    String title(@NonNull ThresholdViolation violation, @NonNull String aquariumName) {
        if (violation.kind == ThresholdViolation.ViolationKind.HUB_DISCONNECTED) {
            return context.getString(R.string.alert_title_hub_disconnected, aquariumName);
        }

        String sensorName = context.getString(ReadingFormatter.nameResIdFor(violation.sensorId));

        if (DatabaseSchema.WATER_LEVEL_KEY.equals(violation.sensorId)
                && violation.kind == ThresholdViolation.ViolationKind.THRESHOLD) {
            return context.getString(R.string.alert_title_water_level, aquariumName);
        }
        if (violation.kind == ThresholdViolation.ViolationKind.SENSOR_OFFLINE) {
            return context.getString(R.string.alert_title_sensor_offline, aquariumName, sensorName);
        }
        if (violation.kind == ThresholdViolation.ViolationKind.SPIKE) {
            return context.getString(R.string.alert_title_spike, aquariumName, sensorName);
        }
        if (violation.severity == SensorStatus.CRITICAL) {
            return context.getString(R.string.alert_title_critical_range, aquariumName, sensorName);
        }
        return context.getString(R.string.alert_title_warning_range, aquariumName, sensorName);
    }

    @NonNull
    String message(@NonNull ThresholdViolation violation) {
        if (violation.kind == ThresholdViolation.ViolationKind.HUB_DISCONNECTED) {
            return context.getString(R.string.alert_body_hub_disconnected);
        }

        String sensorName = context.getString(ReadingFormatter.nameResIdFor(violation.sensorId));

        if (DatabaseSchema.WATER_LEVEL_KEY.equals(violation.sensorId)
                && violation.kind == ThresholdViolation.ViolationKind.THRESHOLD) {
            return context.getString(R.string.alert_body_water_level);
        }
        if (violation.kind == ThresholdViolation.ViolationKind.SENSOR_OFFLINE) {
            return context.getString(R.string.alert_body_sensor_offline, sensorName);
        }
        if (violation.kind == ThresholdViolation.ViolationKind.SPIKE) {
            return context.getString(
                    R.string.alert_body_spike,
                    sensorName,
                    reading(violation.sensorId, violation.previousValue),
                    reading(violation.sensorId, violation.value),
                    delta(violation.sensorId, violation.spikeDelta));
        }

        ThresholdBand band = violation.band;
        if (band == null) {
            if (violation.severity == SensorStatus.CRITICAL) {
                return context.getString(
                        R.string.alert_body_critical_range_no_band,
                        sensorName,
                        reading(violation.sensorId, violation.value));
            }
            return context.getString(
                    R.string.alert_body_warning_range_no_band,
                    sensorName,
                    reading(violation.sensorId, violation.value));
        }
        if (violation.severity == SensorStatus.CRITICAL) {
            return context.getString(
                    R.string.alert_body_critical_range,
                    sensorName,
                    reading(violation.sensorId, violation.value),
                    reading(violation.sensorId, band.getSafeLow()),
                    reading(violation.sensorId, band.getSafeHigh()));
        }
        return context.getString(
                R.string.alert_body_warning_range,
                sensorName,
                reading(violation.sensorId, violation.value),
                reading(violation.sensorId, band.getSafeLow()),
                reading(violation.sensorId, band.getSafeHigh()));
    }

    @NonNull
    private String reading(@NonNull String sensorId, double storedValue) {
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? ReadingFormatter.toDisplayTemperature(storedValue, fahrenheit)
                : storedValue;
        return withUnit(sensorId, displayValue);
    }

    @NonNull
    private String delta(@NonNull String sensorId, double storedDelta) {
        double displayDelta = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? ReadingFormatter.toDisplayTemperatureDelta(storedDelta, fahrenheit)
                : storedDelta;
        return withUnit(sensorId, displayDelta);
    }

    @NonNull
    private String withUnit(@NonNull String sensorId, double displayValue) {
        String number = ReadingFormatter.formatValue(sensorId, displayValue, precise);
        String unit = context.getString(ReadingFormatter.unitResIdFor(sensorId, fahrenheit)).trim();
        return unit.isEmpty() ? number : number + " " + unit;
    }
}
