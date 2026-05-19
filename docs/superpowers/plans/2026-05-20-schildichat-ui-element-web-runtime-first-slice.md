# SchildiChat UI Element Web Runtime First Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first verifiable Android slice of a native SchildiChat-style UI backed by a WebView runtime bridge, without using SchildiChat Legacy's Matrix SDK as the new client runtime.

**Architecture:** Add a new `:rich-client` Android app module beside the SchildiChat fork. The module owns native UI, bridge DTOs, and a single runtime WebView host. For the first slice, the runtime can emit probe/sample events so CI can verify the architecture before live Element Web DOM/login wiring is added.

**Tech Stack:** Android Java/Kotlin-free app module, WebView, JavaScript bridge, RecyclerView, Markwon for markdown/table/math rendering, JUnit tests, GitHub Actions APK build.

---

## File Structure

- Create `rich-client/build.gradle`: standalone Android app module that does not depend on `:matrix-sdk-android`.
- Modify `settings.gradle`: include `:rich-client`.
- Modify `.github/workflows/android.yml`: build `:rich-client:assembleDebug` and upload its APK.
- Create `rich-client/src/main/AndroidManifest.xml`: declare app, internet permission, and main activity.
- Create `rich-client/src/main/java/chat/richclient/MainActivity.java`: native login, room list, room timeline, composer, and hidden runtime host container.
- Create `rich-client/src/main/java/chat/richclient/bridge/BridgeEvent.java`: parse runtime event JSON.
- Create `rich-client/src/main/java/chat/richclient/bridge/BridgeCommand.java`: serialize native commands to JSON.
- Create `rich-client/src/main/java/chat/richclient/bridge/RuntimeBridge.java`: native dispatch boundary between WebView JS and UI state.
- Create `rich-client/src/main/java/chat/richclient/bridge/RuntimeState.java`: reduce bridge events into app state.
- Create `rich-client/src/main/java/chat/richclient/runtime/RuntimeWebViewHost.java`: configure one WebView and inject the JS bridge/probe.
- Create `rich-client/src/main/java/chat/richclient/ui/RichMarkdownRenderer.java`: render markdown/table/math without per-message WebViews.
- Create `rich-client/src/test/java/chat/richclient/bridge/BridgeEventTest.java`: red/green tests for JSON event parsing.
- Create `rich-client/src/test/java/chat/richclient/bridge/BridgeCommandTest.java`: red/green tests for command serialization.
- Create `rich-client/src/test/java/chat/richclient/bridge/RuntimeStateTest.java`: red/green tests for event reduction.

## Task 1: Add Module Skeleton

**Files:**
- Modify: `settings.gradle`
- Create: `rich-client/build.gradle`
- Create: `rich-client/src/main/AndroidManifest.xml`
- Create: `rich-client/src/main/java/chat/richclient/MainActivity.java`

- [ ] **Step 1: Add failing module build target**

Run: `./gradlew --no-daemon :rich-client:assembleDebug --stacktrace`

Expected: FAIL with `Project 'rich-client' not found in root project`.

- [ ] **Step 2: Include the module**

Add to `settings.gradle`:

```groovy
include ':rich-client'
```

- [ ] **Step 3: Create app build file**

Create `rich-client/build.gradle`:

```groovy
apply plugin: 'com.android.application'

android {
    namespace "chat.richclient"
    compileSdk versions.compileSdk

    defaultConfig {
        applicationId "chat.richclient"
        minSdk versions.minSdk
        targetSdk versions.targetSdk
        versionCode 1
        versionName "0.1.0"
    }

    compileOptions {
        sourceCompatibility versions.sourceCompat
        targetCompatibility versions.targetCompat
    }
}

dependencies {
    implementation libs.androidx.recyclerview
    implementation libs.androidx.appCompat
    implementation libs.google.material
    implementation libs.markwon.core
    implementation libs.markwon.extTables
    implementation libs.markwon.extLatex
    implementation libs.markwon.inlineParser

    testImplementation libs.tests.junit
}
```

- [ ] **Step 4: Create manifest and placeholder Activity**

Create `rich-client/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="false"
        android:label="Matrix Rich"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Create `rich-client/src/main/java/chat/richclient/MainActivity.java`:

```java
package chat.richclient;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setText("Matrix Rich runtime shell");
        setContentView(view);
    }
}
```

- [ ] **Step 5: Run build**

Run: `./gradlew --no-daemon :rich-client:assembleDebug --stacktrace`

Expected: PASS and an APK under `rich-client/build/outputs/apk/debug/`.

## Task 2: Bridge DTOs

**Files:**
- Create: `rich-client/src/test/java/chat/richclient/bridge/BridgeEventTest.java`
- Create: `rich-client/src/test/java/chat/richclient/bridge/BridgeCommandTest.java`
- Create: `rich-client/src/main/java/chat/richclient/bridge/BridgeEvent.java`
- Create: `rich-client/src/main/java/chat/richclient/bridge/BridgeCommand.java`

- [ ] **Step 1: Write failing event parse tests**

Create `BridgeEventTest.java`:

```java
package chat.richclient.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class BridgeEventTest {
    @Test
    public void parsesRuntimeReadyEvent() throws Exception {
        BridgeEvent event = BridgeEvent.fromJson("{\"type\":\"runtime.ready\",\"payload\":{\"runtime\":\"element-web\"}}");
        assertEquals("runtime.ready", event.type);
        assertEquals("element-web", event.payload.getString("runtime"));
    }

    @Test
    public void rejectsMissingType() {
        try {
            BridgeEvent.fromJson("{\"payload\":{}}");
            fail("Expected missing type to throw");
        } catch (IllegalArgumentException expected) {
            assertEquals("Bridge event missing type", expected.getMessage());
        }
    }
}
```

- [ ] **Step 2: Write failing command serialization test**

Create `BridgeCommandTest.java`:

```java
package chat.richclient.bridge;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class BridgeCommandTest {
    @Test
    public void serializesLoginPasswordCommand() throws Exception {
        JSONObject payload = new JSONObject()
                .put("homeserver", "https://example.invalid/_matrix/")
                .put("username", "alice")
                .put("password", "secret");
        BridgeCommand command = new BridgeCommand("auth.loginPassword", payload);

        JSONObject json = new JSONObject(command.toJson());

        assertEquals("auth.loginPassword", json.getString("type"));
        assertEquals("alice", json.getJSONObject("payload").getString("username"));
        assertTrue(json.has("id"));
    }
}
```

- [ ] **Step 3: Run tests to verify RED**

Run: `./gradlew --no-daemon :rich-client:testDebugUnitTest --tests chat.richclient.bridge.BridgeEventTest --tests chat.richclient.bridge.BridgeCommandTest`

Expected: FAIL because `BridgeEvent` and `BridgeCommand` do not exist.

- [ ] **Step 4: Implement DTOs**

Create `BridgeEvent.java`:

```java
package chat.richclient.bridge;

import org.json.JSONException;
import org.json.JSONObject;

public final class BridgeEvent {
    public final String type;
    public final JSONObject payload;

    private BridgeEvent(String type, JSONObject payload) {
        this.type = type;
        this.payload = payload;
    }

    public static BridgeEvent fromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            String type = root.optString("type", "");
            if (type.isEmpty()) {
                throw new IllegalArgumentException("Bridge event missing type");
            }
            JSONObject payload = root.optJSONObject("payload");
            return new BridgeEvent(type, payload == null ? new JSONObject() : payload);
        } catch (JSONException failure) {
            throw new IllegalArgumentException("Bridge event is not valid JSON", failure);
        }
    }
}
```

Create `BridgeCommand.java`:

```java
package chat.richclient.bridge;

import java.util.UUID;
import org.json.JSONObject;

public final class BridgeCommand {
    public final String id;
    public final String type;
    public final JSONObject payload;

    public BridgeCommand(String type, JSONObject payload) {
        this(UUID.randomUUID().toString(), type, payload);
    }

    public BridgeCommand(String id, String type, JSONObject payload) {
        this.id = id;
        this.type = type;
        this.payload = payload == null ? new JSONObject() : payload;
    }

    public String toJson() {
        return new JSONObject()
                .put("id", id)
                .put("type", type)
                .put("payload", payload)
                .toString();
    }
}
```

- [ ] **Step 5: Run tests to verify GREEN**

Run: `./gradlew --no-daemon :rich-client:testDebugUnitTest --tests chat.richclient.bridge.BridgeEventTest --tests chat.richclient.bridge.BridgeCommandTest`

Expected: PASS.

## Task 3: Runtime State Reducer

**Files:**
- Create: `rich-client/src/test/java/chat/richclient/bridge/RuntimeStateTest.java`
- Create: `rich-client/src/main/java/chat/richclient/bridge/RuntimeState.java`

- [ ] **Step 1: Write failing reducer tests**

Create `RuntimeStateTest.java`:

```java
package chat.richclient.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class RuntimeStateTest {
    @Test
    public void recordsRuntimeReady() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"runtime.ready\",\"payload\":{\"runtime\":\"element-web\"}}"));
        assertTrue(next.runtimeReady);
        assertEquals("element-web", next.runtimeName);
    }

    @Test
    public void recordsAuthState() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"auth.state\",\"payload\":{\"loggedIn\":true,\"userId\":\"@alice:example.org\"}}"));
        assertTrue(next.loggedIn);
        assertEquals("@alice:example.org", next.userId);
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `./gradlew --no-daemon :rich-client:testDebugUnitTest --tests chat.richclient.bridge.RuntimeStateTest`

Expected: FAIL because `RuntimeState` does not exist.

- [ ] **Step 3: Implement reducer**

Create `RuntimeState.java`:

```java
package chat.richclient.bridge;

public final class RuntimeState {
    public final boolean runtimeReady;
    public final String runtimeName;
    public final boolean loggedIn;
    public final String userId;

    public RuntimeState() {
        this(false, "", false, "");
    }

    private RuntimeState(boolean runtimeReady, String runtimeName, boolean loggedIn, String userId) {
        this.runtimeReady = runtimeReady;
        this.runtimeName = runtimeName;
        this.loggedIn = loggedIn;
        this.userId = userId;
    }

    public RuntimeState reduce(BridgeEvent event) {
        if ("runtime.ready".equals(event.type)) {
            return new RuntimeState(true, event.payload.optString("runtime", runtimeName), loggedIn, userId);
        }
        if ("auth.state".equals(event.type)) {
            return new RuntimeState(runtimeReady, runtimeName, event.payload.optBoolean("loggedIn", loggedIn), event.payload.optString("userId", userId));
        }
        return this;
    }
}
```

- [ ] **Step 4: Run test to verify GREEN**

Run: `./gradlew --no-daemon :rich-client:testDebugUnitTest --tests chat.richclient.bridge.RuntimeStateTest`

Expected: PASS.

## Task 4: WebView Runtime Bridge

**Files:**
- Create: `rich-client/src/main/java/chat/richclient/bridge/RuntimeBridge.java`
- Create: `rich-client/src/main/java/chat/richclient/runtime/RuntimeWebViewHost.java`
- Modify: `rich-client/src/main/java/chat/richclient/MainActivity.java`

- [ ] **Step 1: Implement bridge callback**

Create `RuntimeBridge.java`:

```java
package chat.richclient.bridge;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

public final class RuntimeBridge {
    public interface Listener {
        void onBridgeEvent(BridgeEvent event);
        void onBridgeError(String message);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;

    public RuntimeBridge(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void postEvent(String json) {
        mainHandler.post(() -> {
            try {
                listener.onBridgeEvent(BridgeEvent.fromJson(json));
            } catch (IllegalArgumentException failure) {
                listener.onBridgeError(failure.getMessage());
            }
        });
    }
}
```

- [ ] **Step 2: Implement single WebView host**

Create `RuntimeWebViewHost.java`:

```java
package chat.richclient.runtime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.RuntimeBridge;

public final class RuntimeWebViewHost {
    private final WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    public RuntimeWebViewHost(Context context, RuntimeBridge bridge) {
        webView = new WebView(context.getApplicationContext());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        webView.addJavascriptInterface(bridge, "MatrixRichBridge");
    }

    public WebView view() {
        return webView;
    }

    public void loadProbeRuntime() {
        String html = "<!doctype html><html><body><script>"
                + "window.NativeRuntime={receive:function(command){window.lastCommand=command;}};"
                + "MatrixRichBridge.postEvent(JSON.stringify({type:'runtime.ready',payload:{runtime:'element-web-probe'}}));"
                + "</script></body></html>";
        webView.loadDataWithBaseURL("https://app.element.io/", html, "text/html", "UTF-8", null);
    }

    public void send(BridgeCommand command) {
        String script = "window.NativeRuntime&&window.NativeRuntime.receive(" + JSONObjectEscaper.quote(command.toJson()) + ")";
        webView.evaluateJavascript(script, null);
    }

    private static final class JSONObjectEscaper {
        static String quote(String value) {
            return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
        }
    }
}
```

- [ ] **Step 3: Wire probe runtime to Activity**

Modify `MainActivity.java` to create `RuntimeBridge`, `RuntimeWebViewHost`, and update visible text when `runtime.ready` arrives.

- [ ] **Step 4: Build**

Run: `./gradlew --no-daemon :rich-client:assembleDebug --stacktrace`

Expected: PASS.

## Task 5: Native SchildiChat-Style First UI

**Files:**
- Modify: `rich-client/src/main/java/chat/richclient/MainActivity.java`
- Create: `rich-client/src/main/java/chat/richclient/ui/RichMarkdownRenderer.java`

- [ ] **Step 1: Replace placeholder with native layout**

Use programmatic native views for the first slice:

- top app bar with title `Matrix Rich`;
- login panel with homeserver, username, password fields;
- room list column with sample room rows;
- timeline column with sample incoming/outgoing bubbles;
- composer row with text input and send button;
- hidden 1 px WebView runtime host.

- [ ] **Step 2: Add rich markdown renderer**

Create `RichMarkdownRenderer.java`:

```java
package chat.richclient.ui;

import android.content.Context;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public final class RichMarkdownRenderer {
    private final Markwon markwon;

    public RichMarkdownRenderer(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(textView -> textView.getTextSize()))
                .usePlugin(MarkwonInlineParserPlugin.create())
                .build();
    }

    public void setMarkdown(TextView view, String markdown) {
        markwon.setMarkdown(view, markdown);
    }
}
```

- [ ] **Step 3: Render sample table and formula in timeline**

Add sample message text:

```text
| item | value |
| --- | ---: |
| alpha | 1 |

Inline math $a^2+b^2=c^2$ and block:

$$
E = mc^2
$$
```

- [ ] **Step 4: Build**

Run: `./gradlew --no-daemon :rich-client:assembleDebug --stacktrace`

Expected: PASS.

## Task 6: GitHub Actions Build Artifact

**Files:**
- Modify: `.github/workflows/android.yml`

- [ ] **Step 1: Change Actions target**

Replace the build command with:

```yaml
run: ./gradlew --no-daemon :rich-client:assembleDebug --stacktrace
```

Replace artifact path with:

```yaml
path: rich-client/build/outputs/apk/debug/*.apk
```

Set artifact name:

```yaml
name: matrix-rich-native-ui-web-runtime-debug-apk
```

- [ ] **Step 2: Commit and push**

Run:

```powershell
git add settings.gradle rich-client .github/workflows/android.yml docs/superpowers
git commit -m "feat: add native UI web runtime shell"
git push
```

- [ ] **Step 3: Verify GitHub Actions**

Open the latest Actions run and verify:

- `:rich-client:assembleDebug` completes successfully;
- artifact `matrix-rich-native-ui-web-runtime-debug-apk` is uploaded;
- artifact contains the debug APK.

## Self-Review

- The plan starts with a new module that does not depend on `:matrix-sdk-android`.
- Tests cover bridge parsing, command serialization, and state reduction before implementation.
- The first runtime uses one WebView and does not create per-message or per-formula WebViews.
- The first UI is native and SchildiChat-style, not foreground Element Web.
- Live Element Web DOM login and E2EE wiring are intentionally the next slice after this bridge slice builds and runs.
