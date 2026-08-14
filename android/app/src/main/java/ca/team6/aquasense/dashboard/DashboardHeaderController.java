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
import ca.team6.aquasense.logging.ScopedLogger;
import ca.team6.aquasense.aquarium.WaterType;

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
        if (this.shownStatus == aquariumStatus) {
            return;
        }

        AquariumStatus previousStatus = this.shownStatus;
        this.shownStatus = aquariumStatus;

        if (this.statusTransition != null) {
            this.statusTransition.cancel();
            this.statusTransition = null;
        }

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
            this.dashboardHeaderGradientView.invalidate();
        }
    }

    private static int blend(Context context, int fromColorResId, int toColorResId, float fraction) {
        return (int) ARGB.evaluate(
                fraction,
                ContextCompat.getColor(context, fromColorResId),
                ContextCompat.getColor(context, toColorResId));
    }

    public void cancelStatusTransition() {
        if (this.statusTransition != null) {
            this.statusTransition.cancel();
            this.statusTransition = null;
        }
    }

    public void setDashboardLastUpdated(CharSequence lastUpdated) {
        this.aquariumSensorsStatusUpdated.setText(lastUpdated);
    }

    public void setOnClickAquariumSelector(View.OnClickListener listener) {
        this.aquariumSelector.setOnClickListener(listener);
    }
}
