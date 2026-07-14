package ca.team6.aquasense.model;

public class AquariumSensorsStatus {
    public static final String SUNNY = "☀️";
    public static final String FAIR = "⛅";
    public static final String CLOUDY = "☁️";
    public static final String RAINY = "🌧️";
    public static final String STORMY = "⛈️";

    public static String forScore(int score) {
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
