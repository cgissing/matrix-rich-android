package io.github.cgissing.matrixrich;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MatrixSyncParser {
    private MatrixSyncParser() {
    }

    public static MatrixSyncResult parse(String body, String ownUserId) throws Exception {
        JSONObject root = new JSONObject(body == null ? "{}" : body);
        JSONObject join = root.optJSONObject("rooms") == null
                ? null
                : root.optJSONObject("rooms").optJSONObject("join");
        List<NativeRoom> rooms = new ArrayList<>();
        Map<String, List<NativeMessage>> messagesByRoom = new HashMap<>();
        if (join != null) {
            Iterator<String> keys = join.keys();
            while (keys.hasNext()) {
                String roomId = keys.next();
                JSONObject roomJson = join.optJSONObject(roomId);
                if (roomJson == null) {
                    continue;
                }
                List<NativeMessage> messages = parseTimeline(roomJson, ownUserId);
                String title = roomTitle(roomId, roomJson);
                String subtitle = messages.isEmpty() ? "No messages yet" : messages.get(messages.size() - 1).bodyMarkdown;
                int unread = roomJson.optJSONObject("unread_notifications") == null
                        ? 0
                        : roomJson.optJSONObject("unread_notifications").optInt("notification_count", 0);
                rooms.add(new NativeRoom(roomId, title, subtitle, initials(title), unread));
                messagesByRoom.put(roomId, messages);
            }
        }
        return new MatrixSyncResult(rooms, messagesByRoom, root.optString("next_batch", ""));
    }

    private static List<NativeMessage> parseTimeline(JSONObject roomJson, String ownUserId) {
        List<NativeMessage> messages = new ArrayList<>();
        JSONObject timeline = roomJson.optJSONObject("timeline");
        JSONArray events = timeline == null ? null : timeline.optJSONArray("events");
        if (events == null) {
            return messages;
        }
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            NativeMessage message = parseTimelineEvent(event, ownUserId);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private static NativeMessage parseTimelineEvent(JSONObject event, String ownUserId) {
        if (event == null) {
            return null;
        }
        String type = event.optString("type", "");
        String sender = event.optString("sender", "");
        boolean outbound = !ownUserId.isEmpty() && ownUserId.equals(sender);
        String time = formatTime(event.optLong("origin_server_ts", 0L));
        if ("m.room.encrypted".equals(type)) {
            return new NativeMessage(displaySender(sender), time, "Encrypted message", outbound);
        }
        if (!"m.room.message".equals(type)) {
            return null;
        }
        JSONObject content = event.optJSONObject("content");
        if (content == null) {
            return null;
        }
        String msgtype = content.optString("msgtype", "");
        if (!"m.text".equals(msgtype) && !"m.notice".equals(msgtype) && !"m.emote".equals(msgtype)) {
            return null;
        }
        String body = content.optString("body", "");
        if ("m.emote".equals(msgtype) && !sender.isEmpty()) {
            body = "* " + displaySender(sender) + " " + body;
        }
        if (body.isEmpty()) {
            return null;
        }
        return new NativeMessage(displaySender(sender), time, body, outbound);
    }

    private static String roomTitle(String roomId, JSONObject roomJson) {
        String fromState = roomNameFrom(roomJson.optJSONObject("state"));
        if (!fromState.isEmpty()) {
            return fromState;
        }
        String fromTimeline = roomNameFrom(roomJson.optJSONObject("timeline"));
        if (!fromTimeline.isEmpty()) {
            return fromTimeline;
        }
        return roomId;
    }

    private static String roomNameFrom(JSONObject container) {
        if (container == null) {
            return "";
        }
        JSONArray events = container.optJSONArray("events");
        if (events == null) {
            return "";
        }
        String memberName = "";
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) {
                continue;
            }
            JSONObject content = event.optJSONObject("content");
            if (content == null) {
                continue;
            }
            String type = event.optString("type", "");
            if ("m.room.name".equals(type) && !content.optString("name", "").isEmpty()) {
                return content.optString("name");
            }
            if ("m.room.canonical_alias".equals(type) && !content.optString("alias", "").isEmpty()) {
                return content.optString("alias");
            }
            if ("m.room.member".equals(type) && memberName.isEmpty() && !content.optString("displayname", "").isEmpty()) {
                memberName = content.optString("displayname");
            }
        }
        return memberName;
    }

    private static String initials(String title) {
        String clean = title == null ? "" : title.trim();
        if (clean.isEmpty()) {
            return "?";
        }
        int firstCodePoint = clean.codePointAt(0);
        return new String(Character.toChars(firstCodePoint)).toUpperCase(Locale.ROOT);
    }

    private static String displaySender(String sender) {
        if (sender == null || sender.isEmpty()) {
            return "Matrix";
        }
        int colon = sender.indexOf(':');
        String local = colon > 0 ? sender.substring(0, colon) : sender;
        return local.startsWith("@") ? local.substring(1) : local;
    }

    private static String formatTime(long millis) {
        if (millis <= 0L) {
            return "";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
