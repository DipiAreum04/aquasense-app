package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;

public class SettingsHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile card → Accounts & Backup
        view.findViewById(R.id.rowProfile)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_accounts));

        // Sensor Calibration
        view.findViewById(R.id.rowSensorCalibration)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_sensorCalibration));

        // Display & Units
        view.findViewById(R.id.rowDisplayUnits)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_displayUnits));

        // Notifications
        view.findViewById(R.id.rowNotifications)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_notifications));

        // Accounts & Backup
        view.findViewById(R.id.rowAccountsBackup)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_accounts));

        // Data & Sync
        view.findViewById(R.id.rowDataSync)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_dataSync));

        // Contact & Support
        view.findViewById(R.id.rowContactSupport)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_contactSupport));

        // Rows owned by other teams - placeholder until they wire up their screens
        View.OnClickListener comingSoon = v ->
                Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show();
        view.findViewById(R.id.rowTankProfiles).setOnClickListener(comingSoon);
        view.findViewById(R.id.rowWaterParameters).setOnClickListener(comingSoon);
        view.findViewById(R.id.rowLightingPumps).setOnClickListener(comingSoon);
    }
}
