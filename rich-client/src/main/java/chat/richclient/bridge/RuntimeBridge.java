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
