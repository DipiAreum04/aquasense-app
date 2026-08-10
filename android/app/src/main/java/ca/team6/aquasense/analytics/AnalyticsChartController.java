package ca.team6.aquasense.analytics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private final LineChart chart;

    // Whether the sensor being plotted is currently unreachable, which draws the line in the grey
    // the health card's lamp uses instead of the accent. Held rather than passed to each plot,
    // because the two move independently: the line is redrawn when a selection is fetched, and the
    // status when a reading lands or the staleness tick decides one is overdue.
    private boolean disconnected;

    public AnalyticsChartController(@NonNull LineChart chart) {
        this.chart = chart;
        styleChart(chart.getContext(), chart);
    }

    /**
     * Says whether the sensor on show is currently disconnected, restyling what is already plotted.
     *
     * <p>The readings behind the line are history and stay exactly as they were; what changes is
     * that they are no longer being added to. Drawing them in the accent while the card above says
     * the sensor is unreachable reads as a live line, so the colour follows the status - the same
     * grey, from the same resource, that the lamp and the word "Disconnected" are drawn in, which
     * carries its own day and night values.
     */
    public void setDisconnected(boolean disconnected) {
        if (this.disconnected == disconnected) {
            return;
        }
        this.disconnected = disconnected;

        LineData data = this.chart.getData();
        if (data == null) {
            return;
        }
        for (ILineDataSet run : data.getDataSets()) {
            if (run instanceof LineDataSet) {
                styleSeries(this.chart.getContext(), (LineDataSet) run, disconnected);
            }
        }
        this.chart.invalidate();
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
     * @param yAxisRange       the scale the sensor is read against, or null to let the readings
     *                         scale the axis themselves - which is the right answer for all but a
     *                         reading that is already a share of something. See {@link AxisRange}.
     * @return whether anything was plotted. False means the window holds nothing to draw a line
     *         from, and a line of text has been put where the plot would be, so the caller hides
     *         the unit labels it draws around axes that are no longer there.
     */
    public boolean setBuckets(@StringRes int seriesLabelResId,
                              @NonNull AnalyticsPeriod period,
                              @NonNull List<SensorReading> buckets,
                              @Nullable AxisRange yAxisRange) {
        Context context = this.chart.getContext();
        String label = context.getString(seriesLabelResId);

        List<ILineDataSet> runs = new ArrayList<>();
        for (List<Entry> points : BucketSeries.split(buckets, period.getSecondsPerXUnit())) {
            LineDataSet run = new LineDataSet(points, label);
            styleSeries(context, run, this.disconnected);
            runs.add(run);
        }

        if (runs.isEmpty()) {
            this.showMessage(R.string.analytics_chart_no_data);
            return false;
        }

        this.showPeriodOnXAxis(period);
        // Either pins the axis to the sensor's own scale, or undoes the pinning left behind by
        // showEmpty and by whichever sensor was on the tabs before, so the readings scale it again.
        YAxis leftAxis = this.chart.getAxisLeft();
        if (yAxisRange == null) {
            leftAxis.resetAxisMinimum();
            leftAxis.resetAxisMaximum();
        } else {
            leftAxis.setAxisMinimum(yAxisRange.getMinimum());
            leftAxis.setAxisMaximum(yAxisRange.getMaximum());
        }
        leftAxis.setDrawLabels(true);

        this.chart.setData(new LineData(runs));
        // Drops any pan or zoom left over from the buckets that were on screen before.
        this.chart.fitScreen();
        this.chart.invalidate();
        return true;
    }

    /**
     * Empties the chart and puts a line of text where the plot would be.
     *
     * <p>This state clears the data outright, so the axes go with it and the caller hides the unit
     * labels around them to match. It covers every case where there is no line to draw: waiting on
     * a read, a read that came back cancelled, having no aquarium to read from, and a window the
     * board has genuinely put nothing in yet.
     *
     * <p>That last one is a fact about the tank rather than about the app, and it used to be drawn
     * as the period's bare axes over an empty plot. The axes turned out to say it too quietly -
     * they are what a chart looks like either way - so it is now said in words like the rest.
     */
    public void showMessage(@StringRes int messageResId) {
        this.chart.setNoDataText(this.chart.getContext().getString(messageResId));
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

    private static void styleSeries(Context context, LineDataSet series, boolean disconnected) {
        // Line, dots and fill all take the one colour, so the whole series states the sensor's
        // reachability rather than only the stroke around it.
        int seriesColor = ContextCompat.getColor(context,
                disconnected ? R.color.status_gray : R.color.accent);

        series.setColor(seriesColor);
        series.setLineWidth(LINE_WIDTH_DP);
        series.setMode(LineDataSet.Mode.LINEAR);
        // Each bucket is marked, so a run cut down to a single point by gaps on both sides still
        // shows up, and so the echoed value that opens a run after an outage is visible as its own
        // reading rather than as the start of a line.
        series.setDrawCircles(true);
        series.setCircleColor(seriesColor);
        series.setCircleRadius(POINT_RADIUS_DP);
        series.setDrawCircleHole(false);
        series.setDrawValues(false);
        series.setDrawFilled(true);
        series.setFillColor(seriesColor);
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
