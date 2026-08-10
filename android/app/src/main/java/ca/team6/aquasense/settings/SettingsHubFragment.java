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

import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.auth.SignOutDialog;

public class SettingsHubFragment extends Fragment {

    /** Shown when neither the name nor the email can supply a usable initial. */
    private static final String INITIAL_PLACEHOLDER = "?";

    private TextView tvHubProfileInitial;
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
        authRepository = AuthRepository.getInstance(requireContext());
        tvHubProfileInitial = view.findViewById(R.id.tvHubProfileInitial);
        tvHubProfileName = view.findViewById(R.id.tvHubProfileName);
        tvHubProfileEmail = view.findViewById(R.id.tvHubProfileEmail);

        // Sign out button on the profile card. But the card itself is not clickable.
        view.findViewById(R.id.btnHubSignOut)
                .setOnClickListener(v -> SignOutDialog.show(requireActivity(), authRepository));

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

        // Privacy Policy
        view.findViewById(R.id.rowPrivacyPolicy)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_privacyPolicy));

        // Contact & Support
        view.findViewById(R.id.rowContactSupport)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_contactSupport));

        // Troubleshooting Guide
        view.findViewById(R.id.rowTroubleshootingGuide)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_troubleshootingGuide));

        // Aquarium Templates
        view.findViewById(R.id.rowAquariumTemplates)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_aquariumTemplates));

        // My Aquariums: the same aquariumselector as the dashboard header opens
        view.findViewById(R.id.rowTankProfiles)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_aquariumSelector));

        // Water Parameters
        view.findViewById(R.id.rowWaterParameters)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.action_hub_to_waterParameters));
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        if (authRepository == null || tvHubProfileInitial == null
                || tvHubProfileName == null || tvHubProfileEmail == null) {
            return;
        }
        authRepository.syncProfileCacheFromFirebase();

        String name = authRepository.getProfileDisplayName();
        String email = authRepository.getProfileEmail();
        tvHubProfileInitial.setText(initialFor(name, email));
        tvHubProfileName.setText(TextUtils.isEmpty(name)
                ? getString(R.string.profile_name_empty)
                : name);
        tvHubProfileEmail.setText(TextUtils.isEmpty(email)
                ? getString(R.string.profile_email_empty)
                : email);
    }

    /**
     * First character of the display name for the avatar, falling back to the email and then to
     * {@link #INITIAL_PLACEHOLDER}.
     */
    private static String initialFor(@Nullable String name, @Nullable String email) {
        String source = !TextUtils.isEmpty(name) ? name : email;
        if (source == null) {
            return INITIAL_PLACEHOLDER;
        }
        source = source.trim();
        if (source.isEmpty()) {
            return INITIAL_PLACEHOLDER;
        }

        int codePoint = source.codePointAt(0);
        if (!Character.isLetterOrDigit(codePoint)) {
            return INITIAL_PLACEHOLDER;
        }
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT);
    }
}
