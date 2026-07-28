package ca.team6.aquasense.model.aquarium_sensors;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.ScopedLogger;

public abstract class AquariumSensor {
    private int unitResId;
    private String value;
    protected SensorStatus status;

    protected AquariumSensor() {
        this.setUnitResId(R.string.unit_dimensionless);
        this.setValue("-");
        this.updateSensorStatus();
    }

    public int getUnitResId() {
        return this.unitResId;
    }
    
    public String getValue() {
        return this.value;
    }

    public void setUnitResId(int unitResId) {
        this.unitResId = unitResId;
    }

    public void setValue(String value) {
        this.value = value;
        this.updateSensorStatus();
    }

    // TODO: REMOVE IF ACTUALLY UNUSED BY END OF SPRINT 2
    @SuppressWarnings("unused")
    public SensorStatus getSensorStatus() {
        return this.status;
    }
    
    public int getStatusIconResId() {
        switch (this.status) {
            case OFFLINE:
                return R.drawable.gray_circle_24;
            case CRITICAL:
                return R.drawable.red_circle_24;
            case WARNING:
                return R.drawable.orange_circle_24;
            case NOMINAL:
                return R.drawable.green_circle_24;
        }
        ScopedLogger.error("Unknown SensorStatus enum "+this.status+", defaulting to offline.");
        return R.drawable.gray_circle_24;
    }
    
    public int getStatusTextResId() {
        switch (this.status) {
            case OFFLINE:
                return R.string.offline;
            case CRITICAL:
                return R.string.critical;
            case WARNING:
                return R.string.warning;
            case NOMINAL:
                return R.string.nominal;
        }
        ScopedLogger.error("Unknown SensorStatus enum "+this.status+", defaulting to offline.");
        return R.string.offline;
    }
    
    public int getStatusColorResId() {
        switch (this.status) {
            case OFFLINE:
                return R.color.status_gray;
            case CRITICAL:
                return R.color.status_red;
            case WARNING:
                return R.color.status_orange;
            case NOMINAL:
                return R.color.status_green;
        }
        ScopedLogger.error("Unknown SensorStatus enum "+this.status+", defaulting to offline.");
        return R.color.status_gray;
    }

    public InfoSheetSection[] getInfoSheetSections() {
        switch (this.status) {
            case OFFLINE:
                return this.getInfoSheetSectionsForOffline();
            case CRITICAL:
                return this.getInfoSheetSectionsForCritical();
            case WARNING:
                return this.getInfoSheetSectionsForWarning();
            case NOMINAL:
                return this.getInfoSheetSectionsForNominal();
        }
        ScopedLogger.error("Unknown SensorStatus enum "+this.status+", defaulting to offline.");
        return this.getInfoSheetSectionsForOffline();
    }

    protected void updateSensorStatus() {
        this.status = SensorStatus.OFFLINE;
    }

    // Stable identifier used to persist per-sensor preferences (dashboard card order and visibility).
    // Values match the telemetry node names in database/schema.json.
    public abstract String getId();

    public abstract int getTitleIconResId();
    
    public abstract int getNameResId();
    
    public abstract int getInfoSheetTitleResId();
    
    public abstract int getInfoSheetDescResId();

    public abstract InfoSheetSection[] getInfoSheetSectionsForOffline();

    public abstract InfoSheetSection[] getInfoSheetSectionsForCritical();

    public abstract InfoSheetSection[] getInfoSheetSectionsForWarning();

    public abstract InfoSheetSection[] getInfoSheetSectionsForNominal();
}
