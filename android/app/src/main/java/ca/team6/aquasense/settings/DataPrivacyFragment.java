package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class DataPrivacyFragment extends Fragment {

    // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    @SuppressWarnings("FieldCanBeLocal") 
    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());

        SwitchCompat switchUsageAnalytics = view.findViewById(R.id.switchUsageAnalytics);
        SwitchCompat switchLocationData   = view.findViewById(R.id.switchLocationData);

        repo.loadSettings(s -> {
            switchUsageAnalytics.setChecked(s.usageAnalytics);
            switchLocationData.setChecked(s.locationData);
        });

        // Toggles persist locally but underlying features are not wired yet.
        // TODO: Wire up the features when they are implemented
        switchUsageAnalytics.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_USAGE_ANALYTICS, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        switchLocationData.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_LOCATION_DATA, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        view.findViewById(R.id.rowDownloadData).setOnClickListener(v ->
                SharedPreferenceHelper.showComingSoon(requireContext()));

        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_dataPrivacy_to_privacyPolicy));
    }
}
