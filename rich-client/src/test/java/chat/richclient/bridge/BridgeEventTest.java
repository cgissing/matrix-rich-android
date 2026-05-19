package chat.richclient.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class BridgeEventTest {
    @Test
    public void parsesRuntimeReadyEvent() throws Exception {
        BridgeEvent event = BridgeEvent.fromJson("{\"type\":\"runtime.ready\",\"payload\":{\"runtime\":\"element-web\"}}");

        assertEquals("runtime.ready", event.type);
        assertEquals("element-web", event.payload.getString("runtime"));
    }

    @Test
    public void rejectsMissingType() {
        try {
            BridgeEvent.fromJson("{\"payload\":{}}");
            fail("Expected missing type to throw");
        } catch (IllegalArgumentException expected) {
            assertEquals("Bridge event missing type", expected.getMessage());
        }
    }
}
