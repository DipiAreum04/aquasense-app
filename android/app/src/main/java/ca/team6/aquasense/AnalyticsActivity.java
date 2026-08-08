package ca.team6.aquasense;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.analytics.AnalyticsChartController;
import ca.team6.aquasense.analytics.AnalyticsPeriod;
import ca.team6.aquasense.analytics.AnalyticsSummaryController;
import ca.team6.aquasense.analytics.AquariumDropdown;
import ca.team6.aquasense.analytics.PeriodStatistics;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.DatabaseSchema;
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
import ca.team6.aquasense.model.aquarium_sensors.TemperatureSensor;
import ca.team6.aquasense.model.aquarium_sensors.WaterLevelSensor;

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

    // One tab each, in the order the dashboard lays its cards out.
    private static final List<AquariumSensor> SENSORS = Collections.unmodifiableList(Arrays.asList(
            new WaterLevelSensor(),
            new TemperatureSensor(),
            new DissolvedSolidsSensor(),
            new PhLevelSensor()));

    // A sensor goes quiet without saying so, so nothing arrives to redraw the health line when it
    // stops being true. The dashboard re-reads its own on the same interval.
    private static final long STALENESS_CHECK_INTERVAL_MS = 10_000;

    private AnalyticsChartController chartController;
    private AnalyticsSummaryController summaryController;
    private AquariumRepository aquariumRepository;
    private TelemetryRepository telemetryRepository;
    private TextView aquariumNameText;
    private ImageView aquariumIcon;
    private TextView yAxisLabel;
    private TextView xAxisLabel;

    private AquariumSensor selectedSensor = SENSORS.get(0);
    private AnalyticsPeriod selectedPeriod = AnalyticsPeriod.LAST_1H;

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

    // The buckets the page is reporting on, raw as the database stores them. Held so that a change
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

        LineChart chart = findViewById(R.id.analyticsChart);
        this.chartController = new AnalyticsChartController(chart);
        this.summaryController =
                new AnalyticsSummaryController(findViewById(R.id.analyticsContent));
        // The period half of the summary is drawn from a read that may never be asked for - there
        // may be no aquarium to ask about - so it is put into its empty state up front rather than
        // left showing the blanks the layout inflates with.
        this.summaryController.showPeriodLoading();
        this.yAxisLabel = findViewById(R.id.analyticsYAxisLabel);
        this.xAxisLabel = findViewById(R.id.analyticsXAxisLabel);
        this.aquariumNameText = findViewById(R.id.analyticsAquariumName);
        this.aquariumIcon = findViewById(R.id.analyticsAquariumIcon);
        // Mutated so the tile's tint is this view's own: the drawable is shared with the template
        // cards, which tint their copy per template.
        this.aquariumIcon.getBackground().mutate().setTint(
                ContextCompat.getColor(this, R.color.aquarium_icon_bg));

        this.setUpAquariumSelector();
        this.setUpSensorTabs();
        this.setUpPeriodButtons();

        this.telemetryRepository = TelemetryRepository.getInstance();
        this.aquariumRepository = AquariumRepository.getInstance(this);
        // Both fire immediately with whatever their caches hold, so the usual case - arriving from
        // the dashboard, which loaded these long ago - draws the page on these calls. Opening it
        // cold instead gets an empty list now and the real one when the snapshot lands.
        this.telemetryRepository.addObserver(this.telemetryObserver);
        this.aquariumRepository.addObserver(this.aquariumsObserver);
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

    /**
     * Builds the tabs from {@link #SENSORS}, so their labels are the sensors' own names.
     *
     * <p>The listener goes on after the tabs are in place. TabLayout selects the first tab as it is
     * added, and a listener registered before that would be called back to plot while the
     * repositories it needs are still null.
     */
    private void setUpSensorTabs() {
        TabLayout tabs = findViewById(R.id.analyticsSensorTabs);
        for (AquariumSensor sensor : SENSORS) {
            tabs.addTab(tabs.newTab().setText(sensor.getNameResId()));
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(@NonNull TabLayout.Tab tab) {
                selectedSensor = SENSORS.get(tab.getPosition());
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

    /** Moves the raised state onto the chosen button, since the drawable keys off it. */
    private void markSelectedPeriod(@NonNull LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setSelected(AnalyticsPeriod.ALL.get(i) == this.selectedPeriod);
        }
    }

    /** Names the aquarium every other row on the page is about, and draws its icon. */
    private void showActiveAquarium() {
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

        for (AquariumSensor sensor : SENSORS) {
            sensor.applyReading(this, readings.get(sensor.getId()),
                    this.thresholdFor(active, sensor), nowMillis);
        }
        this.summaryController.showSensorHealth(this.selectedSensor,
                readings.get(this.selectedSensor.getId()), nowMillis);
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
            return;
        }

        AquariumSensor sensor = this.selectedSensor;
        AnalyticsPeriod period = this.selectedPeriod;
        String key = aquariumId + "/" + sensor.getId() + "/" + period.getDatabaseKey()
                + "/" + this.temperatureUnitKey();
        if (key.equals(this.requestKey)) {
            return;
        }
        this.requestKey = key;
        this.loadedBuckets = null;

        // Named now rather than on arrival, so they are ready the moment there are axes to name.
        // Hidden until then: the loading state clears the chart outright, and a pair of unit
        // labels around an empty card is furniture for axes that are not being drawn.
        this.yAxisLabel.setText(this.yAxisLabelResId(sensor));
        this.xAxisLabel.setText(period.getXAxisLabelResId());
        this.showAxisLabels(false);
        this.chartController.showLoading();
        this.summaryController.showPeriodLoading();

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
                        // Cut to the window once, here, so the graph and the card below it are
                        // reporting on exactly the same readings.
                        loadedBuckets = period.within(buckets);
                        showAxisLabels(true);
                        chartController.setBuckets(sensor.getNameResId(), period,
                                inDisplayUnits(sensor, loadedBuckets));
                        showPeriodSummary();
                    }

                    @Override
                    public void onError() {
                        if (isDestroyed() || !key.equals(requestKey)) {
                            return;
                        }
                        // Forgetting the request lets the next tab selection or snapshot subscribe
                        // again, rather than leaving the page pinned to one that failed.
                        requestKey = null;
                        showAxisLabels(true);
                        chartController.showEmpty(period);
                        summaryController.showPeriodLoading();
                    }
                });
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
     * water level is a float switch whose buckets are averages, so what is plotted is the share of
     * each bucket the switch spent submerged. Temperature is asked of the preferences instead of the
     * sensor, since the sensor's own unit is fixed at Celsius while the reading may be converted.
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
     * Puts the temperature sensor's unit in step with Display &amp; Units, so the statistics row
     * labels a converted reading with the unit it was converted into. The dashboard's cards do the
     * same on their own copy of the sensor; the conversion itself is ReadingFormatter's, and runs
     * per reading rather than per resume.
     */
    private void applyTemperatureUnitPreference() {
        for (AquariumSensor sensor : SENSORS) {
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
     * Converts readings into the unit the app is set to read them in, which for now means
     * temperature: the board stores Celsius, and Display &amp; Units may ask for Fahrenheit. The
     * dashboard cards already do this, and a graph disagreeing with the card above it would be
     * worse than either choice on its own.
     *
     * <p>Only the chart is handed these. The statistics are read off the raw buckets and converted
     * a value at a time, because they are also graded against a threshold band, and a band is
     * written in the unit the database stores rather than the one the app displays.
     *
     * <p>Gap markers pass through untouched. The sentinel is a flag rather than a measurement, and
     * putting it through a unit conversion would stop it reading as one.
     */
    @NonNull
    private List<SensorReading> inDisplayUnits(@NonNull AquariumSensor sensor,
                                               @NonNull List<SensorReading> buckets) {
        if (!DatabaseSchema.TEMPERATURE_KEY.equals(sensor.getId())) {
            return buckets;
        }

        List<SensorReading> converted = new ArrayList<>(buckets.size());
        for (SensorReading bucket : buckets) {
            converted.add(bucket.isOffline()
                    ? bucket
                    : new SensorReading(
                            ReadingFormatter.toDisplayTemperature(this, bucket.getValue()),
                            bucket.getTimestampSeconds()));
        }
        return converted;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
