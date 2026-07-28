package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.DissolvedSolidsSensor;
import ca.team6.aquasense.model.aquarium_sensors.PhLevelSensor;
import ca.team6.aquasense.model.aquarium_sensors.TemperatureSensor;
import ca.team6.aquasense.model.aquarium_sensors.WaterLevelSensor;

public class DisplayUnitsFragment extends Fragment {

    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

    private LinearLayout containerDashboardCards;

    // Card rows are driven by this list; index order is the saved display order.
    private final List<AquariumSensor> orderedSensors = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_display_units, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());

        RadioGroup rgTemp       = view.findViewById(R.id.rgTempUnit);
        RadioGroup rgPrecision  = view.findViewById(R.id.rgPrecision);
        RadioGroup rgThemeMode  = view.findViewById(R.id.rgThemeMode);
        SwitchCompat switch24h  = view.findViewById(R.id.switch24HourClock);
        containerDashboardCards = view.findViewById(R.id.containerDashboardCards);

        repo.loadSettings(s -> {
            rgTemp.check("C".equals(s.tempUnit) ? R.id.rbCelsius : R.id.rbFahrenheit);
            rgPrecision.check(precisionToId(s.readingPrecision));
            rgThemeMode.check(themeModeToId(s.themeMode));
            switch24h.setChecked(s.use24HourClock);
        });

        // Dashboard temperature unit reads this preference.
        rgTemp.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.rbCelsius) ? "C" : "F";
            prefs.updateField(SettingsRepository.KEY_TEMP_UNIT, unit);
        });

        // TODO: Wire into sensor cards and charts once firmware sends live numeric readings.
        // Call ReadingFormatter.format(context, sensor.getId(), value) at the point where a
        // raw reading is turned into card text.
        rgPrecision.setOnCheckedChangeListener((group, checkedId) ->
                prefs.updateField(SettingsRepository.KEY_READING_PRECISION,
                        idToPrecision(checkedId)));

        rgThemeMode.setOnCheckedChangeListener((group, checkedId) ->
                prefs.setThemeMode(idToThemeMode(checkedId)));

        // TODO: Apply 24-hour clock formatting where times are shown.
        switch24h.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_24H_CLOCK, checked));

        setUpDashboardCardRows();
    }


    // Dashboard card order and visibility
    private void setUpDashboardCardRows() {
        List<AquariumSensor> all = new ArrayList<>();
        all.add(new WaterLevelSensor());
        all.add(new TemperatureSensor());
        all.add(new DissolvedSolidsSensor());
        all.add(new PhLevelSensor());

        List<String> knownIds = new ArrayList<>();
        for (AquariumSensor sensor : all) {
            knownIds.add(sensor.getId());
        }

        orderedSensors.clear();
        for (String id : prefs.getSensorOrder(knownIds)) {
            for (AquariumSensor sensor : all) {
                if (sensor.getId().equals(id)) {
                    orderedSensors.add(sensor);
                    break;
                }
            }
        }
        renderDashboardCardRows();
    }

    // Rebuilds every row so the up/down buttons reflect the current position.
    private void renderDashboardCardRows() {
        containerDashboardCards.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < orderedSensors.size(); i++) {
            AquariumSensor sensor = orderedSensors.get(i);
            View row = inflater.inflate(
                    R.layout.item_dashboard_card_pref, containerDashboardCards, false);

            ImageView icon = row.findViewById(R.id.imgCardIcon);
            TextView name = row.findViewById(R.id.tvCardName);
            ImageButton moveUp = row.findViewById(R.id.btnCardMoveUp);
            ImageButton moveDown = row.findViewById(R.id.btnCardMoveDown);
            SwitchCompat visible = row.findViewById(R.id.switchCardVisible);

            icon.setImageResource(sensor.getTitleIconResId());
            name.setText(sensor.getNameResId());

            boolean isFirst = (i == 0);
            boolean isLast = (i == orderedSensors.size() - 1);
            moveUp.setEnabled(!isFirst);
            moveUp.setAlpha(isFirst ? 0.3f : 1f);
            moveDown.setEnabled(!isLast);
            moveDown.setAlpha(isLast ? 0.3f : 1f);

            final int position = i;
            moveUp.setOnClickListener(v -> moveCard(position, position - 1));
            moveDown.setOnClickListener(v -> moveCard(position, position + 1));

            visible.setChecked(!prefs.isSensorHidden(sensor.getId()));
            visible.setOnCheckedChangeListener((b, checked) -> {
                // Hiding every card would leave the dashboard blank with no way back to it.
                if (!checked && countVisible() <= 1) {
                    b.setChecked(true);
                    Toast.makeText(requireContext(),
                            R.string.dashboard_card_last_visible, Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setSensorHidden(sensor.getId(), !checked);
            });

            containerDashboardCards.addView(row);
        }
    }

    private void moveCard(int from, int to) {
        if (to < 0 || to >= orderedSensors.size()) {
            return;
        }
        AquariumSensor moved = orderedSensors.remove(from);
        orderedSensors.add(to, moved);

        List<String> ids = new ArrayList<>();
        for (AquariumSensor sensor : orderedSensors) {
            ids.add(sensor.getId());
        }
        prefs.setSensorOrder(ids);
        renderDashboardCardRows();
    }

    private int countVisible() {
        int visible = 0;
        for (AquariumSensor sensor : orderedSensors) {
            if (!prefs.isSensorHidden(sensor.getId())) {
                visible++;
            }
        }
        return visible;
    }

    // Preference value <-> ID mapping

    private static int precisionToId(String precision) {
        return SettingsRepository.PRECISION_PRECISE.equals(precision)
                ? R.id.rbPrecisionPrecise
                : R.id.rbPrecisionStandard;
    }

    private static String idToPrecision(int checkedId) {
        return (checkedId == R.id.rbPrecisionPrecise)
                ? SettingsRepository.PRECISION_PRECISE
                : SettingsRepository.PRECISION_STANDARD;
    }

    private static int themeModeToId(String mode) {
        switch (mode) {
            case SettingsRepository.THEME_LIGHT:
                return R.id.rbThemeLight;
            case SettingsRepository.THEME_DARK:
                return R.id.rbThemeDark;
            default:
                return R.id.rbThemeSystem;
        }
    }

    private static String idToThemeMode(int checkedId) {
        if (checkedId == R.id.rbThemeLight) {
            return SettingsRepository.THEME_LIGHT;
        }
        if (checkedId == R.id.rbThemeDark) {
            return SettingsRepository.THEME_DARK;
        }
        return SettingsRepository.THEME_SYSTEM;
    }
}
