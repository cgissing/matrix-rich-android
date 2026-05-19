package io.github.cgissing.matrixrich;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class MatrixHomeserverResolver {
    interface Fetcher {
        String get(String url) throws Exception;
    }

    static final class Resolved {
        final String input;
        final String baseUrl;
        final String source;

        Resolved(String input, String baseUrl, String source) {
            this.input = input;
            this.baseUrl = baseUrl;
            this.source = source;
        }
    }

    private MatrixHomeserverResolver() {
    }

    static Resolved resolve(String value) {
        return resolve(value, MatrixHomeserverResolver::httpGet);
    }

    static Resolved resolve(String value, Fetcher fetcher) {
        String input = MatrixApiClient.normalizeHomeserver(value);

        String elementBase = elementWebHomeserverBase(fetchJson(fetcher, input + "/config.json"));
        if (!elementBase.isEmpty()) {
            String baseUrl = MatrixApiClient.normalizeHomeserver(elementBase);
            return new Resolved(input, baseUrl, baseUrl.equals(input) ? "direct" : "element-web-config");
        }

        if (isMatrixVersionsResponse(fetchJson(fetcher, input + "/_matrix/client/versions"))) {
            return new Resolved(input, input, "direct");
        }

        String wellKnownBase = "";
        try {
            URI uri = URI.create(input);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            wellKnownBase = wellKnownHomeserverBase(fetchJson(fetcher, origin + "/.well-known/matrix/client"));
        } catch (Exception ignored) {
        }
        if (!wellKnownBase.isEmpty()) {
            String baseUrl = MatrixApiClient.normalizeHomeserver(wellKnownBase);
            return new Resolved(input, baseUrl, baseUrl.equals(input) ? "direct" : "well-known");
        }

        return new Resolved(input, input, "direct");
    }

    private static JSONObject fetchJson(Fetcher fetcher, String url) {
        try {
            String body = fetcher.get(url);
            if (body == null || body.trim().isEmpty()) {
                return null;
            }
            return new JSONObject(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String elementWebHomeserverBase(JSONObject config) {
        if (config == null) {
            return "";
        }
        JSONObject defaultConfig = config.optJSONObject("default_server_config");
        JSONObject homeserver = defaultConfig == null ? null : defaultConfig.optJSONObject("m.homeserver");
        String baseUrl = homeserver == null ? "" : homeserver.optString("base_url", "");
        if (!baseUrl.trim().isEmpty()) {
            return baseUrl;
        }
        return config.optString("default_hs_url", "");
    }

    private static String wellKnownHomeserverBase(JSONObject config) {
        if (config == null) {
            return "";
        }
        JSONObject homeserver = config.optJSONObject("m.homeserver");
        return homeserver == null ? "" : homeserver.optString("base_url", "");
    }

    private static boolean isMatrixVersionsResponse(JSONObject config) {
        return config != null && config.optJSONArray("versions") != null;
    }

    private static String httpGet(String value) throws Exception {
        URL url = new URL(value);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return "";
            }
            try (InputStream input = connection.getInputStream()) {
                return readUtf8(input);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readUtf8(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }
}
