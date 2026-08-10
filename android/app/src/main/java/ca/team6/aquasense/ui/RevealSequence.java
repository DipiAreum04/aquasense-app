package ca.team6.aquasense.ui;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

/**
 * Fades a page's elements in one behind the other, each rising slightly as it appears.
 *
 * <p>Used by the first-installation wizard's welcome and completion pages, which are read rather
 * than operated: staggering the reveal walks the eye down the page in the order it is meant to be
 * read, instead of dropping the whole thing at once.
 */
public final class RevealSequence {

    private static final long DURATION_MS = 420L;
    private static final long STAGGER_MS = 90L;
    // How far below its resting place each element starts, in dp.
    private static final float RISE_DP = 16f;

    private RevealSequence() {
    }

    /**
     * Runs the reveal over {@code views}, in the order given.
     *
     * <p>Only call this on a first showing. A rotation re-creates the view with the page already
     * read, and replaying the reveal there reads as the screen reloading.
     */
    public static void play(@NonNull View... views) {
        if (views.length == 0) {
            return;
        }
        float rise = RISE_DP * views[0].getResources().getDisplayMetrics().density;

        long delay = 0L;
        for (View view : views) {
            view.setAlpha(0f);
            view.setTranslationY(rise);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator());
            delay += STAGGER_MS;
        }
    }
}
