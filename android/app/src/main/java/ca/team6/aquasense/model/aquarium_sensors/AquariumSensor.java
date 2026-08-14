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
    public static final long STALE_THRESHOLD_SECONDS = 30;

    private int unitResId;
    private String value;
    protected SensorStatus status;

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

    public boolean applyReading(Context context, @Nullable SensorReading reading, @Nullable ThresholdBand thresholdBand, double spikeDelta, long nowMillis) {
        String previousValue = this.value;
        SensorStatus previousStatus = this.status;

        this.thresholdBand = thresholdBand;

        if (reading == null) {
            this.spikeTracker.reset();
        }

        long nowSeconds = nowMillis / 1000L;
        if (reading == null || reading.isOffline() || Math.abs(reading.ageSeconds(nowSeconds)) > STALE_THRESHOLD_SECONDS) {
            this.value = "-";
            this.status = SensorStatus.DISCONNECTED;
            this.spikeTracker.pause();
        } else {
            double value = reading.getValue();
            this.value = ReadingFormatter.format(context, this.getId(), value);
            boolean spiking = this.spikeTracker.grade(
                    value, reading.getTimestampSeconds(), spikeDelta);
            this.status = gradedStatus(spiking, this.statusFor(value, thresholdBand));
        }

        return !Objects.equals(previousValue, this.value) || previousStatus != this.status;
    }

    static SensorStatus gradedStatus(boolean spiking, SensorStatus banded) {
        return spiking && banded == SensorStatus.NORMAL ? SensorStatus.WARNING : banded;
    }

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

    protected InfoSheetSection section(Context context, @StringRes int titleResId, String... bullets) {
        return new InfoSheetSection(context.getString(titleResId), bullets);
    }

    protected String rangeText(Context context, double low, double high) {
        return this.withUnit(context, this.range(context, low, high));
    }

    private String range(Context context, double low, double high) {
        return context.getString(R.string.sensor_info_range,
                this.number(context, low), this.number(context, high));
    }

    private String number(Context context, double value) {
        return ReadingFormatter.format(context, this.getId(), value);
    }

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
