package ca.team6.aquasense.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.SettingsRepository;

public class ContactFormFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioButton rbFeedback = view.findViewById(R.id.rbContactFeedback);
        RadioButton rbIssueHardware = view.findViewById(R.id.rbIssueHardware);
        RadioButton rbIssueApp = view.findViewById(R.id.rbIssueApp);
        EditText etName = view.findViewById(R.id.etContactName);
        EditText etEmail = view.findViewById(R.id.etContactEmail);
        EditText etMessage = view.findViewById(R.id.etContactMessage);
        Button btnSend = view.findViewById(R.id.btnSendContact);

        // Prefill from local profile (SharedPreferences).
        new SettingsRepository(requireContext()).loadSettings(settings -> {
            if (!TextUtils.isEmpty(settings.profileName)) {
                etName.setText(settings.profileName);
            }
            if (!TextUtils.isEmpty(settings.profileEmail)) {
                etEmail.setText(settings.profileEmail);
            }
        });

        btnSend.setOnClickListener(v -> {
            String name = etName.getText() != null
                    ? etName.getText().toString().trim()
                    : "";
            if (ProfileInputValidator.isInvalidOptionalName(name)) {
                Toast.makeText(requireContext(),
                        R.string.contact_name_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String userEmail = etEmail.getText() != null
                    ? etEmail.getText().toString().trim()
                    : "";
            if (TextUtils.isEmpty(userEmail)) {
                Toast.makeText(requireContext(),
                        R.string.contact_email_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (ProfileInputValidator.isInvalidEmail(userEmail)) {
                Toast.makeText(requireContext(),
                        R.string.contact_email_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String message = etMessage.getText() != null
                    ? etMessage.getText().toString().trim()
                    : "";
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(requireContext(),
                        R.string.contact_message_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isFeedback = rbFeedback.isChecked();
            String issueWith;
            if (rbIssueHardware.isChecked()) {
                issueWith = getString(R.string.contact_issue_hardware);
            } else if (rbIssueApp.isChecked()) {
                issueWith = getString(R.string.contact_issue_app);
            } else {
                issueWith = getString(R.string.contact_issue_na);
            }

            String subject = getString(isFeedback
                    ? R.string.contact_subject_feedback
                    : R.string.contact_subject_support)
                    + " (Issue: " + issueWith + ")";
            String intro = getString(isFeedback
                    ? R.string.contact_body_feedback_intro
                    : R.string.contact_body_support_intro);

            StringBuilder body = new StringBuilder();
            body.append(getString(R.string.contact_body_from)).append(": ");
            if (!TextUtils.isEmpty(name)) {
                body.append(name).append(" <").append(userEmail).append(">");
            } else {
                body.append(userEmail);
            }
            body.append("\n")
                    .append(getString(R.string.contact_body_issue_with_label))
                    .append(": ")
                    .append(issueWith)
                    .append("\n\n")
                    .append(intro)
                    .append("\n\n")
                    .append(message)
                    .append("\n");

            // TODO: After Firebase is set up, implement automatic email send within the app
            // User writes and submits the form, which will be written to Firebase Firestore and/or Cloud Function,
            // The email will be formatted automatically and sent to the support team.
            openSupportEmail(subject, body.toString());
        });
    }

    private void openSupportEmail(@NonNull String subject, @NonNull String body) {
        String supportEmail = getString(R.string.support_email);
        // mailto opens the user's preferred email app (Gmail, Outlook, etc.)
        Uri mailto = Uri.parse("mailto:" + supportEmail +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body));
        Intent intent = new Intent(Intent.ACTION_SENDTO, mailto);
        startActivity(Intent.createChooser(intent, getString(R.string.contact_choose_email_app)));
    }
}
