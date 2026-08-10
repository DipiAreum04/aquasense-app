package ca.team6.aquasense.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.model.ScopedLogger;

public final class MonitorBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        ScopedLogger.info("Restoring threshold monitoring after " + action);
        // Re-checks sign-in and the notification settings rather than assuming it should run.
        MonitoringController.sync(context);
    }
}
