package ca.team6.aquasense.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;

public class ContactSupportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.rowEmailSupport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:support@aquasense.io"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "AquaSense Support Request");
            startActivity(Intent.createChooser(intent, "Send email"));
        });

        view.findViewById(R.id.rowLiveChat).setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Live chat available Mon–Fri, 9 AM – 6 PM EST",
                        Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.rowDocumentation).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://guides.aquasense.io"));
            startActivity(intent);
        });
    }
}
