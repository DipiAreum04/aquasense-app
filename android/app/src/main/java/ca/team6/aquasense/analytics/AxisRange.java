package ca.team6.aquasense.analytics;

public final class AxisRange {

    private final float minimum;
    private final float maximum;

    public AxisRange(float minimum, float maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public float getMinimum() {
        return this.minimum;
    }

    public float getMaximum() {
        return this.maximum;
    }
}
