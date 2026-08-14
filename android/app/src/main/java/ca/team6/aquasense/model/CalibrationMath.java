package ca.team6.aquasense.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/** Pure calibration calculations shared by the sampling flow and telemetry ingestion. */
public final class CalibrationMath {

    /**
     * A number with at most one decimal place, in either separator - a comma-locale keyboard emits
     * one, and the user typing it means the same thing they would mean with a full stop.
     */
    private static final Pattern OPERATING_POINT = Pattern.compile("-?\\d+([.,]\\d)?");

    private CalibrationMath() {}

    /**
     * Reads an operating point the user typed, or {@link Double#NaN} when it is not one.
     *
     * <p>One decimal place at most, because this is a value the user has to physically bring a
     * container of water to and read off an instrument. A target of 25 or 25.5 is something they
     * can hit and verify; 25.53 is a precision the procedure does not have, and accepting it would
     * put a figure in the stored offset that no part of the measurement earned.
     */
    public static double parseOperatingPoint(@Nullable String typed) {
        if (typed == null) {
            return Double.NaN;
        }
        String trimmed = typed.trim();
        if (!OPERATING_POINT.matcher(trimmed).matches()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(trimmed.replace(',', '.'));
        } catch (NumberFormatException notANumber) {
            return Double.NaN;
        }
    }

    public static double average(@NonNull List<Double> samples) {
        if (samples.isEmpty()) throw new IllegalArgumentException("At least one sample is required");
        double total = 0d;
        for (double sample : samples) {
            if (!Double.isFinite(sample)) throw new IllegalArgumentException("Samples must be finite");
            total += sample;
        }
        return total / samples.size();
    }

    /**
     * Whether an operating point is one this sensor can be calibrated at: inside the safe band, so
     * the correction is taken where the tank is actually meant to sit.
     *
     * <p>A single offset is exact only where it was measured and drifts either side of it. Taken
     * out at the edge of the warning band it would be most accurate at a value the tank is not
     * supposed to hold, and least accurate across the range it usually sits in.
     */
    public static boolean isWithinSafeRange(double value, double safeLow, double safeHigh) {
        return Double.isFinite(value) && value >= safeLow && value <= safeHigh;
    }

    /**
     * How large the correction is against the width of the sensor's safe band, as a percentage.
     *
     * <p>An offset on its own says nothing about whether a probe is in good shape: 5 ppm is noise
     * on a dissolved-solids band hundreds wide and enormous on a pH one. Measured against the span
     * the sensor is graded on, the same number means the same thing for all of them.
     *
     * <p>{@link Double#NaN} when the band has no width, which is a band that cannot say how big
     * anything is relative to it.
     */
    public static double percentError(double offset, double safeLow, double safeHigh) {
        double span = safeHigh - safeLow;
        if (!Double.isFinite(offset) || !Double.isFinite(span) || span <= 0d) {
            return Double.NaN;
        }
        return Math.abs(offset) / span * 100d;
    }

    /** Offset definition requested by the calibration flow: reference minus raw average. */
    public static double offset(double reference, double rawAverage) {
        return reference - rawAverage;
    }

    public static double correct(double rawValue, double offset) {
        return rawValue + offset;
    }
}
