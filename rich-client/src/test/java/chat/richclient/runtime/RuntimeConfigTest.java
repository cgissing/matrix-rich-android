package chat.richclient.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import chat.richclient.bridge.BridgeCommand;
import org.json.JSONObject;
import org.junit.Test;

public class RuntimeConfigTest {
    @Test
    public void loadsElementRuntimeFromHttpsAppAssetsOrigin() {
        assertEquals("https://appassets.androidplatform.net/element/index.html", RuntimeConfig.ELEMENT_ENTRY_URL);
        assertTrue(RuntimeConfig.isLocalRuntimeUrl(RuntimeConfig.ELEMENT_ENTRY_URL));
        assertTrue(RuntimeConfig.isLocalRuntimeUrl("https://appassets.androidplatform.net/element/bundles/app.js"));
    }

    @Test
    public void rejectsExternalNavigationFromHiddenRuntime() {
        assertFalse(RuntimeConfig.isLocalRuntimeUrl("https://app.element.io/"));
        assertFalse(RuntimeConfig.isLocalRuntimeUrl("https://example.org/element/index.html"));
        assertFalse(RuntimeConfig.isLocalRuntimeUrl("file:///android_asset/element/index.html"));
    }

    @Test
    public void quotesCommandsForEvaluateJavascript() throws Exception {
        BridgeCommand command = new BridgeCommand(
                "cmd-1",
                "messages.sendText",
                new JSONObject()
                        .put("roomId", "!room:example")
                        .put("body", "hello'); window.pwned=true; //"));

        String script = RuntimeConfig.commandDispatchScript(command);

        assertTrue(script.startsWith("window.MatrixRichRuntime&&window.MatrixRichRuntime.receive("));
        assertTrue(script.endsWith(");"));
        assertFalse(script.contains("pwned=true; //');"));
    }
}
