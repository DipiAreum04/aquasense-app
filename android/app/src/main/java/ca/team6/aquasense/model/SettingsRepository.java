package ca.team6.aquasense.model;

import android.content.Context;

public class SettingsRepository {

    // Keys
    public static final String KEY_TEMP_UNIT              = "tempUnit";
    public static final String KEY_METRIC_UNITS           = "metricUnits";
    public static final String KEY_DARK_MODE              = "darkMode";
    public static final String KEY_24H_CLOCK              = "use24HourClock";

    public static final String KEY_PUSH_NOTIF             = "pushNotifications";
    public static final String KEY_EMAIL_ALERTS           = "emailAlerts";
    public static final String KEY_SMS_ALERTS             = "smsAlerts";
    public static final String KEY_CRITICAL_ONLY          = "criticalAlertsOnly";
    public static final String KEY_QUIET_HOURS            = "quietHours";
    public static final String KEY_QUIET_START            = "quietHoursStart";
    public static final String KEY_QUIET_END              = "quietHoursEnd";
    public static final String KEY_FEEDING_SILENCE        = "feedingModeSilence";
    public static final String KEY_NOTIFY_PARAM           = "notifyParamOutOfRange";
    public static final String KEY_NOTIFY_SENSOR          = "notifySensorOffline";
    public static final String KEY_NOTIFY_EQUIPMENT       = "notifyEquipmentFailure";
    public static final String KEY_NOTIFY_SUMMARY         = "notifyDailySummary";
    public static final String KEY_NOTIFY_FIRMWARE        = "notifyFirmwareUpdate";

    public static final String KEY_PROFILE_NAME           = "profileName";
    public static final String KEY_PROFILE_EMAIL          = "profileEmail";
    public static final String KEY_PROFILE_PLAN           = "profilePlan";
    public static final String KEY_AUTO_BACKUP            = "autoBackup";

    public static final String KEY_USAGE_ANALYTICS        = "usageAnalytics";
    public static final String KEY_LOCATION_DATA          = "locationData";
    public static final String KEY_FIREBASE_SYNC          = "firebaseRealtimeSync";

    public static final String KEY_CALIB_LIQUID          = "lastCalibratedLiquid";
    public static final String KEY_CALIB_TEMP            = "lastCalibratedTemp";
    public static final String KEY_CALIB_TDS             = "lastCalibratedTds";
    public static final String KEY_CALIB_PH              = "lastCalibratedPh";

    private final SharedPreferenceHelper prefs;

    public SettingsRepository(Context context) {
        prefs = SharedPreferenceHelper.getInstance(context);
    }

    public interface OnSettingsLoaded {
        void onLoaded(AppSettings settings);
    }

    public void loadSettings(OnSettingsLoaded callback) {
        AppSettings s = new AppSettings();

        s.tempUnit              = prefs.getString(KEY_TEMP_UNIT, "F");
        s.metricUnits           = prefs.getBoolean(KEY_METRIC_UNITS, false);
        s.darkMode              = prefs.getBoolean(KEY_DARK_MODE, false);
        s.use24HourClock        = prefs.getBoolean(KEY_24H_CLOCK, true);

        s.pushNotifications     = prefs.getBoolean(KEY_PUSH_NOTIF, true);
        s.emailAlerts           = prefs.getBoolean(KEY_EMAIL_ALERTS, false);
        s.smsAlerts             = prefs.getBoolean(KEY_SMS_ALERTS, true);
        s.criticalAlertsOnly    = prefs.getBoolean(KEY_CRITICAL_ONLY, false);
        s.quietHours            = prefs.getBoolean(KEY_QUIET_HOURS, false);
        s.quietHoursStart       = prefs.getString(KEY_QUIET_START, "22:00");
        s.quietHoursEnd         = prefs.getString(KEY_QUIET_END, "07:00");
        s.feedingModeSilence    = prefs.getBoolean(KEY_FEEDING_SILENCE, false);
        s.notifyParamOutOfRange = prefs.getBoolean(KEY_NOTIFY_PARAM, true);
        s.notifySensorOffline   = prefs.getBoolean(KEY_NOTIFY_SENSOR, true);
        s.notifyEquipmentFailure= prefs.getBoolean(KEY_NOTIFY_EQUIPMENT, true);
        s.notifyDailySummary    = prefs.getBoolean(KEY_NOTIFY_SUMMARY, false);
        s.notifyFirmwareUpdate  = prefs.getBoolean(KEY_NOTIFY_FIRMWARE, true);

        s.profileName           = prefs.getString(KEY_PROFILE_NAME, "");
        s.profileEmail          = prefs.getString(KEY_PROFILE_EMAIL, "");
        s.profilePlan           = prefs.getString(KEY_PROFILE_PLAN, "");
        s.autoBackup            = prefs.getBoolean(KEY_AUTO_BACKUP, true);

        s.usageAnalytics        = prefs.getBoolean(KEY_USAGE_ANALYTICS, false);
        s.locationData          = prefs.getBoolean(KEY_LOCATION_DATA, false);
        s.firebaseRealtimeSync  = prefs.getBoolean(KEY_FIREBASE_SYNC, true);

        s.lastCalibratedLiquid     = prefs.getLong(KEY_CALIB_LIQUID, 0L);
        s.lastCalibratedTemp       = prefs.getLong(KEY_CALIB_TEMP, 0L);
        s.lastCalibratedTds        = prefs.getLong(KEY_CALIB_TDS, 0L);
        s.lastCalibratedPh         = prefs.getLong(KEY_CALIB_PH, 0L);

        callback.onLoaded(s);
    }
}
