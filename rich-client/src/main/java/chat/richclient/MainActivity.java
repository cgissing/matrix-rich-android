package chat.richclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.BridgeEvent;
import chat.richclient.bridge.RuntimeBridge;
import chat.richclient.bridge.RuntimeState;
import chat.richclient.push.RichUnifiedPushReceiver;
import chat.richclient.runtime.RuntimeWebViewHost;
import chat.richclient.ui.RichMarkdownRenderer;
import chat.richclient.ui.RoomListAdapter;
import chat.richclient.ui.TimelineAdapter;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.unifiedpush.android.connector.UnifiedPush;

public class MainActivity extends AppCompatActivity implements RuntimeBridge.Listener {
    private RuntimeState runtimeState = new RuntimeState();
    private RuntimeWebViewHost runtimeHost;
    private RoomListAdapter roomListAdapter;
    private TimelineAdapter timelineAdapter;
    private LinearLayout loginPanel;
    private LinearLayout chatContainer;
    private LinearLayout roomListPane;
    private LinearLayout roomDetailPane;
    private View masterDetailDivider;
    private TextView runtimeStatus;
    private TextView authStatus;
    private TextView syncStatus;
    private TextView pushStatus;
    private TextView roomListHeader;
    private TextView roomToolbarAvatar;
    private ImageView roomToolbarDecoration;
    private ImageButton roomToolbarBackButton;
    private TextView roomToolbarTitle;
    private TextView roomToolbarSubtitle;
    private LinearLayout verificationList;
    private TextView typingStatus;
    private EditText homeserverInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText composerInput;
    private RecyclerView timelineRecyclerView;
    private BroadcastReceiver pushReceiver;
    private boolean showingRoomList = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_home);

        RichMarkdownRenderer markdownRenderer = new RichMarkdownRenderer(this);
        RuntimeBridge bridge = new RuntimeBridge(this);
        runtimeHost = new RuntimeWebViewHost(this, bridge);

        bindViews();
        configureLists(markdownRenderer);
        configureActions();
        attachHiddenRuntime();

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

    @Override
    public void onBackPressed() {
        if (isCompact() && !showingRoomList) {
            showingRoomList = true;
            renderPanes();
            return;
        }
        super.onBackPressed();
    }

    private void bindViews() {
        loginPanel = findViewById(R.id.loginPanel);
        chatContainer = findViewById(R.id.chatContainer);
        roomListPane = findViewById(R.id.roomListPane);
        roomDetailPane = findViewById(R.id.roomDetailPane);
        masterDetailDivider = findViewById(R.id.masterDetailDivider);
        runtimeStatus = findViewById(R.id.runtimeStatus);
        authStatus = findViewById(R.id.authStatus);
        syncStatus = findViewById(R.id.syncStatus);
        pushStatus = findViewById(R.id.pushStatus);
        roomListHeader = findViewById(R.id.roomListHeader);
        roomToolbarAvatar = findViewById(R.id.roomToolbarAvatarImageView);
        roomToolbarDecoration = findViewById(R.id.roomToolbarDecorationImageView);
        roomToolbarBackButton = findViewById(R.id.roomToolbarBackButton);
        roomToolbarTitle = findViewById(R.id.roomToolbarTitleView);
        roomToolbarSubtitle = findViewById(R.id.roomToolbarSubtitleView);
        verificationList = findViewById(R.id.verificationList);
        typingStatus = findViewById(R.id.typingStatus);
        homeserverInput = findViewById(R.id.homeserverInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        composerInput = findViewById(R.id.composerEditText);
        timelineRecyclerView = findViewById(R.id.timelineRecyclerView);
    }

    private void configureLists(RichMarkdownRenderer markdownRenderer) {
        RecyclerView roomRecyclerView = findViewById(R.id.roomRecyclerView);
        roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomListAdapter = new RoomListAdapter(this::openRoom);
        roomRecyclerView.setAdapter(roomListAdapter);

        LinearLayoutManager timelineLayoutManager = new LinearLayoutManager(this);
        timelineLayoutManager.setStackFromEnd(true);
        timelineRecyclerView.setLayoutManager(timelineLayoutManager);
        timelineAdapter = new TimelineAdapter(markdownRenderer, this::sendReaction);
        timelineRecyclerView.setAdapter(timelineAdapter);
    }

    private void configureActions() {
        MaterialButton loginButton = findViewById(R.id.loginButton);
        ImageButton sendButton = findViewById(R.id.sendButton);

        loginButton.setOnClickListener(view -> sendLogin());
        sendButton.setOnClickListener(view -> sendComposerMessage());
        roomToolbarBackButton.setOnClickListener(view -> {
            showingRoomList = true;
            renderPanes();
        });
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
    }

    private void attachHiddenRuntime() {
        FrameLayout runtimeContainer = findViewById(R.id.runtimeContainer);
        WebView view = runtimeHost.view();
        view.setAlpha(0F);
        runtimeContainer.addView(view, new FrameLayout.LayoutParams(dp(1), dp(1)));
    }

    private void renderState() {
        renderStatus();
        renderRoomToolbar();
        renderVerification();
        roomListAdapter.submit(runtimeState);
        timelineAdapter.submit(runtimeState);
        scrollTimelineToBottom();
        renderPanes();
    }

    private void renderStatus() {
        if (!runtimeState.runtimeError.isEmpty()) {
            runtimeStatus.setText("Runtime error: " + runtimeState.runtimeError);
        } else if (runtimeState.runtimeReady) {
            runtimeStatus.setText("Runtime ready: " + runtimeState.runtimeName);
        } else {
            runtimeStatus.setText("Runtime starting");
        }
        authStatus.setText(runtimeState.loggedIn ? "Session: " + runtimeState.userId : "Not logged in");
        syncStatus.setText("Sync: " + runtimeState.syncState
                + (runtimeState.syncError.isEmpty() ? "" : " / " + runtimeState.syncError));
        pushStatus.setText("Push: " + runtimeState.pushStatus);
        typingStatus.setText(runtimeState.typingSummaryForSelectedRoom());
        roomListHeader.setText(runtimeState.rooms.isEmpty() ? "Rooms" : "Rooms (" + runtimeState.rooms.size() + ")");
    }

    private void renderRoomToolbar() {
        RuntimeState.RoomSummary selected = selectedRoom();
        if (selected == null) {
            roomToolbarTitle.setText("No room selected");
            roomToolbarSubtitle.setText(runtimeState.loggedIn ? "Choose a room" : "Login to load rooms");
            roomToolbarAvatar.setText("M");
            roomToolbarDecoration.setVisibility(View.GONE);
            return;
        }

        String name = selected.name.isEmpty() ? selected.roomId : selected.name;
        roomToolbarTitle.setText(name);
        roomToolbarSubtitle.setText(selected.lastMessage.isEmpty() ? selected.roomId : selected.lastMessage);
        roomToolbarAvatar.setText(initials(name));
        roomToolbarDecoration.setVisibility(selected.encrypted ? View.VISIBLE : View.GONE);
    }

    private void renderVerification() {
        verificationList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (RuntimeState.VerificationSummary verification : runtimeState.verifications) {
            View row = inflater.inflate(R.layout.view_rich_verification, verificationList, false);
            TextView label = row.findViewById(R.id.verificationLabel);
            MaterialButton accept = row.findViewById(R.id.verificationAcceptButton);
            label.setText("Verification " + verification.state + " from " + verification.userId);
            accept.setOnClickListener(view -> sendVerificationAction(verification, "accept"));
            verificationList.addView(row);
        }
        verificationList.setVisibility(runtimeState.verifications.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderPanes() {
        boolean hasSessionSurface = runtimeState.loggedIn || !runtimeState.rooms.isEmpty();
        loginPanel.setVisibility(runtimeState.loggedIn ? View.GONE : View.VISIBLE);
        chatContainer.setVisibility(hasSessionSurface ? View.VISIBLE : View.GONE);
        composerInput.setEnabled(!runtimeState.selectedRoomId.isEmpty());

        if (isCompact()) {
            LinearLayout.LayoutParams roomParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            roomListPane.setLayoutParams(roomParams);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            roomDetailPane.setLayoutParams(detailParams);
            roomListPane.setVisibility(showingRoomList ? View.VISIBLE : View.GONE);
            roomDetailPane.setVisibility(showingRoomList ? View.GONE : View.VISIBLE);
            masterDetailDivider.setVisibility(View.GONE);
            roomToolbarBackButton.setVisibility(showingRoomList ? View.GONE : View.VISIBLE);
        } else {
            LinearLayout.LayoutParams roomParams = new LinearLayout.LayoutParams(
                    dp(320),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            roomListPane.setLayoutParams(roomParams);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1F);
            roomDetailPane.setLayoutParams(detailParams);
            roomListPane.setVisibility(View.VISIBLE);
            roomDetailPane.setVisibility(View.VISIBLE);
            masterDetailDivider.setVisibility(View.VISIBLE);
            roomToolbarBackButton.setVisibility(View.GONE);
        }
    }

    private void scrollTimelineToBottom() {
        int count = timelineAdapter.getItemCount();
        if (count > 0) {
            timelineRecyclerView.scrollToPosition(count - 1);
        }
    }

    private void sendLogin() {
        try {
            JSONObject payload = new JSONObject()
                    .put("homeserver", homeserverInput.getText().toString())
                    .put("username", usernameInput.getText().toString())
                    .put("password", passwordInput.getText().toString());
            runtimeHost.send(new BridgeCommand("auth.loginPassword", payload));
        } catch (JSONException failure) {
            onBridgeError("Unable to create login command");
        }
    }

    private void openRoom(String roomId) {
        showingRoomList = false;
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

    private RuntimeState.RoomSummary selectedRoom() {
        for (RuntimeState.RoomSummary room : runtimeState.rooms) {
            if (room.roomId.equals(runtimeState.selectedRoomId)) {
                return room;
            }
        }
        return null;
    }

    private boolean isCompact() {
        return getResources().getConfiguration().screenWidthDp < 600;
    }

    private String initials(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        char first = trimmed.charAt(0);
        if (first == '!' || first == '#' || first == '@') {
            return String.valueOf(Character.toUpperCase(trimmed.length() > 1 ? trimmed.charAt(1) : first));
        }
        return String.valueOf(Character.toUpperCase(first));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5F);
    }
}
