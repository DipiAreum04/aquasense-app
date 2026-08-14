package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Map;

import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;

/**
 * One entry of /{uid}/aquariums. The database key is carried on the object as {@link #getId()},
 * since every telemetry path needs it.
 *
 * <p>Only name and water type are required by the schema. Thresholds and spike deltas are
 * optional and are frequently absent, so their accessors return {@code null} rather than a
 * default; the caller decides what an unconfigured sensor should fall back to.
 */
public final class Aquarium {

    private final String id;
    private final String name;
    private final String waterType;
    private final Map<String, ThresholdBand> thresholds;
    private final Map<String, Double> spikeDeltas;

    public Aquarium(@NonNull String id,
                    @NonNull String name,
                    @NonNull String waterType,
                    @NonNull Map<String, ThresholdBand> thresholds,
                    @NonNull Map<String, Double> spikeDeltas) {
        this.id = id;
        this.name = name;
        this.waterType = waterType;
        this.thresholds = thresholds;
        this.spikeDeltas = spikeDeltas;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getWaterType() {
        return waterType;
    }

    //Checks if the sensor is applicable to this aquarium's water type.
    public boolean isSensorApplicable(@NonNull String sensorId) {
        return template().isSensorApplicable(sensorId);
    }

    // Returns the resource ID of the note to display when a sensor is disabled due to water type.
    @StringRes
    public int getSensorDisabledNoteResId() {
        return template().getDisabledNoteResId();
    }

    @NonNull
    private AquariumTemplate template() {
        return BuiltInTemplates.forWaterType(WaterType.fromKey(waterType));
    }

    @Nullable
    public ThresholdBand thresholdFor(@NonNull String sensorId) {
        return thresholds.get(sensorId);
    }

    /**
     * The band this aquarium is actually graded against for a sensor: the one configured for it, or
     * this water type's default where it has not been overridden.
     *
     * <p>Null when the sensor is not one this kind of tank measures. Borrowing a band from a
     * different water type would be worse than having none - it is the difference between "no
     * opinion" and a confidently wrong one.
     */
    @Nullable
    public ThresholdBand effectiveThresholdFor(@NonNull String sensorId) {
        ThresholdBand configured = thresholdFor(sensorId);
        if (configured != null) {
            return configured;
        }
        AquariumTemplate template = template();
        if (!template.isSensorApplicable(sensorId)) {
            return null;
        }
        return template.getThresholds(sensorId);
    }

    @Nullable
    public Double spikeDeltaFor(@NonNull String sensorId) {
        return spikeDeltas.get(sensorId);
    }
}
