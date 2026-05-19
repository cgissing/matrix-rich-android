package chat.richclient.runtime;

import chat.richclient.bridge.BridgeCommand;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

public final class RuntimeConfig {
    public static final String ASSET_DOMAIN = "appassets.androidplatform.net";
    public static final String ELEMENT_PATH = "/element/";
    public static final String ELEMENT_ENTRY_URL = "https://" + ASSET_DOMAIN + ELEMENT_PATH + "index.html";
    public static final String BRIDGE_SCRIPT_NAME = "matrix-rich-runtime.js";

    private RuntimeConfig() {
    }

    public static boolean isLocalRuntimeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && ASSET_DOMAIN.equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith(ELEMENT_PATH);
        } catch (URISyntaxException failure) {
            return false;
        }
    }

    public static String commandDispatchScript(BridgeCommand command) {
        return "window.MatrixRichRuntime&&window.MatrixRichRuntime.receive("
                + JSONObject.quote(command.toJson())
                + ");";
    }
}
