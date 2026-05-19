package io.github.cgissing.matrixrich;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class HeadlessMatrixRuntime {
    public interface Listener {
        void onRuntimeReady();

        void onRuntimeStatus(String status);

        void onLoginResult(MatrixLoginResult result);

        void onSyncSnapshot(MatrixSyncResult result);

        void onVerificationUpdate(MatrixVerificationState state);

        void onSendComplete(String roomId, String body);

        void onRuntimeError(String message);
    }

    private static final String RUNTIME_URL = "https://appassets.androidplatform.net/assets/matrix-runtime/index.html";

    private final Activity activity;
    private final Listener listener;
    private final Queue<String> pendingCommands = new ArrayDeque<>();
    private final ExecutorService resolverExecutor = Executors.newSingleThreadExecutor();
    private WebView webView;
    private boolean ready;

    public HeadlessMatrixRuntime(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public void attach(ViewGroup host) {
        webView = new WebView(activity);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setAlpha(0f);
        webView.setVisibility(View.INVISIBLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(activity))
                .build();
        webView.addJavascriptInterface(new Bridge(), "AndroidMatrixRuntime");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });
        host.addView(webView, new ViewGroup.LayoutParams(1, 1));
        webView.loadUrl(RUNTIME_URL);
    }

    public void destroy() {
        if (webView == null) {
            return;
        }
        webView.destroy();
        webView = null;
        ready = false;
        pendingCommands.clear();
        resolverExecutor.shutdownNow();
    }

    public void loginPassword(String homeserver, String account, String password, String recoveryKey) {
        dispatchWithResolvedHomeserver("loginPassword", homeserver, resolvedHomeserver ->
                put(put(put(put(new JSONObject(),
                        "homeserver", resolvedHomeserver),
                        "account", account),
                        "password", password),
                        "recoveryKey", recoveryKey)
        );
    }

    public void startSession(String homeserver, String userId, String deviceId, String accessToken, String recoveryKey) {
        dispatchWithResolvedHomeserver("startSession", homeserver, resolvedHomeserver ->
                put(put(put(put(put(new JSONObject(),
                        "homeserver", resolvedHomeserver),
                        "userId", userId),
                        "deviceId", deviceId),
                        "accessToken", accessToken),
                        "recoveryKey", recoveryKey)
        );
    }

    public void requestSnapshot() {
        dispatch("snapshot", new JSONObject());
    }

    public void sendText(String roomId, String body) {
        JSONObject payload = put(put(new JSONObject(), "roomId", roomId), "body", body);
        dispatch("sendText", payload);
    }

    public void startOwnVerification() {
        dispatch("startOwnVerification", new JSONObject());
    }

    public void acceptVerification() {
        dispatch("acceptVerification", new JSONObject());
    }

    public void startSasVerification() {
        dispatch("startSasVerification", new JSONObject());
    }

    public void generateQrVerification() {
        dispatch("generateQrVerification", new JSONObject());
    }

    public void scanQrVerification(String qrCodeBase64) {
        dispatch("scanQrVerification", put(new JSONObject(), "qrCodeBase64", qrCodeBase64));
    }

    public void confirmQrVerification() {
        dispatch("confirmQrVerification", new JSONObject());
    }

    public void confirmSasVerification() {
        dispatch("confirmSasVerification", new JSONObject());
    }

    public void mismatchSasVerification() {
        dispatch("mismatchSasVerification", new JSONObject());
    }

    public void cancelVerification() {
        dispatch("cancelVerification", new JSONObject());
    }

    private void dispatch(String op, JSONObject payload) {
        JSONObject envelope = put(put(new JSONObject(), "op", op), "payload", payload);
        String command = envelope.toString();
        if (!ready || webView == null) {
            pendingCommands.add(command);
            return;
        }
        evaluate(command);
    }

    private void dispatchWithResolvedHomeserver(String op, String homeserver, PayloadFactory payloadFactory) {
        try {
            resolverExecutor.execute(() -> {
                MatrixHomeserverResolver.Resolved resolved = MatrixHomeserverResolver.resolve(homeserver);
                JSONObject payload = payloadFactory.create(resolved.baseUrl);
                activity.runOnUiThread(() -> {
                    if (!"direct".equals(resolved.source)) {
                        listener.onRuntimeStatus("Resolved " + resolved.input + " to " + resolved.baseUrl);
                    }
                    dispatch(op, payload);
                });
            });
        } catch (RejectedExecutionException e) {
            listener.onRuntimeError("Matrix runtime is shutting down.");
        }
    }

    private void evaluate(String command) {
        String js = "window.MatrixRichRuntime.dispatch(" + JSONObject.quote(command) + ")";
        webView.evaluateJavascript(js, null);
    }

    private static JSONObject put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
            return object;
        } catch (Exception e) {
            throw new IllegalStateException("Could not build Matrix runtime command", e);
        }
    }

    private void markReady() {
        ready = true;
        listener.onRuntimeReady();
        while (!pendingCommands.isEmpty()) {
            evaluate(pendingCommands.remove());
        }
    }

    private void handleRuntimeMessage(String rawMessage) {
        activity.runOnUiThread(() -> {
            try {
                JSONObject message = new JSONObject(rawMessage);
                String type = message.optString("type");
                JSONObject payload = message.optJSONObject("payload");
                if ("ready".equals(type)) {
                    markReady();
                } else if ("status".equals(type)) {
                    listener.onRuntimeStatus(message.optString("message"));
                } else if ("login".equals(type) && payload != null) {
                    listener.onLoginResult(MatrixRuntimeMapper.loginFromPayload(payload));
                } else if ("snapshot".equals(type) && payload != null) {
                    listener.onSyncSnapshot(MatrixRuntimeMapper.syncFromSnapshot(payload));
                } else if ("verification".equals(type) && payload != null) {
                    listener.onVerificationUpdate(MatrixRuntimeMapper.verificationFromPayload(payload));
                } else if ("send".equals(type) && payload != null) {
                    listener.onSendComplete(payload.optString("roomId"), payload.optString("body"));
                } else if ("error".equals(type)) {
                    listener.onRuntimeError(message.optString("message", "Matrix runtime error"));
                }
            } catch (Exception e) {
                listener.onRuntimeError(e.getMessage());
            }
        });
    }

    private interface PayloadFactory {
        JSONObject create(String homeserver);
    }

    private final class Bridge {
        @JavascriptInterface
        public void postMessage(String message) {
            handleRuntimeMessage(message);
        }
    }
}
