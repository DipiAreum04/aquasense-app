package ca.team6.aquasense.dashboard;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumBoardStatus;
import ca.team6.aquasense.model.AquariumSensorsStatus;
import ca.team6.aquasense.model.ScopedLogger;

public class DashboardHeaderController {
    private final LinearLayout aquariumSelector;
    private final TextView aquariumTitleText;
    private final ImageView aquariumBoardStatusIcon;
    private final TextView aquariumBoardStatusText;
    private final TextView aquariumSensorsStatusIcon;
    private final TextView aquariumSensorsStatusHeading;
    private final TextView aquariumSensorsStatusDescription;
    private final GradientDrawable dashboardHeaderGradient;

    public DashboardHeaderController(@NonNull View view) {
        this.aquariumSelector = view.findViewById(R.id.aquariumSelector);
        this.aquariumTitleText = view.findViewById(R.id.aquariumTitleText);
        this.aquariumBoardStatusIcon = view.findViewById(R.id.aquariumBoardStatusIcon);
        this.aquariumBoardStatusText = view.findViewById(R.id.aquariumBoardStatusText);
        this.aquariumSensorsStatusIcon = view.findViewById(R.id.aquariumSensorsStatusIcon);
        this.aquariumSensorsStatusHeading = view.findViewById(R.id.aquariumSensorsStatusHeading);
        this.aquariumSensorsStatusDescription = view.findViewById(R.id.aquariumSensorsStatusDescription);

        Drawable potentialGradientDrawable = view.findViewById(R.id.dashboardHeaderGradient).getBackground();
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

    public void setDashboardSensorsStatus(int score) {
        AquariumSensorsStatus aquariumSensorsStatus = AquariumSensorsStatus.forScore(score);
        this.aquariumSensorsStatusIcon.setText(aquariumSensorsStatus.iconResId);
        this.aquariumSensorsStatusHeading.setText(aquariumSensorsStatus.headingResId);
        this.aquariumSensorsStatusDescription.setText(aquariumSensorsStatus.descriptionResId);

        if (this.dashboardHeaderGradient != null) {
            this.dashboardHeaderGradient.setColors(new int[]{
                    ContextCompat.getColor(
                            this.aquariumSensorsStatusIcon.getContext(),
                            aquariumSensorsStatus.gradientStartColorResId
                    ),
                    ContextCompat.getColor(
                            this.aquariumSensorsStatusIcon.getContext(),
                            aquariumSensorsStatus.gradientEndColorResId
                    ),
            });
        }
    }

    public void setDashboardBoardStatus(AquariumBoardStatus aquariumBoardStatus) {
        this.aquariumBoardStatusIcon.setImageResource(aquariumBoardStatus.iconResourceId);
        this.aquariumBoardStatusText.setText(aquariumBoardStatus.textResourceId);
        this.aquariumBoardStatusText.setTextColor(
                ContextCompat.getColor(
                        this.aquariumBoardStatusText.getContext(),
                        aquariumBoardStatus.colorResourceId
                )
        );
    }

    public void setOnClickAquariumSelector(View.OnClickListener listener) {
        this.aquariumSelector.setOnClickListener(listener);
    }
}
