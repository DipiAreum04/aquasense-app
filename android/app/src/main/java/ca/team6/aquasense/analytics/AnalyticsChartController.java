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
import ca.team6.aquasense.aquarium.SensorReading;

public class AnalyticsChartController {

    private static final float LINE_WIDTH_DP = 2f;
    private static final int FILL_ALPHA = 40;
    private static final float POINT_RADIUS_DP = 2.5f;
    private static final float SPACE_PERCENT = 10f;

    private final LineChart chart;

    private boolean disconnected;

    public AnalyticsChartController(@NonNull LineChart chart) {
        this.chart = chart;
        styleChart(chart.getContext(), chart);
    }

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
        this.chart.fitScreen();
        this.chart.invalidate();
        return true;
    }

    public void showMessage(@StringRes int messageResId) {
        this.chart.setNoDataText(this.chart.getContext().getString(messageResId));
        this.chart.clear();
    }

    private void showPeriodOnXAxis(@NonNull AnalyticsPeriod period) {
        XAxis xAxis = this.chart.getXAxis();
        xAxis.setAxisMinimum(-period.getSpanInXUnits());
        xAxis.setAxisMaximum(0f);
    }

    private static void styleSeries(Context context, LineDataSet series, boolean disconnected) {
        int seriesColor = ContextCompat.getColor(context,
                disconnected ? R.color.status_gray : R.color.accent);

        series.setColor(seriesColor);
        series.setLineWidth(LINE_WIDTH_DP);
        series.setMode(LineDataSet.Mode.LINEAR);
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
        chart.setDrawGridBackground(false);
        chart.setNoDataTextColor(labelColor);

        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);

        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(labelColor);
        xAxis.setAxisLineColor(gridColor);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
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
        leftAxis.setSpaceBottom(SPACE_PERCENT);
        leftAxis.setSpaceTop(SPACE_PERCENT);

        chart.getAxisRight().setEnabled(false);
    }
}
