package ca.team6.aquasense.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.SettingsActivity;
import ca.team6.aquasense.model.AquariumBoardStatus;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.GridSpacingItemDecoration;
import ca.team6.aquasense.model.aquarium_sensors.DissolvedSolidsSensor;
import ca.team6.aquasense.model.aquarium_sensors.WaterLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.PhLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.TemperatureSensor;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class DashboardFragment extends Fragment {
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private DashboardHeaderController dashboardHeaderController;
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private RecyclerView recycler;
    private DashboardSensorAdapter sensorAdapter;

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
        @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @SuppressLint("SetTextI18n") // TODO: TEMPORARY, MUST BE REMOVED WHEN AQUARIUM NAME IS FETCHED
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout navbarDashboard = view.findViewById(R.id.navbarDashboard);
        LinearLayout navbarNotifications = view.findViewById(R.id.navbarNotifications);
        LinearLayout navbarAnalytics = view.findViewById(R.id.navbarAnalytics);
        LinearLayout navbarSettings = view.findViewById(R.id.navbarSettings);

        // TODO: ONCLICK HANDLERS MUST BE DEFINED PROPERLY.
        navbarDashboard.setOnClickListener(v -> {});
        navbarNotifications.setOnClickListener(v -> {});
        navbarAnalytics.setOnClickListener(v -> {});
        navbarSettings.setOnClickListener(v ->
                startActivity(new Intent(v.getContext(), SettingsActivity.class))
        );

        this.dashboardHeaderController = new DashboardHeaderController(view);

        // FIXME: MY AQUARIUM PLACEHOLDER SHOULD BE REPLACED WITH LOCALIZED STRING.
        this.dashboardHeaderController.setDashboardHeaderTitle("My Aquarium");

        // FIXME: SHOULD BE REPLACED WITH VALUE FROM SENSORS.
        this.dashboardHeaderController.setDashboardSensorsStatus(0);

        // FIXME: SHOULD BE REPLACED WITH VALUE FROM BOARD.
        this.dashboardHeaderController.setDashboardBoardStatus(AquariumBoardStatus.OFFLINE);

        // TODO: IMPLEMENT AQUARIUM SELECTOR REDIRECTION HERE.
        this.dashboardHeaderController.setOnClickAquariumSelector(v -> {});

        recycler = view.findViewById(R.id.sensorGrid);
        recycler.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        recycler.addItemDecoration(new GridSpacingItemDecoration(
                this.getResources().getDisplayMetrics()
        ));

        List<AquariumSensor> sensors = new ArrayList<>();
        sensors.add(WATER_LEVEL);
        sensors.add(TEMPERATURE);
        sensors.add(DISSOLVED_SOLIDS);
        sensors.add(PH_LEVEL);

        applyTemperatureUnitPreference();
        sensorAdapter = new DashboardSensorAdapter(getParentFragmentManager(), sensors);
        recycler.setAdapter(sensorAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-apply when returning from Settings --> Display & Units.
        applyTemperatureUnitPreference();
        if (sensorAdapter != null) {
            sensorAdapter.notifySensorChanged(TEMPERATURE);
        }
    }

    /** Updates the Temperature card unit label from Display & Units (°C / °F). */
    private void applyTemperatureUnitPreference() {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null) return;
        String tempUnit = prefs.getString(SettingsRepository.KEY_TEMP_UNIT, "F");
        TEMPERATURE.setUnitResId("C".equals(tempUnit) ? R.string.unit_celsius : R.string.unit_fahrenheit);
        // TODO: When live temperature values arrive, convert C↔F for display as well.
    }
}
