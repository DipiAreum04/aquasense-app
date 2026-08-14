package ca.team6.aquasense.aquarium;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NewAquariumConfig implements Parcelable {

    private final String name;
    private final WaterType waterType;
    private final Map<String, ThresholdBand> thresholds;
    private final Map<String, Double> spikeDeltas;

    public NewAquariumConfig(@NonNull String name,
                             @NonNull WaterType waterType,
                             @NonNull Map<String, ThresholdBand> thresholds,
                             @NonNull Map<String, Double> spikeDeltas) {
        this.name = name;
        this.waterType = waterType;
        this.thresholds = Collections.unmodifiableMap(new LinkedHashMap<>(thresholds));
        this.spikeDeltas = Collections.unmodifiableMap(new LinkedHashMap<>(spikeDeltas));
    }

    @NonNull
    public String getName() {
        return this.name;
    }

    @NonNull
    public WaterType getWaterType() {
        return this.waterType;
    }

    @NonNull
    public Map<String, ThresholdBand> getThresholds() {
        return this.thresholds;
    }

    @NonNull
    public Map<String, Double> getSpikeDeltas() {
        return this.spikeDeltas;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(this.name);
        out.writeString(this.waterType.getKey());

        out.writeInt(this.thresholds.size());
        for (Map.Entry<String, ThresholdBand> entry : this.thresholds.entrySet()) {
            ThresholdBand band = entry.getValue();
            out.writeString(entry.getKey());
            out.writeDouble(band.getWarnLow());
            out.writeDouble(band.getSafeLow());
            out.writeDouble(band.getSafeHigh());
            out.writeDouble(band.getWarnHigh());
        }

        out.writeInt(this.spikeDeltas.size());
        for (Map.Entry<String, Double> entry : this.spikeDeltas.entrySet()) {
            out.writeString(entry.getKey());
            out.writeDouble(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NewAquariumConfig> CREATOR = new Creator<>() {
        @Override
        public NewAquariumConfig createFromParcel(@NonNull Parcel in) {
            String name = in.readString();
            WaterType waterType = WaterType.fromKey(in.readString());

            Map<String, ThresholdBand> thresholds = new LinkedHashMap<>();
            for (int written = in.readInt(); written > 0; written--) {
                String sensorId = in.readString();
                ThresholdBand band = ThresholdBand.fromValues(
                        in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
                if (sensorId != null && band != null) {
                    thresholds.put(sensorId, band);
                }
            }

            Map<String, Double> spikeDeltas = new LinkedHashMap<>();
            for (int written = in.readInt(); written > 0; written--) {
                String sensorId = in.readString();
                double delta = in.readDouble();
                if (sensorId != null) {
                    spikeDeltas.put(sensorId, delta);
                }
            }

            return new NewAquariumConfig(
                    name == null ? "" : name,
                    waterType == null ? WaterType.FRESHWATER : waterType,
                    thresholds,
                    spikeDeltas);
        }

        @Override
        public NewAquariumConfig[] newArray(int size) {
            return new NewAquariumConfig[size];
        }
    };
}
