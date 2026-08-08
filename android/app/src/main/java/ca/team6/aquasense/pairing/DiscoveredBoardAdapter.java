package ca.team6.aquasense.pairing;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ca.team6.aquasense.R;

/**
 * Renders the hubs a bluetooth scan has discovered.
 */
public class DiscoveredBoardAdapter extends ListAdapter<DiscoveredBoard, DiscoveredBoardAdapter.BoardViewHolder> {

    public enum RowState {

        IDLE,           // Discovered, not chosen

        SELECTED,       // The user selected this hub

        BUSY,           // The app is sending it credentials

        DONE            // The board received the credentials and came online   
    }

    public interface OnBoardClickListener {
        void onBoardClicked(@NonNull DiscoveredBoard board);
    }

    private static final DiffUtil.ItemCallback<DiscoveredBoard> DIFF =
            new DiffUtil.ItemCallback<DiscoveredBoard>() {
                @Override
                public boolean areItemsTheSame(@NonNull DiscoveredBoard oldBoard,
                                               @NonNull DiscoveredBoard newBoard) {
                    return oldBoard.equals(newBoard);
                }

                @Override
                public boolean areContentsTheSame(@NonNull DiscoveredBoard oldBoard,
                                                  @NonNull DiscoveredBoard newBoard) {
                    // Compared on the banding rather than the raw RSSI, so the row is only redrawn
                    // when the meter would actually look different.
                    return oldBoard.getSignalBars() == newBoard.getSignalBars();
                }
            };

    private final OnBoardClickListener clickListener;

    // The picked hub's address, or null while the user is still choosing.
    @Nullable
    private String selectedAddress;
    @NonNull
    private RowState selectedState = RowState.IDLE;

    public DiscoveredBoardAdapter(@NonNull OnBoardClickListener clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelected(@Nullable String address, @NonNull RowState state) {
        if (equalAddress(this.selectedAddress, address) && this.selectedState == state) {
            return;
        }
        this.selectedAddress = address;
        this.selectedState = state;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discovered_board, parent, false);
        return new BoardViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull BoardViewHolder holder, int position) {
        DiscoveredBoard board = getItem(position);
        boolean selected = equalAddress(this.selectedAddress, board.getAddress());
        holder.bind(board, selected ? this.selectedState : RowState.IDLE,
                this.selectedState == RowState.IDLE ? this.clickListener : null);
    }

    private static boolean equalAddress(@Nullable String left, @Nullable String right) {
        return left == null ? right == null : left.equals(right);
    }

    static class BoardViewHolder extends RecyclerView.ViewHolder {

        private final View row;
        private final TextView name;
        private final TextView subtitle;
        private final TextView action;
        private final ProgressBar progress;
        private final ImageView done;
        private final View signalMeter;
        private final View[] signalBars;

        BoardViewHolder(@NonNull View itemView) {
            super(itemView);
            this.row = itemView.findViewById(R.id.boardRow);
            this.name = itemView.findViewById(R.id.tvBoardName);
            this.subtitle = itemView.findViewById(R.id.tvBoardSubtitle);
            this.action = itemView.findViewById(R.id.tvBoardAction);
            this.progress = itemView.findViewById(R.id.boardProgress);
            this.done = itemView.findViewById(R.id.ivBoardDone);
            this.signalMeter = itemView.findViewById(R.id.boardSignalMeter);
            this.signalBars = new View[]{
                    itemView.findViewById(R.id.boardSignalBar1),
                    itemView.findViewById(R.id.boardSignalBar2),
                    itemView.findViewById(R.id.boardSignalBar3),
                    itemView.findViewById(R.id.boardSignalBar4),
            };
        }

        void bind(@NonNull DiscoveredBoard board,
                  @NonNull RowState state,
                  @Nullable OnBoardClickListener clickListener) {
            String advertised = board.getName();
            this.name.setText(advertised == null || advertised.isEmpty()
                    ? this.row.getContext().getString(R.string.pairing_board_unnamed)
                    : advertised);

            this.showSignal(board);
            this.showState(state);

            this.row.setOnClickListener(
                    clickListener == null ? null : v -> clickListener.onBoardClicked(board));
            this.row.setClickable(clickListener != null);
        }

        private void showState(@NonNull RowState state) {
            this.action.setVisibility(state == RowState.IDLE || state == RowState.SELECTED
                    ? View.VISIBLE : View.GONE);
            this.progress.setVisibility(state == RowState.BUSY ? View.VISIBLE : View.GONE);
            this.done.setVisibility(state == RowState.DONE ? View.VISIBLE : View.GONE);

            switch (state) {
                case SELECTED:
                    this.row.setBackgroundResource(R.drawable.bg_pairing_row_selected);
                    this.action.setText(R.string.pairing_board_selected);
                    this.subtitle.setText(R.string.pairing_board_subtitle);
                    break;
                case BUSY:
                    this.row.setBackgroundResource(R.drawable.bg_pairing_row_selected);
                    this.subtitle.setText(R.string.pairing_board_sending);
                    break;
                case DONE:
                    this.row.setBackgroundResource(R.drawable.bg_pairing_row_success);
                    this.subtitle.setText(R.string.pairing_board_paired);
                    break;
                default:
                    this.row.setBackgroundResource(R.drawable.bg_pairing_row);
                    this.action.setText(R.string.pairing_board_tap_to_pair);
                    this.subtitle.setText(R.string.pairing_board_subtitle);
                    break;
            }
        }

        private void showSignal(@NonNull DiscoveredBoard board) {
            // getSignalBars is 0..3, and an entirely unlit meter would read as "no signal" for a
            // board the scan is currently hearing, so the weakest band still lights one bar.
            int lit = board.getSignalBars() + 1;

            for (int index = 0; index < this.signalBars.length; index++) {
                int color = index < lit ? R.color.pairing_signal_on : R.color.pairing_signal_off;
                this.signalBars[index].setBackgroundTintList(
                        ContextCompat.getColorStateList(this.row.getContext(), color));
            }

            this.signalMeter.setContentDescription(this.row.getContext()
                    .getString(R.string.pairing_board_signal_description, lit));
        }
    }
}
