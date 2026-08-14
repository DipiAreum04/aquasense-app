package ca.team6.aquasense.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.logging.ScopedLogger;

public final class BackgroundMonitoringPrompt {

    private BackgroundMonitoringPrompt() {}

    public static boolean isNotificationPermissionMissing(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static void openAppNotificationSettings(@NonNull Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        try {
            activity.startActivity(intent);
        } catch (RuntimeException e) {
            ScopedLogger.warn("App notification settings unavailable: " + e.getMessage());
            try {
                activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", activity.getPackageName(), null)));
            } catch (RuntimeException fallbackFailure) {
                ScopedLogger.error(
                        "No app details screen: " + fallbackFailure.getMessage());
            }
        }
    }
}
