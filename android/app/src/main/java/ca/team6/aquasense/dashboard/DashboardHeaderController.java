package ca.team6.aquasense.dashboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumStatus;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.WaterType;

public class DashboardHeaderController {
    private static final long STATUS_TRANSITION_MS = 450L;
    private static final float WATER_BADGE_ICON_DP = 16f;
    private static final ArgbEvaluator ARGB = new ArgbEvaluator();

    private final LinearLayout aquariumSelector;
    private final TextView aquariumTitleText;
    private final TextView aquariumWaterTypeBadge;
    private final TextView maintenanceModeBadge;
    private final ImageView aquariumSensorsStatusIcon;
    private final TextView aquariumSensorsStatusHeading;
    private final TextView aquariumSensorsStatusDescription;
    private final TextView aquariumSensorsStatusUpdated;
    private final ImageView dashboardHeaderWave;
    private final View dashboardHeaderGradientView;
    private final GradientDrawable dashboardHeaderGradient;

    // The status currently on show, which is what a new one is compared against and faded from.
    @Nullable
    private AquariumStatus shownStatus;
    @Nullable
    private ValueAnimator statusTransition;

    public DashboardHeaderController(@NonNull View view) {
        this.aquariumSelector = view.findViewById(R.id.aquariumSelector);
        this.aquariumTitleText = view.findViewById(R.id.aquariumTitleText);
        this.aquariumWaterTypeBadge = view.findViewById(R.id.aquariumWaterTypeBadge);
        this.maintenanceModeBadge = view.findViewById(R.id.maintenanceModeBadge);
        applyWaterTypeBadgeIcon(this.aquariumWaterTypeBadge);
        this.aquariumSensorsStatusIcon = view.findViewById(R.id.aquariumSensorsStatusIcon);
        this.aquariumSensorsStatusHeading = view.findViewById(R.id.aquariumSensorsStatusHeading);
        this.aquariumSensorsStatusDescription = view.findViewById(R.id.aquariumSensorsStatusDescription);
        this.aquariumSensorsStatusUpdated = view.findViewById(R.id.aquariumSensorsStatusUpdated);
        this.dashboardHeaderWave = view.findViewById(R.id.dashboardHeaderWave);

        this.dashboardHeaderGradientView = view.findViewById(R.id.dashboardHeaderGradient);
        // mutate() so recolouring this header cannot reach any other view that inflated the same
        // drawable resource, since a drawable straight out of XML shares its constant state.
        Drawable potentialGradientDrawable = this.dashboardHeaderGradientView.getBackground().mutate();
        if (potentialGradientDrawable instanceof GradientDrawable) {
            this.dashboardHeaderGradient = (GradientDrawable) potentialGradientDrawable;
        } else {
            this.dashboardHeaderGradient = null;
            ScopedLogger.error("potentialGradientDrawable is not an instance of GradientDrawable.");
        }
    }

    public void setDashboardHeaderTitle(String aquariumTitle) {
        this.aquariumTitleText.setText(aquariumTitle);
    }

    public void setDashboardWaterType(@Nullable WaterType waterType) {
        if (waterType == null) {
            this.aquariumWaterTypeBadge.setVisibility(View.GONE);
            return;
        }

        this.aquariumWaterTypeBadge.setText(waterType.getLabelResId());
        this.aquariumWaterTypeBadge.setVisibility(View.VISIBLE);
    }

    /** Shows the active maintenance-mode countdown, or hides it when the aquarium is not snoozed. */
    public void setMaintenanceModeBadge(@Nullable CharSequence remainingLabel) {
        if (remainingLabel == null) {
            this.maintenanceModeBadge.setVisibility(View.GONE);
            return;
        }
        this.maintenanceModeBadge.setText(remainingLabel);
        this.maintenanceModeBadge.setVisibility(View.VISIBLE);
    }

    private static void applyWaterTypeBadgeIcon(@NonNull TextView badge) {
        Drawable icon = ContextCompat.getDrawable(badge.getContext(), R.drawable.water_drops_24px);
        if (icon == null) {
            return;
        }
        int size = Math.round(badge.getResources().getDisplayMetrics().density * WATER_BADGE_ICON_DP);
        icon.setBounds(0, 0, size, size);
        badge.setCompoundDrawablesRelative(icon, null, null, null);
    }

    public void setDashboardSensorsStatus(AquariumStatus aquariumStatus) {
        // Readings land about once a second and each one re-reports the status, so only an actual
        // change may start a transition. Without this the header would restart the fade endlessly.
        if (this.shownStatus == aquariumStatus) {
            return;
        }

        AquariumStatus previousStatus = this.shownStatus;
        this.shownStatus = aquariumStatus;

        if (this.statusTransition != null) {
            // Ends on the status it was heading for, which is where the new fade starts from.
            this.statusTransition.cancel();
            this.statusTransition = null;
        }

        // Nothing to fade from on the first bind, so the opening status is simply drawn.
        if (previousStatus == null) {
            applyStatusColors(aquariumStatus, aquariumStatus, 1f);
            applyStatusContent(aquariumStatus);
            setStatusContentAlpha(1f);
            return;
        }

        this.statusTransition = startStatusTransition(previousStatus, aquariumStatus);
    }

    private ValueAnimator startStatusTransition(AquariumStatus from, AquariumStatus to) {
        ValueAnimator transition = ValueAnimator.ofFloat(0f, 1f);
        transition.setDuration(STATUS_TRANSITION_MS);
        transition.setInterpolator(new AccelerateDecelerateInterpolator());
        transition.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private boolean contentSwapped;

            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                float fraction = (float) animation.getAnimatedValue();
                applyStatusColors(from, to, fraction);

                // Colours tween, but an icon and a line of text cannot: they cross-fade out to
                // the midpoint, swap while invisible, and come back in on the far side.
                if (!contentSwapped && fraction >= 0.5f) {
                    contentSwapped = true;
                    applyStatusContent(to);
                }
                setStatusContentAlpha(Math.abs(fraction * 2f - 1f));
            }
        });
        transition.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Also runs on cancel, which settles the header on `to` so a replacement fade has
                // a whole status to start from rather than a half-faded one.
                applyStatusColors(to, to, 1f);
                applyStatusContent(to);
                setStatusContentAlpha(1f);
            }
        });
        transition.start();
        return transition;
    }

    private void applyStatusContent(AquariumStatus aquariumStatus) {
        this.aquariumSensorsStatusIcon.setImageResource(aquariumStatus.iconResId);
        this.aquariumSensorsStatusHeading.setText(aquariumStatus.headingResId);
        this.aquariumSensorsStatusDescription.setText(aquariumStatus.descriptionResId);
    }

    private void setStatusContentAlpha(float alpha) {
        this.aquariumSensorsStatusIcon.setAlpha(alpha);
        this.aquariumSensorsStatusHeading.setAlpha(alpha);
        this.aquariumSensorsStatusDescription.setAlpha(alpha);
    }

    private void applyStatusColors(AquariumStatus from, AquariumStatus to, float fraction) {
        Context context = this.aquariumSensorsStatusIcon.getContext();

        int accent = blend(context, from.accentColorResId, to.accentColorResId, fraction);
        this.aquariumSensorsStatusIcon.setImageTintList(ColorStateList.valueOf(accent));
        this.aquariumSensorsStatusHeading.setTextColor(accent);

        this.dashboardHeaderWave.setImageTintList(ColorStateList.valueOf(
                blend(context, from.waveColorResId, to.waveColorResId, fraction)));

        if (this.dashboardHeaderGradient != null) {
            this.dashboardHeaderGradient.setColors(new int[]{
                    blend(context, from.gradientStartColorResId, to.gradientStartColorResId, fraction),
                    blend(context, from.gradientEndColorResId, to.gradientEndColorResId, fraction),
            });
            // The gradient is this ViewGroup's background, and the drawable invalidating itself
            // is not enough to get the group's display list re-recorded mid-animation: the
            // children fading on top redraw, the background behind them keeps its old colours
            // until something else dirties the group. Ask for that redraw directly.
            this.dashboardHeaderGradientView.invalidate();
        }
    }

    private static int blend(Context context, int fromColorResId, int toColorResId, float fraction) {
        return (int) ARGB.evaluate(
                fraction,
                ContextCompat.getColor(context, fromColorResId),
                ContextCompat.getColor(context, toColorResId));
    }

    /** Stops a fade in progress, so a torn-down header is not left driving detached views. */
    public void cancelStatusTransition() {
        if (this.statusTransition != null) {
            this.statusTransition.cancel();
            this.statusTransition = null;
        }
    }

    /** The moment of the newest reading, shown on its own line beneath the status description. */
    public void setDashboardLastUpdated(CharSequence lastUpdated) {
        this.aquariumSensorsStatusUpdated.setText(lastUpdated);
    }

    public void setOnClickAquariumSelector(View.OnClickListener listener) {
        this.aquariumSelector.setOnClickListener(listener);
    }
}
