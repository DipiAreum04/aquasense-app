package ca.team6.aquasense.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public final class CalibrationMath {

    private static final Pattern OPERATING_POINT = Pattern.compile("-?\\d+([.,]\\d)?");

    private CalibrationMath() {}

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

    public static boolean isWithinSafeRange(double value, double safeLow, double safeHigh) {
        return Double.isFinite(value) && value >= safeLow && value <= safeHigh;
    }

    public static double percentError(double offset, double safeLow, double safeHigh) {
        double span = safeHigh - safeLow;
        if (!Double.isFinite(offset) || !Double.isFinite(span) || span <= 0d) {
            return Double.NaN;
        }
        return Math.abs(offset) / span * 100d;
    }

    public static double offset(double reference, double rawAverage) {
        return reference - rawAverage;
    }

    public static double correct(double rawValue, double offset) {
        return rawValue + offset;
    }
}
