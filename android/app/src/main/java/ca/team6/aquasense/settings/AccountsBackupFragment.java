package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class AccountsBackupFragment extends Fragment {

    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfilePlan;
    private SwitchCompat switchAutoBackup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accounts_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePlan = view.findViewById(R.id.tvProfilePlan);
        switchAutoBackup = view.findViewById(R.id.switchAutoBackup);

        switchAutoBackup.setOnCheckedChangeListener((btn, checked) ->
                prefs.updateField(SettingsRepository.KEY_AUTO_BACKUP, checked));

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Edit profile coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnExportData).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export started", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnImportBackup).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Import coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Delete account coming soon", Toast.LENGTH_LONG).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        repo.loadSettings(this::applySettings);
    }

    private void applySettings(AppSettings settings) {
        tvProfileName.setText(TextUtils.isEmpty(settings.profileName)
                ? getString(R.string.profile_name_placeholder)
                : settings.profileName);
        tvProfileEmail.setText(TextUtils.isEmpty(settings.profileEmail)
                ? getString(R.string.profile_email_placeholder)
                : settings.profileEmail);
        tvProfilePlan.setText(TextUtils.isEmpty(settings.profilePlan)
                ? getString(R.string.profile_plan_placeholder)
                : settings.profilePlan);
        switchAutoBackup.setChecked(settings.autoBackup);
    }
}
