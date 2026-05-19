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
    private final WebViewAssetLoader assetLoader;

    @SuppressLint("SetJavaScriptEnabled")
    public RuntimeWebViewHost(Context context, RuntimeBridge bridge) {
        this.context = context;
        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(RuntimeConfig.ASSET_DOMAIN)
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(context))
                .build();

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
        webView.loadUrl(RuntimeConfig.ELEMENT_ENTRY_URL);
    }

    public void send(BridgeCommand command) {
        webView.evaluateJavascript(RuntimeConfig.commandDispatchScript(command), null);
    }

    private final class LockedRuntimeWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return assetLoader.shouldInterceptRequest(request.getUrl());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return assetLoader.shouldInterceptRequest(Uri.parse(url));
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
            if (RuntimeConfig.isLocalRuntimeUrl(url)) {
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
