package ca.team6.aquasense.analytics;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;

public class AquariumDropdown {

    public interface OnAquariumPicked {
        void onAquariumPicked(@NonNull Aquarium aquarium);
    }

    private static final float CHEVRON_CLOSED_DEGREES = 90f;
    private static final float CHEVRON_OPEN_DEGREES = 270f;
    private static final long CHEVRON_TURN_MS = 150L;

    private final View anchor;
    private final View chevron;
    private final OnAquariumPicked listener;

    public AquariumDropdown(@NonNull View anchor,
                            @NonNull View chevron,
                            @NonNull OnAquariumPicked listener) {
        this.anchor = anchor;
        this.chevron = chevron;
        this.listener = listener;
    }

    public void show(@NonNull List<Aquarium> aquariums, @Nullable String activeAquariumId) {
        if (aquariums.isEmpty()) {
            return;
        }

        Context context = this.anchor.getContext();
        ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAnchorView(this.anchor);
        popup.setModal(true);
        popup.setWidth(this.anchor.getWidth());
        popup.setVerticalOffset(context.getResources()
                .getDimensionPixelSize(R.dimen.analytics_dropdown_offset));
        popup.setBackgroundDrawable(
                ContextCompat.getDrawable(context, R.drawable.bg_analytics_dropdown));
        popup.setAdapter(new AquariumAdapter(context, aquariums, activeAquariumId));

        popup.setOnItemClickListener((parent, view, position, id) -> {
            popup.dismiss();
            this.listener.onAquariumPicked(aquariums.get(position));
        });
        popup.setOnDismissListener(() -> this.turnChevron(CHEVRON_CLOSED_DEGREES));

        this.turnChevron(CHEVRON_OPEN_DEGREES);
        popup.show();
    }

    private void turnChevron(float degrees) {
        this.chevron.animate().rotation(degrees).setDuration(CHEVRON_TURN_MS).start();
    }

    private static final class AquariumAdapter extends ArrayAdapter<String> {

        private final int checkedPosition;

        AquariumAdapter(@NonNull Context context,
                        @NonNull List<Aquarium> aquariums,
                        @Nullable String activeAquariumId) {
            super(context, R.layout.item_analytics_aquarium,
                    R.id.analyticsAquariumOptionName, namesOf(aquariums));
            this.checkedPosition = indexOf(aquariums, activeAquariumId);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            row.findViewById(R.id.analyticsAquariumOptionCheck).setVisibility(
                    position == this.checkedPosition ? View.VISIBLE : View.INVISIBLE);
            return row;
        }

        @NonNull
        private static List<String> namesOf(@NonNull List<Aquarium> aquariums) {
            List<String> names = new ArrayList<>(aquariums.size());
            for (Aquarium aquarium : aquariums) {
                names.add(aquarium.getName());
            }
            return names;
        }

        private static int indexOf(@NonNull List<Aquarium> aquariums,
                                   @Nullable String aquariumId) {
            for (int i = 0; i < aquariums.size(); i++) {
                if (aquariums.get(i).getId().equals(aquariumId)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
