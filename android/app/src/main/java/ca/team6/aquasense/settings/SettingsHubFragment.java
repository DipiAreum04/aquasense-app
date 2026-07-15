package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class SettingsHubFragment extends Fragment {

    private TextView tvHubProfileName;
    private TextView tvHubProfileEmail;
    private SettingsRepository repo;

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
        repo = new SettingsRepository(requireContext());
        tvHubProfileName = view.findViewById(R.id.tvHubProfileName);
        tvHubProfileEmail = view.findViewById(R.id.tvHubProfileEmail);

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
                SharedPreferenceHelper.showComingSoon(requireContext());
        view.findViewById(R.id.rowTankProfiles).setOnClickListener(comingSoon);
        view.findViewById(R.id.rowWaterParameters).setOnClickListener(comingSoon);
        view.findViewById(R.id.rowLightingPumps).setOnClickListener(comingSoon);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        if (repo == null || tvHubProfileName == null || tvHubProfileEmail == null) {
            return;
        }
        repo.loadSettings(this::applyProfile);
    }

    private void applyProfile(AppSettings settings) {
        tvHubProfileName.setText(TextUtils.isEmpty(settings.profileName)
                ? getString(R.string.profile_name_empty)
                : settings.profileName);
        tvHubProfileEmail.setText(TextUtils.isEmpty(settings.profileEmail)
                ? getString(R.string.profile_email_empty)
                : settings.profileEmail);
    }
}
