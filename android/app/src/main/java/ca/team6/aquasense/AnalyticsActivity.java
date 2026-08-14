package ca.team6.aquasense;

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
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.CalibrationOffsetStore;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.FirebaseDatabaseHelper;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.TelemetryRepository;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.DissolvedSolidsSensor;
import ca.team6.aquasense.model.aquarium_sensors.PhLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;
import ca.team6.aquasense.model.aquarium_sensors.TemperatureSensor;
import ca.team6.aquasense.model.aquarium_sensors.WaterLevelSensor;
import ca.team6.aquasense.notifications.SensorThresholds;

/**
 * DASH-03 - Analytics.
 *
 * <p>Reads one sensor's period for one aquarium and reports on it: the sensor's live health, what
 * the period's hundred buckets came to, the line they draw, and how they fell across the
 * aquarium's threshold band. The aquarium, the sensor and the window are all chosen at the top of
 * the page, and between them they are the whole of what the screen is showing.
 *
 * <p>Two live subscriptions run while this page is open. The selected aquarium's
 * {@code last_instant} nodes, which the health line is read from and which the dashboard runs
 * anyway; and the one period node behind the graph, so the line follows the board rather than
 * showing the moment the page was opened. That second one is a hundred buckets the board keeps
 * closing another of, so it is held to exactly the selection on screen: changing the aquarium, the
 * sensor or the window swaps it, and leaving the page releases it.
 */
public class AnalyticsActivity extends AppCompatActivity {

    // A sensor goes quiet without saying so, so nothing arrives to redraw the health line when it
    // stops being true. The dashboard re-reads its own on the same interval.
    private static final long STALENESS_CHECK_INTERVAL_MS = 10_000;
    private static final String STATE_SENSOR_ID = "analytics_sensor_id";
    private static final String STATE_PERIOD_NAME = "analytics_period_name";

    /** How a disabled chart action is drawn, since an ImageButton does not dim its own icon. */
    private static final float DISABLED_ACTION_ALPHA = 0.4f;

    // The saved copy of a period. Every field is fixed width and runs largest unit to smallest, so
    // the moment sorts as text in the order it happened; the separators are hyphens throughout
    // because the two characters that would ordinarily divide a date from a clock, a space and a
    // colon, are the ones a file name should not carry. See downloadFileName.
    private static final String DOWNLOAD_MIME_TYPE = "application/json";
    private static final String DOWNLOAD_TIMESTAMP_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    private static final String UNSAFE_FILE_NAME_CHARS = "[^A-Za-z0-9._-]";

    /** The whole of what a percentage can be, which is what a share has to be read against. */
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
    // Read here only to key the cache on: the buckets themselves arrive already corrected.
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

    // Held down for as long as a save is in flight, so a second tap cannot start a second one and
    // leave two copies of the same window in Downloads a moment apart.
    private boolean downloading;

    // Whether the node the two actions act on holds anything. Read from the subscription's own
    // report rather than from the buckets drawn on screen: those have been cut to the window, and a
    // node whose readings all fall outside it is still a node with something to save and something
    // to clear. False while a selection is being fetched, there being nothing known to act on yet.
    private boolean selectionHasBuckets;

    // Identifies what the page is showing or fetching, as aquarium + sensor + period + the unit
    // readings are displayed in. Two jobs: the aquariums observer fires on changes that leave the
    // selection alone - a rename, another tank being added - and there is no reason to re-read a
    // hundred buckets for those; and a tab or period switched twice in quick succession must not be
    // repainted by the first read landing last.
    //
    // The unit is in there because it is not only a label. Display & Units may be changed while
    // this screen is in the background, and the buckets are converted on the way to the chart, so
    // coming back to a page that skipped the re-read would leave Celsius plotted under a °F axis.
    @Nullable
    private String requestKey;

    // The selection the buckets below were last drawn for, which is not always the one being asked
    // for. Leaving the page drops the request key so that coming back re-subscribes, and without
    // this that re-subscription would look exactly like a tab being switched: the graph blanked to
    // its loading state and repainted a moment later with the same line it already had. Whereas a
    // selection that genuinely moved has to be blanked, since the buckets in hand are another
    // sensor's. The two are told apart by whether the key being asked for is the one on screen.
    //
    // Only set once a fetch has actually drawn something, so a selection that failed to read is
    // fetched again rather than repainted from whatever preceded it.
    @Nullable
    private String lastDrawnKey;

    // The buckets the page is reporting on, in the unit the database stores them in but with the
    // sensor's calibration correction already applied - the same numbers the dashboard's card is
    // showing, which is what lets the two screens be read against each other. Held so that a change
    // to the aquarium's thresholds can regrade them, which moves the distribution without moving a
    // single reading, and so a fetch does not have to be repeated to do it.
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
        // The buckets are unchanged, but the band they are graded against may not be.
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
        // Mutated so the tile's tint is this view's own: the drawable is shared with the template
        // cards, which tint their copy per template.
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
        // Both fire immediately with whatever their caches hold, so the usual case - arriving from
        // the dashboard, which loaded these long ago - draws the page on these calls. Opening it
        // cold instead gets an empty list now and the real one when the snapshot lands.
        this.telemetryRepository.addObserver(this.telemetryObserver);
        this.aquariumRepository.addObserver(this.aquariumsObserver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SENSOR_ID, this.selectedSensor.getId());
        outState.putString(STATE_PERIOD_NAME, this.selectedPeriod.name());
    }

    /**
     * Puts the selection back after a rotation, a theme change, or anything else that rebuilds the
     * page. Called before the tabs and the period buttons are built, since both draw their raised
     * state from it.
     */
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
        // Display & Units may have been changed since this screen was last in front, and so may
        // the active aquarium. The unit is part of the request key, so a change to it is what
        // makes the read below happen again.
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
        // Leaving the page lets the period go. It is a hundred buckets that the board keeps
        // closing another of, and syncing that to draw a graph nobody is looking at is the cost
        // this screen exists to only pay while it is open.
        //
        // Forgetting the request is what brings it back: onResume plots the selection again, and
        // without this it would find the key unchanged and decide it was already watching.
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
            // Deliberately not unwatching: as on the dashboard, the last_instant subscription is
            // meant to run for the whole signed-in session.
            this.telemetryRepository.removeObserver(this.telemetryObserver);
        }
    }

    /**
     * Wires the aquarium card to the list of aquariums it names one of.
     *
     * <p>The choice is the app's one active aquarium rather than a selection local to this screen.
     * Picking a tank here is picking the tank the dashboard shows, which is the only reading of it
     * that does not leave two screens disagreeing about which aquarium the user is looking at.
     *
     * <p>The list is read when the card is tapped rather than held from here, so it is whatever the
     * subscription has at that moment: a tank added or renamed on another device is in the dropdown
     * without this screen having to be told about it.
     */
    private void setUpAquariumSelector() {
        View card = findViewById(R.id.analyticsAquariumSelector);
        AquariumDropdown dropdown = new AquariumDropdown(
                card, findViewById(R.id.analyticsAquariumChevron), this::selectAquarium);

        card.setOnClickListener(v -> dropdown.show(
                this.aquariumRepository.getAquariums(), this.activeAquariumId()));
    }

    /**
     * Moves the page, and the app, onto another aquarium.
     *
     * <p>Setting the active ID is a preference write and publishes nothing, so the redraw is asked
     * for here rather than waited on: the name, then the subscription the health line reads, then
     * the period, whose request key has just changed and so is fetched again.
     */
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

    /**
     * Builds the period buttons from {@link AnalyticsPeriod#ALL}, so the row offers exactly the
     * windows the board records and no button can point at a period that is not fetchable.
     */
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

    /**
     * Wires the ⓘ on each of the two summary cards.
     *
     * <p>One per card rather than one for the page, because the two cards count different things
     * and that is the part worth explaining: the ring is a share of time with outages counted in,
     * the distribution is a share of readings with outages left out. A single sheet would be
     * explaining one card's denominator from the other card's corner.
     *
     * <p>Neither sheet is about a sensor, so neither follows the tabs: what uptime measures is the
     * same answer whichever one is selected. They are built on the press rather than held, since a
     * sheet that is never opened should cost nothing.
     */
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

    /**
     * One block of an info sheet, from a heading and its bullets.
     *
     * <p>The section takes resolved text rather than resource IDs, since the sensor sheets quote
     * live thresholds into theirs. These have nothing to quote, so they are only being read out of
     * the resources a step early.
     */
    @NonNull
    private InfoSheetSection infoSection(@StringRes int titleResId, @StringRes int... itemResIds) {
        String[] items = new String[itemResIds.length];
        for (int i = 0; i < itemResIds.length; i++) {
            items[i] = getString(itemResIds[i]);
        }
        return new InfoSheetSection(getString(titleResId), items);
    }

    /** Moves the raised state onto the chosen button, since the drawable keys off it. */
    private void markSelectedPeriod(@NonNull LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setSelected(AnalyticsPeriod.ALL.get(i) == this.selectedPeriod);
        }
    }

    /**
     * Wires the two actions on the graph card. Both read the selection at the moment they are
     * pressed rather than being rebound as it moves, so what they act on is always the window on
     * screen.
     */
    private void setUpChartActions() {
        this.downloadButton = findViewById(R.id.analyticsDownloadButton);
        this.clearButton = findViewById(R.id.analyticsClearButton);

        this.downloadButton.setOnClickListener(v -> this.downloadSelection());
        this.clearButton.setOnClickListener(v -> this.confirmClearSelection());
        // Off until an aquarium is named, which the first showActiveAquarium does. Set here rather
        // than by calling showChartActionsEnabled, which reads the repositories: this runs while
        // they are still being built, and there is nothing they could say yet anyway.
        this.setActionEnabled(this.downloadButton, false);
        this.setActionEnabled(this.clearButton, false);
    }

    /**
     * Turns the two actions on only while there is an aquarium for them to act on and a node with
     * something in it, and the save off again while one is running.
     *
     * <p>An aquarium may be absent for a moment on a cold open and permanently for a user with no
     * tanks, and neither is worth a press that has to answer "nothing selected". An empty node is
     * the same story told by the other end: there is nothing to copy into Downloads and nothing to
     * clear, so both actions would report having done nothing. Greying them out says that before
     * the press rather than after it, and is also how the page reports a window it has just cleared
     * - the subscription delivers the removal as an empty snapshot, which lands here.
     *
     * <p>The alpha is applied by hand because ImageButton, unlike a text button, draws its icon at
     * full strength whether or not it is enabled.
     */
    private void showChartActionsEnabled() {
        boolean actionable = this.activeAquariumId() != null && this.selectionHasBuckets;
        this.setActionEnabled(this.downloadButton, actionable && !this.downloading);
        this.setActionEnabled(this.clearButton, actionable);
    }

    private void setActionEnabled(@NonNull ImageButton action, boolean enabled) {
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : DISABLED_ACTION_ALPHA);
    }

    /** Names the aquarium every other row on the page is about, and draws its icon. */
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

    /**
     * The same icon My Aquariums gives this aquarium, so a tank is the same picture on both
     * screens. Freshwater is the resting image, which is also what the card shows while there is
     * no aquarium to ask - the tile is a shape in the card's row rather than a claim about a tank,
     * and blanking it would leave a hole beside a name that is still there.
     */
    private void showAquariumIcon(@Nullable Aquarium aquarium) {
        boolean saltwater = aquarium != null
                && WaterType.fromKey(aquarium.getWaterType()) == WaterType.SALTWATER;
        this.aquariumIcon.setImageResource(
                saltwater ? R.drawable.aquarium_saltwater : R.drawable.aquarium_freshwater);
    }

    /** Points the live subscription at the aquarium on show. A no-op when it is already there. */
    private void watchActiveAquarium() {
        String aquariumId = this.activeAquariumId();
        if (aquariumId == null) {
            this.telemetryRepository.unwatch();
            return;
        }
        this.telemetryRepository.watchAquarium(aquariumId);
    }

    /**
     * Redraws the health line from the live readings.
     *
     * <p>Every sensor is graded, not only the one on show, so switching tabs has the answer ready
     * rather than showing the previous sensor's lamp until the next reading lands.
     */
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
        // The graph is drawn from history and the lamp from the live reading, but they are about
        // the same sensor, so the line follows the verdict the card above it has just reached.
        this.chartController.setDisconnected(
                this.selectedSensor.getSensorStatus() == SensorStatus.DISCONNECTED);
    }

    /** Re-reports the loaded period, which is what a change to the aquarium's band moves. */
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

    /**
     * Points the period subscription at the current aquarium, sensor and window, and leaves it
     * there until one of them changes.
     *
     * <p>A no-op when it is already watching that selection, which is what the request key is for.
     * The aquariums observer fires on changes that leave the selection alone - a rename, another
     * tank being added - and tearing the subscription down and building it back up for those would
     * flash the graph through its loading state for nothing.
     */
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
        // The calibration offset is in the key for the same reason the unit is: the repository
        // corrects the buckets on the way here, and Sensor Calibration may have been run while this
        // screen sat in the background, so a page that skipped the re-read would keep plotting the
        // old correction under a card the dashboard has already moved on from.
        String key = aquariumId + "/" + sensor.getId() + "/" + period.getDatabaseKey()
                + "/" + this.temperatureUnitKey()
                + "/" + this.calibrationOffsets.get(aquariumId, sensor.getId());
        if (key.equals(this.requestKey)) {
            return;
        }
        this.requestKey = key;
        this.setChartRetryEnabled(false);

        // Named now rather than on arrival, so they are ready the moment there are axes to name.
        this.yAxisLabel.setText(this.yAxisLabelResId(sensor));
        this.xAxisLabel.setText(period.getXAxisLabelResId());

        if (key.equals(this.lastDrawnKey) && this.loadedBuckets != null) {
            // Coming back to the selection already on screen. The subscription below still has to
            // be made - leaving the page released it - but what is held is what it is about to ask
            // for, so it stays up and the snapshot overwrites it when it lands, rather than the
            // page emptying itself to fetch a copy of what it is already showing. Nothing is reset
            // either: the actions are acting on the same node they were before, and its verdict
            // has not been contradicted.
            this.drawLoadedBuckets(sensor, period);
        } else {
            this.loadedBuckets = null;
            // Nothing is known about the node being asked for until it answers, and the actions
            // stay off until then rather than carrying the last selection's verdict onto this one.
            this.selectionHasBuckets = false;
            this.showChartActionsEnabled();
            // Hidden until it lands: the loading state clears the chart outright, and a pair of
            // unit labels around an empty card is furniture for axes that are not being drawn.
            this.showAxisLabels(false);
            this.chartController.showMessage(R.string.analytics_chart_loading);
            this.summaryController.clearPeriod();
        }

        this.telemetryRepository.watchPeriod(aquariumId, sensor.getId(), period.getDatabaseKey(),
                new TelemetryRepository.PeriodCallback() {
                    @Override
                    public void onBuckets(@NonNull List<SensorReading> buckets) {
                        // The subscription is released on the way out and whenever the selection
                        // moves, but a delivery already on the main thread's queue when that
                        // happens still arrives, and it belongs to the selection that asked for it.
                        if (isDestroyed() || !key.equals(requestKey)) {
                            return;
                        }
                        // Judged on the node's own buckets, before the window is applied: the two
                        // actions take the whole node, so what falls outside the window is still
                        // theirs to save and to clear.
                        selectionHasBuckets = !buckets.isEmpty();
                        showChartActionsEnabled();
                        // Cut to the window once, here, so the graph and the card below it are
                        // reporting on exactly the same readings. They arrive already carrying the
                        // sensor's calibration correction, applied by the repository alongside the
                        // one it puts on the live reading the dashboard shows.
                        loadedBuckets = period.within(buckets);
                        drawLoadedBuckets(sensor, period);
                        // On screen now, so coming back to this selection can repaint it rather
                        // than blanking the page to ask for it again.
                        lastDrawnKey = key;
                    }

                    @Override
                    public void onError() {
                        if (isDestroyed() || !key.equals(requestKey)) {
                            return;
                        }
                        requestKey = null;
                        // Whatever was up for this selection is coming down below, so there is
                        // nothing left to repaint it from should it be asked for again.
                        lastDrawnKey = null;
                        loadedBuckets = null;
                        // A node that could not be read is not one to act on either.
                        selectionHasBuckets = false;
                        showChartActionsEnabled();
                        showAxisLabels(false);
                        chartController.showMessage(R.string.analytics_chart_error);
                        summaryController.clearPeriod();
                        setChartRetryEnabled(true);
                    }
                });
    }

    /**
     * Draws the buckets in hand as the graph and the card under it, for the selection they were
     * fetched for.
     *
     * <p>Called both when a snapshot lands and when the page comes back to the selection it was
     * already showing, so the two paths cannot drift into drawing the same buckets differently.
     *
     * <p>A window the board has put nothing in draws no axes, only the line of text saying so, and
     * the unit labels belong to the axes rather than to the card - so they follow whatever the
     * chart decided it could draw.
     */
    private void drawLoadedBuckets(@NonNull AquariumSensor sensor,
                                   @NonNull AnalyticsPeriod period) {
        if (this.loadedBuckets == null) {
            return;
        }
        this.showAxisLabels(this.chartController.setBuckets(sensor.getNameResId(), period,
                this.inDisplayUnits(sensor, this.loadedBuckets), this.yAxisRangeFor(sensor)));
        this.showPeriodSummary();
    }

    /**
     * Takes the page out of reading anything and says why, for the cases where there is no
     * selection to read: no aquarium yet, or none left.
     */
    private void stopReading(@StringRes int messageResId) {
        this.telemetryRepository.unwatchPeriod();
        this.requestKey = null;
        this.lastDrawnKey = null;
        this.loadedBuckets = null;
        // There is no node behind the page any more, so neither action has anything to act on.
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

    /**
     * Saves the selected window's node into Downloads as JSON.
     *
     * <p>Read fresh rather than written from the buckets already on screen. Those have been cut to
     * the window, converted into the unit the app displays and reduced to the readings a line is
     * drawn from; a copy of the database should be the node, cursor and unreached slots included,
     * so that it can be checked against the tree it came out of.
     *
     * <p>An absent node is not an error. It means the board has committed nothing to this window,
     * or it has already been cleared, and a file with nothing in it would look like a success worth
     * having. The user is told instead, and no file is written.
     */
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
                        // The selection may have moved while the read was out, so the name is built
                        // from what was asked for rather than from what is on screen now.
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

    /** Releases the save button and reports how it went. */
    private void finishDownload(@StringRes int messageResId) {
        this.downloading = false;
        if (isDestroyed()) {
            return;
        }
        this.showChartActionsEnabled();
        this.toast(messageResId);
    }

    /**
     * Names the saved file after exactly what is in it: which tank, which sensor, which window, and
     * when the copy was taken.
     *
     * <p>The contents are the node as the database stores it and carry none of that - a period node
     * is a cursor and a hundred buckets, and knows nothing of the three selections that reach it -
     * so the name is the only place the copy is identified. The moment is in there because a window
     * is a moving one: two saves of the same hour a day apart hold different readings, and without
     * it they would be told apart only by MediaStore having numbered the second.
     */
    @NonNull
    private String downloadFileName(@NonNull String aquariumId,
                                    @NonNull AquariumSensor sensor,
                                    @NonNull AnalyticsPeriod period) {
        String takenAt = DateTimeFormatter.ofPattern(DOWNLOAD_TIMESTAMP_PATTERN, Locale.ROOT)
                .format(LocalDateTime.now());
        // The aquarium ID is a paired board's UID rather than anything the user typed, but it is
        // going into a file name, so it is held to characters a file name can carry regardless.
        String safeAquariumId = aquariumId.replaceAll(UNSAFE_FILE_NAME_CHARS, "_");
        return String.format(Locale.ROOT, "aquasense_%s_%s_%s_%s.json",
                safeAquariumId, sensor.getId(), period.getDatabaseKey(), takenAt);
    }

    /**
     * Asks before clearing the selected window, and clears it if the answer is yes.
     *
     * <p>The page is not redrawn afterwards and does not need to be. The period subscription is
     * still on the node that was just emptied, so the removal arrives as a snapshot with nothing in
     * it and the graph and the cards below empty themselves the same way they follow every other
     * change to it.
     */
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

    /** Says something short, unless the screen it would be said on has gone. */
    private void toast(@StringRes int messageResId) {
        if (isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows or hides the units named around the plot. They belong to the axes rather than to the
     * card, so they come and go with them.
     *
     * <p>INVISIBLE rather than GONE: the y label is turned on its side inside a strip as tall as
     * the plot, and taking either out of the layout would move the chart while it is loading and
     * move it back when it lands.
     */
    private void showAxisLabels(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        this.yAxisLabel.setVisibility(visibility);
        this.xAxisLabel.setVisibility(visibility);
    }

    /** The aquarium every read on this page is made against, or null before one can be named. */
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

    /**
     * Names the unit the y axis is in.
     *
     * <p>Two sensors have no unit of their own to offer. pH is a scale rather than a quantity, and
     * water level is a detector whose buckets are averages, so what is plotted is the share of
     * each bucket's readings that had water at the sensor - scaled to a percentage by
     * {@link #inDisplayUnits}, which is what the axis is named for.
     */
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
                // Dissolved solids, and anything added later that does carry a unit.
                return sensor.getUnitResId();
        }
    }

    /**
     * The scale this sensor's graph is read against, or null to let its readings scale the axis.
     *
     * <p>Only water level has one. What is plotted for it is the share of each bucket that had
     * water at the sensor, and a share is read against the whole it is a share of: scaled to the
     * readings instead, a tank that dipped to 97% for one bucket draws the same cliff as a tank
     * that emptied. Every other sensor is a quantity rather than a share and is better off scaled
     * to what it actually did - see {@link AxisRange}.
     */
    @Nullable
    private AxisRange yAxisRangeFor(@NonNull AquariumSensor sensor) {
        return DatabaseSchema.WATER_LEVEL_KEY.equals(sensor.getId()) ? PERCENT_AXIS : null;
    }

    /**
     * Puts the temperature sensor's unit in step with Display &amp; Units, so the statistics row
     * labels a converted reading with the unit it was converted into. The dashboard's cards do the
     * same on their own copy of the sensor; the conversion itself is ReadingFormatter's, and runs
     * per reading rather than per resume.
     */
    private void applyTemperatureUnitPreference() {
        for (AquariumSensor sensor : this.sensors) {
            if (DatabaseSchema.TEMPERATURE_KEY.equals(sensor.getId())) {
                sensor.setUnitResId(ReadingFormatter.isCelsius(this)
                        ? R.string.unit_celsius
                        : R.string.unit_fahrenheit);
            }
        }
    }

    /** The Display &amp; Units temperature setting, as one character of the request key. */
    @NonNull
    private String temperatureUnitKey() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs == null) {
            return new AppSettings().tempUnit;
        }
        return prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit);
    }

    /**
     * Converts readings into the unit the graph names them in.
     *
     * <p>Only the chart is handed these. The statistics are read off the raw buckets and converted
     * a value at a time, because they are also graded against a threshold band, and a band is
     * written in the unit the database stores rather than the one the app displays.
     *
     * <p>Gap markers pass through untouched. The sentinel is a flag rather than a measurement, and
     * putting it through a conversion would stop it reading as one.
     */
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
