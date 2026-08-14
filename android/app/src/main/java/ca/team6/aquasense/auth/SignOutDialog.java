package ca.team6.aquasense.auth;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.Activity;

import androidx.annotation.NonNull;
import ca.team6.aquasense.R;

public final class SignOutDialog {

    private SignOutDialog() {}

    public static void show(@NonNull Activity activity,
                            @NonNull AuthRepository authRepository) {
        new MaterialAlertDialogBuilder(
                activity, R.style.ThemeOverlay_AquaSense_MaterialAlertDialog_Danger)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.sign_out, (dialog, which) -> {
                    authRepository.signOut();
                    AuthNavigator.goToLogin(activity);
                })
                .show();
    }
}
