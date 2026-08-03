package ca.team6.aquasense.model.aquarium_sensors;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.Objects;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.ThresholdBand;

public abstract class AquariumSensor {
    private static final long STALE_THRESHOLD_SECONDS = 30;

    private int unitResId;
    private String value;
    protected SensorStatus status;

    protected AquariumSensor() {
        this.setUnitResId(R.string.unit_dimensionless);
        this.value = "-";
        this.status = SensorStatus.DISCONNECTED;
    }

    public int getUnitResId() {
        return this.unitResId;
    }

    public String getValue() {
        return this.value;
    }

    public void setUnitResId(int unitResId) {
        this.unitResId = unitResId;
    }

    /** @return whether the displayed value or status actually changed, so callers can skip rebinding when it didn't. */
    public boolean applyReading(Context context, @Nullable SensorReading reading, @Nullable ThresholdBand thresholdBand, long nowMillis) {
        String previousValue = this.value;
        SensorStatus previousStatus = this.status;

        long nowSeconds = nowMillis / 1000L;
        if (reading == null || reading.isOffline() || Math.abs(reading.ageSeconds(nowSeconds)) > STALE_THRESHOLD_SECONDS) {
            this.value = "-";
            this.status = SensorStatus.DISCONNECTED;
        } else {
            this.value = ReadingFormatter.format(context, this.getId(), reading.getValue());
            this.status = thresholdBand != null ? thresholdBand.statusFor(reading.getValue()) : SensorStatus.NORMAL;
        }

        return !Objects.equals(previousValue, this.value) || previousStatus != this.status;
    }

    @SuppressWarnings("unused")
    public SensorStatus getSensorStatus() {
        return this.status;
    }
    
    public int getStatusIconResId() {
        return this.status.iconResourceId;
    }
    
    public int getStatusTextResId() {
        return this.status.textResourceId;
    }
    
    public int getStatusColorResId() {
        return this.status.colorResourceId;
    }

    public InfoSheetSection[] getInfoSheetSections() {
        switch (this.status) {
            case NORMAL:
                return this.getInfoSheetSectionsForNormal();
            case CRITICAL:
                return this.getInfoSheetSectionsForCritical();
            case WARNING:
                return this.getInfoSheetSectionsForWarning();
            default:
                return this.getInfoSheetSectionsForDisconnected();
        }
    }

    public abstract String getId();

    public abstract int getTitleIconResId();
    
    public abstract int getNameResId();
    
    public abstract int getInfoSheetTitleResId();
    
    public abstract int getInfoSheetDescResId();

    public abstract InfoSheetSection[] getInfoSheetSectionsForDisconnected();

    public abstract InfoSheetSection[] getInfoSheetSectionsForCritical();

    public abstract InfoSheetSection[] getInfoSheetSectionsForWarning();

    public abstract InfoSheetSection[] getInfoSheetSectionsForNormal();
}
