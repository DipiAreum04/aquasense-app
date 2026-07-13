package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class DataSyncFragment extends Fragment {

    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());

        SwitchCompat switchUsageAnalytics = view.findViewById(R.id.switchUsageAnalytics);
        SwitchCompat switchCrashReports   = view.findViewById(R.id.switchCrashReports);
        SwitchCompat switchLocationData   = view.findViewById(R.id.switchLocationData);
        SwitchCompat switchE2E            = view.findViewById(R.id.switchE2EEncryption);
        SwitchCompat switchEncryptAtRest  = view.findViewById(R.id.switchEncryptAtRest);
        SwitchCompat switchFirebaseSync   = view.findViewById(R.id.switchFirebaseSync);
        SwitchCompat switchRealtimeDb     = view.findViewById(R.id.switchRealtimeDb);

        repo.loadSettings(s -> {
            switchUsageAnalytics.setChecked(s.usageAnalytics);
            switchCrashReports.setChecked(s.crashReports);
            switchLocationData.setChecked(s.locationData);
            switchE2E.setChecked(s.endToEndEncryption);
            switchEncryptAtRest.setChecked(s.encryptAtRest);
            switchFirebaseSync.setChecked(s.firebaseRealtimeSync);
            switchRealtimeDb.setChecked(s.realtimeDatabase);
        });

        switchUsageAnalytics.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_USAGE_ANALYTICS, checked));
        switchCrashReports.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_CRASH_REPORTS, checked));
        switchLocationData.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_LOCATION_DATA, checked));
        switchE2E.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_E2E_ENCRYPTION, checked));
        switchEncryptAtRest.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_ENCRYPT_AT_REST, checked));
        switchFirebaseSync.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_FIREBASE_SYNC, checked));
        switchRealtimeDb.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_REALTIME_DB, checked));

        view.findViewById(R.id.rowDownloadData).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Preparing your data export…", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Privacy policy coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnForceSync).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Full sync triggered", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnDisconnectFirebase).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Disconnect Firebase coming soon", Toast.LENGTH_SHORT).show());
    }
}
