package chat.richclient;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.BridgeEvent;
import chat.richclient.bridge.RuntimeBridge;
import chat.richclient.bridge.RuntimeState;
import chat.richclient.runtime.RuntimeWebViewHost;
import chat.richclient.ui.RichMarkdownRenderer;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements RuntimeBridge.Listener {
    private RuntimeState runtimeState = new RuntimeState();
    private RuntimeWebViewHost runtimeHost;
    private TextView runtimeStatus;
    private TextView authStatus;
    private LinearLayout timeline;
    private RichMarkdownRenderer markdownRenderer;

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

        runtimeHost.loadProbeRuntime();
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
            content.addView(createRoomList(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(154)));
            content.addView(createTimelinePanel(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F));
        } else {
            content.addView(createRoomList(), new LinearLayout.LayoutParams(dp(132), ViewGroup.LayoutParams.MATCH_PARENT));
            content.addView(createTimelinePanel(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1F));
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

        runtimeStatus = new TextView(this);
        runtimeStatus.setText("Runtime starting");
        runtimeStatus.setTextColor(Color.rgb(91, 104, 124));
        runtimeStatus.setTextSize(12);
        toolbar.addView(runtimeStatus);

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

        authStatus = new TextView(this);
        authStatus.setText("Not logged in");
        authStatus.setTextColor(Color.rgb(91, 104, 124));
        authStatus.setTextSize(12);
        panel.addView(authStatus);

        login.setOnClickListener(view -> sendLogin(homeserver, username, password));

        return panel;
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

    private View createRoomList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, dp(10), 0);

        list.addView(roomRow("Hermes", "markdown + E2EE runtime", true));
        list.addView(roomRow("Matrix", "Bridge probe ready", false));
        list.addView(roomRow("Notes", "Native timeline", false));

        return list;
    }

    private View roomRow(String name, String preview, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(panelBackground(
                selected ? Color.rgb(225, 241, 237) : Color.TRANSPARENT,
                selected ? Color.rgb(47, 125, 109) : Color.TRANSPARENT,
                dp(8)));

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(Color.rgb(23, 31, 42));
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title);

        TextView sub = new TextView(this);
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
        return row;
    }

    private View createTimelinePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(panelBackground(Color.WHITE, Color.rgb(226, 232, 240), dp(8)));

        ScrollView scroll = new ScrollView(this);
        timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.VERTICAL);
        timeline.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(timeline);
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F));

        addIncomingMarkdown(sampleMarkdown());
        addOutgoingText("Native SchildiChat-style UI, JS runtime behind the bridge.");

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(10));
        EditText input = compactInput("Message");
        Button send = new Button(this);
        send.setText("Send");
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(44), 1F));
        composer.addView(send, new LinearLayout.LayoutParams(dp(86), dp(44)));
        panel.addView(composer);

        send.setOnClickListener(view -> {
            String text = input.getText().toString();
            if (!text.trim().isEmpty()) {
                addOutgoingText(text);
                input.setText("");
            }
        });

        return panel;
    }

    private String sampleMarkdown() {
        return "| item | value |\n"
                + "| --- | ---: |\n"
                + "| alpha | 1 |\n\n"
                + "Inline math $a^2+b^2=c^2$ and block:\n\n"
                + "$$\nE = mc^2\n$$";
    }

    private void addIncomingMarkdown(String markdown) {
        TextView view = messageView(false);
        markdownRenderer.setMarkdown(view, markdown);
        addBubble(view, false);
    }

    private void addOutgoingText(String text) {
        TextView view = messageView(true);
        view.setText(text);
        addBubble(view, true);
    }

    private TextView messageView(boolean outgoing) {
        TextView view = new TextView(this);
        view.setTextSize(14);
        view.setTextColor(outgoing ? Color.WHITE : Color.rgb(23, 31, 42));
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        view.setBackground(panelBackground(
                outgoing ? Color.rgb(47, 125, 109) : Color.rgb(241, 245, 249),
                Color.TRANSPARENT,
                dp(8)));
        return view;
    }

    private void addBubble(View view, boolean outgoing) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(outgoing ? Gravity.RIGHT : Gravity.LEFT);
        line.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        timeline.addView(line, params);
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

    @Override
    public void onBridgeEvent(BridgeEvent event) {
        runtimeState = runtimeState.reduce(event);
        if (runtimeState.runtimeReady) {
            runtimeStatus.setText("Runtime ready: " + runtimeState.runtimeName);
        }
        if (runtimeState.loggedIn) {
            authStatus.setText("Runtime session: " + runtimeState.userId);
        }
    }

    @Override
    public void onBridgeError(String message) {
        runtimeStatus.setText("Runtime bridge error: " + message);
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
