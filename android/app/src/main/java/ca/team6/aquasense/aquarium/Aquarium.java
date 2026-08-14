package ca.team6.aquasense.aquarium;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Map;

import ca.team6.aquasense.aquarium.templates.AquariumTemplate;
import ca.team6.aquasense.aquarium.templates.BuiltInTemplates;

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

    public boolean isSensorApplicable(@NonNull String sensorId) {
        return template().isSensorApplicable(sensorId);
    }

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
