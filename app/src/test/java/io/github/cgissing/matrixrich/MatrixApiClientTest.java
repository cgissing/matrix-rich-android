package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatrixApiClientTest {
    @Test
    public void normalizesHomeserverUrls() {
        assertEquals("https://matrix.org", MatrixApiClient.normalizeHomeserver("matrix.org/"));
        assertEquals("https://example.org", MatrixApiClient.normalizeHomeserver(" https://example.org/// "));
    }

    @Test
    public void buildsEncodedRoomSendPath() {
        MatrixApiClient client = new MatrixApiClient("https://matrix.org");

        assertEquals(
                "https://matrix.org/_matrix/client/v3/rooms/%21room%3Aexample.org/send/m.room.message/txn%201",
                client.clientUrl("/_matrix/client/v3/rooms/"
                        + MatrixApiClient.encodePathSegment("!room:example.org")
                        + "/send/m.room.message/"
                        + MatrixApiClient.encodePathSegment("txn 1"))
        );
    }
}
