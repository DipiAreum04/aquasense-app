package ca.team6.aquasense.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ca.team6.aquasense.R;

public class PrivacyPolicyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean nightMode = isNightMode();

        WebView webView = view.findViewById(R.id.webPrivacyPolicy);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                    @NonNull WebResourceRequest request) {
                return openExternally(request.getUrl());
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        webView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));
        webView.loadDataWithBaseURL(
                null,
                applyTheme(loadPrivacyPolicyHtml(), nightMode),
                "text/html",
                "UTF-8",
                null);
    }

    // The policy is a local asset, so the WebView cannot navigate anywhere
    // To allow the user to tap the contact link, redirect the schemes to the browser instead
    private boolean openExternally(@Nullable Uri uri) {
        String scheme = uri == null ? null : uri.getScheme();
        if (scheme == null) {
            return false;
        }

        Intent intent;
        switch (scheme) {
            case "mailto":
                intent = Intent.createChooser(
                        new Intent(Intent.ACTION_SENDTO, uri),
                        getString(R.string.contact_choose_email_app));
                break;
            case "http":
            case "https":
            case "tel":
                intent = new Intent(Intent.ACTION_VIEW, uri);
                break;
            default:
                return false;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(),
                    R.string.privacy_policy_link_failed,
                    Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // Reflects whatever AppCompatDelegate resolved, so it follows the in-app theme setting rather than the system setting.
    private boolean isNightMode() {
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // The stylesheet keys its dark palette off :root.dark, so flip the class on <html>.
    private static String applyTheme(@NonNull String html, boolean nightMode) {
        if (!nightMode) {
            return html;
        }
        return html.replace("<html lang=\"en\">", "<html lang=\"en\" class=\"dark\">");
    }

    private String loadPrivacyPolicyHtml() {
        try (InputStream in = requireContext().getAssets().open("privacy_policy.html");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return "<html><body><p>Unable to load the privacy policy.</p></body></html>";
        }
    }
}
