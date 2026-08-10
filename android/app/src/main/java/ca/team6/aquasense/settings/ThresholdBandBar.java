package ca.team6.aquasense.settings;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.ThresholdBand;

/**
 * Sizes the five segments of a threshold bar in proportion to the real band widths, so a set of
 * bounds with little headroom shows a visibly narrower safe stretch.
 *
 * <p>Shared by the template cards, which draw a band the user is choosing, and the Water Parameters
 * screen, which draws the one they are editing. Any layout holding the five segment IDs can be
 * passed in; the geometry lives here so the two screens cannot drift apart on what a band looks
 * like.
 */
public final class ThresholdBandBar {

    // The critical bands run to infinity, so they have no real width. These caps are a fraction of
    // the bounded span, which keeps them looking consistent.
    private static final float CRITICAL_CAP_FRACTION = 0.15f;

    private ThresholdBandBar() {}

    public static void apply(@NonNull View container, @NonNull ThresholdBand band) {
        float warningLow = (float) (band.getSafeLow() - band.getWarnLow());
        float safe = (float) (band.getSafeHigh() - band.getSafeLow());
        float warningHigh = (float) (band.getWarnHigh() - band.getSafeHigh());
        float bounded = warningLow + safe + warningHigh;

        // Guard against a band set where every bound coincides, which would leave the bar with no
        // weight at all.
        float cap = bounded > 0 ? bounded * CRITICAL_CAP_FRACTION : 1f;

        setWeight(container, R.id.segmentCriticalLow, cap);
        setWeight(container, R.id.segmentWarningLow, warningLow);
        setWeight(container, R.id.segmentSafe, safe);
        setWeight(container, R.id.segmentWarningHigh, warningHigh);
        setWeight(container, R.id.segmentCriticalHigh, cap);
    }

    private static void setWeight(View container, int segmentId, float weight) {
        View segment = container.findViewById(segmentId);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) segment.getLayoutParams();
        params.weight = weight;
        segment.setLayoutParams(params);
    }
}
