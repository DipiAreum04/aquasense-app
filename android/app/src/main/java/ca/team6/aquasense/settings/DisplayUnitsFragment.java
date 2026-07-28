package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class DisplayUnitsFragment extends Fragment {

    // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    @SuppressWarnings("FieldCanBeLocal") 
    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

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

        RadioGroup rgTemp        = view.findViewById(R.id.rgTempUnit);
        SwitchCompat switchDark  = view.findViewById(R.id.switchDarkMode);
        SwitchCompat switch24h   = view.findViewById(R.id.switch24HourClock);

        repo.loadSettings(s -> {
            rgTemp.check("C".equals(s.tempUnit) ? R.id.rbCelsius : R.id.rbFahrenheit);
            switchDark.setChecked(s.darkMode);
            switch24h.setChecked(s.use24HourClock);
        });

        // Dashboard temperature unit reads this preference.
        rgTemp.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.rbCelsius) ? "C" : "F";
            prefs.updateField(SettingsRepository.KEY_TEMP_UNIT, unit);
        });

        switchDark.setOnCheckedChangeListener((b, checked) ->
                prefs.setDarkModeEnabled(checked));
        // TODO: Apply 24-hour clock formatting where times are shown.
        switch24h.setOnCheckedChangeListener((b, checked) ->
                prefs.updateField(SettingsRepository.KEY_24H_CLOCK, checked));
    }
}
