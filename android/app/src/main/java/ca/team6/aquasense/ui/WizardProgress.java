package ca.team6.aquasense.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import ca.team6.aquasense.R;

public final class WizardProgress {

    public static final int TOTAL_STEPS = 3;

    private WizardProgress() {
    }

    public static void show(@NonNull View root, int step) {
        View strip = root.findViewById(R.id.wizardProgress);
        if (strip == null) {
            return;
        }
        strip.setVisibility(View.VISIBLE);

        ((TextView) strip.findViewById(R.id.tvWizardStepLabel))
                .setText(root.getContext().getString(R.string.setup_step_label, step, TOTAL_STEPS));

        strip.findViewById(R.id.wizardSegment1).setBackgroundResource(segmentFor(step, 1));
        strip.findViewById(R.id.wizardSegment2).setBackgroundResource(segmentFor(step, 2));
        strip.findViewById(R.id.wizardSegment3).setBackgroundResource(segmentFor(step, 3));
    }

    public static void show(@NonNull Activity activity, int step) {
        show(activity.findViewById(android.R.id.content), step);
    }

    private static int segmentFor(int step, int segment) {
        return step >= segment
                ? R.drawable.bg_wizard_progress_done
                : R.drawable.bg_wizard_progress_todo;
    }
}
