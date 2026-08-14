package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.model.SensorReading;

final class BucketSeries {

    private BucketSeries() {}

    @NonNull
    static List<List<Entry>> split(@NonNull List<SensorReading> buckets, float secondsPerXUnit) {
        if (buckets.isEmpty()) {
            return Collections.emptyList();
        }

        long latestSeconds = buckets.get(buckets.size() - 1).getTimestampSeconds();
        List<List<Entry>> runs = new ArrayList<>();
        List<Entry> run = new ArrayList<>();
        Float lastValue = null;

        for (SensorReading bucket : buckets) {
            float x = (bucket.getTimestampSeconds() - latestSeconds) / secondsPerXUnit;

            if (!bucket.isOffline()) {
                lastValue = (float) bucket.getValue();
                run.add(new Entry(x, lastValue));
                continue;
            }

            if (!run.isEmpty()) {
                runs.add(run);
            }
            run = new ArrayList<>();
            if (lastValue != null) {
                run.add(new Entry(x, lastValue));
            }
        }

        if (!run.isEmpty()) {
            runs.add(run);
        }
        return runs;
    }
}
