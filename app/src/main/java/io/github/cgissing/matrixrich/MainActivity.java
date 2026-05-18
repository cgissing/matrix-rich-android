package io.github.cgissing.matrixrich;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_FILE_CHOOSER = 1001;
    private static final int REQUEST_NOTIFICATIONS = 1002;
    private static final int TAB_CHAT = 0;
    private static final int TAB_PUSH = 1;
    private static final int TAB_SETTINGS = 2;
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36 MatrixRich/0.2";

    private WebView webView;
    private FrameLayout content;
    private View pushPanel;
    private View settingsPanel;
    private TextView title;
    private TextView subtitle;
    private TextView pushStatus;
    private Button chatNav;
    private Button pushNav;
    private Button settingsNav;
    private EditText elementUrlInput;
    private EditText ntfyServerInput;
    private EditText ntfyTopicInput;
    private EditText ntfyTokenInput;
    private CheckBox desktopUaInput;
    private CheckBox pushEnabledInput;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();
        buildUi();
        configureWebView();
        showTab(TAB_CHAT);
        handleIntent(getIntent());
        if (webView.getUrl() == null) {
            loadConfiguredUrl();
        }
        updatePushService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILE_CHOOSER || filePathCallback == null) {
            return;
        }
        Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 248, 247));

        root.addView(createAppBar(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content = new FrameLayout(this);
        webView = new WebView(this);
        pushPanel = createPushPanel();
        settingsPanel = createSettingsPanel();
        content.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(pushPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(settingsPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(createBottomNavigation(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private View createAppBar() {
        LinearLayout appBar = new LinearLayout(this);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setPadding(dp(16), dp(10), dp(10), dp(8));
        appBar.setBackgroundColor(Color.WHITE);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        title = text("Chats", 20, Color.rgb(18, 21, 23), true);
        subtitle = text("Element Web runtime", 12, Color.rgb(91, 101, 106), false);
        titleBox.addView(title);
        titleBox.addView(subtitle);
        appBar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button back = actionButton("Back");
        back.setOnClickListener((View v) -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        appBar.addView(back);

        Button reload = actionButton("Reload");
        reload.setOnClickListener((View v) -> webView.reload());
        appBar.addView(reload);
        return appBar;
    }

    private View createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(8));
        nav.setBackgroundColor(Color.WHITE);

        chatNav = navButton("Chats");
        pushNav = navButton("Push");
        settingsNav = navButton("Settings");
        chatNav.setOnClickListener((View v) -> showTab(TAB_CHAT));
        pushNav.setOnClickListener((View v) -> showTab(TAB_PUSH));
        settingsNav.setOnClickListener((View v) -> showTab(TAB_SETTINGS));

        nav.addView(chatNav, new LinearLayout.LayoutParams(0, dp(44), 1));
        nav.addView(pushNav, new LinearLayout.LayoutParams(0, dp(44), 1));
        nav.addView(settingsNav, new LinearLayout.LayoutParams(0, dp(44), 1));
        return nav;
    }

    private View createPushPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = panelBody();
        body.addView(sectionTitle("Push status"));
        pushStatus = text("", 14, Color.rgb(44, 52, 56), false);
        body.addView(card(pushStatus));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button start = actionButton("Start");
        start.setOnClickListener((View v) -> {
            AppPrefs.get(this).edit().putBoolean(AppPrefs.KEY_PUSH_ENABLED, true).apply();
            refreshSettingsFields();
            updatePushService();
        });
        Button stop = actionButton("Stop");
        stop.setOnClickListener((View v) -> {
            AppPrefs.get(this).edit().putBoolean(AppPrefs.KEY_PUSH_ENABLED, false).apply();
            refreshSettingsFields();
            updatePushService();
        });
        actions.addView(start, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(stop, new LinearLayout.LayoutParams(0, dp(44), 1));
        body.addView(actions);

        body.addView(sectionTitle("Wake contract"));
        TextView contract = text("Publish ntfy messages to the configured topic. A Click header like matrixrich://open?url=https%3A%2F%2Fapp.element.io%2F opens this client and foregrounds the WebView.", 14, Color.rgb(58, 68, 72), false);
        contract.setPadding(dp(14), dp(12), dp(14), dp(12));
        body.addView(card(contract));

        scroll.addView(body);
        return scroll;
    }

    private View createSettingsPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = panelBody();

        body.addView(sectionTitle("Element Web runtime"));
        elementUrlInput = input("Element Web URL", AppPrefs.elementUrl(this), InputType.TYPE_TEXT_VARIATION_URI);
        desktopUaInput = checkbox("Use desktop user agent and mobile-guide bypass", AppPrefs.desktopUserAgent(this));
        body.addView(card(elementUrlInput));
        body.addView(card(desktopUaInput));

        body.addView(sectionTitle("ntfy push"));
        ntfyServerInput = input("ntfy server", AppPrefs.ntfyServer(this), InputType.TYPE_TEXT_VARIATION_URI);
        ntfyTopicInput = input("ntfy topic", AppPrefs.ntfyTopic(this), InputType.TYPE_CLASS_TEXT);
        ntfyTokenInput = input("ntfy bearer token", AppPrefs.ntfyToken(this), InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pushEnabledInput = checkbox("Enable foreground listener", AppPrefs.pushEnabled(this));
        body.addView(card(ntfyServerInput));
        body.addView(card(ntfyTopicInput));
        body.addView(card(ntfyTokenInput));
        body.addView(card(pushEnabledInput));

        Button save = actionButton("Save settings");
        save.setOnClickListener((View v) -> saveSettings());
        body.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        scroll.addView(body);
        return scroll;
    }

    private LinearLayout panelBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(18));
        return body;
    }

    private View card(View child) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(8));
        bg.setStroke(1, Color.rgb(224, 231, 228));
        card.setBackground(bg);
        card.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 13, Color.rgb(79, 92, 96), true);
        view.setPadding(dp(2), dp(10), dp(2), dp(8));
        return view;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value == null ? "" : value);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setTextSize(14);
        return editText;
    }

    private CheckBox checkbox(String text, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setChecked(checked);
        checkBox.setTextSize(14);
        return checkBox;
    }

    private Button navButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        return button;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(20, 103, 84));
        button.setTextSize(13);
        return button;
    }

    private void showTab(int tab) {
        webView.setVisibility(tab == TAB_CHAT ? View.VISIBLE : View.GONE);
        pushPanel.setVisibility(tab == TAB_PUSH ? View.VISIBLE : View.GONE);
        settingsPanel.setVisibility(tab == TAB_SETTINGS ? View.VISIBLE : View.GONE);
        chatNav.setSelected(tab == TAB_CHAT);
        pushNav.setSelected(tab == TAB_PUSH);
        settingsNav.setSelected(tab == TAB_SETTINGS);
        tintNav(chatNav, tab == TAB_CHAT);
        tintNav(pushNav, tab == TAB_PUSH);
        tintNav(settingsNav, tab == TAB_SETTINGS);
        if (tab == TAB_CHAT) {
            title.setText("Chats");
            subtitle.setText("Element Web runtime");
        } else if (tab == TAB_PUSH) {
            title.setText("Push");
            subtitle.setText(AppPrefs.pushEnabled(this) ? "ntfy listener enabled" : "ntfy listener disabled");
            updatePushStatus();
        } else {
            title.setText("Settings");
            subtitle.setText("Runtime and notification bridge");
            refreshSettingsFields();
        }
    }

    private void tintNav(Button button, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(selected ? Color.rgb(218, 246, 237) : Color.TRANSPARENT);
        button.setTextColor(selected ? Color.rgb(7, 99, 76) : Color.rgb(76, 88, 92));
        button.setBackground(bg);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setUserAgentString(AppPrefs.desktopUserAgent(this) ? DESKTOP_USER_AGENT : WebSettings.getDefaultUserAgent(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        webView.setWebViewClient(new MatrixWebViewClient());
        webView.setWebChromeClient(new MatrixChromeClient());
    }

    private void loadConfiguredUrl() {
        String url = AppPrefs.elementUrl(this);
        setElementMobileBypassCookie(url);
        webView.loadUrl(url);
    }

    private void setElementMobileBypassCookie(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getScheme() == null || uri.getHost() == null) {
            return;
        }
        String origin = uri.getScheme() + "://" + uri.getHost();
        CookieManager.getInstance().setCookie(origin, "element_mobile_redirect_to_guide=false; Path=/; Max-Age=31536000; SameSite=Lax");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String openUrl = intent.getStringExtra("open_url");
        if (openUrl != null && !openUrl.isEmpty()) {
            if (NtfyMessage.isClientDeepLink(openUrl)) {
                handleIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)));
                return;
            }
            showTab(TAB_CHAT);
            if (openUrl.startsWith("http://") || openUrl.startsWith("https://")) {
                webView.loadUrl(openUrl);
            } else {
                loadConfiguredUrl();
            }
            return;
        }
        Uri data = intent.getData();
        if (data == null) {
            return;
        }
        if ("matrixrich".equals(data.getScheme())) {
            String target = data.getQueryParameter("url");
            showTab(TAB_CHAT);
            if (target != null && (target.startsWith("http://") || target.startsWith("https://"))) {
                webView.loadUrl(target);
            } else {
                loadConfiguredUrl();
            }
            return;
        }
        if ("ntfy".equals(data.getScheme())) {
            applyNtfyDeepLink(data);
        }
    }

    private void applyNtfyDeepLink(Uri uri) {
        String host = uri.getHost();
        String topic = uri.getLastPathSegment();
        if (host == null || topic == null || topic.isEmpty()) {
            Toast.makeText(this, "Open settings to configure ntfy", Toast.LENGTH_SHORT).show();
            showTab(TAB_SETTINGS);
            return;
        }
        AppPrefs.get(this).edit()
                .putString(AppPrefs.KEY_NTFY_SERVER, "https://" + host)
                .putString(AppPrefs.KEY_NTFY_TOPIC, topic)
                .putBoolean(AppPrefs.KEY_PUSH_ENABLED, true)
                .apply();
        refreshSettingsFields();
        updatePushService();
        showTab(TAB_PUSH);
        Toast.makeText(this, "ntfy topic saved", Toast.LENGTH_SHORT).show();
    }

    private void refreshSettingsFields() {
        if (elementUrlInput == null) {
            return;
        }
        elementUrlInput.setText(AppPrefs.elementUrl(this));
        ntfyServerInput.setText(AppPrefs.ntfyServer(this));
        ntfyTopicInput.setText(AppPrefs.ntfyTopic(this));
        ntfyTokenInput.setText(AppPrefs.ntfyToken(this));
        desktopUaInput.setChecked(AppPrefs.desktopUserAgent(this));
        pushEnabledInput.setChecked(AppPrefs.pushEnabled(this));
    }

    private void saveSettings() {
        SharedPreferences prefs = AppPrefs.get(this);
        prefs.edit()
                .putString(AppPrefs.KEY_ELEMENT_URL, elementUrlInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_SERVER, ntfyServerInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_TOPIC, ntfyTopicInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_TOKEN, ntfyTokenInput.getText().toString().trim())
                .putBoolean(AppPrefs.KEY_DESKTOP_USER_AGENT, desktopUaInput.isChecked())
                .putBoolean(AppPrefs.KEY_PUSH_ENABLED, pushEnabledInput.isChecked())
                .apply();
        configureWebView();
        loadConfiguredUrl();
        updatePushService();
        showTab(TAB_CHAT);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void updatePushService() {
        Intent intent = new Intent(this, NtfyPushService.class);
        if (AppPrefs.pushEnabled(this) && !AppPrefs.ntfyTopic(this).trim().isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } else {
            stopService(intent);
        }
        updatePushStatus();
    }

    private void updatePushStatus() {
        if (pushStatus == null) {
            return;
        }
        String topic = AppPrefs.ntfyTopic(this).trim();
        boolean enabled = AppPrefs.pushEnabled(this) && !topic.isEmpty();
        pushStatus.setText(enabled
                ? "Listening on " + NtfyEndpoint.jsonStreamUrl(AppPrefs.ntfyServer(this), topic)
                : "Disabled. Configure an ntfy server and topic, then enable the foreground listener.");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class MatrixWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if ("http".equals(scheme) || "https".equals(scheme)) {
                return false;
            }
            if ("matrixrich".equals(scheme) || "ntfy".equals(scheme)) {
                handleIntent(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this, "No app can open " + scheme, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.evaluateJavascript(MobileElementAdapter.injectionScript(), null);
        }
    }

    private class MatrixChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;
            Intent intent = params.createIntent();
            try {
                startActivityForResult(intent, REQUEST_FILE_CHOOSER);
            } catch (ActivityNotFoundException e) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources());
            }
        }
    }
}
