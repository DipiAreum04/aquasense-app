package ca.team6.aquasense.model;

public enum SensorType {
    TEMPERATURE, WATER_LEVEL, PH_LEVEL, DISSOLVED_SOLIDS,

    /**
     * The board itself rather than one of its sensors, which is what a hub-disconnected alert is
     * about. Without it those alerts were logged against WATER_LEVEL and showed up in the history
     * as water level events, including under that sensor's filter.
     */
    HUB,
}
