package chat.richclient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class DependencyGuardTest {
    @Test
    public void richClientDoesNotDependOnMatrixSdkAndroidOrLeakCanary() throws Exception {
        File root = findRepoRoot();
        String buildGradle = new String(
                Files.readAllBytes(new File(root, "rich-client/build.gradle").toPath()),
                StandardCharsets.UTF_8);

        assertFalse(buildGradle.contains(":matrix-sdk-android"));
        assertFalse(buildGradle.toLowerCase().contains("leakcanary"));
        assertTrue(buildGradle.contains("androidx.webkit:webkit"));
    }

    @Test
    public void githubActionsOnlyBuildsRichClientArtifact() throws Exception {
        File root = findRepoRoot();
        String workflow = new String(
                Files.readAllBytes(new File(root, ".github/workflows/android.yml").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(workflow.contains(":rich-client:testDebugUnitTest"));
        assertTrue(workflow.contains(":rich-client:assembleDebug"));
        assertFalse(workflow.contains(":vector-app:assemble"));
        assertFalse(workflow.toLowerCase().contains("homeserver"));
        assertFalse(workflow.toLowerCase().contains("access_token"));
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
