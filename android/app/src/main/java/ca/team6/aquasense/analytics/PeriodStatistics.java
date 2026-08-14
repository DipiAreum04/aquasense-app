package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

public final class PeriodStatistics {

    private final int bucketCount;
    private final int readingCount;
    private final long spanSeconds;
    private final long downtimeSeconds;
    private final double min;
    private final double max;
    private final double mean;
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

    public boolean hasBuckets() {
        return this.bucketCount > 0;
    }

    public boolean hasReadings() {
        return this.readingCount > 0;
    }

    public int getUptimePercent() {
        if (this.spanSeconds <= 0L) {
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
            remainders[largest] = -1;
        }
        return percentages;
    }
}
