package ca.team6.aquasense.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import ca.team6.aquasense.R;

/**
 * Fills in the {@code view_wizard_progress} strip on a first-installation screen.
 *
 * <p>The wizard walks through two screens that also exist outside it: the add-aquarium form and
 * the pairing flow, both reachable from the dashboard once the app is set up. Each includes the
 * strip hidden, and only the first-run route calls {@link #show} on it, so neither screen carries
 * a step count when it is opened on its own.
 */
public final class WizardProgress {

    /** Set up the aquarium, mount the hardware, then pair the hub. */
    public static final int TOTAL_STEPS = 3;

    private WizardProgress() {
    }

    /**
     * Reveals the strip and reports {@code step} of {@link #TOTAL_STEPS}.
     *
     * @param root a view within the hierarchy that included the strip
     */
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

    /** As {@link #show(View, int)}, for a strip included in an activity's own layout. */
    public static void show(@NonNull Activity activity, int step) {
        show(activity.findViewById(android.R.id.content), step);
    }

    private static int segmentFor(int step, int segment) {
        return step >= segment
                ? R.drawable.bg_wizard_progress_done
                : R.drawable.bg_wizard_progress_todo;
    }
}
