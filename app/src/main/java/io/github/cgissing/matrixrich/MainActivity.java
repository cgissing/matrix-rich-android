package io.github.cgissing.matrixrich;

import android.Manifest;
import android.app.Activity;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 1002;
    private static final int TAB_CHAT = 0;
    private static final int TAB_PUSH = 1;
    private static final int TAB_SETTINGS = 2;

    private final List<NativeRoom> rooms = new ArrayList<>();
    private final Map<String, List<NativeMessage>> messagesByRoom = new HashMap<>();

    private NativeRoom selectedRoom;
    private int currentTab = TAB_CHAT;
    private FrameLayout content;
    private View chatPanel;
    private View pushPanel;
    private View settingsPanel;
    private LinearLayout roomRail;
    private LinearLayout timeline;
    private EditText composerInput;
    private TextView title;
    private TextView subtitle;
    private TextView wakeNotice;
    private TextView pushStatus;
    private Button chatNav;
    private Button pushNav;
    private Button settingsNav;
    private EditText homeserverInput;
    private EditText accountHintInput;
    private EditText userIdInput;
    private EditText passwordInput;
    private EditText accessTokenInput;
    private EditText ntfyServerInput;
    private EditText ntfyTopicInput;
    private EditText ntfyTokenInput;
    private CheckBox pushEnabledInput;
    private TextView connectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();
        setDisconnectedState("Open Settings to log in or paste an access token.");
        buildUi();
        showTab(TAB_CHAT);
        handleIntent(getIntent());
        updatePushService();
        if (!AppPrefs.accessToken(this).trim().isEmpty()) {
            syncNow(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (currentTab != TAB_CHAT) {
            showTab(TAB_CHAT);
            return;
        }
        super.onBackPressed();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 248));

        root.addView(createAppBar(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content = new FrameLayout(this);
        chatPanel = createChatPanel();
        pushPanel = createPushPanel();
        settingsPanel = createSettingsPanel();
        content.addView(chatPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        appBar.setPadding(dp(16), dp(10), dp(12), dp(8));
        appBar.setBackgroundColor(Color.WHITE);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        title = text("Messages", 20, Color.rgb(20, 23, 26), true);
        subtitle = text("", 12, Color.rgb(91, 101, 106), false);
        titleBox.addView(title);
        titleBox.addView(subtitle);
        appBar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button sync = actionButton("Sync");
        sync.setOnClickListener((View v) -> syncNow(true));
        appBar.addView(sync, new LinearLayout.LayoutParams(dp(82), dp(40)));
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

    private View createChatPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        wakeNotice = text("", 13, Color.rgb(45, 59, 63), false);
        wakeNotice.setPadding(dp(14), dp(9), dp(14), dp(9));
        wakeNotice.setBackgroundColor(Color.rgb(226, 244, 238));
        wakeNotice.setVisibility(View.GONE);
        root.addView(wakeNotice, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HorizontalScrollView roomScroll = new HorizontalScrollView(this);
        roomScroll.setHorizontalScrollBarEnabled(false);
        roomRail = new LinearLayout(this);
        roomRail.setOrientation(LinearLayout.HORIZONTAL);
        roomRail.setPadding(dp(12), dp(10), dp(12), dp(8));
        roomScroll.addView(roomRail);
        root.addView(roomScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView timelineScroll = new ScrollView(this);
        timelineScroll.setFillViewport(true);
        timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.VERTICAL);
        timeline.setPadding(dp(14), dp(4), dp(14), dp(16));
        timelineScroll.addView(timeline, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(timelineScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(createComposer(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        renderRooms();
        renderSelectedRoom();
        return root;
    }

    private View createComposer() {
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(10));
        composer.setBackgroundColor(Color.WHITE);

        composerInput = new EditText(this);
        composerInput.setSingleLine(false);
        composerInput.setMinLines(1);
        composerInput.setMaxLines(5);
        composerInput.setHint("Message");
        composerInput.setTextSize(15);
        composerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.addView(composerInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button send = actionButton("Send");
        send.setOnClickListener((View v) -> sendLocalDraft());
        composer.addView(send, new LinearLayout.LayoutParams(dp(82), dp(46)));
        return composer;
    }

    private void renderRooms() {
        if (roomRail == null) {
            return;
        }
        roomRail.removeAllViews();
        for (NativeRoom room : rooms) {
            Button button = navButton(room.initials + "  " + room.title + (room.unreadCount > 0 ? "  " + room.unreadCount : ""));
            boolean selected = selectedRoom != null && selectedRoom.id.equals(room.id);
            tintRoomButton(button, selected);
            button.setOnClickListener((View v) -> {
                selectedRoom = room;
                renderRooms();
                renderSelectedRoom();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
            params.setMargins(0, 0, dp(8), 0);
            roomRail.addView(button, params);
        }
    }

    private void renderSelectedRoom() {
        if (timeline == null || selectedRoom == null) {
            return;
        }
        timeline.removeAllViews();
        title.setText(selectedRoom.title);
        subtitle.setText(selectedRoom.subtitle);
        List<NativeMessage> messages = messagesByRoom.get(selectedRoom.id);
        if (messages == null || messages.isEmpty()) {
            messages = new ArrayList<>();
            messages.add(new NativeMessage("Matrix Rich", "", "No timeline events loaded yet.", false));
        }
        for (NativeMessage message : messages) {
            timeline.addView(messageBubble(message));
        }
    }

    private View messageBubble(NativeMessage message) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(message.outbound ? Gravity.END : Gravity.START);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(10));
        bubble.setBackground(bubbleBackground(message.outbound));

        TextView meta = text(message.sender + "  " + message.time, 12, message.outbound ? Color.rgb(37, 90, 77) : Color.rgb(91, 101, 106), true);
        bubble.addView(meta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = text("", 15, Color.rgb(22, 28, 31), false);
        body.setTextIsSelectable(true);
        body.setPadding(0, dp(5), 0, 0);
        RichMarkdownRenderer.render(RichMarkdownRenderer.create(this, body), body, message.bodyMarkdown);
        bubble.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.weight = 0;
        bubbleParams.setMargins(message.outbound ? dp(54) : 0, 0, message.outbound ? 0 : dp(54), dp(10));
        row.addView(bubble, bubbleParams);
        return row;
    }

    private GradientDrawable bubbleBackground(boolean outbound) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(outbound ? Color.rgb(215, 246, 237) : Color.WHITE);
        bg.setCornerRadius(dp(8));
        bg.setStroke(1, outbound ? Color.rgb(168, 224, 207) : Color.rgb(224, 231, 228));
        return bg;
    }

    private void sendLocalDraft() {
        String draft = composerInput.getText().toString().trim();
        if (draft.isEmpty()) {
            return;
        }
        if (selectedRoom == null || "setup".equals(selectedRoom.id)) {
            Toast.makeText(this, "Log in before sending", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = AppPrefs.accessToken(this).trim();
        if (token.isEmpty()) {
            Toast.makeText(this, "Missing Matrix access token", Toast.LENGTH_SHORT).show();
            return;
        }
        NativeRoom targetRoom = selectedRoom;
        composerInput.setText("");
        setBusy("Sending...");
        runMatrixTask(() -> {
            String txnId = "mra" + System.currentTimeMillis();
            new MatrixApiClient(AppPrefs.homeserverUrl(this)).sendTextMessage(token, targetRoom.id, draft, txnId);
            runOnUiThread(() -> {
                addLocalMessage(targetRoom.id, new NativeMessage("You", "now", draft, true));
                clearBusy();
                syncNow(false);
            });
        });
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
        TextView contract = text("Publish ntfy messages to the configured topic. A Click header like matrixrich://open?url=https%3A%2F%2Fmatrix.to%2F%23%2F... wakes this native chat surface.", 14, Color.rgb(58, 68, 72), false);
        contract.setPadding(dp(14), dp(12), dp(14), dp(12));
        body.addView(card(contract));

        scroll.addView(body);
        return scroll;
    }

    private View createSettingsPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = panelBody();

        body.addView(sectionTitle("Matrix account"));
        homeserverInput = input("Homeserver URL", AppPrefs.homeserverUrl(this), InputType.TYPE_TEXT_VARIATION_URI);
        accountHintInput = input("Login name", AppPrefs.accountHint(this), InputType.TYPE_CLASS_TEXT);
        passwordInput = input("Password, not saved", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        userIdInput = input("User ID", AppPrefs.userId(this), InputType.TYPE_CLASS_TEXT);
        accessTokenInput = input("Access token", AppPrefs.accessToken(this), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        connectionStatus = text(accountStatus(), 14, Color.rgb(44, 52, 56), false);
        body.addView(card(homeserverInput));
        body.addView(card(accountHintInput));
        body.addView(card(passwordInput));
        body.addView(card(userIdInput));
        body.addView(card(accessTokenInput));
        body.addView(card(connectionStatus));

        LinearLayout accountActions = new LinearLayout(this);
        accountActions.setOrientation(LinearLayout.HORIZONTAL);
        Button login = actionButton("Login");
        login.setOnClickListener((View v) -> loginOrUseToken());
        Button sync = actionButton("Sync");
        sync.setOnClickListener((View v) -> {
            saveSettings(false);
            syncNow(true);
        });
        accountActions.addView(login, new LinearLayout.LayoutParams(0, dp(44), 1));
        accountActions.addView(sync, new LinearLayout.LayoutParams(0, dp(44), 1));
        body.addView(accountActions);

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
        save.setOnClickListener((View v) -> saveSettings(true));
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
        currentTab = tab;
        chatPanel.setVisibility(tab == TAB_CHAT ? View.VISIBLE : View.GONE);
        pushPanel.setVisibility(tab == TAB_PUSH ? View.VISIBLE : View.GONE);
        settingsPanel.setVisibility(tab == TAB_SETTINGS ? View.VISIBLE : View.GONE);
        tintNav(chatNav, tab == TAB_CHAT);
        tintNav(pushNav, tab == TAB_PUSH);
        tintNav(settingsNav, tab == TAB_SETTINGS);
        if (tab == TAB_CHAT) {
            renderSelectedRoom();
        } else if (tab == TAB_PUSH) {
            title.setText("Push");
            subtitle.setText(AppPrefs.pushEnabled(this) ? "ntfy listener enabled" : "ntfy listener disabled");
            updatePushStatus();
        } else {
            title.setText("Settings");
            subtitle.setText("Matrix account and notification bridge");
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

    private void tintRoomButton(Button button, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(selected ? Color.rgb(24, 129, 104) : Color.WHITE);
        bg.setStroke(1, selected ? Color.rgb(24, 129, 104) : Color.rgb(220, 228, 225));
        button.setTextColor(selected ? Color.WHITE : Color.rgb(42, 54, 58));
        button.setBackground(bg);
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
            showWakeNotice(openUrl);
            syncNow(false);
            return;
        }
        Uri data = intent.getData();
        if (data == null) {
            return;
        }
        if ("matrixrich".equals(data.getScheme())) {
            showTab(TAB_CHAT);
            String target = data.getQueryParameter("url");
            showWakeNotice(target == null || target.isEmpty() ? data.toString() : target);
            syncNow(false);
            return;
        }
        if ("ntfy".equals(data.getScheme())) {
            applyNtfyDeepLink(data);
        }
    }

    private void showWakeNotice(String value) {
        if (wakeNotice == null) {
            return;
        }
        wakeNotice.setText("Wake target: " + value);
        wakeNotice.setVisibility(View.VISIBLE);
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
        if (homeserverInput == null) {
            return;
        }
        homeserverInput.setText(AppPrefs.homeserverUrl(this));
        accountHintInput.setText(AppPrefs.accountHint(this));
        passwordInput.setText("");
        userIdInput.setText(AppPrefs.userId(this));
        accessTokenInput.setText(AppPrefs.accessToken(this));
        ntfyServerInput.setText(AppPrefs.ntfyServer(this));
        ntfyTopicInput.setText(AppPrefs.ntfyTopic(this));
        ntfyTokenInput.setText(AppPrefs.ntfyToken(this));
        pushEnabledInput.setChecked(AppPrefs.pushEnabled(this));
        connectionStatus.setText(accountStatus());
    }

    private void saveSettings(boolean toast) {
        SharedPreferences prefs = AppPrefs.get(this);
        prefs.edit()
                .putString(AppPrefs.KEY_HOMESERVER_URL, homeserverInput.getText().toString().trim())
                .putString(AppPrefs.KEY_ACCOUNT_HINT, accountHintInput.getText().toString().trim())
                .putString(AppPrefs.KEY_USER_ID, userIdInput.getText().toString().trim())
                .putString(AppPrefs.KEY_ACCESS_TOKEN, accessTokenInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_SERVER, ntfyServerInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_TOPIC, ntfyTopicInput.getText().toString().trim())
                .putString(AppPrefs.KEY_NTFY_TOKEN, ntfyTokenInput.getText().toString().trim())
                .putBoolean(AppPrefs.KEY_PUSH_ENABLED, pushEnabledInput.isChecked())
                .apply();
        updatePushService();
        showTab(TAB_CHAT);
        if (toast) {
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        }
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

    private void loginOrUseToken() {
        saveSettings(false);
        String password = passwordInput.getText().toString();
        String account = accountHintInput.getText().toString().trim();
        String token = accessTokenInput.getText().toString().trim();
        if (password.isEmpty()) {
            if (token.isEmpty()) {
                Toast.makeText(this, "Enter a password or access token", Toast.LENGTH_SHORT).show();
                return;
            }
            syncNow(true);
            return;
        }
        if (account.isEmpty()) {
            Toast.makeText(this, "Enter a login name", Toast.LENGTH_SHORT).show();
            return;
        }
        setBusy("Logging in...");
        runMatrixTask(() -> {
            MatrixLoginResult login = new MatrixApiClient(homeserverInput.getText().toString()).loginPassword(account, password);
            AppPrefs.get(this).edit()
                    .putString(AppPrefs.KEY_ACCESS_TOKEN, login.accessToken)
                    .putString(AppPrefs.KEY_USER_ID, login.userId)
                    .putString(AppPrefs.KEY_DEVICE_ID, login.deviceId)
                    .putString(AppPrefs.KEY_SYNC_TOKEN, "")
                    .apply();
            runOnUiThread(() -> {
                accessTokenInput.setText(login.accessToken);
                userIdInput.setText(login.userId);
                passwordInput.setText("");
                connectionStatus.setText(accountStatus());
                clearBusy();
                syncNow(true);
            });
        });
    }

    private void syncNow(boolean userInitiated) {
        String token = AppPrefs.accessToken(this).trim();
        if (token.isEmpty()) {
            if (userInitiated) {
                Toast.makeText(this, "Log in or paste an access token first", Toast.LENGTH_SHORT).show();
            }
            setDisconnectedState("Open Settings to log in or paste an access token.");
            renderRooms();
            renderSelectedRoom();
            return;
        }
        setBusy("Syncing...");
        runMatrixTask(() -> {
            MatrixSyncResult result = new MatrixApiClient(AppPrefs.homeserverUrl(this))
                    .sync(token, AppPrefs.syncToken(this), 0, AppPrefs.userId(this));
            if (!result.nextBatch.isEmpty()) {
                AppPrefs.get(this).edit().putString(AppPrefs.KEY_SYNC_TOKEN, result.nextBatch).apply();
            }
            runOnUiThread(() -> {
                applySyncResult(result);
                clearBusy();
            });
        });
    }

    private void applySyncResult(MatrixSyncResult result) {
        if (result.rooms.isEmpty() && rooms.size() == 1 && "setup".equals(rooms.get(0).id)) {
            setDisconnectedState("Connected, but no joined rooms were returned by sync.");
        } else {
            for (NativeRoom room : result.rooms) {
                upsertRoom(room);
                mergeMessages(room.id, result.messagesFor(room.id));
            }
            if ((selectedRoom == null || "setup".equals(selectedRoom.id)) && !rooms.isEmpty()) {
                selectedRoom = rooms.get(0);
            }
        }
        renderRooms();
        renderSelectedRoom();
        if (connectionStatus != null) {
            connectionStatus.setText(accountStatus());
        }
    }

    private void upsertRoom(NativeRoom room) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id.equals(room.id)) {
                rooms.set(i, room);
                return;
            }
        }
        if (rooms.size() == 1 && "setup".equals(rooms.get(0).id)) {
            rooms.clear();
            messagesByRoom.clear();
        }
        rooms.add(room);
    }

    private void mergeMessages(String roomId, List<NativeMessage> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        List<NativeMessage> existing = messagesByRoom.get(roomId);
        if (existing == null) {
            existing = new ArrayList<>();
            messagesByRoom.put(roomId, existing);
        }
        existing.addAll(incoming);
        while (existing.size() > 80) {
            existing.remove(0);
        }
    }

    private void addLocalMessage(String roomId, NativeMessage message) {
        List<NativeMessage> existing = messagesByRoom.get(roomId);
        if (existing == null) {
            existing = new ArrayList<>();
            messagesByRoom.put(roomId, existing);
        }
        existing.add(message);
        renderSelectedRoom();
    }

    private void setDisconnectedState(String message) {
        rooms.clear();
        messagesByRoom.clear();
        NativeRoom setup = new NativeRoom("setup", "Not connected", message, "M", 0);
        rooms.add(setup);
        List<NativeMessage> setupMessages = new ArrayList<>();
        setupMessages.add(new NativeMessage("Matrix Rich", "", message, false));
        messagesByRoom.put(setup.id, setupMessages);
        selectedRoom = setup;
    }

    private String accountStatus() {
        String userId = AppPrefs.userId(this);
        String token = AppPrefs.accessToken(this);
        if (!userId.trim().isEmpty()) {
            return "Signed in as " + userId;
        }
        if (!token.trim().isEmpty()) {
            return "Access token saved";
        }
        return "Not signed in";
    }

    private void setBusy(String value) {
        if (wakeNotice != null) {
            wakeNotice.setText(value);
            wakeNotice.setVisibility(View.VISIBLE);
        }
    }

    private void clearBusy() {
        if (wakeNotice != null) {
            wakeNotice.setVisibility(View.GONE);
        }
    }

    private void runMatrixTask(MatrixTask task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    clearBusy();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    if (connectionStatus != null) {
                        connectionStatus.setText(e.getMessage());
                    }
                });
            }
        }, "matrix-api").start();
    }

    private interface MatrixTask {
        void run() throws Exception;
    }
}
