package ca.team6.aquasense.dashboard;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.R;
import ca.team6.aquasense.SettingsActivity;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.AquariumStatus;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.GridSpacingItemDecoration;
import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.TelemetryRepository;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.aquarium_sensors.DissolvedSolidsSensor;
import ca.team6.aquasense.model.aquarium_sensors.WaterLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.PhLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.TemperatureSensor;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.WaterType;

public class DashboardFragment extends Fragment {
    private DashboardHeaderController dashboardHeaderController;
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private RecyclerView recycler;
    private DashboardSensorAdapter sensorAdapter;

    private AquariumRepository aquariumRepository;
    private TelemetryRepository telemetryRepository;

   
    private List<View> headerSlackViews;
    private int[] headerSlackMargins;
    private View dashboardFooter;
    private ViewTreeObserver.OnPreDrawListener headerFitListener;

    private final AquariumRepository.AquariumsObserver aquariumsObserver =
            aquariums -> {
                showActiveAquarium();
                refreshTelemetrySubscription();
                showSensorCards();
                maybeDismissLoadingOverlay();
            };

    private final TelemetryRepository.TelemetryObserver telemetryObserver =
            this::applyTelemetry;

    // Longest the loading overlay is allowed to cover the dashboard
    private static final long LOADING_OVERLAY_TIMEOUT_MS = 2_500L;
    private View loadingOverlay;
    private boolean overlayDismissed;
    private final Handler overlayHandler = new Handler(Looper.getMainLooper());
    private final Runnable overlayTimeout = this::dismissLoadingOverlay;

    private static final long STALENESS_CHECK_INTERVAL_MS = 10_000;
    private final Handler stalenessHandler = new Handler(Looper.getMainLooper());
    private final Runnable stalenessTick = new Runnable() {
        @Override
        public void run() {
            applyTelemetry(telemetryRepository.getReadings());
            stalenessHandler.postDelayed(this, STALENESS_CHECK_INTERVAL_MS);
        }
    };

    // TODO: NOT SURE ABOUT THIS; NEED TO DECIDE BY END OF SPRINT 2.
    private static final AquariumSensor TEMPERATURE = new TemperatureSensor();
    private static final AquariumSensor WATER_LEVEL = new WaterLevelSensor();
    private static final AquariumSensor DISSOLVED_SOLIDS = new DissolvedSolidsSensor();
    private static final AquariumSensor PH_LEVEL = new PhLevelSensor();
    private static final List<AquariumSensor> ALL_SENSORS =
            Arrays.asList(WATER_LEVEL, TEMPERATURE, DISSOLVED_SOLIDS, PH_LEVEL);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.dashboardHeaderController = new DashboardHeaderController(view);

        this.dashboardHeaderController.setOnClickAquariumSelector(v -> Navigation.findNavController(v)
                .navigate(R.id.action_dashboardFragment_to_aquariumSelectorFragment));

        telemetryRepository = TelemetryRepository.getInstance();
        aquariumRepository = AquariumRepository.getInstance(requireContext());

        setupLoadingOverlay(view);

        telemetryRepository.addObserver(telemetryObserver);
        aquariumRepository.addObserver(aquariumsObserver);

        LinearLayout navbarDashboard = view.findViewById(R.id.navbarDashboard);
        LinearLayout navbarNotifications = view.findViewById(R.id.navbarNotifications);
        LinearLayout navbarAnalytics = view.findViewById(R.id.navbarAnalytics);
        LinearLayout navbarSettings = view.findViewById(R.id.navbarSettings);

        // TODO: ONCLICK HANDLERS MUST BE DEFINED PROPERLY.
        navbarDashboard.setOnClickListener(v -> {
        });
        navbarNotifications.setOnClickListener(v -> {
        });
        navbarAnalytics.setOnClickListener(v -> {
        });
        navbarSettings.setOnClickListener(v ->
            startActivity(new Intent(v.getContext(), SettingsActivity.class))
        );

        showSensorsStatus();

        recycler = view.findViewById(R.id.sensorGrid);
        recycler.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        recycler.addItemDecoration(new GridSpacingItemDecoration(
                this.getResources().getDisplayMetrics()));

        // A payload-less notifyItemChanged() makes the default animator cross-fade a second
        // ViewHolder over the old one, which reads as the card flashing. Readings land about
        // once a second, so that fires constantly; add/remove/move animations are left on for
        // the sensor order / visibility preference.
        RecyclerView.ItemAnimator itemAnimator = recycler.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        View dashboardHeader = view.findViewById(R.id.dashboardHeader);
        dashboardFooter = view.findViewById(R.id.dashboardFooter);
        headerSlackViews = Arrays.asList(
                view.findViewById(R.id.aquariumSensorsStatusIconCircle),
                view.findViewById(R.id.aquariumSensorsStatusHeading),
                view.findViewById(R.id.dashboardHeaderWave));
        headerSlackMargins = new int[headerSlackViews.size()];
        for (int i = 0; i < headerSlackViews.size(); i++) {
            headerSlackMargins[i] = topMarginOf(headerSlackViews.get(i));
        }

        headerFitListener = () -> fitHeaderAboveSensorGrid(view, dashboardHeader);
        view.getViewTreeObserver().addOnPreDrawListener(headerFitListener);

        applyTemperatureUnitPreference();
        sensorAdapter = new DashboardSensorAdapter(getParentFragmentManager(), visibleSensors());
        recycler.setAdapter(sensorAdapter);
    }

    private boolean fitHeaderAboveSensorGrid(View root, View header) {
        if (headerSlackViews == null || root.getHeight() == 0 || header.getHeight() == 0) {
            return true;
        }

        // Halfway is still measured against the whole screen, footer included, so a screen with
        // the room to spare draws exactly as it did before the grid got a say.
        int target = root.getHeight() / 2;
        if (recycler.getHeight() > 0) {
            int leftByGrid = root.getHeight() - dashboardFooter.getHeight()
                    - recycler.getHeight() - topMarginOf(recycler);
            target = Math.min(target, leftByGrid);
        }

        int currentSlack = 0;
        for (View slackView : headerSlackViews) {
            currentSlack += topMarginOf(slackView);
        }
        int[] margins = distributeHeaderSlack(
                Math.max(0, target - (header.getHeight() - currentSlack)));

        boolean changed = false;
        for (int i = 0; i < headerSlackViews.size(); i++) {
            View slackView = headerSlackViews.get(i);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) slackView.getLayoutParams();
            if (params.topMargin == margins[i]) {
                continue;
            }
            params.topMargin = margins[i];
            slackView.setLayoutParams(params);
            changed = true;
        }
        return !changed;
    }

    private int[] distributeHeaderSlack(int slack) {
        int[] margins = headerSlackMargins.clone();
        int declared = 0;
        for (int margin : headerSlackMargins) {
            declared += margin;
        }

        if (slack >= declared) {
            margins[0] += Math.min(slack - declared, headerSlackMargins[0]);
            return margins;
        }

        int remaining = slack;
        for (int i = margins.length - 1; i > 0; i--) {
            margins[i] = Math.round((float) headerSlackMargins[i] * slack / declared);
            remaining -= margins[i];
        }
        margins[0] = Math.max(0, remaining);
        return margins;
    }

    private static int topMarginOf(View view) {
        return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin;
    }

    private void setupLoadingOverlay(@NonNull View view) {
        loadingOverlay = view.findViewById(R.id.dashboardLoadingOverlay);
        overlayDismissed = false;
        if (loadingOverlay == null) {
            return;
        }

        if (isDashboardReady()) {
            overlayDismissed = true;
            loadingOverlay.setVisibility(View.GONE);
            return;
        }

        loadingOverlay.setAlpha(1f);
        loadingOverlay.setVisibility(View.VISIBLE);
        overlayHandler.postDelayed(overlayTimeout, LOADING_OVERLAY_TIMEOUT_MS);
    }

    /**
     * True once the dashboard has enough to show without the loading overlay: the aquarium list has
     * loaded and either the user has no aquarium or the first full set of readings has landed.
     */
    private boolean isDashboardReady() {
        if (!aquariumRepository.isLoaded()) {
            return false;
        }
        if (aquariumRepository.getActiveAquarium() == null) {
            return true;
        }
        return telemetryRepository.hasReadAllSensors();
    }

    private void maybeDismissLoadingOverlay() {
        if (overlayDismissed || loadingOverlay == null) {
            return;
        }
        if (isDashboardReady()) {
            dismissLoadingOverlay();
        }
    }

    private void dismissLoadingOverlay() {
        if (overlayDismissed) {
            return;
        }
        overlayDismissed = true;
        overlayHandler.removeCallbacks(overlayTimeout);
        if (loadingOverlay == null) {
            return;
        }
        loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction(() -> {
                    if (loadingOverlay != null) {
                        loadingOverlay.setVisibility(View.GONE);
                    }
                });
    }

    private void showActiveAquarium() {
        if (dashboardHeaderController == null) {
            return;
        }

        if (!aquariumRepository.isLoaded()) {
            this.dashboardHeaderController.setDashboardHeaderTitle(getString(R.string.loading_aquariums));
            this.dashboardHeaderController.setDashboardWaterType(null);
            return;
        }

        Aquarium activeAquarium = aquariumRepository.getActiveAquarium();
        if (activeAquarium == null) {
            this.dashboardHeaderController.setDashboardHeaderTitle(getString(R.string.no_aquariums));
            this.dashboardHeaderController.setDashboardWaterType(null);
            return;
        }

        this.dashboardHeaderController.setDashboardHeaderTitle(activeAquarium.getName());
        this.dashboardHeaderController.setDashboardWaterType(
                WaterType.fromKey(activeAquarium.getWaterType()));
    }

    private void showSensorsStatus() {
        if (dashboardHeaderController == null) {
            return;
        }

        // The sensors arrive on four independent listeners, so deciding on every publish walks the
        // header through DISCONNECTED > CRITICAL > NORMAL as they land. Hold it at DISCONNECTED
        // until all four have been read, then decide once from a complete set.
        AquariumStatus aquariumStatus = telemetryRepository.hasReadAllSensors()
                ? AquariumStatus.forSensors(visibleSensors())
                : AquariumStatus.DISCONNECTED;
        this.dashboardHeaderController.setDashboardSensorsStatus(aquariumStatus);
    }

    private void showLastUpdated(Map<String, SensorReading> readingsBySensorId, long nowMillis) {
        if (dashboardHeaderController == null) {
            return;
        }

        // The sensors publish independently, so the header reports the freshest of them: that is
        // the last moment the aquarium was known to be saying anything at all.
        long newestSeconds = Long.MIN_VALUE;
        for (SensorReading reading : readingsBySensorId.values()) {
            if (!reading.isOffline() && reading.getTimestampSeconds() > newestSeconds) {
                newestSeconds = reading.getTimestampSeconds();
            }
        }
        if (newestSeconds == Long.MIN_VALUE) {
            this.dashboardHeaderController.setDashboardLastUpdated(
                    getString(R.string.dashboard_last_updated_pending));
            return;
        }

        long ageSeconds = Math.max(0L, nowMillis / 1000L - newestSeconds);
        Resources resources = getResources();
        CharSequence lastUpdated;
        if (ageSeconds < 60L) {
            lastUpdated = getString(R.string.dashboard_last_updated_now);
        } else if (ageSeconds < 3600L) {
            int minutes = (int) (ageSeconds / 60L);
            lastUpdated = resources.getQuantityString(
                    R.plurals.dashboard_last_updated_minutes, minutes, minutes);
        } else if (ageSeconds < 86400L) {
            int hours = (int) (ageSeconds / 3600L);
            lastUpdated = resources.getQuantityString(
                    R.plurals.dashboard_last_updated_hours, hours, hours);
        } else {
            int days = (int) (ageSeconds / 86400L);
            lastUpdated = resources.getQuantityString(
                    R.plurals.dashboard_last_updated_days, days, days);
        }
        this.dashboardHeaderController.setDashboardLastUpdated(lastUpdated);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        aquariumRepository.removeObserver(aquariumsObserver);
        telemetryRepository.removeObserver(telemetryObserver);

        overlayHandler.removeCallbacks(overlayTimeout);
        if (loadingOverlay != null) {
            loadingOverlay.animate().cancel();
        }
        loadingOverlay = null;
        // Deliberately not calling telemetryRepository.unwatch() here: the RTDB subscription is
        // meant to keep running for the whole signed-in session (it only tears down on sign-out,
        // see TelemetryRepository#onAuthChanged) so a future background-alerts feature can act on
        // live telemetry while the dashboard isn't on screen.
        if (dashboardHeaderController != null) {
            dashboardHeaderController.cancelStatusTransition();
        }
        dashboardHeaderController = null;

        View view = getView();
        if (view != null && headerFitListener != null) {
            view.getViewTreeObserver().removeOnPreDrawListener(headerFitListener);
        }
        headerFitListener = null;
        headerSlackViews = null;
        dashboardFooter = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        stalenessHandler.removeCallbacks(stalenessTick);
    }

    private void refreshTelemetrySubscription() {
        Aquarium activeAquarium = aquariumRepository.getActiveAquarium();
        if (activeAquarium == null) {
            telemetryRepository.unwatch();
            return;
        }
        telemetryRepository.watchAquarium(activeAquarium.getId());
    }

    private void applyTelemetry(Map<String, SensorReading> readingsBySensorId) {
        Aquarium activeAquarium = aquariumRepository.getActiveAquarium();
        long nowMillis = telemetryRepository.nowMillis();
        for (AquariumSensor sensor : ALL_SENSORS) {
            SensorReading reading = readingsBySensorId.get(sensor.getId());
            ThresholdBand thresholdBand =
                    activeAquarium != null ? activeAquarium.thresholdFor(sensor.getId()) : null;
            boolean changed = sensor.applyReading(requireContext(), reading, thresholdBand, nowMillis);
            if (changed && sensorAdapter != null) {
                sensorAdapter.notifySensorChanged(sensor);
            }
        }
        showSensorsStatus();
        showLastUpdated(readingsBySensorId, nowMillis);
        maybeDismissLoadingOverlay();
    }

    @Override
    public void onResume() {
        super.onResume();

        showActiveAquarium();
        refreshTelemetrySubscription();

        applyTemperatureUnitPreference();
        showSensorCards();
        if (sensorAdapter != null) {
            sensorAdapter.notifySensorChanged(TEMPERATURE);
        }
        stalenessHandler.post(stalenessTick);
    }

    // Re-applies the card order and visibility
    private void showSensorCards() {
        if (sensorAdapter == null) {
            return;
        }
        sensorAdapter.setSensors(visibleSensors());
    }

    private List<AquariumSensor> visibleSensors() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null) {
            return ALL_SENSORS;
        }

        Aquarium activeAquarium = aquariumRepository.getActiveAquarium();

        List<String> knownIds = new ArrayList<>();
        for (AquariumSensor sensor : ALL_SENSORS) {
            knownIds.add(sensor.getId());
        }

        List<AquariumSensor> ordered = new ArrayList<>();
        for (String id : prefs.getSensorOrder(knownIds)) {
            if (activeAquarium != null && !activeAquarium.isSensorApplicable(id)) {
                continue;
            }
            if (prefs.isSensorHidden(id)) {
                continue;
            }
            for (AquariumSensor sensor : ALL_SENSORS) {
                if (sensor.getId().equals(id)) {
                    ordered.add(sensor);
                    break;
                }
            }
        }
        return ordered;
    }

    // Sets the unit shown on the temperature card. The reading itself is converted by
    // ReadingFormatter, which runs per reading rather than per resume, so this only has to keep
    // the label agreeing with it.
    private void applyTemperatureUnitPreference() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null)
            return;
        String tempUnit = prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit);
        TEMPERATURE.setUnitResId("C".equals(tempUnit) ? R.string.unit_celsius : R.string.unit_fahrenheit);
    }

}