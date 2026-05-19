package io.github.cgissing.matrixrich;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatrixSyncParserTest {
    @Test
    public void parsesRoomsAndRichTimelineMessages() throws Exception {
        MatrixSyncResult result = MatrixSyncParser.parse("{"
                + "\"next_batch\":\"s123\","
                + "\"rooms\":{\"join\":{\"!room:example.org\":{"
                + "\"state\":{\"events\":[{\"type\":\"m.room.name\",\"content\":{\"name\":\"Agent DM\"}}]},"
                + "\"unread_notifications\":{\"notification_count\":3},"
                + "\"timeline\":{\"events\":["
                + "{\"type\":\"m.room.message\",\"sender\":\"@agent:example.org\",\"origin_server_ts\":1710000000000,\"content\":{\"msgtype\":\"m.text\",\"body\":\"| A | B |\\n|---|---|\\n| $x$ | $$y$$ |\"}},"
                + "{\"type\":\"m.room.message\",\"sender\":\"@me:example.org\",\"origin_server_ts\":1710000060000,\"content\":{\"msgtype\":\"m.text\",\"body\":\"reply\"}}"
                + "]}}}}}", "@me:example.org");

        assertEquals("s123", result.nextBatch);
        assertEquals(1, result.rooms.size());
        assertEquals("!room:example.org", result.rooms.get(0).id);
        assertEquals("Agent DM", result.rooms.get(0).title);
        assertEquals(3, result.rooms.get(0).unreadCount);

        List<NativeMessage> messages = result.messagesFor("!room:example.org");
        assertEquals(2, messages.size());
        assertTrue(messages.get(0).bodyMarkdown.contains("| A | B |"));
        assertTrue(messages.get(0).bodyMarkdown.contains("$x$"));
        assertTrue(messages.get(1).outbound);
    }

    @Test
    public void usesReadableFallbacksForEncryptedRooms() throws Exception {
        MatrixSyncResult result = MatrixSyncParser.parse("{"
                + "\"rooms\":{\"join\":{\"!secret:example.org\":{"
                + "\"timeline\":{\"events\":["
                + "{\"type\":\"m.room.encrypted\",\"sender\":\"@alice:example.org\",\"origin_server_ts\":1710000000000,\"content\":{}}"
                + "]}}}}}", "@me:example.org");

        assertEquals("!secret:example.org", result.rooms.get(0).title);
        assertEquals("Encrypted message", result.rooms.get(0).subtitle);
        assertEquals("Encrypted message", result.messagesFor("!secret:example.org").get(0).bodyMarkdown);
    }
}
