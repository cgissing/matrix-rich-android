package chat.richclient.runtime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.RuntimeBridge;

public final class RuntimeWebViewHost {
    private final Context context;
    private final WebView webView;
    private WebViewAssetLoader assetLoader;
    private String currentRuntimeAuthority = "";
    private String currentRuntimeEntryUrl = "";

    @SuppressLint("SetJavaScriptEnabled")
    public RuntimeWebViewHost(Context context, RuntimeBridge bridge) {
        this.context = context;

        webView = new WebView(context);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.addJavascriptInterface(bridge, "MatrixRichBridge");
        webView.setWebViewClient(new LockedRuntimeWebViewClient());
    }

    public WebView view() {
        return webView;
    }

    public void loadRuntime() {
        loadRuntimeForAuthority(RuntimeConfig.DEFAULT_ASSET_DOMAIN);
    }

    public boolean loadRuntimeForHomeserver(String homeserverUrl) {
        return loadRuntimeForAuthority(RuntimeConfig.runtimeAuthorityForHomeserver(homeserverUrl));
    }

    public boolean hasLoadedRuntime() {
        return !currentRuntimeEntryUrl.isEmpty();
    }

    public void send(BridgeCommand command) {
        webView.evaluateJavascript(RuntimeConfig.commandDispatchScript(command), null);
    }

    private boolean loadRuntimeForAuthority(String runtimeAuthority) {
        String entryUrl = "https://" + runtimeAuthority
                + RuntimeConfig.RUNTIME_PATH + RuntimeConfig.ELEMENT_ASSET_ROOT + "index.html";
        if (entryUrl.equals(currentRuntimeEntryUrl)) {
            return false;
        }

        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(runtimeAuthority)
                .addPathHandler(RuntimeConfig.RUNTIME_PATH, new WebViewAssetLoader.AssetsPathHandler(context))
                .build();
        currentRuntimeAuthority = runtimeAuthority;
        currentRuntimeEntryUrl = entryUrl;
        webView.loadUrl(entryUrl);
        return true;
    }

    private final class LockedRuntimeWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebViewAssetLoader loader = assetLoader;
            return loader == null ? null : loader.shouldInterceptRequest(request.getUrl());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            WebViewAssetLoader loader = assetLoader;
            return loader == null ? null : loader.shouldInterceptRequest(Uri.parse(url));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleNavigation(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleNavigation(Uri.parse(url));
        }

        private boolean handleNavigation(Uri uri) {
            String url = uri == null ? "" : uri.toString();
            if (RuntimeConfig.isLocalRuntimeUrl(url, currentRuntimeAuthority)) {
                return false;
            }
            if (uri == null) {
                return true;
            }
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            return true;
        }
    }
}
