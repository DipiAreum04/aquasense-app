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
import ca.team6.aquasense.model.AquariumSensor;
import ca.team6.aquasense.model.GridSpacingItemDecoration;

public class DashboardFragment extends Fragment {
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private DashboardHeaderController dashboardHeaderController;
    @SuppressWarnings("FieldCanBeLocal") // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    private RecyclerView recycler;

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
        navbarSettings.setOnClickListener(v -> {
            startActivity(new Intent(v.getContext(), SettingsActivity.class));
        });

        this.dashboardHeaderController = new DashboardHeaderController(view);

        // FIXME: MY AQUARIUM PLACEHOLDER SHOULD BE REPLACED WITH LOCALIZED STRING.
        this.dashboardHeaderController.setDashboardHeaderTitle("My Aquarium");

        this.dashboardHeaderController.setDashboardSensorsStatus(0);

        this.dashboardHeaderController.setDashboardBoardStatus(AquariumBoardStatus.OFFLINE);

        recycler = view.findViewById(R.id.sensorGrid);
        recycler.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        recycler.addItemDecoration(new GridSpacingItemDecoration(
                this.getResources().getDisplayMetrics()
        ));

        List<AquariumSensor> sensors = new ArrayList<>();
        sensors.add(AquariumSensor.LIQUID_LEVEL);
        sensors.add(AquariumSensor.TEMPERATURE);
        sensors.add(AquariumSensor.DISSOLVED_SOLIDS);
        sensors.add(AquariumSensor.PH_LEVEL);

        recycler.setAdapter(new DashboardSensorAdapter(getParentFragmentManager(), sensors));
    }
}
