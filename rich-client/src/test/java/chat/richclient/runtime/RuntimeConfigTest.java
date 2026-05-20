package chat.richclient.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import chat.richclient.bridge.BridgeCommand;
import org.json.JSONObject;
import org.junit.Test;

public class RuntimeConfigTest {
    @Test
    public void derivesSameOriginRuntimeUrlFromPathMountedHomeserver() {
        String entryUrl = RuntimeConfig.runtimeEntryUrlForHomeserver("https://matrix.example.org/_h314/");

        assertEquals("https://matrix.example.org/_matrix_rich_runtime/element/index.html", entryUrl);
        assertEquals("matrix.example.org", RuntimeConfig.runtimeAuthorityForHomeserver("https://matrix.example.org/_h314/"));
        assertTrue(RuntimeConfig.isLocalRuntimeUrl(entryUrl, "matrix.example.org"));
        assertTrue(RuntimeConfig.isLocalRuntimeUrl(
                "https://matrix.example.org/_matrix_rich_runtime/element/bundles/app.js",
                "matrix.example.org"));
        assertFalse(RuntimeConfig.isLocalRuntimeUrl(
                "https://matrix.example.org/_h314/_matrix/client/v3/login",
                "matrix.example.org"));
    }

    @Test
    public void prefixesMissingSchemeAndPreservesExplicitPortInRuntimeOrigin() {
        assertEquals(
                "example.org:8448",
                RuntimeConfig.runtimeAuthorityForHomeserver("example.org:8448/matrix/"));
        assertEquals(
                "https://example.org:8448/_matrix_rich_runtime/element/index.html",
                RuntimeConfig.runtimeEntryUrlForHomeserver("example.org:8448/matrix/"));
    }

    @Test
    public void rejectsExternalNavigationFromHiddenRuntime() {
        assertFalse(RuntimeConfig.isLocalRuntimeUrl("https://app.element.io/", "matrix.example.org"));
        assertFalse(RuntimeConfig.isLocalRuntimeUrl(
                "https://example.org/_matrix_rich_runtime/element/index.html",
                "matrix.example.org"));
        assertFalse(RuntimeConfig.isLocalRuntimeUrl("file:///android_asset/element/index.html", "matrix.example.org"));
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
