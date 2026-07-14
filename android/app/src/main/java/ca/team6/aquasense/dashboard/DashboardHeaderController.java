package ca.team6.aquasense.dashboard;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumBoardStatus;
import ca.team6.aquasense.model.AquariumSensorsStatus;

public class DashboardHeaderController {
    private final TextView aquariumTitleIcon;
    private final TextView aquariumTitleText;
    private final ImageView aquariumBoardStatusIcon;
    private final TextView aquariumBoardStatusText;

    public DashboardHeaderController(@NonNull View view) {
        this.aquariumTitleIcon = view.findViewById(R.id.aquariumTitleIcon);
        this.aquariumTitleText = view.findViewById(R.id.aquariumTitleText);
        this.aquariumBoardStatusIcon = view.findViewById(R.id.aquariumBoardStatusIcon);
        this.aquariumBoardStatusText = view.findViewById(R.id.aquariumBoardStatusText);
    }

    public void setDashboardHeaderTitle(String aquariumTitle) {
        this.aquariumTitleText.setText(aquariumTitle);
    }

    public void setDashboardSensorsStatus(int score) {
        this.aquariumTitleIcon.setText(AquariumSensorsStatus.forScore(score));
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
}
