package ca.team6.aquasense.model.aquarium_templates;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/**
 * This class represents a built-in aquarium template definition.
 * 
 * <p>A template has a unique ID, name, description, example species, a water type,
 * a map of sensor IDs to thresholds, and a set of disabled sensors.
 * Sensor keys are the {@code DatabaseSchema} telemetry node names.
 */
public final class AquariumTemplate {

    private final String id;
    @StringRes private final int nameResId;
    @StringRes private final int descriptionResId;
    @ArrayRes private final int exampleSpeciesResId;
    @DrawableRes private final int iconResId;
    @ColorRes private final int accentColorResId;
    private final WaterType waterType;
    private final Map<String, ThresholdBand> thresholds;
    private final Set<String> disabledSensors;
    @StringRes private final int disabledNoteResId;

    /**
     * @param id stable identifier persisted with the aquarium, never localize it.
     * @param thresholds ranges by sensor ID. Sensors absent from this map have no threshold mapping.
     * @param disabledSensors sensors that are not applicable to this water type.
     * @param disabledNoteResId user explanation for {@code disabledSensors}, or {@code 0}
     *     when no sensors are disabled.
     */
    AquariumTemplate(
            String id,
            @StringRes int nameResId,
            @StringRes int descriptionResId,
            @ArrayRes int exampleSpeciesResId,
            @DrawableRes int iconResId,
            @ColorRes int accentColorResId,
            WaterType waterType,
            Map<String, ThresholdBand> thresholds,
            Set<String> disabledSensors,
            @StringRes int disabledNoteResId
    ) {
        this.id = id;
        this.nameResId = nameResId;
        this.descriptionResId = descriptionResId;
        this.exampleSpeciesResId = exampleSpeciesResId;
        this.iconResId = iconResId;
        this.accentColorResId = accentColorResId;
        this.waterType = waterType;
        this.thresholds = Collections.unmodifiableMap(thresholds);
        this.disabledSensors = Collections.unmodifiableSet(disabledSensors);
        this.disabledNoteResId = disabledNoteResId;
    }

    public String getId() {
        return this.id;
    }

    @StringRes
    public int getNameResId() {
        return this.nameResId;
    }

    @StringRes
    public int getDescriptionResId() {
        return this.descriptionResId;
    }

    /** String-array of example species, one entry per chip on the template card. */
    @ArrayRes
    public int getExampleSpeciesResId() {
        return this.exampleSpeciesResId;
    }

    @DrawableRes
    public int getIconResId() {
        return this.iconResId;
    }

    /**
     * Tint for this template's icon tile and species chips.
     */
    @ColorRes
    public int getAccentColorResId() {
        return this.accentColorResId;
    }

    public WaterType getWaterType() {
        return this.waterType;
    }

    // TODO: The dashboard and the display settings screen both should filter on this before consulting the user's hide preference.
    /**
     * Whether this sensor can be measured at all in this water type. 
     * An inapplicable sensor is a hardware limit and stays disabled.
     */
    public boolean isSensorApplicable(String sensorId) {
        return !this.disabledSensors.contains(sensorId);
    }

    /** Explanation shown next to a locked-off sensor, or 0 if no sensors are disabled. */
    @StringRes
    public int getDisabledNoteResId() {
        return this.disabledNoteResId;
    }

    /**
     * Thresholds for a sensor, or {@code null} when it has none because the sensor is 
     * either disabled for this water type or is not a ranged measurement (water level sensor).
     */
    @Nullable
    public ThresholdBand getThresholds(String sensorId) {
        return this.thresholds.get(sensorId);
    }

    /**
     * Converts a reading for one sensor into a status (disconnected, critical, warning, normal).
     */
    @Nullable
    public SensorStatus statusFor(String sensorId, double value) {
        ThresholdBand band = this.getThresholds(sensorId);
        return band == null ? null : band.statusFor(value);
    }
}
