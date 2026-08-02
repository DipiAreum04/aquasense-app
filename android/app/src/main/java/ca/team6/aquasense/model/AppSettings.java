package ca.team6.aquasense.model;

// Single source of truth for settings defaults.
public class AppSettings {

    // Display & Units
    public String tempUnit = "F";           // "C" or "F"
    public String themeMode = SettingsRepository.THEME_SYSTEM;
    public boolean use24HourClock = true;
    public String readingPrecision = SettingsRepository.PRECISION_STANDARD;

    // Notifications
    public boolean pushNotifications = true;
    public boolean emailAlerts = false;
    public boolean smsAlerts = true;
    public boolean criticalAlertsOnly = false;
    public boolean quietHours = false;
    public String quietHoursStart = "22:00";
    public String quietHoursEnd = "07:00";
    public boolean feedingModeSilence = false;
    public boolean notifyParamOutOfRange = true;
    public boolean notifyAbnormalJumps = true;
    public boolean notifySensorOffline = true;
    public boolean notifyHubDisconnected = true;

    // A sensor set to false suppresses every notification type for that sensor except for critical alerts
    public boolean sensorAlertsTemperature = true;
    public boolean sensorAlertsWaterLevel = true;
    public boolean sensorAlertsDissolvedSolids = true;
    public boolean sensorAlertsPhLevel = true;

    // Accounts & Backup
    public String profileName = "";
    public String profileEmail = "";
    public boolean autoBackup = true;

    // Data & Sync: Privacy
    public boolean usageAnalytics = false;
    public boolean locationData = false;

    // Sensor Calibration: last calibrated timestamps (ms)
    public long lastCalibratedLiquid = 0L;
    public long lastCalibratedTemp = 0L;
    public long lastCalibratedTds = 0L;
    public long lastCalibratedPh = 0L;

    public AppSettings() {}
}
