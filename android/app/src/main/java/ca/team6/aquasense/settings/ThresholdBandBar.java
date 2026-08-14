package ca.team6.aquasense.settings;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import ca.team6.aquasense.R;
import ca.team6.aquasense.aquarium.ThresholdBand;

public final class ThresholdBandBar {

    private static final float CRITICAL_CAP_FRACTION = 0.15f;

    private ThresholdBandBar() {}

    public static void apply(@NonNull View container, @NonNull ThresholdBand band) {
        float warningLow = (float) (band.getSafeLow() - band.getWarnLow());
        float safe = (float) (band.getSafeHigh() - band.getSafeLow());
        float warningHigh = (float) (band.getWarnHigh() - band.getSafeHigh());
        float bounded = warningLow + safe + warningHigh;

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
