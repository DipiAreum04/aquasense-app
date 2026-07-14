package ca.team6.aquasense.model;

import ca.team6.aquasense.R;

public class AquariumBoardStatus {
    public static final AquariumBoardStatus OFFLINE =
            new AquariumBoardStatus(
                    R.drawable.gray_circle_24,
                    R.string.offline,
                    R.color.status_gray
            );

    public static final AquariumBoardStatus WARNING =
            new AquariumBoardStatus(
                    R.drawable.orange_circle_24,
                    R.string.warning,
                    R.color.status_orange
            );

    public static final AquariumBoardStatus ERROR =
            new AquariumBoardStatus(
                    R.drawable.red_circle_24,
                    R.string.error,
                    R.color.status_red
            );

    public static final AquariumBoardStatus ONLINE =
            new AquariumBoardStatus(
                    R.drawable.green_circle_24,
                    R.string.online,
                    R.color.status_green
            );

    public final int iconResourceId;
    public final int textResourceId;
    public final int colorResourceId;

    private AquariumBoardStatus(int iconResourceId, int textResourceId, int colorResourceId) {
        this.iconResourceId = iconResourceId;
        this.textResourceId = textResourceId;
        this.colorResourceId = colorResourceId;
    }
}
