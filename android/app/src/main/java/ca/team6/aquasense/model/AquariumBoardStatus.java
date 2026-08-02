package ca.team6.aquasense.model;

import ca.team6.aquasense.R;

public enum AquariumBoardStatus {
    OFFLINE(R.drawable.gray_circle_24, R.string.offline, R.color.status_gray),
    WARNING(R.drawable.orange_circle_24, R.string.warning, R.color.status_orange),
    ERROR(R.drawable.red_circle_24, R.string.error, R.color.status_red),
    ONLINE(R.drawable.green_circle_24, R.string.online, R.color.status_green);

    public final int iconResourceId;
    public final int textResourceId;
    public final int colorResourceId;

    AquariumBoardStatus(int iconResourceId, int textResourceId, int colorResourceId) {
        this.iconResourceId = iconResourceId;
        this.textResourceId = textResourceId;
        this.colorResourceId = colorResourceId;
    }
}
