package ca.team6.aquasense.model;

import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

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
    // Carries the status through the parts of the header that sit on the gradient rather than
    // being coloured by it: the heading, the status icon and the hub button's label.
    public final int accentColorResId;
    // Flat tint for the crest at the foot of the header. bg_auth_wave draws two overlapping
    // paths, and an ImageView tint cannot address them separately, so both take this colour.
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

    /**
     * The aquarium takes the worst of its sensors: a sensor that is critical or has stopped
     * reporting makes the aquarium critical, and a sensor merely out of its comfortable band makes
     * it a warning. Only when nothing is reporting at all does the aquarium read as disconnected,
     * since then there is no reading to call critical.
     *
     * <p>Pass the sensors the dashboard is showing, not every sensor that exists.
     */
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
