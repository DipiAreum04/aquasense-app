package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.aquarium.templates.AquariumTemplate;
import ca.team6.aquasense.aquarium.templates.BuiltInTemplates;

public class AquariumTemplatesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aquarium_templates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout container = view.findViewById(R.id.containerTemplates);
        LayoutInflater inflater = getLayoutInflater();

        for (AquariumTemplate template : BuiltInTemplates.all()) {
            container.addView(
                    AquariumTemplateCardBinder.createCard(inflater, container, template));
        }
    }
}
