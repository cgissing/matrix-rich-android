package chat.richclient.runtime;

import chat.richclient.bridge.BridgeCommand;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

public final class RuntimeConfig {
    public static final String DEFAULT_ASSET_DOMAIN = "appassets.androidplatform.net";
    public static final String RUNTIME_PATH = "/_matrix_rich_runtime/";
    public static final String ELEMENT_ASSET_ROOT = "element/";
    public static final String ELEMENT_ENTRY_URL = "https://" + DEFAULT_ASSET_DOMAIN
            + RUNTIME_PATH + ELEMENT_ASSET_ROOT + "index.html";
    public static final String BRIDGE_SCRIPT_NAME = "matrix-rich-runtime.js";

    private RuntimeConfig() {
    }

    public static boolean isLocalRuntimeUrl(String url) {
        return isLocalRuntimeUrl(url, DEFAULT_ASSET_DOMAIN);
    }

    public static boolean isLocalRuntimeUrl(String url, String runtimeAuthority) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        if (runtimeAuthority == null || runtimeAuthority.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && runtimeAuthority.equalsIgnoreCase(uri.getRawAuthority())
                    && uri.getPath() != null
                    && uri.getPath().startsWith(RUNTIME_PATH);
        } catch (URISyntaxException failure) {
            return false;
        }
    }

    public static String runtimeAuthorityForHomeserver(String homeserverUrl) {
        return parseHomeserver(homeserverUrl).getRawAuthority();
    }

    public static String runtimeEntryUrlForHomeserver(String homeserverUrl) {
        return runtimeEntryUrl(runtimeAuthorityForHomeserver(homeserverUrl));
    }

    public static String commandDispatchScript(BridgeCommand command) {
        return "window.MatrixRichRuntime&&window.MatrixRichRuntime.receive("
                + JSONObject.quote(command.toJson())
                + ");";
    }

    private static String runtimeEntryUrl(String authority) {
        return "https://" + authority + RUNTIME_PATH + ELEMENT_ASSET_ROOT + "index.html";
    }

    private static URI parseHomeserver(String homeserverUrl) {
        String value = homeserverUrl == null ? "" : homeserverUrl.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Homeserver URL is required");
        }
        if (!value.contains("://")) {
            value = "https://" + value;
        }
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Homeserver URL must use https");
            }
            if (uri.getHost() == null || uri.getRawAuthority() == null || uri.getRawAuthority().isEmpty()) {
                throw new IllegalArgumentException("Homeserver URL must include a host");
            }
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Homeserver URL must not include user info");
            }
            return uri;
        } catch (URISyntaxException failure) {
            throw new IllegalArgumentException("Homeserver URL is invalid", failure);
        }
    }
}
