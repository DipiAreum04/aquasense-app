package ca.team6.aquasense.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import ca.team6.aquasense.AuthActivity;
import ca.team6.aquasense.MainActivity;
import ca.team6.aquasense.settings.PairingWizardActivity;

/**
 * Routes the user after a successful auth session based on device pairing completion state.
 */
public final class AuthNavigator {

    private AuthNavigator() {}

    public static void continueAfterAuth(@NonNull Activity activity,
                                         @NonNull AuthRepository authRepository) {
        Intent intent;
        if (authRepository.needsPairing()) {
            // If it is the first login and the device is not paired, redirect to the pairing wizard.
            intent = new Intent(activity, PairingWizardActivity.class);
        } else {
            intent = new Intent(activity, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void goToLogin(@NonNull Activity activity) {
        Intent intent = new Intent(activity, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void goToDashboard(@NonNull Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
