package ca.team6.aquasense.model;

/**
 * The four cut-off points that classify one sensor's reading for an aquarium.
 *
 * <p>The schema requires all four together, so a sensor is either fully configured or has no band
 * at all.
 */
public final class ThresholdBand {

    private final double warnLow;
    private final double safeLow;
    private final double safeHigh;
    private final double warnHigh;

    public ThresholdBand(double warnLow, double safeLow, double safeHigh, double warnHigh) {
        this.warnLow = warnLow;
        this.safeLow = safeLow;
        this.safeHigh = safeHigh;
        this.warnHigh = warnHigh;
    }

    public double getWarnLow() {
        return warnLow;
    }

    public double getSafeLow() {
        return safeLow;
    }

    public double getSafeHigh() {
        return safeHigh;
    }

    public double getWarnHigh() {
        return warnHigh;
    }
}
