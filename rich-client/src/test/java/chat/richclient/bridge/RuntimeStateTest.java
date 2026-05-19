package chat.richclient.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeStateTest {
    @Test
    public void recordsRuntimeReady() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"runtime.ready\",\"payload\":{\"runtime\":\"element-web\"}}"));

        assertTrue(next.runtimeReady);
        assertEquals("element-web", next.runtimeName);
    }

    @Test
    public void recordsAuthState() {
        RuntimeState state = new RuntimeState();
        RuntimeState next = state.reduce(BridgeEvent.fromJson("{\"type\":\"auth.state\",\"payload\":{\"loggedIn\":true,\"userId\":\"@alice:example.org\"}}"));

        assertTrue(next.loggedIn);
        assertEquals("@alice:example.org", next.userId);
    }
}
