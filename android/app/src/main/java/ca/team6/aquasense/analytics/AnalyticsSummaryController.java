package ca.team6.aquasense.analytics;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/**
 * Owns the two cards that sit either side of the analytics graph: the sensor's health, and the
 * distribution of the readings behind the line.
 *
 * <p>They are driven from two different places and are updated separately for that reason. The
 * health line - the lamp, the status and the moment the sensor was last heard from - comes from the
 * live {@code last_instant} subscription and changes about once a second. Everything else is read
 * off a period's hundred buckets, which are fetched once when the selection changes and do not move
 * until it changes again.
 */
public class AnalyticsSummaryController {

    /** The bands of the distribution bar, left to right, worst last. */
    private static final SensorStatus[] DISTRIBUTION_BANDS = {
            SensorStatus.NORMAL, SensorStatus.WARNING, SensorStatus.CRITICAL};

    private final ImageView statusLed;
    private final TextView statusText;
    private final TextView lastSeenText;
    private final CircularProgressIndicator uptimeRing;
    private final TextView uptimeValue;

    private final TextView[] statValues;
    private final TextView[] statUnits;

    private final View[] distributionBands;
    private final TextView[] distributionValues;

    public AnalyticsSummaryController(@NonNull View root) {
        this.statusLed = root.findViewById(R.id.analyticsStatusLed);
        this.statusText = root.findViewById(R.id.analyticsStatusText);
        this.lastSeenText = root.findViewById(R.id.analyticsLastSeen);
        this.uptimeRing = root.findViewById(R.id.analyticsUptimeRing);
        this.uptimeValue = root.findViewById(R.id.analyticsUptimeValue);

        // Minimum, mean, maximum - the order the row draws them in, which the arrays below index by.
        this.statValues = new TextView[]{
                root.findViewById(R.id.analyticsStatMinValue),
                root.findViewById(R.id.analyticsStatAvgValue),
                root.findViewById(R.id.analyticsStatMaxValue)};
        this.statUnits = new TextView[]{
                root.findViewById(R.id.analyticsStatMinUnit),
                root.findViewById(R.id.analyticsStatAvgUnit),
                root.findViewById(R.id.analyticsStatMaxUnit)};

        this.distributionBands = new View[]{
                root.findViewById(R.id.analyticsDistributionNormalBand),
                root.findViewById(R.id.analyticsDistributionWarningBand),
                root.findViewById(R.id.analyticsDistributionCriticalBand)};
        this.distributionValues = new TextView[]{
                root.findViewById(R.id.analyticsDistributionNormalValue),
                root.findViewById(R.id.analyticsDistributionWarningValue),
                root.findViewById(R.id.analyticsDistributionCriticalValue)};
    }

    /**
     * Shows the selected sensor's live state: the same lamp and word its card on the dashboard
     * carries, without the pill around them, and the moment the board last spoke for it.
     *
     * @param reading the sensor's {@code last_instant}, or null when it has never published one.
     *                Its timestamp is used even when it carries the offline sentinel: the board
     *                wrote that too, so it is still the last time the sensor was heard from.
     */
    public void showSensorHealth(@NonNull AquariumSensor sensor,
                                 @Nullable SensorReading reading,
                                 long nowMillis) {
        // The disc arrives already tinted for its status, so nothing here has to colour it. The
        // word beside it stays in the text colour; see the layout for why it is not the status one.
        this.statusLed.setImageResource(sensor.getStatusIconResId());
        this.statusText.setText(sensor.getStatusTextResId());
        this.lastSeenText.setText(
                lastSeen(this.statusText.getContext(), reading, nowMillis));
    }

    /**
     * Blanks the period's half of the summary while a read is in flight.
     *
     * <p>Called for the same reason the chart is emptied: leaving the previous sensor's spread and
     * distribution up under the new tab's name would be reporting one sensor's readings as
     * another's, and they cannot be left to arrive on their own because a fetch that fails would
     * never take them down.
     */
    public void showPeriodLoading() {
        this.showPeriod(null, null);
    }

    /**
     * Shows what the selected period made of the sensor: how much of the time it covers the board
     * was reporting for, the spread of the readings, and how they fell across the aquarium's band.
     *
     * <p>The two halves empty separately, because the two questions do. A period holding nothing
     * but gap markers has an uptime worth reporting - nought - and no spread or distribution to
     * report at all.
     *
     * @param statistics null while the period is being fetched, and for a period the board has
     *                   committed nothing to; both leave the numbers as dashes.
     */
    public void showPeriod(@Nullable PeriodStatistics statistics,
                           @Nullable AquariumSensor sensor) {
        Context context = this.uptimeValue.getContext();
        boolean hasBuckets = statistics != null && statistics.hasBuckets();
        boolean hasReadings = statistics != null && statistics.hasReadings();

        int uptimePercent = hasBuckets ? statistics.getUptimePercent() : 0;
        this.uptimeRing.setProgressCompat(uptimePercent, true);
        this.uptimeValue.setText(hasBuckets
                ? percent(context, uptimePercent)
                : context.getString(R.string.analytics_stat_empty));

        double[] spread = hasReadings
                ? new double[]{statistics.getMin(), statistics.getMean(), statistics.getMax()}
                : null;
        for (int i = 0; i < this.statValues.length; i++) {
            boolean shown = spread != null && sensor != null;
            this.statValues[i].setText(shown
                    ? formatReading(context, sensor, spread[i])
                    : context.getString(R.string.analytics_stat_empty));
            this.statUnits[i].setText(shown ? context.getString(sensor.getUnitResId()) : "");
        }

        int[] shares = hasReadings
                ? statistics.percentagesOf(DISTRIBUTION_BANDS)
                : new int[DISTRIBUTION_BANDS.length];
        for (int i = 0; i < this.distributionBands.length; i++) {
            // The bar is laid out against a weight sum of 100, so the shares are its widths as
            // they stand. Taking the rounded shares rather than the raw counts is what keeps a
            // band the same size as the percentage printed under it.
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) this.distributionBands[i].getLayoutParams();
            params.weight = shares[i];
            this.distributionBands[i].setLayoutParams(params);
            this.distributionValues[i].setText(hasReadings
                    ? percent(context, shares[i])
                    : context.getString(R.string.analytics_stat_empty));
        }
    }

    /**
     * Formats one of the three statistics in the unit the app reads that sensor in.
     *
     * <p>Everything but water level goes through the formatter the sensor cards use, which converts
     * the temperature if Display &amp; Units asks for Fahrenheit and picks the decimals the sensor
     * has any business claiming. Water level cannot: its formatter answers LOW or SAFE, which is
     * the right answer for a switch read once and the wrong one for the average of a bucket. What
     * is being averaged is the share of the bucket the float spent under water, so that is what is
     * shown - the same quantity the graph's y axis plots for it.
     */
    @NonNull
    private static String formatReading(@NonNull Context context,
                                        @NonNull AquariumSensor sensor,
                                        double value) {
        if (DatabaseSchema.WATER_LEVEL_KEY.equals(sensor.getId())) {
            return percent(context, Math.round(value * 100f));
        }
        return ReadingFormatter.format(context, sensor.getId(), value);
    }

    @NonNull
    private static String percent(@NonNull Context context, long value) {
        return context.getString(R.string.analytics_percent, value);
    }

    /**
     * How long ago the board last wrote this sensor's {@code last_instant}, in the largest unit
     * that leaves a number worth reading. The dashboard says the same thing about the aquarium as
     * a whole; here it is about the one sensor on show, which is why the wording differs.
     */
    @NonNull
    private static CharSequence lastSeen(@NonNull Context context,
                                         @Nullable SensorReading reading,
                                         long nowMillis) {
        if (reading == null) {
            return context.getString(R.string.analytics_last_seen_pending);
        }

        // Clamped at zero: the board's clock may be a second or two ahead of the server's, and
        // "last seen -1 minutes ago" is a worse answer than "just now".
        long ageSeconds = Math.max(0L, nowMillis / 1000L - reading.getTimestampSeconds());
        Resources resources = context.getResources();
        if (ageSeconds < 60L) {
            return context.getString(R.string.analytics_last_seen_now);
        }
        if (ageSeconds < 3600L) {
            int minutes = (int) (ageSeconds / 60L);
            return resources.getQuantityString(
                    R.plurals.analytics_last_seen_minutes, minutes, minutes);
        }
        if (ageSeconds < 86400L) {
            int hours = (int) (ageSeconds / 3600L);
            return resources.getQuantityString(
                    R.plurals.analytics_last_seen_hours, hours, hours);
        }
        int days = (int) (ageSeconds / 86400L);
        return resources.getQuantityString(R.plurals.analytics_last_seen_days, days, days);
    }
}
