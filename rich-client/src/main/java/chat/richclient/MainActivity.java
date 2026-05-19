package chat.richclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.BridgeEvent;
import chat.richclient.bridge.RuntimeBridge;
import chat.richclient.bridge.RuntimeState;
import chat.richclient.push.RichUnifiedPushReceiver;
import chat.richclient.runtime.RuntimeWebViewHost;
import chat.richclient.ui.RichMarkdownRenderer;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.unifiedpush.android.connector.UnifiedPush;

public class MainActivity extends AppCompatActivity implements RuntimeBridge.Listener {
    private static final String DEFAULT_REACTION_KEY = "\uD83D\uDC4D";

    private RuntimeState runtimeState = new RuntimeState();
    private RuntimeWebViewHost runtimeHost;
    private TextView runtimeStatus;
    private TextView authStatus;
    private TextView syncStatus;
    private TextView pushStatus;
    private TextView typingStatus;
    private LinearLayout roomList;
    private LinearLayout timeline;
    private LinearLayout verificationList;
    private EditText composerInput;
    private RichMarkdownRenderer markdownRenderer;
    private BroadcastReceiver pushReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        markdownRenderer = new RichMarkdownRenderer(this);

        RuntimeBridge bridge = new RuntimeBridge(this);
        runtimeHost = new RuntimeWebViewHost(this, bridge);

        FrameLayout root = new FrameLayout(this);
        root.addView(createMainContent());
        root.addView(runtimeHost.view(), hiddenRuntimeLayoutParams());
        setContentView(root);

        registerPushReceiver();
        runtimeHost.loadRuntime();
        renderState();
        registerUnifiedPush();
    }

    @Override
    protected void onDestroy() {
        if (pushReceiver != null) {
            unregisterReceiver(pushReceiver);
        }
        super.onDestroy();
    }

    private View createMainContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 247, 249));

        root.addView(createToolbar());
        root.addView(createLoginPanel());

        LinearLayout content = new LinearLayout(this);
        boolean compact = getResources().getConfiguration().screenWidthDp < 600;
        content.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1F));

        if (compact) {
            content.addView(createRoomListPanel(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(176)));
            content.addView(createTimelinePanel(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1F));
        } else {
            content.addView(createRoomListPanel(), new LinearLayout.LayoutParams(
                    dp(220),
                    ViewGroup.LayoutParams.MATCH_PARENT));
            content.addView(createTimelinePanel(), new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1F));
        }

        return root;
    }

    private View createToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(dp(16), dp(12), dp(16), dp(10));
        toolbar.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Matrix Rich");
        title.setTextColor(Color.rgb(23, 31, 42));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        toolbar.addView(title);

        runtimeStatus = smallStatus("Runtime starting");
        syncStatus = smallStatus("Sync idle");
        pushStatus = smallStatus("Push not registered");
        toolbar.addView(runtimeStatus);
        toolbar.addView(syncStatus);
        toolbar.addView(pushStatus);

        return toolbar;
    }

    private View createLoginPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        panel.setBackgroundColor(Color.WHITE);

        EditText homeserver = compactInput("Homeserver URL");
        EditText username = compactInput("Username");
        EditText password = compactInput("Password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(homeserver, new LinearLayout.LayoutParams(0, dp(44), 1.4F));
        row.addView(username, new LinearLayout.LayoutParams(0, dp(44), 1F));
        row.addView(password, new LinearLayout.LayoutParams(0, dp(44), 1F));

        Button login = new Button(this);
        login.setText("Login");
        row.addView(login, new LinearLayout.LayoutParams(dp(92), dp(44)));
        panel.addView(row);

        authStatus = smallStatus("Not logged in");
        panel.addView(authStatus);

        login.setOnClickListener(view -> sendLogin(homeserver, username, password));

        return panel;
    }

    private View createRoomListPanel() {
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(true);

        ScrollView vertical = new ScrollView(this);
        roomList = new LinearLayout(this);
        roomList.setOrientation(LinearLayout.VERTICAL);
        roomList.setPadding(0, 0, dp(10), 0);
        vertical.addView(roomList);
        horizontal.addView(vertical);
        return horizontal;
    }

    private View createTimelinePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(panelBackground(Color.WHITE, Color.rgb(226, 232, 240), dp(8)));

        ScrollView scroll = new ScrollView(this);
        timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.VERTICAL);
        timeline.setPadding(dp(12), dp(12), dp(12), dp(8));
        scroll.addView(timeline);
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F));

        typingStatus = smallStatus("");
        typingStatus.setPadding(dp(12), 0, dp(12), dp(4));
        panel.addView(typingStatus);

        verificationList = new LinearLayout(this);
        verificationList.setOrientation(LinearLayout.VERTICAL);
        verificationList.setPadding(dp(10), 0, dp(10), dp(4));
        panel.addView(verificationList);

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(10));
        composerInput = compactInput("Message");
        Button send = new Button(this);
        send.setText("Send");
        composer.addView(composerInput, new LinearLayout.LayoutParams(0, dp(44), 1F));
        composer.addView(send, new LinearLayout.LayoutParams(dp(86), dp(44)));
        panel.addView(composer);

        composerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendTyping(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        send.setOnClickListener(view -> sendComposerMessage());

        return panel;
    }

    private TextView smallStatus(String text) {
        TextView status = new TextView(this);
        status.setText(text);
        status.setTextColor(Color.rgb(91, 104, 124));
        status.setTextSize(12);
        return status;
    }

    private EditText compactInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setPadding(dp(8), 0, dp(8), 0);
        input.setBackground(panelBackground(Color.rgb(245, 247, 250), Color.rgb(218, 224, 232), dp(6)));
        return input;
    }

    private void renderState() {
        renderStatus();
        renderRooms();
        renderTimeline();
        renderVerification();
    }

    private void renderStatus() {
        if (!runtimeState.runtimeError.isEmpty()) {
            runtimeStatus.setText("Runtime error: " + runtimeState.runtimeError);
        } else if (runtimeState.runtimeReady) {
            runtimeStatus.setText("Runtime ready: " + runtimeState.runtimeName);
        } else {
            runtimeStatus.setText("Runtime starting");
        }
        if (runtimeState.loggedIn) {
            authStatus.setText("Session: " + runtimeState.userId);
        } else {
            authStatus.setText("Not logged in");
        }
        syncStatus.setText("Sync: " + runtimeState.syncState
                + (runtimeState.syncError.isEmpty() ? "" : " / " + runtimeState.syncError));
        pushStatus.setText("Push: " + runtimeState.pushStatus);
        typingStatus.setText(runtimeState.typingSummaryForSelectedRoom());
    }

    private void renderRooms() {
        roomList.removeAllViews();
        if (runtimeState.rooms.isEmpty()) {
            roomList.addView(emptyText("Login to load rooms"));
            return;
        }
        for (RuntimeState.RoomSummary room : runtimeState.rooms) {
            roomList.addView(roomRow(room));
        }
    }

    private View roomRow(RuntimeState.RoomSummary room) {
        boolean selected = room.roomId.equals(runtimeState.selectedRoomId);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(panelBackground(
                selected ? Color.rgb(225, 241, 237) : Color.TRANSPARENT,
                selected ? Color.rgb(47, 125, 109) : Color.TRANSPARENT,
                dp(8)));

        TextView title = new TextView(this);
        title.setText(room.name + (room.encrypted ? "  lock" : ""));
        title.setTextColor(Color.rgb(23, 31, 42));
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        row.addView(title);

        TextView sub = new TextView(this);
        String preview = room.lastMessage.isEmpty() ? room.roomId : room.lastMessage;
        if (room.unreadCount > 0) {
            preview = "(" + room.unreadCount + ") " + preview;
        }
        sub.setText(preview);
        sub.setTextColor(Color.rgb(91, 104, 124));
        sub.setTextSize(11);
        sub.setMaxLines(1);
        row.addView(sub);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        row.setOnClickListener(view -> openRoom(room.roomId));
        return row;
    }

    private void renderTimeline() {
        timeline.removeAllViews();
        if (runtimeState.selectedRoomId.isEmpty()) {
            timeline.addView(emptyText("No room selected"));
            return;
        }
        if (runtimeState.timelineForSelectedRoom().isEmpty()) {
            timeline.addView(emptyText("No messages loaded"));
            return;
        }
        for (RuntimeState.TimelineEvent event : runtimeState.timelineForSelectedRoom()) {
            addEventBubble(event);
        }
    }

    private void addEventBubble(RuntimeState.TimelineEvent event) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(10), dp(8), dp(10), dp(8));
        bubble.setBackground(panelBackground(
                event.outgoing ? Color.rgb(47, 125, 109) : Color.rgb(241, 245, 249),
                Color.TRANSPARENT,
                dp(8)));

        TextView sender = new TextView(this);
        sender.setText(event.outgoing ? "You" : event.sender);
        sender.setTextColor(event.outgoing ? Color.rgb(225, 245, 241) : Color.rgb(71, 85, 105));
        sender.setTextSize(11);
        bubble.addView(sender);

        TextView body = new TextView(this);
        body.setTextSize(14);
        body.setTextColor(event.outgoing ? Color.WHITE : Color.rgb(23, 31, 42));
        markdownRenderer.setMarkdown(body, event.body);
        bubble.addView(body);

        LinearLayout reactions = new LinearLayout(this);
        reactions.setOrientation(LinearLayout.HORIZONTAL);
        for (RuntimeState.ReactionSummary reaction : runtimeState.reactionsForEvent(event.eventId)) {
            reactions.addView(reactionButton(event, reaction));
        }
        if (reactions.getChildCount() > 0) {
            bubble.addView(reactions);
        }

        bubble.setOnLongClickListener(view -> {
            sendReaction(event.eventId, DEFAULT_REACTION_KEY);
            return true;
        });

        LinearLayout line = new LinearLayout(this);
        line.setGravity(event.outgoing ? Gravity.RIGHT : Gravity.LEFT);
        line.addView(bubble, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        timeline.addView(line, params);
    }

    private View reactionButton(RuntimeState.TimelineEvent event, RuntimeState.ReactionSummary reaction) {
        TextView button = new TextView(this);
        button.setText(reaction.key + " " + reaction.count);
        button.setTextSize(12);
        button.setTextColor(reaction.selected ? Color.rgb(47, 125, 109) : Color.rgb(71, 85, 105));
        button.setPadding(dp(8), dp(3), dp(8), dp(3));
        button.setBackground(panelBackground(Color.WHITE, Color.rgb(203, 213, 225), dp(10)));
        button.setOnClickListener(view -> sendReaction(event.eventId, reaction.key));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), dp(6), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void renderVerification() {
        verificationList.removeAllViews();
        for (RuntimeState.VerificationSummary verification : runtimeState.verifications) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            row.setBackground(panelBackground(Color.rgb(255, 247, 237), Color.rgb(251, 146, 60), dp(8)));

            TextView label = smallStatus("Verification " + verification.state + " from " + verification.userId);
            label.setTextColor(Color.rgb(124, 45, 18));
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F));

            Button accept = new Button(this);
            accept.setText("Accept");
            accept.setOnClickListener(view -> sendVerificationAction(verification, "accept"));
            row.addView(accept, new LinearLayout.LayoutParams(dp(92), dp(40)));
            verificationList.addView(row);
        }
    }

    private TextView emptyText(String text) {
        TextView view = smallStatus(text);
        view.setPadding(dp(10), dp(10), dp(10), dp(10));
        return view;
    }

    private void sendLogin(EditText homeserver, EditText username, EditText password) {
        try {
            JSONObject payload = new JSONObject()
                    .put("homeserver", homeserver.getText().toString())
                    .put("username", username.getText().toString())
                    .put("password", password.getText().toString());
            runtimeHost.send(new BridgeCommand("auth.loginPassword", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to create login command");
        }
    }

    private void openRoom(String roomId) {
        runtimeState = runtimeState.selectRoom(roomId);
        renderState();
        try {
            runtimeHost.send(new BridgeCommand("rooms.open", new JSONObject().put("roomId", roomId)));
        } catch (JSONException failure) {
            onBridgeError("Unable to open room");
        }
    }

    private void sendComposerMessage() {
        String text = composerInput.getText().toString();
        if (runtimeState.selectedRoomId.isEmpty() || text.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("roomId", runtimeState.selectedRoomId)
                    .put("body", text);
            runtimeHost.send(new BridgeCommand("messages.sendText", payload));
            composerInput.setText("");
            sendTyping(false);
        } catch (JSONException failure) {
            onBridgeError("Unable to send message");
        }
    }

    private void sendTyping(boolean typing) {
        if (runtimeState.selectedRoomId.isEmpty()) {
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("roomId", runtimeState.selectedRoomId)
                    .put("typing", typing)
                    .put("timeout", 30000);
            runtimeHost.send(new BridgeCommand("typing.set", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to send typing state");
        }
    }

    private void sendReaction(String eventId, String key) {
        if (runtimeState.selectedRoomId.isEmpty()) {
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("roomId", runtimeState.selectedRoomId)
                    .put("eventId", eventId)
                    .put("key", key);
            runtimeHost.send(new BridgeCommand("reactions.send", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to send reaction");
        }
    }

    private void sendVerificationAction(RuntimeState.VerificationSummary verification, String action) {
        try {
            JSONObject payload = new JSONObject()
                    .put("transactionId", verification.transactionId)
                    .put("userId", verification.userId)
                    .put("action", action);
            runtimeHost.send(new BridgeCommand("verification.action", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to send verification action");
        }
    }

    @Override
    public void onBridgeEvent(BridgeEvent event) {
        runtimeState = runtimeState.reduce(event);
        renderState();
        if ("auth.state".equals(event.type) && runtimeState.loggedIn) {
            registerUnifiedPush();
            sendPushEndpointIfAvailable();
            requestRoomsSnapshot();
        }
    }

    @Override
    public void onBridgeError(String message) {
        runtimeStatus.setText("Runtime bridge error: " + message);
    }

    private void registerUnifiedPush() {
        try {
            ArrayList<String> features = new ArrayList<>();
            features.add(UnifiedPush.FEATURE_BYTES_MESSAGE);
            String distributor = UnifiedPush.getDistributor(this);
            if (distributor.isEmpty()) {
                List<String> distributors = UnifiedPush.getDistributors(this, features);
                if (distributors.size() == 1) {
                    UnifiedPush.saveDistributor(this, distributors.get(0));
                } else {
                    runtimeState = runtimeState.reduce(BridgeEvent.fromJson("{\"type\":\"push.registrationState\",\"payload\":{\"registered\":false,\"status\":\"choose ntfy distributor\"}}"));
                    renderState();
                    return;
                }
            }
            UnifiedPush.registerApp(this, "default", features, "");
            if (runtimeState.loggedIn) {
                sendPushEndpointIfAvailable();
            }
        } catch (Throwable failure) {
            runtimeState = runtimeState.reduce(BridgeEvent.fromJson("{\"type\":\"push.registrationState\",\"payload\":{\"registered\":false,\"status\":\"UnifiedPush unavailable\"}}"));
            renderState();
        }
    }

    private void sendPushEndpointIfAvailable() {
        if (!runtimeState.loggedIn) {
            return;
        }
        String endpoint = RichUnifiedPushReceiver.storedEndpoint(this);
        if (endpoint == null || endpoint.isEmpty()) {
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("endpoint", endpoint)
                    .put("gateway", endpoint)
                    .put("appId", getPackageName() + ".unifiedpush");
            runtimeHost.send(new BridgeCommand("push.register", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to register push endpoint");
        }
    }

    private void requestRoomsSnapshot() {
        runtimeHost.send(new BridgeCommand("rooms.subscribe", new JSONObject()));
    }

    private void registerPushReceiver() {
        pushReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (RichUnifiedPushReceiver.ACTION_NEW_ENDPOINT.equals(action)) {
                    sendPushEndpointIfAvailable();
                } else if (RichUnifiedPushReceiver.ACTION_MESSAGE.equals(action)) {
                    runtimeHost.send(new BridgeCommand("sync.once", new JSONObject()));
                } else if (RichUnifiedPushReceiver.ACTION_REGISTRATION_FAILED.equals(action)) {
                    runtimeState = runtimeState.reduce(BridgeEvent.fromJson("{\"type\":\"push.registrationState\",\"payload\":{\"registered\":false,\"status\":\"UnifiedPush registration failed\"}}"));
                    renderState();
                } else if (RichUnifiedPushReceiver.ACTION_UNREGISTERED.equals(action)) {
                    runtimeState = runtimeState.reduce(BridgeEvent.fromJson("{\"type\":\"push.registrationState\",\"payload\":{\"registered\":false,\"status\":\"unregistered\",\"endpoint\":\"\"}}"));
                    renderState();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(RichUnifiedPushReceiver.ACTION_NEW_ENDPOINT);
        filter.addAction(RichUnifiedPushReceiver.ACTION_MESSAGE);
        filter.addAction(RichUnifiedPushReceiver.ACTION_REGISTRATION_FAILED);
        filter.addAction(RichUnifiedPushReceiver.ACTION_UNREGISTERED);
        ContextCompat.registerReceiver(this, pushReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private FrameLayout.LayoutParams hiddenRuntimeLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(1), dp(1));
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        WebView view = runtimeHost.view();
        view.setAlpha(0F);
        return params;
    }

    private GradientDrawable panelBackground(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5F);
    }
}
