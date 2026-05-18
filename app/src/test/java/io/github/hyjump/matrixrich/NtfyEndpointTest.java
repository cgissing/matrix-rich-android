package io.github.hyjump.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NtfyEndpointTest {
    @Test
    public void buildsJsonStreamEndpoint() {
        assertEquals(
                "https://ntfy.example.com/matrix-topic/json",
                NtfyEndpoint.jsonStreamUrl("https://ntfy.example.com/", "matrix-topic")
        );
    }

    @Test
    public void encodesTopicPathSegments() {
        assertEquals(
                "https://ntfy.example.com/matrix%20room/json",
                NtfyEndpoint.jsonStreamUrl("https://ntfy.example.com", "matrix room")
        );
    }

    @Test
    public void acceptsHostOnlyServer() {
        assertEquals(
                "https://ntfy.sh/topic/json",
                NtfyEndpoint.jsonStreamUrl("ntfy.sh", "topic")
        );
    }
}
