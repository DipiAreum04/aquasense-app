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

public final class ThresholdForm {

    public static final List<String> CONFIGURABLE_SENSOR_IDS = Collections.unmodifiableList(
            Arrays.asList(
                    DatabaseSchema.TEMPERATURE_KEY,
                    DatabaseSchema.PH_LEVEL_KEY,
                    DatabaseSchema.DISSOLVED_SOLIDS_KEY));

    public enum Field {
        WARNING_LOW,
        SAFE_LOW,
        SAFE_HIGH,
        WARNING_HIGH,
        SPIKE
    }

    public enum Problem {
        NOT_A_NUMBER,
        NOT_ABOVE_PREVIOUS,
        NOT_POSITIVE
    }

    public interface Units {
        double toStorage(double displayValue);

        double toDisplay(double storedValue);

        double deltaToStorage(double displayDelta);

        double deltaToDisplay(double storedDelta);
    }

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

    @NonNull
    public static Units unitsFor(@NonNull String sensorId, boolean fahrenheit) {
        return fahrenheit && DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? FAHRENHEIT
                : IDENTITY;
    }

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

        @NonNull
        public Map<Field, Problem> getProblems() {
            return this.problems;
        }

        @Nullable
        public Problem problemFor(@NonNull Field field) {
            return this.problems.get(field);
        }

        @Nullable
        public ThresholdBand getBand() {
            return this.band;
        }

        public double getSpikeDelta() {
            return this.spikeDelta;
        }
    }

    private ThresholdForm() {}

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
        ThresholdBand band =
                ThresholdBand.fromValues(stored[0], stored[1], stored[2], stored[3]);
        if (band == null || !ThresholdBand.isStrictlyOrdered(
                band.getWarnLow(), band.getSafeLow(), band.getSafeHigh(), band.getWarnHigh())) {
            problems.put(Field.WARNING_HIGH, Problem.NOT_ABOVE_PREVIOUS);
            return new Result(problems, null, Double.NaN);
        }
        return new Result(problems, band, spikeDelta);
    }

    @Nullable
    private static Double parse(@Nullable String typed) {
        if (typed == null) {
            return null;
        }
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
