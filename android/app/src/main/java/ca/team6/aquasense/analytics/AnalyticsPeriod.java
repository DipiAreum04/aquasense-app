package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.SensorReading;

/**
 * The windows the analytics page can plot: one per period the board records, each with the button
 * that selects it, the unit its x axis is measured in, and how many of that unit it reaches back.
 *
 * <p>Each window reads in the unit that suits its length:
 *
 * <pre>
 *   1H  minutes, out to 60      1M  days, out to 30
 *   1D  hours, out to 24        6M  months, out to 6
 *   1W  days, out to 7          1Y  months, out to 12
 * </pre>
 *
 * <p>The unit has to travel with the window rather than being fixed once for the chart. Every period
 * holds the same hundred buckets, but they cover an hour in one case and a year in another, so one
 * shared unit cannot serve them: minutes would run a year's axis out past half a million, and months
 * would collapse an hour's into a single step at zero.
 *
 * <p>The reach is the other half of that. It is what the x axis is drawn to, so a window shows its
 * whole length whether or not the board has filled it, and it is what {@link #within} measures a
 * bucket against: a ring buffer holds whatever was last written to each slot, and a slot the board
 * has not come back round to still carries a reading from before this window began.
 */
public enum AnalyticsPeriod {

    LAST_1H(DatabaseSchema.LAST_1H_KEY, R.string.analytics_period_1h,
            Seconds.PER_MINUTE, 60, R.string.analytics_axis_minutes_ago),
    LAST_1D(DatabaseSchema.LAST_1D_KEY, R.string.analytics_period_1d,
            Seconds.PER_HOUR, 24, R.string.analytics_axis_hours_ago),
    LAST_1W(DatabaseSchema.LAST_1W_KEY, R.string.analytics_period_1w,
            Seconds.PER_DAY, 7, R.string.analytics_axis_days_ago),
    LAST_1M(DatabaseSchema.LAST_1M_KEY, R.string.analytics_period_1m,
            Seconds.PER_DAY, 30, R.string.analytics_axis_days_ago),
    LAST_6M(DatabaseSchema.LAST_6M_KEY, R.string.analytics_period_6m,
            Seconds.PER_MONTH, 6, R.string.analytics_axis_months_ago),
    LAST_1Y(DatabaseSchema.LAST_1Y_KEY, R.string.analytics_period_1y,
            Seconds.PER_MONTH, 12, R.string.analytics_axis_months_ago);

    /**
     * Held in a class of their own because an enum constant cannot reference a static field of its
     * own enum: the constants are declared first and the field would be a forward reference.
     */
    private static final class Seconds {
        static final float PER_MINUTE = 60f;
        static final float PER_HOUR = 3600f;
        static final float PER_DAY = 86400f;
        /** The 30 days the firmware buckets last_1m over, not a calendar month. */
        static final float PER_MONTH = 2592000f;

        private Seconds() {}
    }

    /** In declaration order, shortest window first, which is the order the buttons appear in. */
    public static final List<AnalyticsPeriod> ALL =
            Collections.unmodifiableList(Arrays.asList(values()));

    private final String databaseKey;
    @StringRes
    private final int labelResId;
    private final float secondsPerXUnit;
    private final int spanInXUnits;
    @StringRes
    private final int xAxisLabelResId;

    AnalyticsPeriod(@NonNull String databaseKey,
                    @StringRes int labelResId,
                    float secondsPerXUnit,
                    int spanInXUnits,
                    @StringRes int xAxisLabelResId) {
        this.databaseKey = databaseKey;
        this.labelResId = labelResId;
        this.secondsPerXUnit = secondsPerXUnit;
        this.spanInXUnits = spanInXUnits;
        this.xAxisLabelResId = xAxisLabelResId;
    }

    /** The period node's key under a sensor, as {@link DatabaseSchema} names it. */
    @NonNull
    public String getDatabaseKey() {
        return this.databaseKey;
    }

    @StringRes
    public int getLabelResId() {
        return this.labelResId;
    }

    /** How many seconds one step along this window's x axis covers. */
    public float getSecondsPerXUnit() {
        return this.secondsPerXUnit;
    }

    /** How many of those steps the window reaches back, which is the length of its x axis. */
    public int getSpanInXUnits() {
        return this.spanInXUnits;
    }

    /** The same reach in seconds, which is what a bucket's age is measured against. */
    public long getWindowSeconds() {
        return (long) this.spanInXUnits * (long) this.secondsPerXUnit;
    }

    /** Names the unit the x axis counts back in, e.g. "Minutes ago". */
    @StringRes
    public int getXAxisLabelResId() {
        return this.xAxisLabelResId;
    }

    /**
     * Drops the buckets that fall outside this window, measured back from the newest one.
     *
     * <p>A period is a ring buffer, and every slot holds whatever was last written to it. A board
     * that has been running for two days has been round last_1h fifty times and every slot is
     * current, but it has only reached a third of the way through last_1w, and the slots ahead of
     * its cursor still hold the week before last. Those are real readings, correctly stored, and
     * they are not in the window being asked about - plotting them would run the line off the left
     * of the axis and drag the spread and the distribution out with it.
     *
     * <p>The buckets arrive in time order, so what is being kept is a suffix of the list and is
     * returned as a view onto it rather than a copy.
     *
     * @param buckets oldest first, as {@code FirebaseDatabaseHelper} unrolls the ring buffer.
     */
    @NonNull
    public List<SensorReading> within(@NonNull List<SensorReading> buckets) {
        if (buckets.isEmpty()) {
            return buckets;
        }

        long newestSeconds = buckets.get(buckets.size() - 1).getTimestampSeconds();
        long oldestInWindow = newestSeconds - this.getWindowSeconds();
        int firstKept = 0;
        while (firstKept < buckets.size()
                && buckets.get(firstKept).getTimestampSeconds() < oldestInWindow) {
            firstKept++;
        }
        return firstKept == 0 ? buckets : buckets.subList(firstKept, buckets.size());
    }
}
