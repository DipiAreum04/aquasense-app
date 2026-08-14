package ca.team6.aquasense.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

        bindDeviceId(view.findViewById(R.id.tvDeviceId));

        view.findViewById(R.id.rowEmailSupport).setOnClickListener(v -> {
            String email = getString(R.string.support_email);

            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            startActivity(Intent.createChooser(intent, getString(R.string.contact_choose_email_app)));
        });

        view.findViewById(R.id.rowContactForm).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_contactSupport_to_contactForm));
    }


    @SuppressWarnings("unused")
    private void bindDeviceId(@NonNull TextView tvDeviceId) {
        String deviceId = null;
    }

    @SuppressWarnings("unused")
    private void displayDeviceId(@NonNull TextView tvDeviceId, @Nullable String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            tvDeviceId.setText(R.string.device_id_not_connected);
        } else {
            tvDeviceId.setText(deviceId);
        }
    }
}
