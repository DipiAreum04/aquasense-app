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

    @ArrayRes
    public int getExampleSpeciesResId() {
        return this.exampleSpeciesResId;
    }

    @DrawableRes
    public int getIconResId() {
        return this.iconResId;
    }

    @ColorRes
    public int getAccentColorResId() {
        return this.accentColorResId;
    }

    public WaterType getWaterType() {
        return this.waterType;
    }

    public boolean isSensorApplicable(String sensorId) {
        return !this.disabledSensors.contains(sensorId);
    }

    @StringRes
    public int getDisabledNoteResId() {
        return this.disabledNoteResId;
    }

    @Nullable
    public ThresholdBand getThresholds(String sensorId) {
        return this.thresholds.get(sensorId);
    }

    public Map<String, ThresholdBand> getAllThresholds() {
        return this.thresholds;
    }

    @Nullable
    public SensorStatus statusFor(String sensorId, double value) {
        ThresholdBand band = this.getThresholds(sensorId);
        return band == null ? null : band.statusFor(value);
    }
}
