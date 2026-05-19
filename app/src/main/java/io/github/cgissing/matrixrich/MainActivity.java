package io.github.cgissing.matrixrich;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_FILE_CHOOSER = 40;
    private static final int REQUEST_CAMERA_PERMISSION = 41;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 42;

    private WebView webView;
    private TextView statusText;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createLayout();
        configureWebView();
        requestNotificationPermission();
        loadElementWeb();
        handleIntent(getIntent());
        updatePushService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILE_CHOOSER || filePathCallback == null) {
            return;
        }
        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        filePathCallback.onReceiveValue(result);
        filePathCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && pendingPermissionRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            } else {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }

    private void createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 249, 248));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(8), dp(10), dp(8));
        toolbar.setBackgroundColor(Color.rgb(235, 242, 239));

        statusText = new TextView(this);
        statusText.setText("Element Web");
        statusText.setTextColor(Color.rgb(33, 45, 49));
        statusText.setTextSize(14);
        statusText.setSingleLine(true);
        toolbar.addView(statusText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button reload = toolbarButton("Reload");
        reload.setOnClickListener((View view) -> {
            if (webView != null) {
                webView.reload();
            }
        });
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(82), dp(42)));

        Button settings = toolbarButton("Settings");
        settings.setOnClickListener((View view) -> showSettingsDialog());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(96), dp(42));
        settingsParams.setMargins(dp(8), 0, 0, 0);
        toolbar.addView(settings, settingsParams);

        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void configureWebView() {
        if (isDebuggable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNonHttpUrl(request == null ? null : request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNonHttpUrl(url == null ? null : Uri.parse(url));
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request == null ? null : request.getUrl();
                if (uri != null && "config.json".equals(uri.getLastPathSegment())) {
                    WebResourceResponse response = patchedConfigResponse(uri);
                    if (response != null) {
                        return response;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                status("Loading " + hostOf(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                status("Element Web: " + hostOf(url));
                view.evaluateJavascript(ElementWebPatch.mobilePatchScript(), null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    status("Load failed");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = params == null ? new Intent(Intent.ACTION_GET_CONTENT) : params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose file"), REQUEST_FILE_CHOOSER);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    return;
                }
                if (wantsVideo(request)) {
                    if (checkSelfPermissionCompat(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pendingPermissionRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                    return;
                }
                request.deny();
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    status("Loading " + progress + "%");
                }
            }
        });
    }

    private WebResourceResponse patchedConfigResponse(Uri uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            StringBuilder raw = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    raw.append(line).append('\n');
                }
            }
            byte[] patched = ElementWebConfigJson.withShellDefaults(raw.toString()).getBytes(StandardCharsets.UTF_8);
            return new WebResourceResponse("application/json", "UTF-8", new ByteArrayInputStream(patched));
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean handleNonHttpUrl(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return false;
        }
        if ("matrixrich".equalsIgnoreCase(scheme) || "ntfy".equalsIgnoreCase(scheme)) {
            handleDeepLink(uri);
            return true;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (Exception ignored) {
            return true;
        }
    }

    private void loadElementWeb() {
        if (webView != null) {
            webView.loadUrl(ElementWebConfig.initialUrl(this));
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null || webView == null) {
            return;
        }
        String openUrl = intent.getStringExtra("open_url");
        if (openUrl != null && !openUrl.trim().isEmpty()) {
            loadTarget(openUrl);
            return;
        }
        Uri data = intent.getData();
        if (data != null) {
            handleDeepLink(data);
        }
    }

    private void handleDeepLink(Uri data) {
        if (data == null) {
            return;
        }
        String url = data.getQueryParameter("url");
        if (url != null && !url.trim().isEmpty()) {
            loadTarget(url);
            return;
        }
        if ("ntfy".equalsIgnoreCase(data.getScheme())) {
            if (webView != null) {
                webView.evaluateJavascript(ElementWebPatch.mobilePatchScript(), null);
                webView.reload();
            }
            return;
        }
        loadElementWeb();
    }

    private void loadTarget(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        if (url.isEmpty()) {
            loadElementWeb();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            loadElementWeb();
            return;
        }
        webView.loadUrl(url);
    }

    private void showSettingsDialog() {
        SharedPreferences prefs = AppPrefs.get(this);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        body.setPadding(padding, padding, padding, padding);

        EditText elementWeb = field("Element Web URL", ElementWebConfig.normalizeElementWebUrl(AppPrefs.elementWebUrl(this)));
        EditText ntfyServer = field("ntfy server", AppPrefs.ntfyServer(this));
        EditText ntfyTopic = field("ntfy topic", AppPrefs.ntfyTopic(this));
        EditText ntfyToken = field("ntfy token", AppPrefs.ntfyToken(this));
        ntfyToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        CheckBox pushEnabled = new CheckBox(this);
        pushEnabled.setText("Enable ntfy foreground listener");
        pushEnabled.setChecked(AppPrefs.pushEnabled(this));
        pushEnabled.setTextColor(Color.rgb(33, 45, 49));

        body.addView(label("Element Web"));
        body.addView(elementWeb);
        body.addView(label("Push"));
        body.addView(ntfyServer);
        body.addView(ntfyTopic);
        body.addView(ntfyToken);
        body.addView(pushEnabled);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);

        new AlertDialog.Builder(this)
                .setTitle("Matrix Rich Shell")
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String normalizedElementWeb = ElementWebConfig.normalizeElementWebUrl(elementWeb.getText().toString());
                    prefs.edit()
                            .putString(AppPrefs.KEY_ELEMENT_WEB_URL, normalizedElementWeb)
                            .putString(AppPrefs.KEY_NTFY_SERVER, ntfyServer.getText().toString().trim())
                            .putString(AppPrefs.KEY_NTFY_TOPIC, ntfyTopic.getText().toString().trim())
                            .putString(AppPrefs.KEY_NTFY_TOKEN, ntfyToken.getText().toString().trim())
                            .putBoolean(AppPrefs.KEY_PUSH_ENABLED, pushEnabled.isChecked())
                            .apply();
                    requestNotificationPermission();
                    updatePushService();
                    webView.loadUrl(normalizedElementWeb);
                })
                .show();
    }

    private void updatePushService() {
        Intent service = new Intent(this, NtfyPushService.class);
        if (AppPrefs.pushEnabled(this) && !AppPrefs.ntfyTopic(this).trim().isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        } else {
            stopService(service);
        }
    }

    private EditText field(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value == null ? "" : value);
        field.setSingleLine(true);
        field.setTextSize(14);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return field;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(22, 28, 31));
        view.setTextSize(13);
        view.setPadding(0, dp(12), 0, dp(2));
        return view;
    }

    private Button toolbarButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setAllCaps(false);
        return button;
    }

    private boolean wantsVideo(PermissionRequest request) {
        if (request == null || request.getResources() == null) {
            return false;
        }
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                return true;
            }
        }
        return false;
    }

    private int checkSelfPermissionCompat(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(permission);
    }

    private boolean isDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermissionCompat(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void status(String text) {
        if (statusText != null) {
            statusText.setText(text);
        }
    }

    private String hostOf(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host == null ? "Element Web" : host;
        } catch (Exception ignored) {
            return "Element Web";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
