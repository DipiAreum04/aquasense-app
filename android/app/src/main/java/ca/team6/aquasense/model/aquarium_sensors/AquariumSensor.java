package ca.team6.aquasense.model.aquarium_sensors;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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

    /**
     * The band the current status was graded against, so the info sheet quotes the same numbers
     * the card was judged by rather than looking the aquarium up again for itself.
     */
    @Nullable
    protected ThresholdBand thresholdBand;

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

        // Recorded even when the reading is stale: the band is what this aquarium is configured
        // with, which the info sheet still has something to say about while the sensor is silent.
        this.thresholdBand = thresholdBand;

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

    public InfoSheetSection[] getInfoSheetSections(Context context) {
        switch (this.status) {
            case NORMAL:
                return this.getInfoSheetSectionsForNormal(context);
            case CRITICAL:
                return this.getInfoSheetSectionsForCritical(context);
            case WARNING:
                return this.getInfoSheetSectionsForWarning(context);
            default:
                return this.getInfoSheetSectionsForDisconnected(context);
        }
    }

    /**
     * The line under the sheet's reading, saying where the sensor stands right now. The status pill
     * above it has already named the status, so this only has to place it against the band, which
     * every ranged sensor does the same way.
     */
    @StringRes
    public int getInfoSheetStatusNoteResId() {
        switch (this.status) {
            case NORMAL:
                return this.thresholdBand == null
                        ? R.string.sensor_info_note_unset
                        : R.string.sensor_info_note_normal;
            case WARNING:
                return R.string.sensor_info_note_warning;
            case CRITICAL:
                return R.string.sensor_info_note_critical;
            default:
                return R.string.sensor_info_note_disconnected;
        }
    }

    /**
     * The three bands this aquarium grades the sensor against, as safe, warning and critical in
     * that order, for the sheet's range rows. Null when there is no band to quote, which hides the
     * block rather than spelling the absence out.
     */
    @Nullable
    public String[] getInfoSheetBandTexts(Context context) {
        ThresholdBand band = this.thresholdBand;
        if (band == null) {
            return null;
        }

        String warningLow = band.getWarnLow() < band.getSafeLow()
                ? this.rangeText(context, band.getWarnLow(), band.getSafeLow())
                : null;
        String warningHigh = band.getSafeHigh() < band.getWarnHigh()
                ? this.rangeText(context, band.getSafeHigh(), band.getWarnHigh())
                : null;

        return new String[] {
                this.rangeText(context, band.getSafeLow(), band.getSafeHigh()),
                this.sides(context, warningLow, warningHigh),
                this.sides(context,
                        this.withUnit(context, context.getString(R.string.sensor_info_band_below,
                                this.number(context, band.getWarnLow()))),
                        this.withUnit(context, context.getString(R.string.sensor_info_band_above,
                                this.number(context, band.getWarnHigh()))))
        };
    }

    private String sides(Context context, @Nullable String low, @Nullable String high) {
        if (low != null && high != null) {
            return context.getString(R.string.sensor_info_band_pair, low, high);
        }
        if (low != null) {
            return low;
        }
        if (high != null) {
            return high;
        }
        return context.getString(R.string.sensor_info_band_none);
    }

    /** One section of the sheet, with its heading and bullets already resolved to text. */
    protected InfoSheetSection section(Context context, @StringRes int titleResId, String... bullets) {
        return new InfoSheetSection(context.getString(titleResId), bullets);
    }

    /** Two bounds as one range, carrying the unit once rather than on each end. */
    protected String rangeText(Context context, double low, double high) {
        return this.withUnit(context, this.range(context, low, high));
    }

    // The pair without a unit, so a row that joins two of them still carries only one.
    private String range(Context context, double low, double high) {
        return context.getString(R.string.sensor_info_range,
                this.number(context, low), this.number(context, high));
    }

    private String number(Context context, double value) {
        return ReadingFormatter.format(context, this.getId(), value);
    }

    // A dimensionless sensor such as pH declares a blank unit, which would otherwise leave the
    // number trailing a space.
    private String withUnit(Context context, String text) {
        String unit = context.getString(this.getUnitResId()).trim();
        return unit.isEmpty() ? text : text + " " + unit;
    }

    public abstract String getId();

    public abstract int getTitleIconResId();

    public abstract int getNameResId();

    public abstract int getInfoSheetTitleResId();

    public abstract int getInfoSheetAboutResId();

    public abstract InfoSheetSection[] getInfoSheetSectionsForDisconnected(Context context);

    public abstract InfoSheetSection[] getInfoSheetSectionsForCritical(Context context);

    public abstract InfoSheetSection[] getInfoSheetSectionsForWarning(Context context);

    public abstract InfoSheetSection[] getInfoSheetSectionsForNormal(Context context);
}
