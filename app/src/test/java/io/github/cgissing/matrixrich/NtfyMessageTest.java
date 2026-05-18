package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NtfyMessageTest {
    @Test
    public void parsesMessageEvents() throws Exception {
        NtfyMessage message = NtfyMessage.fromJsonLine(
                "{\"id\":\"abc\",\"time\":1710000000,\"event\":\"message\",\"topic\":\"matrix\",\"title\":\"Room\",\"message\":\"New reply\",\"click\":\"matrixrich://open\"}"
        );

        assertTrue(message.isDisplayable());
        assertEquals("abc", message.id);
        assertEquals("matrix", message.topic);
        assertEquals("Room", message.title);
        assertEquals("New reply", message.message);
        assertEquals("matrixrich://open", message.click);
    }

    @Test
    public void ignoresOpenAndKeepaliveEvents() throws Exception {
        assertFalse(NtfyMessage.fromJsonLine("{\"event\":\"open\",\"topic\":\"matrix\"}").isDisplayable());
        assertFalse(NtfyMessage.fromJsonLine("{\"event\":\"keepalive\",\"topic\":\"matrix\"}").isDisplayable());
    }

    @Test
    public void toleratesMissingOptionalFields() throws Exception {
        NtfyMessage message = NtfyMessage.fromJsonLine("{\"event\":\"message\",\"message\":\"Ping\"}");

        assertTrue(message.isDisplayable());
        assertNull(message.id);
        assertEquals("Matrix Rich", message.notificationTitle("Matrix Rich"));
        assertEquals("Ping", message.notificationBody());
    }

    @Test
    public void classifiesClientDeepLinksForNotificationWake() {
        assertTrue(NtfyMessage.isClientDeepLink("matrixrich://open?url=https%3A%2F%2Fmatrix.to%2F%23%2Froom"));
        assertTrue(NtfyMessage.isClientDeepLink("NTFY://ntfy.example.com/topic"));
        assertFalse(NtfyMessage.isClientDeepLink("https://matrix.to/#/room"));
        assertFalse(NtfyMessage.isClientDeepLink(""));
        assertFalse(NtfyMessage.isClientDeepLink(null));
    }
}
