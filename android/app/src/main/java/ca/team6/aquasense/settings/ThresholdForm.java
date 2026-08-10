package ca.team6.aquasense.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.ThresholdBand;

/**
 * Grades what the user typed on the Water Parameters screen and, when it holds together, turns it
 * into the {@link ThresholdBand} and spike delta the database stores.
 *
 * <p>Deliberately free of Android types so the rules below can be tested on their own.
 *
 * <h2>The four bounds</h2>
 *
 * Each bound is named after the band it opens, and that one name is used everywhere: on screen, in
 * {@link ThresholdBand}, in the {@code thresholds/{sensor}} node, and in the argument order the
 * built-in templates declare their ranges with. Warning Low is {@code warn_low} is
 * {@link ThresholdBand#getWarnLow()}, so what a user reads is what the database holds.
 *
 * <p>Critical is not a bound and is never entered. It is whatever falls outside the warning bounds,
 * exactly as the templates and the database already treat it.
 *
 * <h2>What counts as valid</h2>
 *
 * Every field must parse, and the four bounds must ascend strictly. That single ordering rule is
 * both halves of what SETTINGS-07 asks for. Two ranges overlap exactly when a bound sits below the
 * one before it, and a range is left uncovered exactly when two bounds coincide and squeeze it out
 * of existence — so ascending strictly leaves the number line partitioned into five bands with no
 * gaps and no overlaps, by construction. It is also what {@code database/rules.json} demands, so a
 * form that passes here is a form the server accepts.
 */
public final class ThresholdForm {

    /**
     * The sensors this screen offers a form for.
     *
     * <p>Water level is absent, and not by omission: its sensor is a non-contact detector that
     * reports only whether water still reaches the height it is mounted at, so there is no range
     * for a reading to sit inside. {@code database/rules.json} says the same thing from the other
     * side and rejects any write to {@code thresholds/water_level} or
     * {@code spike_deltas/water_level}, so a tab offering it would only fail on save.
     *
     * <p>Kept here rather than beside {@code DatabaseSchema.SENSOR_IDS} because it is not a fact
     * about the tree's shape - every one of these keys is already declared there. It is a fact
     * about which of them a person is allowed to edit, which is this screen's business.
     */
    public static final List<String> CONFIGURABLE_SENSOR_IDS = Collections.unmodifiableList(
            Arrays.asList(
                    DatabaseSchema.TEMPERATURE_KEY,
                    DatabaseSchema.PH_LEVEL_KEY,
                    DatabaseSchema.DISSOLVED_SOLIDS_KEY));

    /** The five inputs, ordered low to high so the ordering check can walk them in sequence. */
    public enum Field {
        WARNING_LOW,
        SAFE_LOW,
        SAFE_HIGH,
        WARNING_HIGH,
        SPIKE
    }

    public enum Problem {
        /** Blank, or not a finite number. */
        NOT_A_NUMBER,
        /** Parses, but does not sit strictly above the bound below it. */
        NOT_ABOVE_PREVIOUS,
        /** A spike delta of zero or less, which would make every reading a spike. */
        NOT_POSITIVE
    }

    /**
     * Moves one sensor's values between the unit the user reads the app in and the unit the
     * database stores. Only temperature has two units, so the rest of the app gets
     * {@link #IDENTITY} and never notices.
     */
    public interface Units {
        double toStorage(double displayValue);

        double toDisplay(double storedValue);

        /**
         * The same conversion for a difference rather than a reading. A difference scales but does
         * not shift, so a 2°C spike is 3.6°F and not 35.6°F.
         */
        double deltaToStorage(double displayDelta);

        double deltaToDisplay(double storedDelta);
    }

    /** For every sensor whose readings mean the same number however the app is configured. */
    public static final Units IDENTITY = new Units() {
        @Override
        public double toStorage(double displayValue) {
            return displayValue;
        }

        @Override
        public double toDisplay(double storedValue) {
            return storedValue;
        }

        @Override
        public double deltaToStorage(double displayDelta) {
            return displayDelta;
        }

        @Override
        public double deltaToDisplay(double storedDelta) {
            return storedDelta;
        }
    };

    private static final Units FAHRENHEIT = new Units() {
        @Override
        public double toStorage(double displayValue) {
            return ReadingFormatter.fromDisplayTemperature(displayValue, true);
        }

        @Override
        public double toDisplay(double storedValue) {
            return ReadingFormatter.toDisplayTemperature(storedValue, true);
        }

        @Override
        public double deltaToStorage(double displayDelta) {
            return ReadingFormatter.fromDisplayTemperatureDelta(displayDelta, true);
        }

        @Override
        public double deltaToDisplay(double storedDelta) {
            return ReadingFormatter.toDisplayTemperatureDelta(storedDelta, true);
        }
    };

    /**
     * The conversion one sensor's fields need, which is only ever a real one for a temperature
     * being read in Fahrenheit.
     */
    @NonNull
    public static Units unitsFor(@NonNull String sensorId, boolean fahrenheit) {
        return fahrenheit && DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? FAHRENHEIT
                : IDENTITY;
    }

    /** What one sensor's form adds up to: either a band and a delta, or the reasons it is not. */
    public static final class Result {

        private final Map<Field, Problem> problems;
        @Nullable
        private final ThresholdBand band;
        private final double spikeDelta;

        private Result(Map<Field, Problem> problems,
                       @Nullable ThresholdBand band,
                       double spikeDelta) {
            this.problems = Collections.unmodifiableMap(problems);
            this.band = band;
            this.spikeDelta = spikeDelta;
        }

        public boolean isValid() {
            return this.band != null;
        }

        /** Empty when the form is valid, otherwise one entry per field that needs attention. */
        @NonNull
        public Map<Field, Problem> getProblems() {
            return this.problems;
        }

        @Nullable
        public Problem problemFor(@NonNull Field field) {
            return this.problems.get(field);
        }

        /** The band in stored units, or null when the form does not describe one. */
        @Nullable
        public ThresholdBand getBand() {
            return this.band;
        }

        /** The spike delta in stored units, or {@link Double#NaN} when the form is invalid. */
        public double getSpikeDelta() {
            return this.spikeDelta;
        }
    }

    private ThresholdForm() {}

    /**
     * @param warningLow through {@code spike}, exactly as typed, in the unit {@code units} reads.
     * @param units      the conversion for the sensor being edited, from {@link #unitsFor}.
     */
    @NonNull
    public static Result validate(
            @Nullable String warningLow,
            @Nullable String safeLow,
            @Nullable String safeHigh,
            @Nullable String warningHigh,
            @Nullable String spike,
            @NonNull Units units
    ) {
        Map<Field, Problem> problems = new EnumMap<>(Field.class);

        Field[] boundFields = {
                Field.WARNING_LOW, Field.SAFE_LOW, Field.SAFE_HIGH, Field.WARNING_HIGH
        };
        String[] typed = {warningLow, safeLow, safeHigh, warningHigh};

        double[] stored = new double[boundFields.length];
        boolean[] parsed = new boolean[boundFields.length];

        for (int i = 0; i < boundFields.length; i++) {
            Double value = parse(typed[i]);
            if (value == null) {
                problems.put(boundFields[i], Problem.NOT_A_NUMBER);
                continue;
            }
            parsed[i] = true;
            stored[i] = units.toStorage(value);
        }

        // Each bound is checked against the one below it, and only when both of them parsed: a
        // blank field says nothing about whether its neighbour is in the right place, and marking
        // it too would blame a field the user has not got to yet.
        for (int i = 1; i < boundFields.length; i++) {
            if (parsed[i] && parsed[i - 1] && !(stored[i - 1] < stored[i])) {
                problems.put(boundFields[i], Problem.NOT_ABOVE_PREVIOUS);
            }
        }

        double spikeDelta = Double.NaN;
        Double typedSpike = parse(spike);
        if (typedSpike == null) {
            problems.put(Field.SPIKE, Problem.NOT_A_NUMBER);
        } else {
            spikeDelta = units.deltaToStorage(typedSpike);
            if (spikeDelta <= 0d) {
                problems.put(Field.SPIKE, Problem.NOT_POSITIVE);
            }
        }

        if (!problems.isEmpty()) {
            return new Result(problems, null, Double.NaN);
        }
        // fromValues rather than the constructor: the ordering check above ran on the numbers
        // before conversion, and while converting to Celsius preserves their order, this is the
        // one place that does not have to take that on trust.
        ThresholdBand band =
                ThresholdBand.fromValues(stored[0], stored[1], stored[2], stored[3]);
        if (band == null || !ThresholdBand.isStrictlyOrdered(
                band.getWarnLow(), band.getSafeLow(), band.getSafeHigh(), band.getWarnHigh())) {
            problems.put(Field.WARNING_HIGH, Problem.NOT_ABOVE_PREVIOUS);
            return new Result(problems, null, Double.NaN);
        }
        return new Result(problems, band, spikeDelta);
    }

    /**
     * Reads one field. Returns null for anything that is not a finite number, which covers the
     * blank field, the lone minus sign left behind mid-edit, and an infinity typed as digits.
     */
    @Nullable
    private static Double parse(@Nullable String typed) {
        if (typed == null) {
            return null;
        }
        // The field is formatted with the default locale, so a user in a comma-decimal locale is
        // shown "22,5" and types it back. The keyboard offers no grouping separator, so there is
        // no ambiguity in swapping it for the separator parseDouble expects.
        String text = typed.trim().replace(',', '.');
        if (text.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
