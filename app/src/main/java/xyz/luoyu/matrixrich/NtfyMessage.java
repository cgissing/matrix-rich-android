package xyz.luoyu.matrixrich;

import org.json.JSONException;
import org.json.JSONObject;

public final class NtfyMessage {
    public final String id;
    public final String event;
    public final String topic;
    public final String title;
    public final String message;
    public final String click;
    public final long time;

    private NtfyMessage(String id, String event, String topic, String title, String message, String click, long time) {
        this.id = id;
        this.event = event;
        this.topic = topic;
        this.title = title;
        this.message = message;
        this.click = click;
        this.time = time;
    }

    public static NtfyMessage fromJsonLine(String line) throws JSONException {
        JSONObject json = new JSONObject(line);
        return new NtfyMessage(
                optionalString(json, "id"),
                optionalString(json, "event"),
                optionalString(json, "topic"),
                optionalString(json, "title"),
                optionalString(json, "message"),
                optionalString(json, "click"),
                json.optLong("time", 0L)
        );
    }

    public boolean isDisplayable() {
        return event == null || "message".equals(event);
    }

    public String notificationTitle(String appName) {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        if (topic != null && !topic.isEmpty()) {
            return topic;
        }
        return appName;
    }

    public String notificationBody() {
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return "New notification";
    }

    private static String optionalString(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, null);
        return value == null || value.isEmpty() ? null : value;
    }
}
