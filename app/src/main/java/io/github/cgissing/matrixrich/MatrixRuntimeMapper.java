package io.github.cgissing.matrixrich;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MatrixRuntimeMapper {
    private MatrixRuntimeMapper() {
    }

    public static MatrixLoginResult loginFromPayload(JSONObject payload) {
        return new MatrixLoginResult(
                payload.optString("accessToken"),
                payload.optString("userId"),
                payload.optString("deviceId")
        );
    }

    public static MatrixVerificationState verificationFromPayload(JSONObject payload) {
        return new MatrixVerificationState(
                payload.optString("transactionId"),
                payload.optString("otherUserId"),
                payload.optString("otherDeviceId"),
                payload.optBoolean("isSelfVerification", false),
                payload.optInt("phaseCode", 0),
                payload.optString("phase", "None"),
                payload.optString("source"),
                payload.optBoolean("canAccept", false),
                payload.optBoolean("canStartSas", false),
                payload.optBoolean("canConfirmSas", false),
                payload.optString("sasDecimal"),
                payload.optString("sasEmoji")
        );
    }

    public static MatrixSyncResult syncFromSnapshot(JSONObject payload) {
        List<NativeRoom> rooms = new ArrayList<>();
        Map<String, List<NativeMessage>> messagesByRoom = new HashMap<>();
        JSONArray roomArray = payload.optJSONArray("rooms");
        if (roomArray == null) {
            return new MatrixSyncResult(rooms, messagesByRoom, payload.optString("nextBatch"));
        }
        for (int i = 0; i < roomArray.length(); i++) {
            JSONObject roomJson = roomArray.optJSONObject(i);
            if (roomJson == null) {
                continue;
            }
            String roomId = roomJson.optString("id");
            if (roomId.isEmpty()) {
                continue;
            }
            String title = roomJson.optString("title", roomId);
            String subtitle = roomJson.optString("subtitle");
            String initials = roomJson.optString("initials", initialsFor(title));
            int unread = roomJson.optInt("unreadCount", 0);
            rooms.add(new NativeRoom(roomId, title, subtitle, initials, unread));

            List<NativeMessage> messages = new ArrayList<>();
            JSONArray messageArray = roomJson.optJSONArray("messages");
            if (messageArray != null) {
                for (int j = 0; j < messageArray.length(); j++) {
                    JSONObject messageJson = messageArray.optJSONObject(j);
                    if (messageJson == null) {
                        continue;
                    }
                    String body = messageJson.optString("bodyMarkdown");
                    if (body.isEmpty()) {
                        continue;
                    }
                    messages.add(new NativeMessage(
                            messageJson.optString("sender", "Matrix"),
                            messageJson.optString("time"),
                            body,
                            messageJson.optBoolean("outbound", false)
                    ));
                }
            }
            messagesByRoom.put(roomId, messages);
        }
        return new MatrixSyncResult(rooms, messagesByRoom, payload.optString("nextBatch"));
    }

    private static String initialsFor(String title) {
        String clean = title == null ? "" : title.trim();
        if (clean.isEmpty()) {
            return "M";
        }
        int first = clean.offsetByCodePoints(0, 1);
        String candidate = clean.substring(0, first).toUpperCase();
        return Character.isLetterOrDigit(candidate.charAt(0)) ? candidate : "M";
    }
}
