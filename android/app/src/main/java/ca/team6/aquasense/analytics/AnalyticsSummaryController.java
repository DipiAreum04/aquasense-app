package ca.team6.aquasense.analytics;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import ca.team6.aquasense.R;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ReadingFormatter;
import ca.team6.aquasense.aquarium.SensorReading;
import ca.team6.aquasense.aquarium.sensors.AquariumSensor;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public class AnalyticsSummaryController {

    private static final SensorStatus[] DISTRIBUTION_BANDS = {
            SensorStatus.NORMAL, SensorStatus.WARNING, SensorStatus.CRITICAL};

    private static final int AVERAGE_INDEX = 1;

    private static final int[] STAT_LABELS = {
            R.string.analytics_stat_min,
            R.string.analytics_stat_avg,
            R.string.analytics_stat_max};

    private static final int[] WATER_LEVEL_STAT_LABELS = {
            R.string.analytics_stat_water_level_min,
            R.string.analytics_stat_water_level_avg,
            R.string.analytics_stat_water_level_max};

    private final ImageView statusLed;
    private final TextView statusText;
    private final TextView lastSeenText;
    private final CircularProgressIndicator uptimeRing;
    private final TextView uptimeValue;

    private final TextView[] statLabels;
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

        this.statLabels = new TextView[]{
                root.findViewById(R.id.analyticsStatMinLabel),
                root.findViewById(R.id.analyticsStatAvgLabel),
                root.findViewById(R.id.analyticsStatMaxLabel)};
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

    public void showSensorHealth(@NonNull AquariumSensor sensor,
                                 @Nullable SensorReading reading,
                                 long nowMillis) {
        this.statusLed.setImageResource(sensor.getStatusIconResId());
        this.statusText.setText(sensor.getStatusTextResId());
        this.lastSeenText.setText(
                lastSeen(this.statusText.getContext(), reading, nowMillis));
        this.showAccentedFiguresConnected(
                sensor.getSensorStatus() != SensorStatus.DISCONNECTED);
        this.showStatLabels(sensor);
    }

    private void showStatLabels(@NonNull AquariumSensor sensor) {
        int[] labels = DatabaseSchema.WATER_LEVEL_KEY.equals(sensor.getId())
                ? WATER_LEVEL_STAT_LABELS
                : STAT_LABELS;
        for (int i = 0; i < this.statLabels.length; i++) {
            this.statLabels[i].setText(labels[i]);
        }
    }

    private void showAccentedFiguresConnected(boolean connected) {
        int color = ContextCompat.getColor(this.uptimeValue.getContext(),
                connected ? R.color.accent : R.color.status_gray);

        this.uptimeRing.setIndicatorColor(color);
        this.statValues[AVERAGE_INDEX].setTextColor(color);
    }

    public void clearPeriod() {
        this.showPeriod(null, null);
    }

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
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) this.distributionBands[i].getLayoutParams();
            params.weight = shares[i];
            this.distributionBands[i].setLayoutParams(params);
            this.distributionValues[i].setText(hasReadings
                    ? percent(context, shares[i])
                    : context.getString(R.string.analytics_stat_empty));
        }
    }

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

    @NonNull
    private static CharSequence lastSeen(@NonNull Context context,
                                         @Nullable SensorReading reading,
                                         long nowMillis) {
        if (reading == null) {
            return context.getString(R.string.analytics_last_seen_pending);
        }

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
