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

/**
 * The list of aquariums that drops out of the analytics page's selector card.
 *
 * <p>A dropdown rather than a dialog because of what the choice is: the card names the aquarium the
 * page is reading, and the list is the other answers it could give. Anchored under the card and cut
 * to its width, it reads as that card opening. A dialog would take over the screen to ask a
 * question the card is already asking.
 */
public class AquariumDropdown {

    public interface OnAquariumPicked {
        void onAquariumPicked(@NonNull Aquarium aquarium);
    }

    /**
     * The chevron's resting and open angles. It is drawn from @drawable/ic_chevron_right, so
     * "pointing down" is already a quarter turn, and opening carries it round to pointing up.
     */
    private static final float CHEVRON_CLOSED_DEGREES = 90f;
    private static final float CHEVRON_OPEN_DEGREES = 270f;
    private static final long CHEVRON_TURN_MS = 150L;

    private final View anchor;
    private final View chevron;
    private final OnAquariumPicked listener;

    /**
     * @param anchor   the selector card. The dropdown hangs off its bottom edge and takes its
     *                 width, so it has to be the card itself rather than anything inside it.
     * @param chevron  turned over while the list is open, so the card says whether it is.
     */
    public AquariumDropdown(@NonNull View anchor,
                            @NonNull View chevron,
                            @NonNull OnAquariumPicked listener) {
        this.anchor = anchor;
        this.chevron = chevron;
        this.listener = listener;
    }

    /**
     * Drops the list open, with the aquarium being read already ticked.
     *
     * <p>Built per showing rather than kept between them: the aquariums are a live subscription and
     * the active one changes from this very list, so a window held open across those would be
     * offering a set that has moved on.
     *
     * @param activeAquariumId the aquarium the page is reading. Null, or an ID not in the list,
     *                         simply leaves nothing ticked.
     */
    public void show(@NonNull List<Aquarium> aquariums, @Nullable String activeAquariumId) {
        if (aquariums.isEmpty()) {
            return;
        }

        Context context = this.anchor.getContext();
        ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAnchorView(this.anchor);
        // Modal so a touch outside closes it and the back gesture is caught by the list rather
        // than by the page behind it.
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
        // Also runs when the list is dismissed by touching away from it, which is the case the
        // chevron would otherwise be left pointing the wrong way by.
        popup.setOnDismissListener(() -> this.turnChevron(CHEVRON_CLOSED_DEGREES));

        this.turnChevron(CHEVRON_OPEN_DEGREES);
        popup.show();
    }

    private void turnChevron(float degrees) {
        this.chevron.animate().rotation(degrees).setDuration(CHEVRON_TURN_MS).start();
    }

    /**
     * Draws the rows. The adapter holds the names rather than the aquariums, so the row's label is
     * the one thing it does not have to fill in; all it adds is which row carries the tick.
     */
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
            // INVISIBLE rather than GONE: the tick keeps its place on every row, so the names do
            // not shuffle sideways as the ticked one moves.
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
