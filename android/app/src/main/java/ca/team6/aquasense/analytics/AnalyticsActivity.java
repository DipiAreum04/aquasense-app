package ca.team6.aquasense.analytics;

import ca.team6.aquasense.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.tabs.TabLayout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.team6.aquasense.analytics.AnalyticsChartController;
import ca.team6.aquasense.analytics.AnalyticsClearDialog;
import ca.team6.aquasense.analytics.AnalyticsPeriod;
import ca.team6.aquasense.analytics.AnalyticsSummaryController;
import ca.team6.aquasense.analytics.AquariumDropdown;
import ca.team6.aquasense.analytics.AxisRange;
import ca.team6.aquasense.analytics.DownloadsWriter;
import ca.team6.aquasense.analytics.PeriodStatistics;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.dashboard.SensorInfoBottomSheet;
import ca.team6.aquasense.settings.AppSettings;
import ca.team6.aquasense.aquarium.InfoSheetSection;
import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.aquarium.AquariumRepository;
import ca.team6.aquasense.settings.CalibrationOffsetStore;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.firebase.FirebaseDatabaseHelper;
import ca.team6.aquasense.aquarium.ReadingFormatter;
import ca.team6.aquasense.aquarium.SensorReading;
import ca.team6.aquasense.settings.SettingsRepository;
import ca.team6.aquasense.settings.SharedPreferenceHelper;
import ca.team6.aquasense.aquarium.TelemetryRepository;
import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.WaterType;
import ca.team6.aquasense.aquarium.sensors.AquariumSensor;
import ca.team6.aquasense.aquarium.sensors.DissolvedSolidsSensor;
import ca.team6.aquasense.aquarium.sensors.PhLevelSensor;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;
import ca.team6.aquasense.aquarium.sensors.TemperatureSensor;
import ca.team6.aquasense.aquarium.sensors.WaterLevelSensor;
import ca.team6.aquasense.notifications.SensorThresholds;

public class AnalyticsActivity extends AppCompatActivity {

    private static final long STALENESS_CHECK_INTERVAL_MS = 10_000;
    private static final String STATE_SENSOR_ID = "analytics_sensor_id";
    private static final String STATE_PERIOD_NAME = "analytics_period_name";

    private static final float DISABLED_ACTION_ALPHA = 0.4f;

    private static final String DOWNLOAD_MIME_TYPE = "application/json";
    private static final String DOWNLOAD_TIMESTAMP_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    private static final String UNSAFE_FILE_NAME_CHARS = "[^A-Za-z0-9._-]";

    private static final AxisRange PERCENT_AXIS = new AxisRange(0f, 100f);

    private final List<AquariumSensor> sensors = Collections.unmodifiableList(Arrays.asList(
            new WaterLevelSensor(),
            new TemperatureSensor(),
            new DissolvedSolidsSensor(),
            new PhLevelSensor()));

    private AnalyticsChartController chartController;
    private AnalyticsSummaryController summaryController;
    private AquariumRepository aquariumRepository;
    private TelemetryRepository telemetryRepository;
    private CalibrationOffsetStore calibrationOffsets;
    private TextView aquariumNameText;
    private ImageView aquariumIcon;
    private TextView yAxisLabel;
    private TextView xAxisLabel;
    private View chartCard;
    private ImageButton downloadButton;
    private ImageButton clearButton;

    private AquariumSensor selectedSensor = this.sensors.get(0);
    private AnalyticsPeriod selectedPeriod = AnalyticsPeriod.LAST_1H;

    private boolean downloading;

    private boolean selectionHasBuckets;

    @Nullable
    private String requestKey;

    @Nullable
    private String lastDrawnKey;

    @Nullable
    private List<SensorReading> loadedBuckets;

    private final Handler stalenessHandler = new Handler(Looper.getMainLooper());
    private final Runnable stalenessTick = new Runnable() {
        @Override
        public void run() {
            showSensorHealth();
            stalenessHandler.postDelayed(this, STALENESS_CHECK_INTERVAL_MS);
        }
    };

    private final AquariumRepository.AquariumsObserver aquariumsObserver = aquariums -> {
        showActiveAquarium();
        watchActiveAquarium();
        plotSelection();
        showPeriodSummary();
    };

    private final TelemetryRepository.TelemetryObserver telemetryObserver =
            readings -> showSensorHealth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        AuthRepository authRepository = AuthRepository.getInstance(this);
        if (!authRepository.isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        setContentView(R.layout.activity_analytics);

        Toolbar toolbar = findViewById(R.id.toolbar_analytics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.analytics);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.restoreSelection(savedInstanceState);

        LineChart chart = findViewById(R.id.analyticsChart);
        this.chartController = new AnalyticsChartController(chart);
        this.summaryController =
                new AnalyticsSummaryController(findViewById(R.id.analyticsContent));
        this.chartController.showMessage(R.string.analytics_chart_loading);
        this.summaryController.clearPeriod();
        this.yAxisLabel = findViewById(R.id.analyticsYAxisLabel);
        this.xAxisLabel = findViewById(R.id.analyticsXAxisLabel);
        this.showAxisLabels(false);

        this.chartCard = findViewById(R.id.analyticsChartCard);
        this.chartCard.setOnClickListener(v -> {
            this.setChartRetryEnabled(false);
            this.plotSelection();
        });
        this.setChartRetryEnabled(false);
        this.aquariumNameText = findViewById(R.id.analyticsAquariumName);
        this.aquariumIcon = findViewById(R.id.analyticsAquariumIcon);
        this.aquariumIcon.getBackground().mutate().setTint(
                ContextCompat.getColor(this, R.color.aquarium_icon_bg));

        this.setUpAquariumSelector();
        this.setUpSensorTabs();
        this.setUpPeriodButtons();
        this.setUpChartActions();
        this.setUpInfoSheets();

        this.telemetryRepository = TelemetryRepository.getInstance();
        this.calibrationOffsets = new CalibrationOffsetStore(this);
        this.aquariumRepository = AquariumRepository.getInstance(this);
        this.telemetryRepository.addObserver(this.telemetryObserver);
        this.aquariumRepository.addObserver(this.aquariumsObserver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SENSOR_ID, this.selectedSensor.getId());
        outState.putString(STATE_PERIOD_NAME, this.selectedPeriod.name());
    }

    private void restoreSelection(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        String sensorId = savedInstanceState.getString(STATE_SENSOR_ID);
        for (AquariumSensor sensor : this.sensors) {
            if (sensor.getId().equals(sensorId)) {
                this.selectedSensor = sensor;
                break;
            }
        }

        String periodName = savedInstanceState.getString(STATE_PERIOD_NAME);
        for (AnalyticsPeriod period : AnalyticsPeriod.ALL) {
            if (period.name().equals(periodName)) {
                this.selectedPeriod = period;
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.applyTemperatureUnitPreference();
        this.showActiveAquarium();
        this.watchActiveAquarium();
        this.plotSelection();
        this.stalenessHandler.post(this.stalenessTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.stalenessHandler.removeCallbacks(this.stalenessTick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.telemetryRepository.unwatchPeriod();
        this.requestKey = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.aquariumRepository != null) {
            this.aquariumRepository.removeObserver(this.aquariumsObserver);
        }
        if (this.telemetryRepository != null) {
            this.telemetryRepository.removeObserver(this.telemetryObserver);
        }
    }

    private void setUpAquariumSelector() {
        View card = findViewById(R.id.analyticsAquariumSelector);
        AquariumDropdown dropdown = new AquariumDropdown(
                card, findViewById(R.id.analyticsAquariumChevron), this::selectAquarium);

        card.setOnClickListener(v -> dropdown.show(
                this.aquariumRepository.getAquariums(), this.activeAquariumId()));
    }

    private void selectAquarium(@NonNull Aquarium aquarium) {
        if (aquarium.getId().equals(this.activeAquariumId())) {
            return;
        }
        this.aquariumRepository.setActiveAquariumId(aquarium.getId());
        this.showActiveAquarium();
        this.watchActiveAquarium();
        this.plotSelection();
    }

    private void setUpSensorTabs() {
        TabLayout tabs = findViewById(R.id.analyticsSensorTabs);
        for (AquariumSensor sensor : this.sensors) {
            tabs.addTab(tabs.newTab()
                    .setCustomView(R.layout.item_analytics_sensor_tab)
                    .setText(sensor.getNameResId()));
        }

        TabLayout.Tab restored = tabs.getTabAt(this.sensors.indexOf(this.selectedSensor));
        if (restored != null) {
            restored.select();
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(@NonNull TabLayout.Tab tab) {
                selectedSensor = sensors.get(tab.getPosition());
                showSensorHealth();
                plotSelection();
            }

            @Override
            public void onTabUnselected(@NonNull TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(@NonNull TabLayout.Tab tab) {}
        });
    }

    private void setUpPeriodButtons() {
        LinearLayout container = findViewById(R.id.analyticsPeriodButtons);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AnalyticsPeriod period : AnalyticsPeriod.ALL) {
            TextView button = (TextView) inflater.inflate(
                    R.layout.item_analytics_period, container, false);
            button.setText(period.getLabelResId());
            button.setSelected(period == this.selectedPeriod);
            button.setOnClickListener(v -> {
                if (period == this.selectedPeriod) {
                    return;
                }
                this.selectedPeriod = period;
                this.markSelectedPeriod(container);
                this.plotSelection();
            });
            container.addView(button);
        }
    }

    private void setUpInfoSheets() {
        findViewById(R.id.analyticsHealthInfoButton).setOnClickListener(v ->
                SensorInfoBottomSheet.explaining(
                        R.drawable.info_24px,
                        R.string.analytics_info_health_title,
                        R.string.analytics_info_lead_title,
                        R.string.analytics_info_health_lead,
                        this.infoSection(R.string.analytics_info_health_period_title,
                                R.string.analytics_info_health_period_1),
                        this.infoSection(R.string.analytics_info_health_uptime_title,
                                R.string.analytics_info_health_uptime_1),
                        this.infoSection(R.string.analytics_info_health_stats_title,
                                R.string.analytics_info_health_stats_1,
                                R.string.analytics_info_health_stats_2,
                                R.string.analytics_info_health_stats_3))
                        .show(getSupportFragmentManager(), null));

        findViewById(R.id.analyticsDistributionInfoButton).setOnClickListener(v ->
                SensorInfoBottomSheet.explaining(
                        R.drawable.info_24px,
                        R.string.analytics_info_distribution_title,
                        R.string.analytics_info_lead_title,
                        R.string.analytics_info_distribution_lead,
                        this.infoSection(R.string.analytics_info_distribution_counts_title,
                                R.string.analytics_info_distribution_counts_1),
                        this.infoSection(R.string.analytics_info_distribution_grading_title,
                                R.string.analytics_info_distribution_grading_1))
                        .show(getSupportFragmentManager(), null));
    }

    @NonNull
    private InfoSheetSection infoSection(@StringRes int titleResId, @StringRes int... itemResIds) {
        String[] items = new String[itemResIds.length];
        for (int i = 0; i < itemResIds.length; i++) {
            items[i] = getString(itemResIds[i]);
        }
        return new InfoSheetSection(getString(titleResId), items);
    }

    private void markSelectedPeriod(@NonNull LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setSelected(AnalyticsPeriod.ALL.get(i) == this.selectedPeriod);
        }
    }

    private void setUpChartActions() {
        this.downloadButton = findViewById(R.id.analyticsDownloadButton);
        this.clearButton = findViewById(R.id.analyticsClearButton);

        this.downloadButton.setOnClickListener(v -> this.downloadSelection());
        this.clearButton.setOnClickListener(v -> this.confirmClearSelection());
        this.setActionEnabled(this.downloadButton, false);
        this.setActionEnabled(this.clearButton, false);
    }

    private void showChartActionsEnabled() {
        boolean actionable = this.activeAquariumId() != null && this.selectionHasBuckets;
        this.setActionEnabled(this.downloadButton, actionable && !this.downloading);
        this.setActionEnabled(this.clearButton, actionable);
    }

    private void setActionEnabled(@NonNull ImageButton action, boolean enabled) {
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : DISABLED_ACTION_ALPHA);
    }

    private void showActiveAquarium() {
        this.showChartActionsEnabled();

        if (!this.aquariumRepository.isLoaded()) {
            this.aquariumNameText.setText(R.string.loading_aquariums);
            this.showAquariumIcon(null);
            return;
        }

        Aquarium active = this.aquariumRepository.getActiveAquarium();
        this.aquariumNameText.setText(
                active == null ? getString(R.string.no_aquariums) : active.getName());
        this.showAquariumIcon(active);
    }

    private void showAquariumIcon(@Nullable Aquarium aquarium) {
        boolean saltwater = aquarium != null
                && WaterType.fromKey(aquarium.getWaterType()) == WaterType.SALTWATER;
        this.aquariumIcon.setImageResource(
                saltwater ? R.drawable.aquarium_saltwater : R.drawable.aquarium_freshwater);
    }

    private void watchActiveAquarium() {
        String aquariumId = this.activeAquariumId();
        if (aquariumId == null) {
            this.telemetryRepository.unwatch();
            return;
        }
        this.telemetryRepository.watchAquarium(aquariumId);
    }

    private void showSensorHealth() {
        Aquarium active = this.aquariumRepository.getActiveAquarium();
        Map<String, SensorReading> readings = this.telemetryRepository.getReadings();
        long nowMillis = this.telemetryRepository.nowMillis();

        for (AquariumSensor sensor : this.sensors) {
            sensor.applyReading(this, readings.get(sensor.getId()),
                    this.thresholdFor(active, sensor),
                    SensorThresholds.resolveSpikeDelta(active, sensor.getId()),
                    nowMillis);
        }
        this.summaryController.showSensorHealth(this.selectedSensor,
                readings.get(this.selectedSensor.getId()), nowMillis);
        this.chartController.setDisconnected(
                this.selectedSensor.getSensorStatus() == SensorStatus.DISCONNECTED);
    }

    private void showPeriodSummary() {
        if (this.loadedBuckets == null) {
            return;
        }
        this.summaryController.showPeriod(
                PeriodStatistics.of(this.loadedBuckets, this.selectedSensor,
                        this.thresholdFor(this.aquariumRepository.getActiveAquarium(),
                                this.selectedSensor)),
                this.selectedSensor);
    }

    private void plotSelection() {
        String aquariumId = this.activeAquariumId();
        if (aquariumId == null) {
            this.stopReading(this.aquariumRepository.isLoaded()
                    ? R.string.analytics_chart_no_aquarium
                    : R.string.analytics_chart_loading);
            return;
        }

        AquariumSensor sensor = this.selectedSensor;
        AnalyticsPeriod period = this.selectedPeriod;
        String key = aquariumId + "/" + sensor.getId() + "/" + period.getDatabaseKey()
                + "/" + this.temperatureUnitKey()
                + "/" + this.calibrationOffsets.get(aquariumId, sensor.getId());
        if (key.equals(this.requestKey)) {
            return;
        }
        this.requestKey = key;
        this.setChartRetryEnabled(false);

        this.yAxisLabel.setText(this.yAxisLabelResId(sensor));
        this.xAxisLabel.setText(period.getXAxisLabelResId());

        if (key.equals(this.lastDrawnKey) && this.loadedBuckets != null) {
            this.drawLoadedBuckets(sensor, period);
        } else {
            this.loadedBuckets = null;
            this.selectionHasBuckets = false;
            this.showChartActionsEnabled();
            this.showAxisLabels(false);
            this.chartController.showMessage(R.string.analytics_chart_loading);
            this.summaryController.clearPeriod();
        }

        this.telemetryRepository.watchPeriod(aquariumId, sensor.getId(), period.getDatabaseKey(),
                new TelemetryRepository.PeriodCallback() {
                    @Override
                    public void onBuckets(@NonNull List<SensorReading> buckets) {
                        if (isDestroyed() || !key.equals(requestKey)) {
                            return;
                        }
                        selectionHasBuckets = !buckets.isEmpty();
                        showChartActionsEnabled();
                        loadedBuckets = period.within(buckets);
                        drawLoadedBuckets(sensor, period);
                        lastDrawnKey = key;
                    }

                    @Override
                    public void onError() {
                        if (isDestroyed() || !key.equals(requestKey)) {
                            return;
                        }
                        requestKey = null;
                        lastDrawnKey = null;
                        loadedBuckets = null;
                        selectionHasBuckets = false;
                        showChartActionsEnabled();
                        showAxisLabels(false);
                        chartController.showMessage(R.string.analytics_chart_error);
                        summaryController.clearPeriod();
                        setChartRetryEnabled(true);
                    }
                });
    }

    private void drawLoadedBuckets(@NonNull AquariumSensor sensor,
                                   @NonNull AnalyticsPeriod period) {
        if (this.loadedBuckets == null) {
            return;
        }
        this.showAxisLabels(this.chartController.setBuckets(sensor.getNameResId(), period,
                this.inDisplayUnits(sensor, this.loadedBuckets), this.yAxisRangeFor(sensor)));
        this.showPeriodSummary();
    }

    private void stopReading(@StringRes int messageResId) {
        this.telemetryRepository.unwatchPeriod();
        this.requestKey = null;
        this.lastDrawnKey = null;
        this.loadedBuckets = null;
        this.selectionHasBuckets = false;
        this.showChartActionsEnabled();
        this.setChartRetryEnabled(false);
        this.showAxisLabels(false);
        this.chartController.showMessage(messageResId);
        this.summaryController.clearPeriod();
    }

    private void setChartRetryEnabled(boolean enabled) {
        this.chartCard.setClickable(enabled);
        this.chartCard.setForeground(enabled
                ? ContextCompat.getDrawable(this, R.drawable.rounded_ripple_10dp_radius)
                : null);
    }

    private void downloadSelection() {
        String aquariumId = this.activeAquariumId();
        if (aquariumId == null || this.downloading) {
            return;
        }

        AquariumSensor sensor = this.selectedSensor;
        AnalyticsPeriod period = this.selectedPeriod;
        this.downloading = true;
        this.showChartActionsEnabled();

        this.telemetryRepository.readPeriodJson(aquariumId, sensor.getId(), period.getDatabaseKey(),
                new FirebaseDatabaseHelper.PeriodJsonListener() {
                    @Override
                    public void onJson(@NonNull String json) {
                        if (isDestroyed()) {
                            return;
                        }
                        DownloadsWriter.saveText(AnalyticsActivity.this,
                                downloadFileName(aquariumId, sensor, period),
                                DOWNLOAD_MIME_TYPE, json, new DownloadsWriter.SaveCallback() {
                                    @Override
                                    public void onSaved() {
                                        finishDownload(R.string.analytics_download_saved);
                                    }

                                    @Override
                                    public void onError() {
                                        finishDownload(R.string.analytics_download_failed);
                                    }
                                });
                    }

                    @Override
                    public void onEmpty() {
                        finishDownload(R.string.analytics_download_empty);
                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        finishDownload(R.string.analytics_download_failed);
                    }
                });
    }

    private void finishDownload(@StringRes int messageResId) {
        this.downloading = false;
        if (isDestroyed()) {
            return;
        }
        this.showChartActionsEnabled();
        this.toast(messageResId);
    }

    @NonNull
    private String downloadFileName(@NonNull String aquariumId,
                                    @NonNull AquariumSensor sensor,
                                    @NonNull AnalyticsPeriod period) {
        String takenAt = DateTimeFormatter.ofPattern(DOWNLOAD_TIMESTAMP_PATTERN, Locale.ROOT)
                .format(LocalDateTime.now());
        String safeAquariumId = aquariumId.replaceAll(UNSAFE_FILE_NAME_CHARS, "_");
        return String.format(Locale.ROOT, "aquasense_%s_%s_%s_%s.json",
                safeAquariumId, sensor.getId(), period.getDatabaseKey(), takenAt);
    }

    private void confirmClearSelection() {
        Aquarium active = this.aquariumRepository.getActiveAquarium();
        if (active == null) {
            return;
        }

        AquariumSensor sensor = this.selectedSensor;
        AnalyticsPeriod period = this.selectedPeriod;
        AnalyticsClearDialog.show(this, active.getName(), sensor.getNameResId(), period,
                () -> this.telemetryRepository.deletePeriod(active.getId(), sensor.getId(),
                        period.getDatabaseKey(), new FirebaseDatabaseHelper.DbCallback() {
                            @Override
                            public void onSuccess() {
                                toast(R.string.analytics_clear_done);
                            }

                            @Override
                            public void onError(@Nullable Exception exception) {
                                toast(R.string.analytics_clear_failed);
                            }
                        }));
    }

    private void toast(@StringRes int messageResId) {
        if (isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showAxisLabels(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        this.yAxisLabel.setVisibility(visibility);
        this.xAxisLabel.setVisibility(visibility);
    }

    @Nullable
    private String activeAquariumId() {
        Aquarium active = this.aquariumRepository.getActiveAquarium();
        return active == null ? null : active.getId();
    }

    @Nullable
    private ThresholdBand thresholdFor(@Nullable Aquarium aquarium,
                                       @NonNull AquariumSensor sensor) {
        return aquarium == null ? null : aquarium.thresholdFor(sensor.getId());
    }

    @StringRes
    private int yAxisLabelResId(@NonNull AquariumSensor sensor) {
        switch (sensor.getId()) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return ReadingFormatter.isCelsius(this)
                        ? R.string.unit_celsius
                        : R.string.unit_fahrenheit;
            case DatabaseSchema.PH_LEVEL_KEY:
                return R.string.analytics_axis_ph;
            case DatabaseSchema.WATER_LEVEL_KEY:
                return R.string.analytics_axis_water_level;
            default:
                return sensor.getUnitResId();
        }
    }

    @Nullable
    private AxisRange yAxisRangeFor(@NonNull AquariumSensor sensor) {
        return DatabaseSchema.WATER_LEVEL_KEY.equals(sensor.getId()) ? PERCENT_AXIS : null;
    }

    private void applyTemperatureUnitPreference() {
        for (AquariumSensor sensor : this.sensors) {
            if (DatabaseSchema.TEMPERATURE_KEY.equals(sensor.getId())) {
                sensor.setUnitResId(ReadingFormatter.isCelsius(this)
                        ? R.string.unit_celsius
                        : R.string.unit_fahrenheit);
            }
        }
    }

    @NonNull
    private String temperatureUnitKey() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs == null) {
            return new AppSettings().tempUnit;
        }
        return prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit);
    }

    @NonNull
    private List<SensorReading> inDisplayUnits(@NonNull AquariumSensor sensor,
                                               @NonNull List<SensorReading> buckets) {
        boolean temperature = DatabaseSchema.TEMPERATURE_KEY.equals(sensor.getId());
        boolean waterLevel = DatabaseSchema.WATER_LEVEL_KEY.equals(sensor.getId());
        if (!temperature && !waterLevel) {
            return buckets;
        }

        List<SensorReading> converted = new ArrayList<>(buckets.size());
        for (SensorReading bucket : buckets) {
            if (bucket.isOffline()) {
                converted.add(bucket);
                continue;
            }
            double value = temperature
                    ? ReadingFormatter.toDisplayTemperature(this, bucket.getValue())
                    : bucket.getValue() * 100d;
            converted.add(new SensorReading(value, bucket.getTimestampSeconds()));
        }
        return converted;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
