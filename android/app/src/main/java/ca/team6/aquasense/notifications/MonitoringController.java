package ca.team6.aquasense.notifications;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

import ca.team6.aquasense.settings.AppSettings;
import ca.team6.aquasense.settings.SettingsRepository;
import ca.team6.aquasense.settings.SharedPreferenceHelper;

public final class MonitoringController {

    private MonitoringController() {}

    public static boolean shouldMonitor(@NonNull Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return false;
        }
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return false;
        }
        AppSettings defaults = new AppSettings();
        if (!prefs.getBoolean(SettingsRepository.KEY_PUSH_NOTIF, defaults.pushNotifications)) {
            return false;
        }
        return prefs.getBoolean(SettingsRepository.KEY_NOTIFY_PARAM, defaults.notifyParamOutOfRange)
                || prefs.getBoolean(
                        SettingsRepository.KEY_NOTIFY_ABNORMAL_JUMPS, defaults.notifyAbnormalJumps)
                || prefs.getBoolean(
                        SettingsRepository.KEY_NOTIFY_SENSOR, defaults.notifySensorDisconnected)
                || prefs.getBoolean(
                        SettingsRepository.KEY_NOTIFY_HUB_DISCONNECTED, defaults.notifyHubDisconnected);
    }

    public static void sync(@NonNull Context context) {
        if (shouldMonitor(context)) {
            ThresholdMonitorService.start(context);
        } else {
            ThresholdMonitorService.stop(context);
        }
    }
}
