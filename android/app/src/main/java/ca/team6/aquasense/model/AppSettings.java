package ca.team6.aquasense.model;

public class AppSettings {

    public String tempUnit = "F";
    public String themeMode = SettingsRepository.THEME_SYSTEM;
    public String readingPrecision = SettingsRepository.PRECISION_STANDARD;

    public boolean pushNotifications = true;
    public boolean criticalAlertsOnly = false;
    public boolean quietHours = false;
    public String quietHoursStart = "22:00";
    public String quietHoursEnd = "07:00";
    public boolean notifyParamOutOfRange = true;
    public boolean notifyAbnormalJumps = true;
    public boolean notifySensorDisconnected = true;
    public boolean notifyHubDisconnected = true;

    public boolean sensorAlertsTemperature = true;
    public boolean sensorAlertsWaterLevel = true;
    public boolean sensorAlertsDissolvedSolids = true;
    public boolean sensorAlertsPhLevel = true;

    public String profileName = "";
    public String profileEmail = "";

    public long lastCalibratedLiquid = 0L;
    public long lastCalibratedTemp = 0L;
    public long lastCalibratedTds = 0L;
    public long lastCalibratedPh = 0L;

    public AppSettings() {}
}
