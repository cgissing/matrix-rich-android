package xyz.luoyu.matrixrich;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class NtfyEndpoint {
    private NtfyEndpoint() {
    }

    public static String jsonStreamUrl(String server, String topic) {
        String base = normalizeServer(server);
        return base + "/" + encodeTopic(topic) + "/json";
    }

    static String normalizeServer(String server) {
        String trimmed = server == null ? "" : server.trim();
        if (trimmed.isEmpty()) {
            trimmed = AppPrefs.DEFAULT_NTFY_SERVER;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encodeTopic(String topic) {
        try {
            return URLEncoder.encode(topic == null ? "" : topic.trim(), "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }
}
