package ca.team6.aquasense.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.io.File;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class AccountsBackupFragment extends Fragment {

    private AuthRepository authRepository;
    private SharedPreferenceHelper prefs;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfilePlan;
    private TextView tvLastBackup;
    private TextView tvStorageUsed;
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
        authRepository = new AuthRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());
        SettingsRepository repo = new SettingsRepository(requireContext());

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfilePlan = view.findViewById(R.id.tvProfilePlan);
        tvLastBackup = view.findViewById(R.id.tvLastBackup);
        tvStorageUsed = view.findViewById(R.id.tvStorageUsed);
        switchAutoBackup = view.findViewById(R.id.switchAutoBackup);

        // Bind switch before attaching the listener so the initial value does not toast.
        repo.loadSettings(settings -> switchAutoBackup.setChecked(settings.autoBackup));

        // Toggle persists locally; cloud auto-backup is not implemented yet.
        // TODO: Implement cloud auto-backup.
        switchAutoBackup.setOnCheckedChangeListener((btn, checked) -> {
            prefs.updateField(SettingsRepository.KEY_AUTO_BACKUP, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_accounts_to_editProfile));

        view.findViewById(R.id.btnExportData).setOnClickListener(v ->
                SharedPreferenceHelper.showComingSoon(requireContext()));

        view.findViewById(R.id.btnImportBackup).setOnClickListener(v ->
                SharedPreferenceHelper.showComingSoon(requireContext()));

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteProfileDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        bindProfile();
        bindStorageUsed();
        bindLastBackup();
    }

    private void bindProfile() {
        authRepository.syncProfileCacheFromFirebase();

        String name = authRepository.getProfileDisplayName();
        String email = authRepository.getProfileEmail();
        tvProfileName.setText(TextUtils.isEmpty(name)
                ? getString(R.string.profile_name_empty)
                : name);
        tvProfileEmail.setText(TextUtils.isEmpty(email)
                ? getString(R.string.profile_email_empty)
                : email);

        // TODO: After Firebase Auth is set up, read users/{uid}.plan from Firestore
        // and display it here (e.g. Free / Pro). Until then always show Free Plan.
        tvProfilePlan.setText(R.string.profile_plan_placeholder);
    }

    private void bindLastBackup() {
        // TODO: After cloud backup exists, show the timestamp of the last successful backup.
        tvLastBackup.setText(R.string.never);
    }

    private void bindStorageUsed() {
        Context context = requireContext();
        long bytes = calculateLocalStorageBytes(context);
        String sizeLabel = Formatter.formatFileSize(context, bytes);
        tvStorageUsed.setText(getString(R.string.storage_used_format, sizeLabel));
        // TODO: After Firebase Storage backup exists, show cloud used/quota
        // (e.g. "1.2 / 10 GB") instead of local app data only.
    }

    // Sums local app data: files, cache, databases, and shared preferences.

    private static long calculateLocalStorageBytes(@NonNull Context context) {
        long total = 0L;
        total += sizeOf(context.getFilesDir());
        total += sizeOf(context.getCacheDir());
        File dataDir = context.getDataDir();
        total += sizeOf(new File(dataDir, "databases"));
        total += sizeOf(new File(dataDir, "shared_prefs"));
        return total;
    }

    private static long sizeOf(@Nullable File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        for (File child : children) {
            total += sizeOf(child);
        }
        return total;
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.sign_out, (dialog, which) -> {
                    authRepository.signOut();
                    AuthNavigator.goToLogin(requireActivity());
                })
                .show();
    }

    private void showDeleteProfileDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_message)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> {
                    // TODO: After Firebase is set up, also delete Firebase Auth user or cloud data.
                    prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, "");
                    prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, "");
                    bindProfile();
                    Toast.makeText(requireContext(),
                            R.string.delete_profile_done,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
