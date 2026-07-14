package ca.team6.aquasense.model;

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;

    public GridSpacingItemDecoration(DisplayMetrics displayMetrics) {
        this.spacing = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                displayMetrics
        );
    }

    @Override
    public void getItemOffsets(
            Rect outRect,
            @NonNull View view,
            RecyclerView parent,
            @NonNull RecyclerView.State state
    ) {
        int position = parent.getChildAdapterPosition(view);
        int column = position % 2;

        outRect.left = column == 0 ? 0 : spacing / 2;
        outRect.right = column == 0 ? spacing / 2 : 0;
        outRect.bottom = spacing;

        if (position < 2) {
            outRect.top = 0;
        }
    }
}
