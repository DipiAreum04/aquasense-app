package ca.team6.aquasense.model.aquarium_sensors;

import ca.team6.aquasense.R;

public enum SensorStatus {
    DISCONNECTED(
        R.drawable.gray_circle_24, R.string.disconnected, R.color.status_gray,
        R.color.status_gray_edge, R.color.status_gray_soft,
        R.color.status_gray, R.color.status_gray
    ),
    WARNING(
        R.drawable.orange_circle_24, R.string.warning, R.color.status_orange,
        R.color.status_orange_edge, R.color.status_orange_soft,
        R.color.sensor_content_primary, R.color.sensor_content_primary
    ),
    CRITICAL(
        R.drawable.red_circle_24, R.string.critical, R.color.status_red,
        R.color.status_red_edge, R.color.status_red_soft,
        R.color.sensor_content_primary, R.color.sensor_content_primary
    ),
    NORMAL(
        R.drawable.blue_circle_24, R.string.normal, R.color.status_blue,
        R.color.status_blue_edge, R.color.status_blue_soft,
        R.color.sensor_content_primary, R.color.sensor_content_primary
    );

    public final int iconResourceId;
    public final int textResourceId;
    public final int colorResourceId;
    /** The card's edge, which carries the status around a card that stays white. */
    public final int cardStrokeColorResourceId;
    /** Fill behind the status pill; the pill's dot and label stay on {@link #colorResourceId}. */
    public final int pillBackgroundColorResourceId;
    /**
     * The reading itself, which stays navy whatever the status: the pill beneath it is already
     * saying that, and a number is easier to read in one colour. Only a sensor that has stopped
     * reporting greys it, since then there is no reading to show.
     */
    public final int valueColorResourceId;
    /** The sensor's own icon, which only leaves navy once the sensor stops reporting. */
    public final int titleIconColorResourceId;

    SensorStatus(
        int iconResourceId,
        int textResourceId,
        int colorResourceId,
        int cardStrokeColorResourceId,
        int pillBackgroundColorResourceId,
        int valueColorResourceId,
        int titleIconColorResourceId
    ) {
        this.iconResourceId = iconResourceId;
        this.textResourceId = textResourceId;
        this.colorResourceId = colorResourceId;
        this.cardStrokeColorResourceId = cardStrokeColorResourceId;
        this.pillBackgroundColorResourceId = pillBackgroundColorResourceId;
        this.valueColorResourceId = valueColorResourceId;
        this.titleIconColorResourceId = titleIconColorResourceId;
    }
}
