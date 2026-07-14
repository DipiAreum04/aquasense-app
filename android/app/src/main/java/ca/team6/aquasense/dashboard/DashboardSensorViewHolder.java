package ca.team6.aquasense.dashboard;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ca.team6.aquasense.R;

public class DashboardSensorViewHolder extends RecyclerView.ViewHolder {
    ImageView sensorTitleIcon;
    TextView sensorName;
    ImageView sensorTooltipIcon;
    ImageView sensorStatusIcon;
    TextView sensorStatusText;
    TextView sensorValue;
    TextView sensorUnit;

    public DashboardSensorViewHolder(@NonNull View view) {
        super(view);

        this.sensorTitleIcon = view.findViewById(R.id.sensorTitleIcon);
        this.sensorName = view.findViewById(R.id.sensorName);
        this.sensorTooltipIcon = view.findViewById(R.id.sensorTooltipIcon);
        this.sensorStatusIcon = view.findViewById(R.id.sensorStatusIcon);
        this.sensorStatusText = view.findViewById(R.id.sensorStatusText);
        this.sensorValue = view.findViewById(R.id.sensorValue);
        this.sensorUnit = view.findViewById(R.id.sensorUnit);
    }
}
