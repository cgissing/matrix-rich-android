package chat.richclient.bridge;

import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public final class BridgeCommand {
    public final String id;
    public final String type;
    public final JSONObject payload;

    public BridgeCommand(String type, JSONObject payload) {
        this(UUID.randomUUID().toString(), type, payload);
    }

    public BridgeCommand(String id, String type, JSONObject payload) {
        this.id = id;
        this.type = type;
        this.payload = payload == null ? new JSONObject() : payload;
    }

    public String toJson() {
        try {
            return new JSONObject()
                    .put("id", id)
                    .put("type", type)
                    .put("payload", payload)
                    .toString();
        } catch (JSONException failure) {
            throw new IllegalStateException("Unable to serialize bridge command", failure);
        }
    }
}
