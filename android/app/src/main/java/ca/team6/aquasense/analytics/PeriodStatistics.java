package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/**
 * What one period's buckets add up to: how much of the window the board was reporting for, the
 * range the readings covered, and how they fell across the sensor's threshold band.
 *
 * <p>Read from the raw buckets, in the units the database stores. The chart converts temperature to
 * whatever Display &amp; Units asks for before plotting; this does not, because a threshold band is
 * written in Celsius and grading a Fahrenheit reading against it would call a healthy tank
 * critical. Minimum, mean and maximum survive that conversion - it is linear and increasing, so
 * converting the three answers gives the same numbers as converting the hundred readings first -
 * so the screen converts them on the way to the label instead.
 *
 * <p>Two denominators are at work and they are not the same. Uptime is a share of <em>time</em>,
 * over the stretch the buckets actually cover; see {@link #getUptimePercent()}. The distribution is
 * a share of the buckets that carry a reading, since a gap is not a reading that was in or out of
 * range - the ring beside it is already saying how many of those there were.
 */
public final class PeriodStatistics {

    private final int bucketCount;
    private final int readingCount;
    private final long spanSeconds;
    private final long downtimeSeconds;
    private final double min;
    private final double max;
    private final double mean;
    // Indexed by SensorStatus.ordinal(), so a status added later cannot land in the wrong counter.
    private final int[] countsByStatus;

    private PeriodStatistics(int bucketCount,
                             int readingCount,
                             long spanSeconds,
                             long downtimeSeconds,
                             double min,
                             double max,
                             double mean,
                             @NonNull int[] countsByStatus) {
        this.bucketCount = bucketCount;
        this.readingCount = readingCount;
        this.spanSeconds = spanSeconds;
        this.downtimeSeconds = downtimeSeconds;
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.countsByStatus = countsByStatus;
    }

    /**
     * Summarises a period's buckets as the given sensor reads them.
     *
     * @param buckets       oldest first, as {@code FirebaseDatabaseHelper} unrolls the ring buffer.
     *                      The order is what the outages are measured from, so a list in any other
     *                      one gives nonsense.
     * @param sensor        grades each reading, which is not the same judgement for every sensor:
     *                      the water level switch calls a surfaced float critical whether or not
     *                      the aquarium configures a band for it.
     * @param thresholdBand the band this aquarium sets for that sensor, or null when it sets none,
     *                      in which case every reading is in range because there is no range.
     */
    @NonNull
    public static PeriodStatistics of(@NonNull List<SensorReading> buckets,
                                      @NonNull AquariumSensor sensor,
                                      @Nullable ThresholdBand thresholdBand) {
        int[] countsByStatus = new int[SensorStatus.values().length];
        int readingCount = 0;
        long downtimeSeconds = 0L;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double total = 0d;

        for (int i = 0; i < buckets.size(); i++) {
            SensorReading bucket = buckets.get(i);

            if (bucket.isOffline()) {
                // A gap marker is stamped at the moment the board came back, and one of them
                // stands for the whole outage however long it ran. So the outage is the stretch
                // from the bucket before it - the last thing committed before the board went
                // quiet - up to the marker itself.
                //
                // A period that opens on a marker has no bucket before it. The board was already
                // away when the window began and nothing is known about how long for, which is
                // also why the span below starts at this marker rather than before it.
                if (i > 0) {
                    downtimeSeconds += Math.max(0L, bucket.getTimestampSeconds()
                            - buckets.get(i - 1).getTimestampSeconds());
                }
                continue;
            }

            double value = bucket.getValue();
            readingCount++;
            total += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
            countsByStatus[sensor.statusFor(value, thresholdBand).ordinal()]++;
        }

        // Measured between the buckets that exist rather than over the window's nominal length. A
        // period fills a slot at a time, so a board an hour into its first day has committed a
        // handful of last_1d buckets and the rest of that window has not happened yet. Counting
        // the slots it has not reached as time the board was missing would report a tank running
        // perfectly as 4% up.
        long spanSeconds = buckets.isEmpty() ? 0L
                : buckets.get(buckets.size() - 1).getTimestampSeconds()
                        - buckets.get(0).getTimestampSeconds();

        return new PeriodStatistics(
                buckets.size(),
                readingCount,
                Math.max(0L, spanSeconds),
                downtimeSeconds,
                readingCount == 0 ? 0d : min,
                readingCount == 0 ? 0d : max,
                readingCount == 0 ? 0d : total / readingCount,
                countsByStatus);
    }

    /**
     * Whether the period holds any buckets at all. False only for a window the board has committed
     * nothing to, which is not the same as one it spent offline: that one has buckets in it and an
     * uptime worth reporting. This one has nothing to have been up or down for.
     */
    public boolean hasBuckets() {
        return this.bucketCount > 0;
    }

    /**
     * Whether any bucket in the period carries a reading. False both for a window the board never
     * reached and for one it spent entirely offline, which are the same thing to the spread and the
     * distribution: there is nothing to report a minimum or a share of.
     */
    public boolean hasReadings() {
        return this.readingCount > 0;
    }

    /**
     * The share of the time the period covers that the board was reporting for, as a whole
     * percentage.
     *
     * <p>A share of time rather than of buckets, because the two are not the same thing and only
     * the first is uptime. One gap marker stands for an outage of any length - a board away for
     * ten seconds and one away for six hours each leave a single bucket behind - so counting
     * markers measures how many times the board dropped out, not how long it was gone for.
     *
     * <p>Both ends are held to what the buckets actually cover. The stretch before the oldest
     * bucket is not counted, since a period fills a slot at a time and the slots it has not
     * reached are time that has not happened rather than time the board was missing: a sensor that
     * has committed one reading and no gaps is up, and reads 100%.
     */
    public int getUptimePercent() {
        if (this.spanSeconds <= 0L) {
            // Fewer than two buckets, or all of them stamped at once: there is no stretch to have
            // been up or down over. What is on record is either a reading, and nothing missing
            // beside it, or an outage on its own.
            return this.readingCount > 0 ? 100 : 0;
        }
        long uptime = Math.max(0L, this.spanSeconds - this.downtimeSeconds);
        return Math.round(100f * uptime / this.spanSeconds);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double getMean() {
        return this.mean;
    }

    /**
     * The share of the period's readings in each of the three graded states, as whole percentages
     * summing to 100 - or three zeroes when the period holds no readings.
     *
     * <p>Rounding each share on its own is what would stop them summing: three shares that each
     * round down leave the row reading 99%. So each is floored and the points that leaves over are
     * handed to the shares that lost the most in the flooring, which is the standard way of
     * apportioning a remainder and puts the error where it is smallest.
     *
     * @param statuses the states to report, in the order the caller draws them.
     */
    @NonNull
    public int[] percentagesOf(@NonNull SensorStatus... statuses) {
        int[] percentages = new int[statuses.length];
        if (this.readingCount == 0) {
            return percentages;
        }

        long[] remainders = new long[statuses.length];
        int assigned = 0;
        for (int i = 0; i < statuses.length; i++) {
            int count = this.countsByStatus[statuses[i].ordinal()];
            percentages[i] = count * 100 / this.readingCount;
            remainders[i] = (long) count * 100 % this.readingCount;
            assigned += percentages[i];
        }

        // Never more than one point per share, since each was floored by less than a whole point.
        for (int point = 0; point < 100 - assigned; point++) {
            int largest = -1;
            for (int i = 0; i < statuses.length; i++) {
                if (remainders[i] >= 0 && (largest < 0 || remainders[i] > remainders[largest])) {
                    largest = i;
                }
            }
            if (largest < 0) {
                break;
            }
            percentages[largest]++;
            // Marks it as already topped up, and takes it out of the running for the next point.
            remainders[largest] = -1;
        }
        return percentages;
    }
}
