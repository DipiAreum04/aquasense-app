package ca.team6.aquasense.model.aquarium_sensors;

import ca.team6.aquasense.R;

public enum SensorStatus {
    DISCONNECTED(R.drawable.gray_circle_24, R.string.disconnected, R.color.status_gray),
    WARNING(R.drawable.orange_circle_24, R.string.warning, R.color.status_orange),
    CRITICAL(R.drawable.red_circle_24, R.string.critical, R.color.status_red),
    NORMAL(R.drawable.green_circle_24, R.string.normal, R.color.status_green);

    public final int iconResourceId;
    public final int textResourceId;
    public final int colorResourceId;

    SensorStatus(int iconResourceId, int textResourceId, int colorResourceId) {
        this.iconResourceId = iconResourceId;
        this.textResourceId = textResourceId;
        this.colorResourceId = colorResourceId;
    }
}
