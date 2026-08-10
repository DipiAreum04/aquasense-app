package ca.team6.aquasense.analytics;

/**
 * A y range that belongs to the sensor rather than to the readings on screen.
 *
 * <p>Most sensors have none, and should not: temperature sits around 21 and pH around 7, so an axis
 * fixed to the scale those are measured on would spend the plot on empty space and flatten the
 * variation the graph exists to show. Letting the readings scale the axis is the right default for
 * them.
 *
 * <p>It is the wrong default for a reading that is already a share of something. Water level is
 * plotted as the percentage of each bucket that had water at the sensor, and an axis scaled to those
 * draws a tank that dipped to 97% for one bucket as a line falling off a cliff - the readings differ
 * by three points and the axis is three points tall. What that graph has to be read against is the
 * whole 0-100 it could have covered.
 */
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
