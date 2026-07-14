package ca.team6.aquasense.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumSensor;

public class DashboardSensorAdapter extends RecyclerView.Adapter<DashboardSensorViewHolder> {
    private final FragmentManager fragmentManager;
    private final List<AquariumSensor> sensors;

    public DashboardSensorAdapter(FragmentManager fragmentManager, List<AquariumSensor> sensors) {
        this.fragmentManager = fragmentManager;
        this.sensors = sensors;
    }

    @NonNull
    @Override
    public DashboardSensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sensor_card, parent, false);

        return new DashboardSensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardSensorViewHolder holder, int position) {
        AquariumSensor sensor = sensors.get(position);

        holder.sensorTitleIcon.setImageResource(sensor.titleIconResourceId);
        holder.sensorName.setText(sensor.nameResourceId);
        holder.sensorTooltipIcon.setImageResource(R.drawable.info_24px);
        holder.sensorStatusIcon.setImageResource(sensor.statusIconResourceId);
        holder.sensorStatusText.setText(sensor.statusTextResourceId);
        holder.sensorStatusText.setTextColor(
                ContextCompat.getColor(
                        holder.sensorStatusText.getContext(),
                        sensor.statusColorResourceId
                )
        );
        holder.sensorValue.setText(sensor.value);
        holder.sensorUnit.setText(sensor.unitResourceId);

        holder.sensorTooltipIcon.setOnClickListener(v -> {
            SensorInfoBottomSheet sheet = SensorInfoBottomSheet.from(sensor);
            sheet.show(fragmentManager, "sensor_info");
        });
    }
    
    @Override
    public int getItemCount() {
        return sensors.size();
    }
}
