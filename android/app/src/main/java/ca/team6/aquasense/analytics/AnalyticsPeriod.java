package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.SensorReading;

public enum AnalyticsPeriod {

    LAST_1H(DatabaseSchema.LAST_1H_KEY, R.string.analytics_period_1h,
            R.string.analytics_period_1h_long,
            Seconds.PER_MINUTE, 60, R.string.analytics_axis_minutes_ago),
    LAST_1D(DatabaseSchema.LAST_1D_KEY, R.string.analytics_period_1d,
            R.string.analytics_period_1d_long,
            Seconds.PER_HOUR, 24, R.string.analytics_axis_hours_ago),
    LAST_1W(DatabaseSchema.LAST_1W_KEY, R.string.analytics_period_1w,
            R.string.analytics_period_1w_long,
            Seconds.PER_DAY, 7, R.string.analytics_axis_days_ago),
    LAST_1M(DatabaseSchema.LAST_1M_KEY, R.string.analytics_period_1m,
            R.string.analytics_period_1m_long,
            Seconds.PER_DAY, 30, R.string.analytics_axis_days_ago),
    LAST_6M(DatabaseSchema.LAST_6M_KEY, R.string.analytics_period_6m,
            R.string.analytics_period_6m_long,
            Seconds.PER_MONTH, 6, R.string.analytics_axis_months_ago),
    LAST_1Y(DatabaseSchema.LAST_1Y_KEY, R.string.analytics_period_1y,
            R.string.analytics_period_1y_long,
            Seconds.PER_MONTH, 12, R.string.analytics_axis_months_ago);

    private static final class Seconds {
        static final float PER_MINUTE = 60f;
        static final float PER_HOUR = 3600f;
        static final float PER_DAY = 86400f;
        static final float PER_MONTH = 2592000f;

        private Seconds() {}
    }

    public static final List<AnalyticsPeriod> ALL =
            Collections.unmodifiableList(Arrays.asList(values()));

    private final String databaseKey;
    @StringRes
    private final int labelResId;
    @StringRes
    private final int longLabelResId;
    private final float secondsPerXUnit;
    private final int spanInXUnits;
    @StringRes
    private final int xAxisLabelResId;

    AnalyticsPeriod(@NonNull String databaseKey,
                    @StringRes int labelResId,
                    @StringRes int longLabelResId,
                    float secondsPerXUnit,
                    int spanInXUnits,
                    @StringRes int xAxisLabelResId) {
        this.databaseKey = databaseKey;
        this.labelResId = labelResId;
        this.longLabelResId = longLabelResId;
        this.secondsPerXUnit = secondsPerXUnit;
        this.spanInXUnits = spanInXUnits;
        this.xAxisLabelResId = xAxisLabelResId;
    }

    @NonNull
    public String getDatabaseKey() {
        return this.databaseKey;
    }

    @StringRes
    public int getLabelResId() {
        return this.labelResId;
    }

    @StringRes
    public int getLongLabelResId() {
        return this.longLabelResId;
    }

    public float getSecondsPerXUnit() {
        return this.secondsPerXUnit;
    }

    public int getSpanInXUnits() {
        return this.spanInXUnits;
    }

    public long getWindowSeconds() {
        return (long) this.spanInXUnits * (long) this.secondsPerXUnit;
    }

    @StringRes
    public int getXAxisLabelResId() {
        return this.xAxisLabelResId;
    }

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
