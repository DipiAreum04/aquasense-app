package ca.team6.aquasense.model;

import androidx.annotation.Nullable;

import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;


public final class ThresholdBand {

    private final double warnLow;
    private final double safeLow;
    private final double safeHigh;
    private final double warnHigh;

    public ThresholdBand(double warnLow, double safeLow, double safeHigh, double warnHigh) {
        if (!isMonotonic(warnLow, safeLow, safeHigh, warnHigh)) {
            throw new IllegalArgumentException(
                    "Thresholds must satisfy warnLow <= safeLow <= safeHigh <= warnHigh, but received "
                            + warnLow + ", " + safeLow + ", " + safeHigh + ", " + warnHigh);
        }
        this.warnLow = warnLow;
        this.safeLow = safeLow;
        this.safeHigh = safeHigh;
        this.warnHigh = warnHigh;
    }

    @Nullable
    public static ThresholdBand fromValues(
            double warnLow,
            double safeLow,
            double safeHigh,
            double warnHigh
    ) {
        return isMonotonic(warnLow, safeLow, safeHigh, warnHigh)
                ? new ThresholdBand(warnLow, safeLow, safeHigh, warnHigh)
                : null;
    }

    public static boolean isMonotonic(
            double warnLow,
            double safeLow,
            double safeHigh,
            double warnHigh
    ) {
        return warnLow <= safeLow && safeLow <= safeHigh && safeHigh <= warnHigh;
    }

    public static boolean isStrictlyOrdered(
            double warnLow,
            double safeLow,
            double safeHigh,
            double warnHigh
    ) {
        return warnLow < safeLow && safeLow < safeHigh && safeHigh < warnHigh;
    }

    public SensorStatus statusFor(double value) {
        if (value < this.warnLow || value > this.warnHigh) {
            return SensorStatus.CRITICAL;
        }
        if (value < this.safeLow || value > this.safeHigh) {
            return SensorStatus.WARNING;
        }
        return SensorStatus.NORMAL;
    }

    public double getWarnLow() {
        return this.warnLow;
    }

    public double getSafeLow() {
        return this.safeLow;
    }

    public double getSafeHigh() {
        return this.safeHigh;
    }

    public double getWarnHigh() {
        return this.warnHigh;
    }
}
