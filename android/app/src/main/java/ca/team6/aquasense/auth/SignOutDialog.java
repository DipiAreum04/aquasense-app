package ca.team6.aquasense.auth;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import ca.team6.aquasense.R;

/**
 * Shared sign-out confirmation dialog so every entry point asks the same question and
 * lands the user in the same place.
 */
public final class SignOutDialog {

    private SignOutDialog() {}

    public static void show(@NonNull Activity activity,
                            @NonNull AuthRepository authRepository) {
        new AlertDialog.Builder(activity)
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
