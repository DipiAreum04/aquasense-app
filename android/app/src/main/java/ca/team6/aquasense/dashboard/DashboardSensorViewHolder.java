package ca.team6.aquasense.dashboard;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import ca.team6.aquasense.R;

public class DashboardSensorViewHolder extends RecyclerView.ViewHolder {
    MaterialCardView sensorCard;
    ImageView sensorTitleIcon;
    TextView sensorName;
    ImageView sensorTooltipIcon;
    LinearLayout sensorStatusPill;
    ImageView sensorStatusIcon;
    TextView sensorStatusText;
    TextView sensorValue;
    TextView sensorUnit;

    public DashboardSensorViewHolder(@NonNull View view) {
        super(view);

        this.sensorCard = (MaterialCardView) view;
        this.sensorTitleIcon = view.findViewById(R.id.sensorTitleIcon);
        this.sensorName = view.findViewById(R.id.sensorName);
        this.sensorTooltipIcon = view.findViewById(R.id.sensorTooltipIcon);
        this.sensorStatusPill = view.findViewById(R.id.sensorStatusPill);
        this.sensorStatusIcon = view.findViewById(R.id.sensorStatusIcon);
        this.sensorStatusText = view.findViewById(R.id.sensorStatusText);
        this.sensorValue = view.findViewById(R.id.sensorValue);
        this.sensorUnit = view.findViewById(R.id.sensorUnit);
    }
}
