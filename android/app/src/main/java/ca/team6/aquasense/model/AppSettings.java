package ca.team6.aquasense.model;

public class AppSettings {

    // ── Display & Units ──────────────────────────────────────
    public String tempUnit = "F";           // "C" or "F"
    public boolean metricUnits = false;
    public boolean darkMode = false;
    public boolean use24HourClock = true;
    public boolean extraDecimalPrecision = false;

    // ── Notifications ────────────────────────────────────────
    public boolean pushNotifications = true;
    public boolean emailAlerts = false;
    public boolean smsAlerts = true;
    public boolean criticalAlertsOnly = false;
    public boolean quietHours = false;
    public String quietHoursStart = "22:00";
    public String quietHoursEnd = "07:00";
    public boolean feedingModeSilence = false;
    public boolean notifyParamOutOfRange = true;
    public boolean notifySensorOffline = true;
    public boolean notifyEquipmentFailure = true;
    public boolean notifyDailySummary = false;
    public boolean notifyFirmwareUpdate = true;

    // ── Accounts & Backup ────────────────────────────────────
    public String profileName = "";
    public String profileEmail = "";
    public String profilePlan = "";
    public boolean autoBackup = true;

    // ── Data & Sync — Privacy ────────────────────────────────
    public boolean usageAnalytics = false;
    public boolean crashReports = true;
    public boolean locationData = false;

    // ── Data & Sync — Encryption ─────────────────────────────
    public boolean endToEndEncryption = true;
    public boolean encryptAtRest = true;

    // ── Data & Sync — Firebase ───────────────────────────────
    public boolean firebaseRealtimeSync = true;
    public boolean realtimeDatabase = false;

    // ── Sensor Calibration — last calibrated timestamps (ms) ─
    public long lastCalibratedPh = 0L;
    public long lastCalibratedTemp = 0L;
    public long lastCalibratedSalinity = 0L;
    public long lastCalibratedAmmonia = 0L;
    public long lastCalibratedDissolvedO2 = 0L;

    public AppSettings() {}
}
