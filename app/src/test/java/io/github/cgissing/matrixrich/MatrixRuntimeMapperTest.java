package io.github.cgissing.matrixrich;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatrixRuntimeMapperTest {
    @Test
    public void mapsLoginResultFromJsRuntime() throws Exception {
        JSONObject payload = new JSONObject()
                .put("accessToken", "tok")
                .put("userId", "@alice:example.org")
                .put("deviceId", "MRA1");

        MatrixLoginResult result = MatrixRuntimeMapper.loginFromPayload(payload);

        assertEquals("tok", result.accessToken);
        assertEquals("@alice:example.org", result.userId);
        assertEquals("MRA1", result.deviceId);
    }

    @Test
    public void mapsDecryptedSnapshotIntoNativeRoomsAndMessages() throws Exception {
        JSONObject payload = new JSONObject("{"
                + "\"nextBatch\":\"runtime-sync\","
                + "\"rooms\":[{"
                + "\"id\":\"!room:example.org\","
                + "\"title\":\"Research\","
                + "\"subtitle\":\"$E = mc^2$\","
                + "\"typingSummary\":\"Alice is typing\","
                + "\"initials\":\"R\","
                + "\"unreadCount\":2,"
                + "\"messages\":[{"
                + "\"eventId\":\"$event1\","
                + "\"sender\":\"@alice:example.org\","
                + "\"time\":\"09:30\","
                + "\"bodyMarkdown\":\"| A | B |\\n| - | - |\\n| 1 | 2 |\","
                + "\"outbound\":false,"
                + "\"reactions\":[{\"key\":\"👍\",\"count\":2,\"reactedByMe\":true}]"
                + "}]"
                + "}]"
                + "}");

        MatrixSyncResult result = MatrixRuntimeMapper.syncFromSnapshot(payload);

        assertEquals("runtime-sync", result.nextBatch);
        assertEquals(1, result.rooms.size());
        assertEquals("!room:example.org", result.rooms.get(0).id);
        assertEquals("Research", result.rooms.get(0).title);
        assertEquals("$E = mc^2$", result.rooms.get(0).subtitle);
        assertEquals("Alice is typing", result.rooms.get(0).typingSummary);
        assertEquals(2, result.rooms.get(0).unreadCount);
        NativeMessage message = result.messagesFor("!room:example.org").get(0);
        assertEquals("$event1", message.eventId);
        assertEquals("| A | B |\n| - | - |\n| 1 | 2 |", message.bodyMarkdown);
        assertEquals("👍", message.reactions.get(0).key);
        assertEquals(2, message.reactions.get(0).count);
        assertTrue(message.reactions.get(0).reactedByMe);
    }

    @Test
    public void suppliesReadableDefaultsForSparseRuntimePayloads() throws Exception {
        JSONObject payload = new JSONObject("{"
                + "\"rooms\":[{"
                + "\"id\":\"!sparse:example.org\","
                + "\"messages\":[{\"bodyMarkdown\":\"hello\"}]"
                + "}]"
                + "}");

        MatrixSyncResult result = MatrixRuntimeMapper.syncFromSnapshot(payload);

        assertEquals("", result.nextBatch);
        assertEquals("!sparse:example.org", result.rooms.get(0).title);
        assertEquals("M", result.rooms.get(0).initials);
        assertEquals("Matrix", result.messagesFor("!sparse:example.org").get(0).sender);
        assertEquals("hello", result.messagesFor("!sparse:example.org").get(0).bodyMarkdown);
    }
}
