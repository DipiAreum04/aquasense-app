package ca.team6.aquasense.settings;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

public class TroubleshootingGuideFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_troubleshooting_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean nightMode = isNightMode();

        WebView webView = view.findViewById(R.id.webTroubleshootingGuide);
        // The guide is a self-contained local asset with no links, so block navigation outright
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                    @NonNull WebResourceRequest request) {
                return true;
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        webView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background));
        webView.loadDataWithBaseURL(
                null,
                applyTheme(loadTroubleshootingGuideHtml(), nightMode),
                "text/html",
                "UTF-8",
                null);
    }

    // Reflects whatever AppCompatDelegate resolved, so it follows the in-app theme settings
    private boolean isNightMode() {
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static String applyTheme(@NonNull String html, boolean nightMode) {
        if (!nightMode) {
            return html;
        }
        return html.replace("<html lang=\"en\">", "<html lang=\"en\" class=\"dark\">");
    }

    private String loadTroubleshootingGuideHtml() {
        try (InputStream in = requireContext().getAssets().open("troubleshooting_guide.html");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return "<html><body><p>Unable to load the troubleshooting guide.</p></body></html>";
        }
    }
}
