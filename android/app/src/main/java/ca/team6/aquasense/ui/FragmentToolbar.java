package ca.team6.aquasense.ui;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;

public final class FragmentToolbar {

    private FragmentToolbar() {
    }

    public static void setup(@NonNull Fragment fragment, @NonNull View root, @IdRes int toolbarId) {
        Toolbar toolbar = root.findViewById(toolbarId);

        if (hostSuppliesActionBar(fragment)) {
            toolbar.setVisibility(View.GONE);
            return;
        }

        NavController navController = Navigation.findNavController(root);
        // This toolbar belongs to this fragment, so keep its state fixed while the fragment
        // exits. A destination listener would update the still-visible toolbar one frame early.
        NavDestination destination = navController.getCurrentDestination();
        if (destination != null && destination.getLabel() != null) {
            toolbar.setTitle(destination.getLabel());
        }

        if (navController.getPreviousBackStackEntry() != null) {
            DrawerArrowDrawable backArrow = new DrawerArrowDrawable(toolbar.getContext());
            backArrow.setProgress(1f);
            toolbar.setNavigationIcon(backArrow);
            toolbar.setNavigationContentDescription(R.string.action_back);
            toolbar.setNavigationOnClickListener(v -> navController.navigateUp());
        } else {
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
        }
    }

    private static boolean hostSuppliesActionBar(@NonNull Fragment fragment) {
        return fragment.requireActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) fragment.requireActivity()).getSupportActionBar() != null;
    }
}
