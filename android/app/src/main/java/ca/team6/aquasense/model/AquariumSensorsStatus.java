package ca.team6.aquasense.model;

import ca.team6.aquasense.R;

public class AquariumSensorsStatus {
    private static final AquariumSensorsStatus SUNNY = new AquariumSensorsStatus(
        R.string.aquarium_sensors_status_sunny_icon,
        R.string.aquarium_sensors_status_sunny_heading,
        R.string.aquarium_sensors_status_sunny_description,
        R.color.aquarium_sensors_status_sunny_gradient_start,
        R.color.aquarium_sensors_status_sunny_gradient_end
    );
    private static final AquariumSensorsStatus FAIR = new AquariumSensorsStatus(
        R.string.aquarium_sensors_status_fair_icon,
        R.string.aquarium_sensors_status_fair_heading,
        R.string.aquarium_sensors_status_fair_description,
        R.color.aquarium_sensors_status_fair_gradient_start,
        R.color.aquarium_sensors_status_fair_gradient_end
    );
    private static final AquariumSensorsStatus CLOUDY = new AquariumSensorsStatus(
        R.string.aquarium_sensors_status_cloudy_icon,
        R.string.aquarium_sensors_status_cloudy_heading,
        R.string.aquarium_sensors_status_cloudy_description,
        R.color.aquarium_sensors_status_cloudy_gradient_start,
        R.color.aquarium_sensors_status_cloudy_gradient_end
    );
    private static final AquariumSensorsStatus RAINY = new AquariumSensorsStatus(
        R.string.aquarium_sensors_status_rainy_icon,
        R.string.aquarium_sensors_status_rainy_heading,
        R.string.aquarium_sensors_status_rainy_description,
        R.color.aquarium_sensors_status_rainy_gradient_start,
        R.color.aquarium_sensors_status_rainy_gradient_end
    );
    private static final AquariumSensorsStatus STORMY = new AquariumSensorsStatus(
        R.string.aquarium_sensors_status_stormy_icon,
        R.string.aquarium_sensors_status_stormy_heading,
        R.string.aquarium_sensors_status_stormy_description,
        R.color.aquarium_sensors_status_stormy_gradient_start,
        R.color.aquarium_sensors_status_stormy_gradient_end
    );

    public final int iconResId;
    public final int headingResId;
    public final int descriptionResId;
    public final int gradientStartColorResId;
    public final int gradientEndColorResId;

    private AquariumSensorsStatus(
        int iconResId,
        int headingResId,
        int descriptionResId,
        int gradientStartColorResId,
        int gradientEndColorResId
    ) {
        this.iconResId = iconResId;
        this.headingResId = headingResId;
        this.descriptionResId = descriptionResId;
        this.gradientStartColorResId = gradientStartColorResId;
        this.gradientEndColorResId = gradientEndColorResId;
    }

    public static AquariumSensorsStatus forScore(int score) {
        if (score > 100 || score < 0) {
            throw new IllegalArgumentException("Invalid score: "+score);
        } if (score >= 90) {
            return AquariumSensorsStatus.SUNNY;
        } if (score >= 75) {
            return AquariumSensorsStatus.FAIR;
        } if (score >= 50) {
            return AquariumSensorsStatus.CLOUDY;
        } if (score >= 25) {
            return AquariumSensorsStatus.RAINY;
        }
        return AquariumSensorsStatus.STORMY;
    }
}
