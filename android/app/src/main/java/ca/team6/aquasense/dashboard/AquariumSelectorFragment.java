package ca.team6.aquasense.dashboard;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.aquarium.AquariumRepository;
import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.aquarium.WaterType;
import ca.team6.aquasense.ui.FragmentToolbar;

public class AquariumSelectorFragment extends Fragment {

    private AquariumRepository aquariumRepository;

    private final AquariumRepository.AquariumsObserver aquariumsObserver = this::showAquariums;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aquarium_selector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentToolbar.setup(this, view, R.id.toolbar_aquarium_selector);

        View btnAdd = view.findViewById(R.id.btnAddNewAquarium);
        btnAdd.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_aquariumSelectorFragment_to_addAquariumFragment));

        aquariumRepository = AquariumRepository.getInstance(requireContext());
        aquariumRepository.addObserver(aquariumsObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        aquariumRepository.removeObserver(aquariumsObserver);
    }

    private void showAquariums(@NonNull List<Aquarium> aquariums) {
        View view = getView();
        if (view == null) {
            return;
        }
        populateAquariumCards(view.findViewById(R.id.aquariumListContainer), aquariums);
    }

    private void populateAquariumCards(LinearLayout container, @NonNull List<Aquarium> aquariums) {
        container.removeAllViews();

        if (aquariums.isEmpty()) {
            if (aquariumRepository.isLoaded()) {
                container.addView(buildEmptyView());
            }
            return;
        }

        String activeAquariumId = aquariumRepository.getActiveAquariumId();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Aquarium aquarium : aquariums) {
            boolean isActive = aquarium.getId().equals(activeAquariumId);

            View card = inflater.inflate(R.layout.item_aquarium_card, container, false);

            TextView titleView = card.findViewById(R.id.tvAquariumName);
            titleView.setText(aquarium.getName());

            bindAquariumIcon(card.findViewById(R.id.ivAquariumIcon), aquarium);

            bindWaterTypeBadge(card.findViewById(R.id.tvAquariumStatus), aquarium);

            TextView activeBadge = card.findViewById(R.id.tvActiveBadge);
            activeBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);

            ImageView deleteButton = card.findViewById(R.id.btnDeleteAquarium);
            deleteButton.setOnClickListener(v -> confirmDeleteAquarium(aquarium));

            card.setOnClickListener(v -> {
                aquariumRepository.setActiveAquariumId(aquarium.getId());
                Toast.makeText(getContext(),
                        getString(R.string.toast_selected_aquarium, aquarium.getName()),
                        Toast.LENGTH_SHORT).show();
                Navigation.findNavController(v).popBackStack();
            });

            container.addView(card);
        }
    }

    private TextView buildEmptyView() {
        TextView emptyView = new TextView(requireContext());
        emptyView.setText(R.string.no_aquariums);
        emptyView.setTextSize(14f);
        emptyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500));
        int paddingPx = dpToPx(32);
        emptyView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        return emptyView;
    }

    private String waterTypeLabel(@NonNull Aquarium aquarium) {
        WaterType waterType = WaterType.fromKey(aquarium.getWaterType());
        return waterType == null ? aquarium.getWaterType() : getString(waterType.getLabelResId());
    }

    private void bindWaterTypeBadge(@NonNull TextView badge, @NonNull Aquarium aquarium) {
        badge.setText(waterTypeLabel(aquarium));
        badge.setBackgroundResource(
                WaterType.fromKey(aquarium.getWaterType()) == WaterType.SALTWATER
                        ? R.drawable.bg_water_badge_saltwater
                        : R.drawable.bg_water_badge_freshwater);
    }

    private void bindAquariumIcon(ImageView tile, @NonNull Aquarium aquarium) {
        boolean saltwater = WaterType.fromKey(aquarium.getWaterType()) == WaterType.SALTWATER;

        tile.setImageResource(
                saltwater ? R.drawable.aquarium_saltwater : R.drawable.aquarium_freshwater);
        tile.getBackground().mutate().setTint(ContextCompat.getColor(requireContext(), R.color.aquarium_icon_bg));
    }

    private void confirmDeleteAquarium(@NonNull Aquarium aquarium) {
        if (!aquariumRepository.canRemoveAquarium()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_min_aquarium_title)
                    .setMessage(R.string.dialog_min_aquarium_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_AquaSense_MaterialAlertDialog_Danger)
                .setTitle(R.string.delete_aquarium_title)
                .setMessage(getString(R.string.delete_aquarium_message, aquarium.getName()))
                .setNegativeButton(R.string.delete_aquarium_cancel, null)
                .setPositiveButton(R.string.delete_aquarium_confirm, (dialog, which) ->
                        aquariumRepository.removeAquarium(aquarium, new AquariumRepository.WriteCallback() {
                            @Override
                            public void onSuccess() {
                                toastIfVisible(getString(R.string.delete_aquarium_done, aquarium.getName()));
                            }

                            @Override
                            public void onError() {
                                toastIfVisible(getString(R.string.delete_aquarium_failed));
                            }
                        }))
                .show();
    }

    private void toastIfVisible(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

}
