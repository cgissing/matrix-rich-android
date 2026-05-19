package chat.richclient.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class BridgeCommandTest {
    @Test
    public void serializesLoginPasswordCommand() throws Exception {
        JSONObject payload = new JSONObject()
                .put("homeserver", "https://example.invalid/_matrix/")
                .put("username", "alice")
                .put("password", "secret");
        BridgeCommand command = new BridgeCommand("auth.loginPassword", payload);

        JSONObject json = new JSONObject(command.toJson());

        assertEquals("auth.loginPassword", json.getString("type"));
        assertEquals("alice", json.getJSONObject("payload").getString("username"));
        assertTrue(json.has("id"));
    }
}
