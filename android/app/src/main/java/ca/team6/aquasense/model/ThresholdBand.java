package ca.team6.aquasense.model;

import androidx.annotation.Nullable;

import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/**
 * This class represents the alert thresholds for a single sensor parameter, expressed as four numbers.
 *
 * <pre>
 *   critical | warning |      safe      | warning | critical
 *  ----------+---------+----------------+---------+----------
 *          warnLow  safeLow          safeHigh  warnHigh
 * </pre>
 *
 * <p> The schema requires all four together, so a sensor is either fully configured or has no band
 * at all. </p>
 * <p>A reading in [safeLow, safeHigh] is {@link SensorStatus#NORMAL}.
 * Outside that but within [warnLow, warnHigh] is {@link SensorStatus#WARNING}.
 * Strictly below warnLow or above warnHigh is {@link SensorStatus#CRITICAL}.
 */

public final class ThresholdBand {

    private final double warnLow;
    private final double safeLow;
    private final double safeHigh;
    private final double warnHigh;

    /**
     * @throws IllegalArgumentException if the bounds are not in non-decreasing order, which would
     *     make one of the bands inverted and unreachable.
     */
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

    /**
     * Builds a band from values that are not trusted to be well formed: a database record written
     * by an older build, or a half-finished custom threshold form.
     */
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

    /** True when the four bounds are ordered such that every band is in ascending order. */
    public static boolean isMonotonic(
            double warnLow,
            double safeLow,
            double safeHigh,
            double warnHigh
    ) {
        return warnLow <= safeLow && safeLow <= safeHigh && safeHigh <= warnHigh;
    }

    /**
     * True when each bound sits strictly above the one before it, so every band spans a real range
     * rather than collapsing to nothing.
     *
     * <p>This is the stricter half of {@link #isMonotonic}, and the two are deliberately kept
     * apart. Reading stays tolerant: a record already in the database with, say, equal warnLow and
     * safeLow is graded rather than discarded, since dropping it would leave the sensor with no
     * band at all. Writing has to be strict, because {@code database/rules.json} requires
     * {@code warn_low < safe_low < safe_high < warn_high} and rejects anything else outright — so
     * a form that only checked {@link #isMonotonic} would build a band the server refuses.
     */
    public static boolean isStrictlyOrdered(
            double warnLow,
            double safeLow,
            double safeHigh,
            double warnHigh
    ) {
        return warnLow < safeLow && safeLow < safeHigh && safeHigh < warnHigh;
    }

    /** Classifies a reading into a sensor status */
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
