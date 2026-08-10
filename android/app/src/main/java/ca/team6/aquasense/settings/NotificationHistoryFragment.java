package ca.team6.aquasense.settings;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.NotificationLogEntry;
import ca.team6.aquasense.model.NotificationLogRepository;
import ca.team6.aquasense.model.SensorType;

/**
 * NF-3.2: lists past logged notifications, filterable by aquarium and sensor.
 *
 * <p>Reached from the Notifications tab of the dashboard's navigation bar, which opens it as a
 * shortcut destination so the Settings hub is never shown on the way in or out. It opens on every
 * aquarium, newest first.
 */
public class NotificationHistoryFragment extends Fragment {

    // Fixed display order for the sensor filter, matching the order sensors already
    // appear in on the Dashboard (Water Level, Temperature, Dissolved Solids, pH Level).
    private static final SensorType[] SENSOR_FILTER_ORDER = {
            SensorType.WATER_LEVEL, SensorType.TEMPERATURE, SensorType.DISSOLVED_SOLIDS, SensorType.PH_LEVEL
    };

    /** One screenful of history. The log itself is kept whole; only the list is divided. */
    private static final int PAGE_SIZE = 20;

    private List<Aquarium> aquariums = java.util.Collections.emptyList();
    private AquariumRepository aquariumRepository;
    private NotificationLogRepository notificationLogRepository;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private Spinner aquariumSpinner;
    private View paginationBar;
    private TextView tvPageIndicator;
    private View btnPreviousPage;
    private View btnNextPage;
    private View btnClearAll;

    private String selectedAquariumId; // null = all aquariums
    private SensorType selectedSensorType; // null = all sensors

    private int currentPage; // 0-based

    private final AquariumRepository.AquariumsObserver aquariumsObserver = this::onAquariumsChanged;
    private final NotificationLogRepository.HistoryObserver notificationHistoryObserver = () -> {
        // A new alert belongs at the front of the newest-first history.
        showPage(0);
        if (recyclerView != null) recyclerView.scrollToPosition(0);
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_notification_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerNotificationHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        aquariumSpinner = view.findViewById(R.id.spinnerAquariumFilter);

        paginationBar = view.findViewById(R.id.paginationBar);
        tvPageIndicator = view.findViewById(R.id.tvPageIndicator);
        btnPreviousPage = view.findViewById(R.id.btnPreviousPage);
        btnNextPage = view.findViewById(R.id.btnNextPage);
        btnClearAll = view.findViewById(R.id.btnClearAllNotifications);
        btnPreviousPage.setOnClickListener(v -> goToPage(currentPage - 1));
        btnNextPage.setOnClickListener(v -> goToPage(currentPage + 1));
        btnClearAll.setOnClickListener(v -> confirmClearAll());
        attachSwipeToDelete();

        notificationLogRepository = new NotificationLogRepository(requireContext());
        notificationLogRepository.addObserver(notificationHistoryObserver);

        aquariumRepository = AquariumRepository.getInstance(requireContext());
        aquariumRepository.addObserver(aquariumsObserver);

        setupSensorFilter(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (aquariumRepository != null) {
            aquariumRepository.removeObserver(aquariumsObserver);
        }
        if (notificationLogRepository != null) {
            notificationLogRepository.removeObserver(notificationHistoryObserver);
        }
        recyclerView = null;
        aquariumSpinner = null;
    }

    private void onAquariumsChanged(@NonNull List<Aquarium> updatedAquariums) {
        if (getView() == null) {
            return;
        }
        this.aquariums = updatedAquariums;
        refreshAquariumFilterLabels();
        showPage(currentPage);
    }

    private void refreshAquariumFilterLabels() {
        if (aquariumSpinner == null) {
            return;
        }

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.filter_all_aquariums));
        for (Aquarium aquarium : aquariums) {
            labels.add(aquarium.getName());
        }

        int previousSelection = aquariumSpinner.getSelectedItemPosition();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aquariumSpinner.setAdapter(adapter);

        if (previousSelection >= 0 && previousSelection < labels.size()) {
            aquariumSpinner.setSelection(previousSelection, false);
        } else {
            selectedAquariumId = null;
            aquariumSpinner.setSelection(0, false);
        }
    }

    private void setupAquariumFilter() {
        if (aquariumSpinner == null) {
            return;
        }

        refreshAquariumFilterLabels();

        aquariumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedAquariumId = position == 0 ? null : aquariums.get(position - 1).getId();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSensorFilter(View view) {
        Spinner spinner = view.findViewById(R.id.spinnerSensorFilter);

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.filter_all_sensors));
        for (SensorType sensorType : SENSOR_FILTER_ORDER) {
            labels.add(getString(sensorNameResId(sensorType)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedSensorType = position == 0 ? null : SENSOR_FILTER_ORDER[position - 1];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupAquariumFilter();
    }

    private void applyFilters() {
        showPage(0);
    }

    private void goToPage(int requestedPage) {
        showPage(requestedPage);
        recyclerView.scrollToPosition(0);
    }

    private void showPage(int requestedPage) {
        if (notificationLogRepository == null || recyclerView == null) return;

        long entryCount = notificationLogRepository.countEntries(
                selectedAquariumId, selectedSensorType);
        int pageCount = entryCount == 0 ? 0 : (int) ((entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
        currentPage = pageCount == 0 ? 0 : Math.max(0, Math.min(requestedPage, pageCount - 1));

        List<NotificationLogEntry> pageEntries = notificationLogRepository.loadPage(
                currentPage, PAGE_SIZE, selectedAquariumId, selectedSensorType);
        boolean empty = pageEntries.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        recyclerView.setAdapter(new NotificationHistoryAdapter(pageEntries, aquariums));

        paginationBar.setVisibility(entryCount > 0 ? View.VISIBLE : View.GONE);
        if (entryCount > 0) {
            tvPageIndicator.setText(getString(
                    R.string.notification_history_page, currentPage + 1, pageCount));
            setPageButtonEnabled(btnPreviousPage, currentPage > 0);
            setPageButtonEnabled(btnNextPage, currentPage + 1 < pageCount);
        }
    }

    private void attachSwipeToDelete() {
        Paint deleteBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        deleteBackground.setColor(ContextCompat.getColor(requireContext(), R.color.danger));
        Drawable deleteIcon = ContextCompat.getDrawable(
                requireContext(), R.drawable.delete_forever_24px);
        if (deleteIcon != null) deleteIcon.setTint(Color.WHITE);
        float cornerRadius = dp(10);
        int iconSize = Math.round(dp(24));
        int iconEndMargin = Math.round(dp(20));

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                int position = viewHolder.getAdapterPosition();
                if (adapter instanceof NotificationHistoryAdapter
                        && position != RecyclerView.NO_POSITION) {
                    NotificationLogEntry entry =
                            ((NotificationHistoryAdapter) adapter).getEntry(position);
                    notificationLogRepository.deleteEntry(entry.localId);
                    showPage(currentPage);
                } else {
                    showPage(currentPage);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX,
                                    float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                if (dX < 0) {
                    View item = viewHolder.itemView;
                    int saveCount = canvas.save();
                    // Paint only the portion uncovered by the card as it moves left.
                    canvas.clipRect(item.getRight() + dX, item.getTop(),
                            item.getRight(), item.getBottom());
                    canvas.drawRoundRect(new RectF(item.getLeft(), item.getTop(),
                                    item.getRight(), item.getBottom()),
                            cornerRadius, cornerRadius, deleteBackground);

                    if (deleteIcon != null) {
                        int iconRight = item.getRight() - iconEndMargin;
                        int iconLeft = iconRight - iconSize;
                        int iconTop = item.getTop() + (item.getHeight() - iconSize) / 2;
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconTop + iconSize);
                        deleteIcon.draw(canvas);
                    }
                    canvas.restoreToCount(saveCount);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void confirmClearAll() {
        View content = getLayoutInflater().inflate(
                R.layout.dialog_notification_history_clear, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(content)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            // The custom card owns the 24dp corners; remove the platform's square backdrop.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        content.findViewById(R.id.notificationHistoryClearCancel)
                .setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.notificationHistoryClearConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            notificationLogRepository.clearAll();
            showPage(0);
        });
        dialog.show();
    }

    private static void setPageButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.35f);
    }

    private static int sensorNameResId(SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
                return R.string.temperature;
            case DISSOLVED_SOLIDS:
                return R.string.dissolved_solids;
            case PH_LEVEL:
                return R.string.ph_level;
            case WATER_LEVEL:
            default:
                return R.string.water_level;
        }
    }
}
