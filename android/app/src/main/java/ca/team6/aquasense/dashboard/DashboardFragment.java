package ca.team6.aquasense.dashboard;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.SettingsActivity;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.AquariumBoardStatus;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.GridSpacingItemDecoration;
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

    // The repository pushes the list on login and on every database change, so the header follows
    // an aquarium being renamed, added or deleted without this screen polling for it.
    private final AquariumRepository.AquariumsObserver aquariumsObserver =
            aquariums -> showActiveAquarium();

    // TODO: NOT SURE ABOUT THIS; NEED TO DECIDE BY END OF SPRINT 2.
    private static final AquariumSensor TEMPERATURE = new TemperatureSensor();
    private static final AquariumSensor WATER_LEVEL = new WaterLevelSensor();
    private static final AquariumSensor DISSOLVED_SOLIDS = new DissolvedSolidsSensor();
    private static final AquariumSensor PH_LEVEL = new PhLevelSensor();

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

        // Open dedicated "My Aquariums" fragment on click
        this.dashboardHeaderController.setOnClickAquariumSelector(v -> Navigation.findNavController(v)
                .navigate(R.id.action_dashboardFragment_to_aquariumSelectorFragment));

        aquariumRepository = AquariumRepository.getInstance(requireContext());
        // Fills the header now and again on every change.
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
        navbarSettings.setOnClickListener(v -> {
            startActivity(new Intent(v.getContext(), SettingsActivity.class));
        });

        // FIXME: SHOULD BE REPLACED WITH VALUE FROM SENSORS.
        this.dashboardHeaderController.setDashboardSensorsStatus(0);

        recycler = view.findViewById(R.id.sensorGrid);
        recycler.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        recycler.addItemDecoration(new GridSpacingItemDecoration(
                this.getResources().getDisplayMetrics()));

        applyTemperatureUnitPreference();
        sensorAdapter = new DashboardSensorAdapter(getParentFragmentManager(), visibleSensors());
        recycler.setAdapter(sensorAdapter);
    }

    private void showActiveAquarium() {
        if (dashboardHeaderController == null) {
            return;
        }

        // Until the first snapshot lands the list is empty because nothing has been fetched yet,
        // not because the user has no aquariums. Claiming "no aquariums" here would be wrong for
        // most users and would visibly correct itself a moment later.
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
        // TODO: /{uid}/aquariums carries no board status. Derive it from how recently the
        // aquarium's telemetry was written once this screen subscribes to /{uid}/telemetry.
        this.dashboardHeaderController.setDashboardBoardStatus(AquariumBoardStatus.OFFLINE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // The observer holds this fragment, and through it the destroyed view hierarchy.
        aquariumRepository.removeObserver(aquariumsObserver);
        dashboardHeaderController = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Re-apply when returning from Settings --> Display & Units.
        applyTemperatureUnitPreference();
        if (sensorAdapter != null) {
            sensorAdapter.setSensors(visibleSensors());
            sensorAdapter.notifySensorChanged(TEMPERATURE);
        }
    }

    // Builds the card list using the order and visibility saved in Display & Units.
    // Falls back to the declaration order below when no preference has been saved
    // yet.
    private List<AquariumSensor> visibleSensors() {
        List<AquariumSensor> all = new ArrayList<>();
        all.add(WATER_LEVEL);
        all.add(TEMPERATURE);
        all.add(DISSOLVED_SOLIDS);
        all.add(PH_LEVEL);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null) {
            return all;
        }

        List<String> knownIds = new ArrayList<>();
        for (AquariumSensor sensor : all) {
            knownIds.add(sensor.getId());
        }

        List<AquariumSensor> ordered = new ArrayList<>();
        for (String id : prefs.getSensorOrder(knownIds)) {
            if (prefs.isSensorHidden(id)) {
                continue;
            }
            for (AquariumSensor sensor : all) {
                if (sensor.getId().equals(id)) {
                    ordered.add(sensor);
                    break;
                }
            }
        }
        return ordered;
    }

    /** Updates the Temperature card unit label from Display & Units (°C / °F). */
    private void applyTemperatureUnitPreference() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null)
            return;
        String tempUnit = prefs.getString(SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit);
        TEMPERATURE.setUnitResId("C".equals(tempUnit) ? R.string.unit_celsius : R.string.unit_fahrenheit);
        // TODO: When live temperature values arrive, convert C↔F for display as well.
    }

}