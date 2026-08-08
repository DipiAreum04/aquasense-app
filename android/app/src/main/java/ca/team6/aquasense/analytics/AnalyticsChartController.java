package ca.team6.aquasense.analytics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.StringRes;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SensorReading;

/**
 * Owns the analytics line chart: the styling that keeps it in the app's day/night palette, and the
 * plotting of a period's buckets via {@link #setBuckets}.
 *
 * <p>Every colour is read from resources rather than hard-coded, so the chart follows the theme the
 * same way the rest of the screens do. MPAndroidChart resolves nothing itself; a colour it is not
 * handed defaults to black, which disappears against the dark card.
 */
public class AnalyticsChartController {

    private static final float LINE_WIDTH_DP = 2f;
    /** Out of 255. Enough for the fill to read as a band under the line without muddying the grid. */
    private static final int FILL_ALPHA = 40;
    /** Buckets are 36 seconds apart in last_1h, so a dot this size marks one without crowding. */
    private static final float POINT_RADIUS_DP = 2.5f;
    /** Headroom above and below the readings, as a percentage of the range they cover. */
    private static final float SPACE_PERCENT = 10f;
    /** The y range an empty plot is pinned to; see {@link #showEmpty}. */
    private static final float EMPTY_AXIS_MINIMUM = 0f;
    private static final float EMPTY_AXIS_MAXIMUM = 1f;

    private final LineChart chart;

    public AnalyticsChartController(@NonNull LineChart chart) {
        this.chart = chart;
        styleChart(chart.getContext(), chart);
    }

    /**
     * Plots a period's buckets, oldest first, breaking the line wherever the board reported a gap.
     *
     * <p>Each unbroken run becomes a dataset of its own. That is what draws the break: MPAndroidChart
     * joins consecutive points within a dataset and never joins across two, so the runs
     * {@link BucketSeries} hands back arrive on screen already separated. The alternative - one
     * dataset holding a NaN where the gap falls - is not something the renderer supports.
     *
     * @param seriesLabelResId names what is plotted. Passed per call rather than held for the life
     *                         of the controller, since the sensor on show changes with the tabs.
     * @param period           supplies both the unit the x axis is counted in and how far it
     *                         reaches, which change with the window: minutes over an hour, months
     *                         over a year.
     * @param buckets          already cut to the window by {@link AnalyticsPeriod#within}, so what
     *                         is plotted cannot reach past the axis drawn for it.
     */
    public void setBuckets(@StringRes int seriesLabelResId,
                           @NonNull AnalyticsPeriod period,
                           @NonNull List<SensorReading> buckets) {
        Context context = this.chart.getContext();
        String label = context.getString(seriesLabelResId);

        List<ILineDataSet> runs = new ArrayList<>();
        for (List<Entry> points : BucketSeries.split(buckets, period.getSecondsPerXUnit())) {
            LineDataSet run = new LineDataSet(points, label);
            styleSeries(context, run);
            runs.add(run);
        }

        if (runs.isEmpty()) {
            this.showEmpty(period);
            return;
        }

        this.showPeriodOnXAxis(period);
        // Undoes the pinning showEmpty leaves behind, so the readings scale the axis again.
        YAxis leftAxis = this.chart.getAxisLeft();
        leftAxis.resetAxisMinimum();
        leftAxis.resetAxisMaximum();
        leftAxis.setDrawLabels(true);

        this.chart.setData(new LineData(runs));
        // Drops any pan or zoom left over from the buckets that were on screen before.
        this.chart.fitScreen();
        this.chart.invalidate();
    }

    /**
     * Draws the period's axes with nothing on them.
     *
     * <p>An empty window is not an error and does not need to be announced: the axis says which
     * window is being looked at and the empty plot says the board has put nothing in it, which is
     * the whole of the message a line of text would carry.
     *
     * <p>Two things make that drawable. The chart is handed an empty {@link LineData} rather than
     * being cleared, because a chart with null data draws its no-data text and nothing else - no
     * axes, no grid. And the y axis is pinned to a fixed range with its labels turned off, because
     * there are no readings to scale it to: left to work it out from empty data it computes an
     * infinite range, and a scale invented out of nothing is worse than no scale at all.
     */
    public void showEmpty(@NonNull AnalyticsPeriod period) {
        this.showPeriodOnXAxis(period);

        YAxis leftAxis = this.chart.getAxisLeft();
        leftAxis.setAxisMinimum(EMPTY_AXIS_MINIMUM);
        leftAxis.setAxisMaximum(EMPTY_AXIS_MAXIMUM);
        leftAxis.setDrawLabels(false);

        this.chart.setData(new LineData());
        this.chart.fitScreen();
        this.chart.invalidate();
    }

    /**
     * Empties the chart while a read is in flight.
     *
     * <p>Switching tabs has to take the previous sensor's line down - leaving it up under the new
     * tab's name would be showing one sensor's readings labelled as another's - and an empty plot
     * would say the new sensor has no readings, which is not what waiting for them looks like. So
     * this is the one state that clears the data outright: the axes go with it, and the caller
     * hides the unit labels around them to match.
     */
    public void showLoading() {
        this.chart.setNoDataText(
                this.chart.getContext().getString(R.string.analytics_chart_loading));
        this.chart.clear();
    }

    /**
     * Draws the axis to the window's full length, whether or not the board has filled it.
     *
     * <p>x is measured back from the newest bucket, so the axis runs from 0 at the right edge out
     * to minus the window's length at the left; the labels drop the sign (see the formatter in
     * {@link #styleChart}). Fixing both ends is what stops an hour with two minutes of readings in
     * it drawing those two minutes across the whole card as though they were the hour.
     */
    private void showPeriodOnXAxis(@NonNull AnalyticsPeriod period) {
        XAxis xAxis = this.chart.getXAxis();
        xAxis.setAxisMinimum(-period.getSpanInXUnits());
        xAxis.setAxisMaximum(0f);
    }

    private static void styleSeries(Context context, LineDataSet series) {
        int accent = ContextCompat.getColor(context, R.color.accent);

        series.setColor(accent);
        series.setLineWidth(LINE_WIDTH_DP);
        series.setMode(LineDataSet.Mode.LINEAR);
        // Each bucket is marked, so a run cut down to a single point by gaps on both sides still
        // shows up, and so the echoed value that opens a run after an outage is visible as its own
        // reading rather than as the start of a line.
        series.setDrawCircles(true);
        series.setCircleColor(accent);
        series.setCircleRadius(POINT_RADIUS_DP);
        series.setDrawCircleHole(false);
        series.setDrawValues(false);
        series.setDrawFilled(true);
        series.setFillColor(accent);
        series.setFillAlpha(FILL_ALPHA);
    }

    private static void styleChart(Context context, LineChart chart) {
        int labelColor = ContextCompat.getColor(context, R.color.text_secondary);
        int gridColor = ContextCompat.getColor(context, R.color.divider);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        // The card behind the chart already supplies the surface colour.
        chart.setDrawGridBackground(false);
        chart.setNoDataTextColor(labelColor);

        // Panning and pinching along x is how you read back through a live series; letting y scale
        // too just tilts the line for no gain.
        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);

        // Tapping a reading drops a crosshair through it and nothing else: there is no marker or
        // readout on this chart for it to be pointing at, so all it does is put a line across the
        // plot that the next tap somewhere else moves.
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        // Counted back from the newest bucket, so the axis runs 0 at the right edge out to the
        // window's length at the left: the current reading, then the past behind it.
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(labelColor);
        xAxis.setAxisLineColor(gridColor);
        xAxis.setDrawGridLines(false);
        // Whole minutes only. Left to pick its own step the axis lands on values like 8.3, which
        // is a spacing no one reads a clock in.
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        // The values behind the axis have to be negative - that is what puts 0 at the right edge
        // and the oldest reading at the left - but the labels are read against "Minutes ago"
        // underneath them, which already says which way they run. Printing the sign as well says
        // it twice and invites the axis to be read as minutes into the future.
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.format(Locale.getDefault(), "%.0f", Math.abs(value));
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(labelColor);
        leftAxis.setAxisLineColor(gridColor);
        leftAxis.setGridColor(gridColor);
        // A tenth of the data's own range of headroom at each end, and no more. Readings sit well
        // away from zero - temperature around 21, pH around 7 - so an axis anchored at 0 spends
        // most of the plot on empty space and flattens the variation that is the point of the
        // graph. Stated rather than left to the library, which happens to default to the same 10:
        // the number is a decision about how the graph reads, not a default worth inheriting
        // silently. No axis minimum is set, since setting one is what makes these ignored.
        leftAxis.setSpaceBottom(SPACE_PERCENT);
        leftAxis.setSpaceTop(SPACE_PERCENT);

        // One y scale is enough for a single series, and the right axis would only repeat it.
        chart.getAxisRight().setEnabled(false);
    }
}
