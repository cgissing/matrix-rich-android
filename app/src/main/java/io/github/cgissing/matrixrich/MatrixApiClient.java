package io.github.cgissing.matrixrich;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MatrixApiClient {
    private final String homeserver;

    public MatrixApiClient(String homeserver) {
        this.homeserver = normalizeHomeserver(homeserver);
    }

    public MatrixLoginResult loginPassword(String user, String password) throws Exception {
        JSONObject identifier = new JSONObject()
                .put("type", "m.id.user")
                .put("user", user);
        JSONObject request = new JSONObject()
                .put("type", "m.login.password")
                .put("identifier", identifier)
                .put("password", password)
                .put("initial_device_display_name", "Matrix Rich Android");
        JSONObject response = new JSONObject(request("POST", clientUrl("/_matrix/client/v3/login"), "", request.toString()));
        return new MatrixLoginResult(
                response.optString("access_token", ""),
                response.optString("user_id", ""),
                response.optString("device_id", "")
        );
    }

    public MatrixSyncResult sync(String accessToken, String since, int timeoutMs, String ownUserId) throws Exception {
        StringBuilder path = new StringBuilder("/_matrix/client/v3/sync?timeout=").append(Math.max(timeoutMs, 0));
        if (since != null && !since.trim().isEmpty()) {
            path.append("&since=").append(encodeQueryValue(since.trim()));
        }
        String response = request("GET", clientUrl(path.toString()), accessToken, null);
        return MatrixSyncParser.parse(response, ownUserId == null ? "" : ownUserId);
    }

    public void sendTextMessage(String accessToken, String roomId, String body, String txnId) throws Exception {
        JSONObject request = new JSONObject()
                .put("msgtype", "m.text")
                .put("body", body);
        String path = "/_matrix/client/v3/rooms/"
                + encodePathSegment(roomId)
                + "/send/m.room.message/"
                + encodePathSegment(txnId);
        request("PUT", clientUrl(path), accessToken, request.toString());
    }

    String clientUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return homeserver + (path.startsWith("/") ? path : "/" + path);
    }

    static String normalizeHomeserver(String value) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) {
            result = AppPrefs.DEFAULT_HOMESERVER_URL;
        }
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            result = "https://" + result;
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static String encodePathSegment(String value) {
        return encode(value == null ? "" : value);
    }

    private static String encodeQueryValue(String value) {
        return encode(value == null ? "" : value);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }

    private String request(String method, String url, String accessToken, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(45000);
        connection.setRequestProperty("Accept", "application/json");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + accessToken.trim());
        }
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        String response = readFully(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new MatrixApiException(errorMessage(code, response));
        }
        return response;
    }

    private static String readFully(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String errorMessage(int code, String response) {
        if (response == null || response.trim().isEmpty()) {
            return "Matrix request failed with HTTP " + code;
        }
        try {
            JSONObject json = new JSONObject(response);
            String error = json.optString("error", "");
            String errcode = json.optString("errcode", "");
            if (!error.isEmpty()) {
                return errcode.isEmpty() ? error : errcode + ": " + error;
            }
        } catch (Exception ignored) {
        }
        return "Matrix request failed with HTTP " + code + ": " + response;
    }
}
