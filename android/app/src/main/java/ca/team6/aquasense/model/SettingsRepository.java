package ca.team6.aquasense.model;

import android.content.Context;

public class SettingsRepository {

    // Keys
    public static final String KEY_TEMP_UNIT              = "tempUnit";
    public static final String KEY_THEME_MODE             = "themeMode";
    public static final String KEY_24H_CLOCK              = "use24HourClock";
    public static final String KEY_READING_PRECISION      = "readingPrecision";
    public static final String KEY_SENSOR_ORDER           = "sensorCardOrder";
    public static final String KEY_SENSOR_HIDDEN          = "sensorCardHidden";

    // KEY_THEME_MODE values
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";

    // KEY_READING_PRECISION values
    public static final String PRECISION_STANDARD = "standard";
    public static final String PRECISION_PRECISE  = "precise";

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
    public static final String KEY_NOTIFY_HUB_DISCONNECTED = "notifyHubDisconnected";
    public static final String KEY_NOTIFY_SUMMARY         = "notifyDailySummary";

    public static final String KEY_PROFILE_NAME           = "profileName";
    public static final String KEY_PROFILE_EMAIL          = "profileEmail";
    public static final String KEY_AUTO_BACKUP            = "autoBackup";

    // True after the SETTINGS-03 pairing wizard has been completed on this device on first install
    public static final String KEY_PAIRING_COMPLETE       = "pairingComplete";

    // True after a successful register/login on this device; until then auth starts at register page.
    public static final String KEY_HAS_AUTHENTICATED      = "hasAuthenticated";

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
        // Defaults come from AppSettings' field initializers, never from literals.
        AppSettings d = new AppSettings();
        AppSettings s = new AppSettings();

        s.tempUnit              = prefs.getString(KEY_TEMP_UNIT, d.tempUnit);
        s.use24HourClock        = prefs.getBoolean(KEY_24H_CLOCK, d.use24HourClock);
        s.themeMode             = prefs.getString(KEY_THEME_MODE, d.themeMode);
        s.readingPrecision      = prefs.getString(KEY_READING_PRECISION, d.readingPrecision);

        s.pushNotifications     = prefs.getBoolean(KEY_PUSH_NOTIF, d.pushNotifications);
        s.emailAlerts           = prefs.getBoolean(KEY_EMAIL_ALERTS, d.emailAlerts);
        s.smsAlerts             = prefs.getBoolean(KEY_SMS_ALERTS, d.smsAlerts);
        s.criticalAlertsOnly    = prefs.getBoolean(KEY_CRITICAL_ONLY, d.criticalAlertsOnly);
        s.quietHours            = prefs.getBoolean(KEY_QUIET_HOURS, d.quietHours);
        s.quietHoursStart       = prefs.getString(KEY_QUIET_START, d.quietHoursStart);
        s.quietHoursEnd         = prefs.getString(KEY_QUIET_END, d.quietHoursEnd);
        s.feedingModeSilence    = prefs.getBoolean(KEY_FEEDING_SILENCE, d.feedingModeSilence);
        s.notifyParamOutOfRange = prefs.getBoolean(KEY_NOTIFY_PARAM, d.notifyParamOutOfRange);
        s.notifySensorOffline   = prefs.getBoolean(KEY_NOTIFY_SENSOR, d.notifySensorOffline);
        s.notifyHubDisconnected = prefs.getBoolean(KEY_NOTIFY_HUB_DISCONNECTED, d.notifyHubDisconnected);
        s.notifyDailySummary    = prefs.getBoolean(KEY_NOTIFY_SUMMARY, d.notifyDailySummary);

        s.profileName           = prefs.getString(KEY_PROFILE_NAME, d.profileName);
        s.profileEmail          = prefs.getString(KEY_PROFILE_EMAIL, d.profileEmail);
        s.autoBackup            = prefs.getBoolean(KEY_AUTO_BACKUP, d.autoBackup);

        s.usageAnalytics        = prefs.getBoolean(KEY_USAGE_ANALYTICS, d.usageAnalytics);
        s.locationData          = prefs.getBoolean(KEY_LOCATION_DATA, d.locationData);
        s.firebaseRealtimeSync  = prefs.getBoolean(KEY_FIREBASE_SYNC, d.firebaseRealtimeSync);

        s.lastCalibratedLiquid     = prefs.getLong(KEY_CALIB_LIQUID, d.lastCalibratedLiquid);
        s.lastCalibratedTemp       = prefs.getLong(KEY_CALIB_TEMP, d.lastCalibratedTemp);
        s.lastCalibratedTds        = prefs.getLong(KEY_CALIB_TDS, d.lastCalibratedTds);
        s.lastCalibratedPh         = prefs.getLong(KEY_CALIB_PH, d.lastCalibratedPh);

        callback.onLoaded(s);
    }
}
