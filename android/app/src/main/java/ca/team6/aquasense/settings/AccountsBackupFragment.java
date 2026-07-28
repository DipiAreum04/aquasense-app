package ca.team6.aquasense.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import ca.team6.aquasense.auth.SignOutDialog;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class AccountsBackupFragment extends Fragment {

    private AuthRepository authRepository;
    private SharedPreferenceHelper prefs;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvLastBackup;
    private TextView tvStorageUsed;
    private SwitchCompat switchAutoBackup;
    private boolean passwordVisible;

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

        view.findViewById(R.id.btnImportBackup).setOnClickListener(v ->
                SharedPreferenceHelper.showComingSoon(requireContext()));

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> SignOutDialog.show(requireActivity(), authRepository));

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

    private void showDeleteProfileDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_message)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> {
                    // Firebase requires a recent login before delete(), so confirm credentials
                    // with whichever provider this account actually signed in through.
                    if (authRepository.hasPasswordProvider()) {
                        showConfirmPasswordDialog();
                    } else {
                        showConfirmGoogleDialog();
                    }
                })
                .show();
    }

    private void showConfirmPasswordDialog() {
        // The inflated field always starts masked, so reset the tracked state with it.
        passwordVisible = false;
        View content = getLayoutInflater().inflate(R.layout.dialog_confirm_password, null);
        EditText etPassword = content.findViewById(R.id.etConfirmPassword);
        ImageButton btnToggle = content.findViewById(R.id.btnToggleConfirmPassword);
        btnToggle.setOnClickListener(v -> togglePasswordVisibility(etPassword, btnToggle));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile_confirm_password_title)
                .setView(content)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.delete_profile_confirm, null)
                .create();

        // Bound after show() so an empty password does not dismiss the dialog.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = etPassword.getText().toString();
                    if (TextUtils.isEmpty(password)) {
                        etPassword.setError(getString(R.string.auth_password_required));
                        return;
                    }
                    setDeleteDialogBusy(dialog, true);
                    authRepository.deleteAccountWithPassword(password, new AuthRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) {
                                return;
                            }
                            dialog.dismiss();
                            finishAccountDeleted();
                        }

                        @Override
                        public void onError(int messageResId) {
                            if (!isAdded()) {
                                return;
                            }
                            setDeleteDialogBusy(dialog, false);
                            Toast.makeText(requireContext(), messageResId, Toast.LENGTH_LONG).show();
                        }
                    });
                }));
        dialog.show();
    }

    private void showConfirmGoogleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile_confirm_google_title)
                .setMessage(R.string.delete_profile_confirm_google_message)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.delete_profile_continue, (dialog, which) -> {
                    Toast.makeText(requireContext(),
                            R.string.delete_profile_in_progress,
                            Toast.LENGTH_SHORT).show();
                    authRepository.deleteAccountWithGoogle(requireActivity(),
                            new AuthRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    if (isAdded()) {
                                        finishAccountDeleted();
                                    }
                                }

                                @Override
                                public void onError(int messageResId) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), messageResId,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                })
                .show();
    }

    private void setDeleteDialogBusy(@NonNull AlertDialog dialog, boolean busy) {
        dialog.setCancelable(!busy);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!busy);
    }

    private void finishAccountDeleted() {
        Toast.makeText(requireContext(), R.string.delete_profile_done, Toast.LENGTH_SHORT).show();
        AuthNavigator.goToLogin(requireActivity());
    }

    private void togglePasswordVisibility(@NonNull EditText field, @NonNull ImageButton toggle) {
        passwordVisible = !passwordVisible;
        int selection = field.getSelectionEnd();
        if (passwordVisible) {
            field.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggle.setImageResource(R.drawable.visibility_off_24px);
            toggle.setContentDescription(getString(R.string.auth_hide_password));
        } else {
            field.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggle.setImageResource(R.drawable.visibility_24px);
            toggle.setContentDescription(getString(R.string.auth_show_password));
        }
        field.setSelection(Math.max(selection, 0));
    }
}
