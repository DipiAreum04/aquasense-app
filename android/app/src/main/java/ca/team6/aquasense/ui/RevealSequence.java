package ca.team6.aquasense.ui;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

public final class RevealSequence {

    private static final long DURATION_MS = 420L;
    private static final long STAGGER_MS = 90L;
    private static final float RISE_DP = 16f;

    private RevealSequence() {
    }

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
