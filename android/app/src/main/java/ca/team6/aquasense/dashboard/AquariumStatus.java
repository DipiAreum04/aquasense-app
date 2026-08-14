package ca.team6.aquasense.dashboard;

import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.aquarium.sensors.AquariumSensor;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public enum AquariumStatus {
    NORMAL(
        R.drawable.sentiment_very_satisfied_24px,
        R.string.aquarium_sensors_status_normal_heading,
        R.string.aquarium_sensors_status_normal_description,
        R.color.aquarium_sensors_status_normal_gradient_start,
        R.color.aquarium_sensors_status_normal_gradient_end,
        R.color.aquarium_sensors_status_normal_accent,
        R.color.aquarium_sensors_status_normal_wave
    ),
    WARNING(
        R.drawable.sentiment_stressed_24px,
        R.string.aquarium_sensors_status_warning_heading,
        R.string.aquarium_sensors_status_warning_description,
        R.color.aquarium_sensors_status_warning_gradient_start,
        R.color.aquarium_sensors_status_warning_gradient_end,
        R.color.aquarium_sensors_status_warning_accent,
        R.color.aquarium_sensors_status_warning_wave
    ),
    CRITICAL(
        R.drawable.sentiment_frustrated_24px,
        R.string.aquarium_sensors_status_critical_heading,
        R.string.aquarium_sensors_status_critical_description,
        R.color.aquarium_sensors_status_critical_gradient_start,
        R.color.aquarium_sensors_status_critical_gradient_end,
        R.color.aquarium_sensors_status_critical_accent,
        R.color.aquarium_sensors_status_critical_wave
    ),
    DISCONNECTED(
        R.drawable.sentiment_very_dissatisfied_24px,
        R.string.aquarium_sensors_status_disconnected_heading,
        R.string.aquarium_sensors_status_disconnected_description,
        R.color.aquarium_sensors_status_disconnected_gradient_start,
        R.color.aquarium_sensors_status_disconnected_gradient_end,
        R.color.aquarium_sensors_status_disconnected_accent,
        R.color.aquarium_sensors_status_disconnected_wave
    );

    public final int iconResId;
    public final int headingResId;
    public final int descriptionResId;
    public final int gradientStartColorResId;
    public final int gradientEndColorResId;
    public final int accentColorResId;
    public final int waveColorResId;

    AquariumStatus(
        int iconResId,
        int headingResId,
        int descriptionResId,
        int gradientStartColorResId,
        int gradientEndColorResId,
        int accentColorResId,
        int waveColorResId
    ) {
        this.iconResId = iconResId;
        this.headingResId = headingResId;
        this.descriptionResId = descriptionResId;
        this.gradientStartColorResId = gradientStartColorResId;
        this.gradientEndColorResId = gradientEndColorResId;
        this.accentColorResId = accentColorResId;
        this.waveColorResId = waveColorResId;
    }

    public static AquariumStatus forSensors(List<AquariumSensor> sensors) {
        if (sensors.isEmpty()) {
            return AquariumStatus.DISCONNECTED;
        }

        int disconnectedCount = 0;
        boolean anyCritical = false;
        boolean anyWarning = false;

        for (AquariumSensor sensor : sensors) {
            SensorStatus status = sensor.getSensorStatus();
            if (status == SensorStatus.DISCONNECTED) {
                disconnectedCount++;
            } else if (status == SensorStatus.CRITICAL) {
                anyCritical = true;
            } else if (status == SensorStatus.WARNING) {
                anyWarning = true;
            }
        }

        if (disconnectedCount == sensors.size()) {
            return AquariumStatus.DISCONNECTED;
        }
        if (anyCritical || disconnectedCount > 0) {
            return AquariumStatus.CRITICAL;
        }
        if (anyWarning) {
            return AquariumStatus.WARNING;
        }
        return AquariumStatus.NORMAL;
    }
}
