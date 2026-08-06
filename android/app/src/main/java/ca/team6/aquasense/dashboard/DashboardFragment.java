package ca.team6.aquasense.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class DashboardFragment extends Fragment {
    private DashboardHeaderController dashboardHeaderController;
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private RecyclerView recycler;
    private DashboardSensorAdapter sensorAdapter;

    private AquariumRepository aquariumRepository;
    private TelemetryRepository telemetryRepository;

    private final AquariumRepository.AquariumsObserver aquariumsObserver =
            aquariums -> {
                showActiveAquarium();
                refreshTelemetrySubscription();
                showSensorCards();
            };

    private final TelemetryRepository.TelemetryObserver telemetryObserver =
            this::applyTelemetry;

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

        // FIXME: SHOULD BE REPLACED WITH VALUE FROM SENSORS.
        this.dashboardHeaderController.setDashboardSensorsStatus(0);

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

        applyTemperatureUnitPreference();
        sensorAdapter = new DashboardSensorAdapter(getParentFragmentManager(), visibleSensors());
        recycler.setAdapter(sensorAdapter);
    }

    private void showActiveAquarium() {
        if (dashboardHeaderController == null) {
            return;
        }

        if (!aquariumRepository.isLoaded()) {
            this.dashboardHeaderController.setDashboardHeaderTitle(getString(R.string.loading_aquariums));
            return;
        }

        Aquarium activeAquarium = aquariumRepository.getActiveAquarium();
        if (activeAquarium == null) {
            this.dashboardHeaderController.setDashboardHeaderTitle(getString(R.string.no_aquariums));
            return;
        }

        this.dashboardHeaderController.setDashboardHeaderTitle(activeAquarium.getName());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        aquariumRepository.removeObserver(aquariumsObserver);
        telemetryRepository.removeObserver(telemetryObserver);
        // Deliberately not calling telemetryRepository.unwatch() here: the RTDB subscription is
        // meant to keep running for the whole signed-in session (it only tears down on sign-out,
        // see TelemetryRepository#onAuthChanged) so a future background-alerts feature can act on
        // live telemetry while the dashboard isn't on screen.
        dashboardHeaderController = null;
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