package ca.team6.aquasense.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import ca.team6.aquasense.AuthActivity;
import ca.team6.aquasense.MainActivity;

public final class AuthNavigator {

    private AuthNavigator() {}

    public static void continueAfterAuth(@NonNull Activity activity,
                                         @NonNull AuthRepository authRepository) {
        continueAfterAuth(activity, authRepository, true);
    }

    public static void continueAfterAuth(@NonNull Activity activity,
                                         @NonNull AuthRepository authRepository,
                                         boolean finishActivity) {
        if (finishActivity) {
            goToDashboard(activity);
            return;
        }
        activity.startActivity(new Intent(activity, MainActivity.class));
    }

    public static void goToLogin(@NonNull Activity activity) {
        goToLogin(activity, true);
    }

    public static void goToLogin(@NonNull Activity activity, boolean finishActivity) {
        Intent intent = new Intent(activity, AuthActivity.class);
        if (finishActivity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        activity.startActivity(intent);
        if (finishActivity) {
            activity.finish();
        }
    }

    public static void goToDashboard(@NonNull Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void applyNoAnimation(@NonNull Activity activity) {
        activity.overridePendingTransition(0, 0);
    }
}
