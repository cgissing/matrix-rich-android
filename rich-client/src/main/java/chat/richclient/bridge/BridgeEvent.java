package chat.richclient.bridge;

import org.json.JSONException;
import org.json.JSONObject;

public final class BridgeEvent {
    public final String type;
    public final JSONObject payload;

    private BridgeEvent(String type, JSONObject payload) {
        this.type = type;
        this.payload = payload;
    }

    public static BridgeEvent fromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            String type = root.optString("type", "");
            if (type.isEmpty()) {
                throw new IllegalArgumentException("Bridge event missing type");
            }
            JSONObject payload = root.optJSONObject("payload");
            return new BridgeEvent(type, payload == null ? new JSONObject() : payload);
        } catch (JSONException failure) {
            throw new IllegalArgumentException("Bridge event is not valid JSON", failure);
        }
    }
}
