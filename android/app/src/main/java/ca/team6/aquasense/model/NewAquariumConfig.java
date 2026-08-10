package ca.team6.aquasense.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the add-aquarium form collected, carried intact to the moment it can be written.
 *
 * <p>An aquarium is keyed by the paired board's UID, so nothing can be stored until the board
 * reports it over BLE — several screens and an activity boundary after the user finished typing.
 * This is what crosses that gap.
 *
 * <p>It is deliberately a single value rather than the four extras the pairing flow used to thread
 * through {@code PairingActivity}, {@code PairingFragment} and {@code PairingRepository}: the parts
 * are only ever meaningful together, and passing them separately meant every screen in between had
 * to know the shape of a configuration it does nothing with but forward.
 *
 * <p>The template itself is not carried. {@code database/rules.json} has nowhere to put it —
 * {@code $other: false} allows only name, water type, thresholds and spike deltas — so a template
 * is a source of starting numbers at creation time and nothing afterwards. By the time this is
 * built, those numbers have already been read out of it.
 */
public final class NewAquariumConfig implements Parcelable {

    private final String name;
    private final WaterType waterType;
    private final Map<String, ThresholdBand> thresholds;
    private final Map<String, Double> spikeDeltas;

    /**
     * @param thresholds  bands by sensor ID, in the units the database stores.
     * @param spikeDeltas deltas by sensor ID, likewise stored units and every one positive.
     */
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

    // The two maps are written as independent sections rather than one row per sensor. They happen
    // to carry the same keys today, but nothing in the schema requires it: thresholds and spike
    // deltas are separate optional nodes, and a sensor may have either without the other.
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
                // fromValues rather than the constructor: a band that somehow arrives out of order
                // is dropped, leaving that sensor unconfigured, rather than throwing on the far
                // side of a parcel where there is no user action to blame it on.
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
