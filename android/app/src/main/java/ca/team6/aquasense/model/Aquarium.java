package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

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

    @Nullable
    public ThresholdBand thresholdFor(@NonNull String sensorId) {
        return thresholds.get(sensorId);
    }

    @Nullable
    public Double spikeDeltaFor(@NonNull String sensorId) {
        return spikeDeltas.get(sensorId);
    }
}
