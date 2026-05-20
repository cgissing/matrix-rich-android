package chat.richclient.runtime;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class RuntimeAssetsTest {
    @Test
    public void bundledFallbackIndexLoadsMatrixRichBridgeBeforeElementBundle() throws Exception {
        File root = findRepoRoot();
        String index = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/index.html").toPath()),
                StandardCharsets.UTF_8);

        int bridge = index.indexOf("matrix-rich-runtime.js");
        int bundle = index.indexOf("bundle.js");

        assertTrue("fallback index must load bridge", bridge >= 0);
        assertTrue("bridge must run before Element bundle", bundle < 0 || bridge < bundle);
    }

    @Test
    public void bridgeScriptDeclaresSupportedCommandAndEventSurface() throws Exception {
        File root = findRepoRoot();
        String script = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/matrix-rich-runtime.js").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("auth.loginPassword"));
        assertTrue(script.contains("messages.sendText"));
        assertTrue(script.contains("reactions.send"));
        assertTrue(script.contains("verification.action"));
        assertTrue(script.contains("push.register"));
        assertTrue(script.contains("crypto.provideRecoveryKey"));
        assertTrue(script.contains("runtime.ready"));
        assertTrue(script.contains("rooms.snapshot"));
        assertTrue(script.contains("timeline.append"));
        assertTrue(script.contains("crypto.state"));
    }

    @Test
    public void bridgeScriptPolyfillsPromiseWithResolversBeforeElementBundleRuns() throws Exception {
        File root = findRepoRoot();
        String script = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/matrix-rich-runtime.js").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("Promise.withResolvers"));
        assertTrue(script.contains("resolve: resolve"));
        assertTrue(script.contains("reject: reject"));
    }

    @Test
    public void bridgeScriptReportsLoginErrorsInsteadOfOpaqueBridgeCommandFailed() throws Exception {
        File root = findRepoRoot();
        String script = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/matrix-rich-runtime.js").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("function describeError"));
        assertTrue(script.contains("Login failed: "));
        assertTrue(script.contains("login_failed"));
    }

    @Test
    public void bridgeScriptResolvesElementWebEndpointConfigBeforePasswordLogin() throws Exception {
        File root = findRepoRoot();
        String script = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/matrix-rich-runtime.js").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("function resolveHomeserverBaseUrl"));
        assertTrue(script.contains("/config.json"));
        assertTrue(script.contains("default_server_config"));
        assertTrue(script.contains("\"m.homeserver\""));
        assertTrue(script.contains("var homeserver = await resolveHomeserverBaseUrl(payload.homeserver);"));
    }

    @Test
    public void bridgeScriptRequestsSecretStorageRecoveryAndRefreshesAfterDecrypt() throws Exception {
        File root = findRepoRoot();
        String script = new String(
                Files.readAllBytes(new File(root, "rich-client/src/main/assets/element/matrix-rich-runtime.js").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("getSecretStorageKey"));
        assertTrue(script.contains("decodeRecoveryKey"));
        assertTrue(script.contains("deriveRecoveryKeyFromPassphrase"));
        assertTrue(script.contains("loadSessionBackupPrivateKeyFromSecretStorage"));
        assertTrue(script.contains("MatrixEventEvent"));
        assertTrue(script.contains("Decrypted"));
        assertTrue(script.contains("emitTimelineSnapshot(roomId)"));
    }

    private File findRepoRoot() {
        File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (current != null) {
            if (new File(current, "settings.gradle").isFile()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Unable to find repo root");
    }
}
