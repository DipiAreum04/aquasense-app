package ca.team6.aquasense.model;

import android.content.Context;

public class SettingsRepository {

    public static final String KEY_TEMP_UNIT              = "tempUnit";
    public static final String KEY_THEME_MODE             = "themeMode";
    public static final String KEY_READING_PRECISION      = "readingPrecision";
    public static final String KEY_SENSOR_ORDER           = "sensorCardOrder";
    public static final String KEY_SENSOR_HIDDEN          = "sensorCardHidden";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";

    public static final String PRECISION_STANDARD = "standard";
    public static final String PRECISION_PRECISE  = "precise";

    public static final String KEY_PUSH_NOTIF             = "pushNotifications";
    public static final String KEY_CRITICAL_ONLY          = "criticalAlertsOnly";
    public static final String KEY_QUIET_HOURS            = "quietHours";
    public static final String KEY_QUIET_START            = "quietHoursStart";
    public static final String KEY_QUIET_END              = "quietHoursEnd";
    public static final String KEY_NOTIFY_PARAM           = "notifyParamOutOfRange";
    public static final String KEY_NOTIFY_ABNORMAL_JUMPS  = "notifyAbnormalJumps";
    public static final String KEY_NOTIFY_SENSOR          = "notifySensorDisconnected";
    public static final String KEY_NOTIFY_HUB_DISCONNECTED = "notifyHubDisconnected";

    public static final String KEY_SENSOR_ALERTS_TEMP     = "sensorAlertsTemperature";
    public static final String KEY_SENSOR_ALERTS_LEVEL    = "sensorAlertsWaterLevel";
    public static final String KEY_SENSOR_ALERTS_TDS      = "sensorAlertsDissolvedSolids";
    public static final String KEY_SENSOR_ALERTS_PH       = "sensorAlertsPhLevel";

    public static final String KEY_PROFILE_NAME           = "profileName";
    public static final String KEY_PROFILE_EMAIL          = "profileEmail";

    public static final String KEY_PAIRING_COMPLETE       = "pairingComplete";
    public static final String KEY_ACTIVE_AQUARIUM        = "activeAquariumId";

    public static final String KEY_HAS_AUTHENTICATED      = "hasAuthenticated";

    public static final String KEY_NOTIF_PERMISSION_ASKED = "notificationPermissionAsked";

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
        AppSettings d = new AppSettings();
        AppSettings s = new AppSettings();

        s.tempUnit              = prefs.getString(KEY_TEMP_UNIT, d.tempUnit);
        s.themeMode             = prefs.getString(KEY_THEME_MODE, d.themeMode);
        s.readingPrecision      = prefs.getString(KEY_READING_PRECISION, d.readingPrecision);

        s.pushNotifications     = prefs.getBoolean(KEY_PUSH_NOTIF, d.pushNotifications);
        s.criticalAlertsOnly    = prefs.getBoolean(KEY_CRITICAL_ONLY, d.criticalAlertsOnly);
        s.quietHours            = prefs.getBoolean(KEY_QUIET_HOURS, d.quietHours);
        s.quietHoursStart       = prefs.getString(KEY_QUIET_START, d.quietHoursStart);
        s.quietHoursEnd         = prefs.getString(KEY_QUIET_END, d.quietHoursEnd);
        s.notifyParamOutOfRange = prefs.getBoolean(KEY_NOTIFY_PARAM, d.notifyParamOutOfRange);
        s.notifyAbnormalJumps   = prefs.getBoolean(KEY_NOTIFY_ABNORMAL_JUMPS, d.notifyAbnormalJumps);
        s.notifySensorDisconnected = prefs.getBoolean(KEY_NOTIFY_SENSOR, d.notifySensorDisconnected);
        s.notifyHubDisconnected = prefs.getBoolean(KEY_NOTIFY_HUB_DISCONNECTED, d.notifyHubDisconnected);

        s.profileName           = prefs.getString(KEY_PROFILE_NAME, d.profileName);
        s.profileEmail          = prefs.getString(KEY_PROFILE_EMAIL, d.profileEmail);

        s.sensorAlertsTemperature     = prefs.getBoolean(KEY_SENSOR_ALERTS_TEMP, d.sensorAlertsTemperature);
        s.sensorAlertsWaterLevel      = prefs.getBoolean(KEY_SENSOR_ALERTS_LEVEL, d.sensorAlertsWaterLevel);
        s.sensorAlertsDissolvedSolids = prefs.getBoolean(KEY_SENSOR_ALERTS_TDS, d.sensorAlertsDissolvedSolids);
        s.sensorAlertsPhLevel         = prefs.getBoolean(KEY_SENSOR_ALERTS_PH, d.sensorAlertsPhLevel);

        s.lastCalibratedLiquid     = prefs.getLong(KEY_CALIB_LIQUID, d.lastCalibratedLiquid);
        s.lastCalibratedTemp       = prefs.getLong(KEY_CALIB_TEMP, d.lastCalibratedTemp);
        s.lastCalibratedTds        = prefs.getLong(KEY_CALIB_TDS, d.lastCalibratedTds);
        s.lastCalibratedPh         = prefs.getLong(KEY_CALIB_PH, d.lastCalibratedPh);

        callback.onLoaded(s);
    }
}
