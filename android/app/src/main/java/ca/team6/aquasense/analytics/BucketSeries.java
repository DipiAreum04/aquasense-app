package ca.team6.aquasense.analytics;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.model.SensorReading;

/**
 * Turns a period's buckets into the runs of points a chart can draw as unbroken lines.
 *
 * <p>A bucket carrying the offline sentinel is not a reading. It is the board reporting that it
 * was away, and its timestamp is the moment it came back - one marker covers the whole outage,
 * however long it ran, and nothing is known about the stretch before it. So the line must not be
 * carried across one. Each marker ends the run before it and opens the next, and the new run
 * starts by echoing the last real value at the moment the board returned: the reading resumes
 * where it left off, while the break says that join was never measured.
 *
 * <p>Worked through the case it exists for - B00..B07 carry readings, B08 is a marker stamped 30
 * seconds after B07, B09 onwards carry readings again:
 *
 * <pre>
 *   run 1: B00 B01 B02 B03 B04 B05 B06 B07
 *   run 2: (B08's timestamp, B07's value) B09 B10 ...
 * </pre>
 *
 * <p>Nothing joins B07 to the echo, which is the gap the board actually reported.
 */
final class BucketSeries {

    private BucketSeries() {}

    /**
     * Splits buckets into runs of connected points, in the order given - which the caller is
     * expected to have already put oldest first, since a period's slots are a ring buffer and
     * their key order is not their time order.
     *
     * <p>x is measured from the newest bucket, so it runs from 0 at the right edge back through
     * negative values to roughly minus the window's length: the reading on the right is the current
     * one, and the graph reads backwards into the past from there.
     *
     * <p>Measured from something, rather than being the epoch second itself, because an
     * {@link Entry} holds x as a float, and up at epoch scale a float's steps are wider than two
     * minutes - whole buckets would collapse onto each other.
     *
     * @param secondsPerXUnit how much time one step along x stands for, which is the selected
     *                        period's own unit - see {@link AnalyticsPeriod}.
     */
    @NonNull
    static List<List<Entry>> split(@NonNull List<SensorReading> buckets, float secondsPerXUnit) {
        if (buckets.isEmpty()) {
            return Collections.emptyList();
        }

        // The last bucket is the one the period's cursor points at, so it is 0 - now.
        long latestSeconds = buckets.get(buckets.size() - 1).getTimestampSeconds();
        List<List<Entry>> runs = new ArrayList<>();
        List<Entry> run = new ArrayList<>();
        // Null until the first real reading. A period can open on a marker - a board that was
        // already away when the window began - and there is nothing to echo yet when it does.
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
