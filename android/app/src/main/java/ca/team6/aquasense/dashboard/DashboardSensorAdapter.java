package ca.team6.aquasense.dashboard;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;

public class DashboardSensorAdapter extends RecyclerView.Adapter<DashboardSensorViewHolder> {
    private final FragmentManager fragmentManager;
    private final List<AquariumSensor> sensors;

    public DashboardSensorAdapter(FragmentManager fragmentManager, List<AquariumSensor> sensors) {
        this.fragmentManager = fragmentManager;
        this.sensors = new ArrayList<>(sensors);
    }

    // Replaces the card list after the order / visibility preference changes.
    // The list is at most four cards, so a full rebind is cheaper than diffing it.
    @SuppressLint("NotifyDataSetChanged")
    public void setSensors(List<AquariumSensor> updated) {
        sensors.clear();
        sensors.addAll(updated);
        notifyDataSetChanged();
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

        holder.sensorTitleIcon.setImageResource(sensor.getTitleIconResId());
        holder.sensorName.setText(sensor.getNameResId());
        holder.sensorTooltipIcon.setImageResource(R.drawable.info_24px);
        holder.sensorStatusIcon.setImageResource(sensor.getStatusIconResId());
        holder.sensorStatusText.setText(sensor.getStatusTextResId());
        holder.sensorStatusText.setTextColor(
                ContextCompat.getColor(
                        holder.sensorStatusText.getContext(),
                        sensor.getStatusColorResId()
                )
        );
        holder.sensorValue.setText(sensor.getValue());
        holder.sensorUnit.setText(sensor.getUnitResId());

        holder.sensorTooltipIcon.setOnClickListener(v -> {
            SensorInfoBottomSheet sheet = SensorInfoBottomSheet.from(sensor);
            sheet.show(fragmentManager, "sensor_info");
        });
    }
    
    @Override
    public int getItemCount() {
        return sensors.size();
    }

    // Rebinds a single sensor card after changes (e.g. after the temperature unit preference changes)
    public void notifySensorChanged(AquariumSensor sensor) {
        int index = sensors.indexOf(sensor);
        if (index != -1) {
            notifyItemChanged(index);
        }
    }
}
