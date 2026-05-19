package chat.richclient.runtime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import chat.richclient.bridge.BridgeCommand;
import chat.richclient.bridge.RuntimeBridge;
import org.json.JSONObject;

public final class RuntimeWebViewHost {
    private final WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    public RuntimeWebViewHost(Context context, RuntimeBridge bridge) {
        webView = new WebView(context);
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
                + "window.NativeRuntime={receive:function(commandJson){"
                + "var command=JSON.parse(commandJson);"
                + "if(command.type==='auth.loginPassword'){"
                + "MatrixRichBridge.postEvent(JSON.stringify({type:'auth.state',payload:{loggedIn:true,userId:'@'+command.payload.username+':runtime'}}));"
                + "}"
                + "}};"
                + "MatrixRichBridge.postEvent(JSON.stringify({type:'runtime.ready',payload:{runtime:'element-web-probe'}}));"
                + "</script></body></html>";
        webView.loadDataWithBaseURL("https://app.element.io/", html, "text/html", "UTF-8", null);
    }

    public void send(BridgeCommand command) {
        String script = "window.NativeRuntime&&window.NativeRuntime.receive(" + JSONObject.quote(command.toJson()) + ")";
        webView.evaluateJavascript(script, null);
    }
}
