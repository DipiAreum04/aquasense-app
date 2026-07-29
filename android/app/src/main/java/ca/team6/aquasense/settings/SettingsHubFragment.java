package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.auth.SignOutDialog;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class SettingsHubFragment extends Fragment {

    private TextView tvHubProfileName;
    private TextView tvHubProfileEmail;
    private AuthRepository authRepository;

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
        authRepository = new AuthRepository(requireContext());
        tvHubProfileName = view.findViewById(R.id.tvHubProfileName);
        tvHubProfileEmail = view.findViewById(R.id.tvHubProfileEmail);

        addSignOutMenu();

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

        // Data & Privacy
        view.findViewById(R.id.rowDataPrivacy)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_dataPrivacy));

        // Contact & Support
        view.findViewById(R.id.rowContactSupport)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_contactSupport));

        // Troubleshooting Guide
        view.findViewById(R.id.rowTroubleshootingGuide)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_troubleshootingGuide));

        // TODO: Rows to be added by others - not implemented yet
        View.OnClickListener comingSoon = v ->
                SharedPreferenceHelper.showComingSoon(requireContext());
        view.findViewById(R.id.rowTankProfiles).setOnClickListener(comingSoon);
        view.findViewById(R.id.rowWaterParameters).setOnClickListener(comingSoon);
    }

    /**
     * The top toolbar of SettingsActivity is shared by every destination in SettingsHubFragment,
     * so the sign-out action is registered here rather than on SettingsActivity;
     * tying it to the view lifecycle keeps it off the sub-screens we navigate into.
     */
    private void addSignOutMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_settings_hub, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_sign_out) {
                    SignOutDialog.show(requireActivity(), authRepository);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        if (authRepository == null || tvHubProfileName == null || tvHubProfileEmail == null) {
            return;
        }
        authRepository.syncProfileCacheFromFirebase();

        String name = authRepository.getProfileDisplayName();
        String email = authRepository.getProfileEmail();
        tvHubProfileName.setText(TextUtils.isEmpty(name)
                ? getString(R.string.profile_name_empty)
                : name);
        tvHubProfileEmail.setText(TextUtils.isEmpty(email)
                ? getString(R.string.profile_email_empty)
                : email);
    }
}
