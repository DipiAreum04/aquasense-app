package ca.team6.aquasense.ui;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

/**
 * Hooks a fragment's own toolbar up to the navigation graph, unless the host activity already
 * supplies one.
 *
 * <p>MainActivity carries no action bar, so its screens each draw a toolbar of their own.
 * SettingsActivity sets one toolbar as the action bar for every destination it hosts, and drives
 * its title and up arrow from the graph. A screen reachable from both hosts would otherwise stack
 * two title bars when opened from Settings.
 */
public final class FragmentToolbar {

    private FragmentToolbar() {
    }

    public static void setup(@NonNull Fragment fragment, @NonNull View root, @IdRes int toolbarId) {
        Toolbar toolbar = root.findViewById(toolbarId);

        if (hostSuppliesActionBar(fragment)) {
            toolbar.setVisibility(View.GONE);
            return;
        }
        NavigationUI.setupWithNavController(toolbar, Navigation.findNavController(root));
    }

    private static boolean hostSuppliesActionBar(@NonNull Fragment fragment) {
        return fragment.requireActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) fragment.requireActivity()).getSupportActionBar() != null;
    }
}
