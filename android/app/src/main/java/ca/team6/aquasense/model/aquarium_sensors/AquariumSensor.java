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

    private final SpikeTracker spikeTracker = new SpikeTracker();

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

    /**
     * @param spikeDelta how far this reading must have moved from the previous one to count as a
     *     spike, from {@code SensorThresholds.resolveSpikeDelta}. {@link Double#NaN} for a sensor
     *     that cannot spike, which disables the check.
     * @return whether the displayed value or status actually changed, so callers can skip rebinding when it didn't.
     */
    public boolean applyReading(Context context, @Nullable SensorReading reading, @Nullable ThresholdBand thresholdBand, double spikeDelta, long nowMillis) {
        String previousValue = this.value;
        SensorStatus previousStatus = this.status;

        if (reading == null) {
            // No sample at all, which is how a switch to another aquarium arrives.
            this.spikeTracker.reset();
        }

        long nowSeconds = nowMillis / 1000L;
        if (reading == null || reading.isOffline() || Math.abs(reading.ageSeconds(nowSeconds)) > STALE_THRESHOLD_SECONDS) {
            this.value = "-";
            this.status = SensorStatus.DISCONNECTED;
            this.spikeTracker.pause();
        } else {
            this.value = ReadingFormatter.format(context, this.getId(), reading.getValue());
            boolean spiking = this.spikeTracker.grade(
                    reading.getValue(), reading.getTimestampSeconds(), spikeDelta);
            this.status = gradedStatus(
                    spiking, this.statusFor(reading.getValue(), thresholdBand));
        }

        return !Objects.equals(previousValue, this.value) || previousStatus != this.status;
    }

    /**
     * Settles one reading's status between what its band says and whether it jumped to get there.
     *
     * <p>A jump only says something the band does not while the reading is otherwise in range: a
     * spike is the one warning a normal value can earn. Once the reading is out of range the band
     * is the direct account of it and stands alone, so a jump that lands in warning or critical is
     * that band and nothing more.
     *
     * <p>Nothing here carries over between readings, so the sample after a spike is whatever its
     * own band says, and a card that spiked while normal drops back to normal on the next reading
     * that does not jump.
     */
    static SensorStatus gradedStatus(boolean spiking, SensorStatus banded) {
        return spiking && banded == SensorStatus.NORMAL ? SensorStatus.WARNING : banded;
    }

    /**
     * Grades a reading. A sensor with no band configured has nothing to be out of range of, so it
     * reads as normal; a sensor whose reading carries a meaning of its own, regardless of any
     * band, says so by overriding this.
     *
     * <p>Public because it grades a value rather than this sensor's own current one: the analytics
     * page runs a whole period's buckets through it to say how many of them were in range, and
     * that has to be the same judgement the dashboard's card is making on the live reading.
     */
    public SensorStatus statusFor(double value, @Nullable ThresholdBand thresholdBand) {
        return thresholdBand != null ? thresholdBand.statusFor(value) : SensorStatus.NORMAL;
    }

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

    public int getStatusCardStrokeColorResId() {
        return this.status.cardStrokeColorResourceId;
    }

    public int getStatusPillBackgroundColorResId() {
        return this.status.pillBackgroundColorResourceId;
    }

    public int getValueColorResId() {
        return this.status.valueColorResourceId;
    }

    public int getTitleIconColorResId() {
        return this.status.titleIconColorResourceId;
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
