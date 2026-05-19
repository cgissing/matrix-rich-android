package chat.richclient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class RichClientUiGuardTest {
    @Test
    public void mainActivityInflatesSchildiStyleNativeLayoutsInsteadOfHandBuiltDemoUi() throws Exception {
        File root = findRepoRoot();
        String activity = read(new File(root, "rich-client/src/main/java/chat/richclient/MainActivity.java"));

        assertTrue(activity.contains("R.layout.activity_rich_home"));
        assertFalse(activity.contains("createMainContent"));
        assertFalse(activity.contains("new LinearLayout(this)"));
        assertFalse(activity.contains("new ScrollView(this)"));
        assertFalse(activity.contains("new HorizontalScrollView(this)"));
    }

    @Test
    public void richClientOwnsSchildiChatStyleSurfaceResources() throws Exception {
        File root = findRepoRoot();

        assertLayoutContains(root, "activity_rich_home.xml", "roomRecyclerView", "timelineRecyclerView");
        assertLayoutContains(root, "item_rich_room.xml", "roomAvatarContainer", "roomUnreadCounterBadgeView");
        assertLayoutContains(root, "item_rich_timeline_message.xml", "bubbleWrapper", "reactionsContainer");
        assertLayoutContains(root, "view_rich_composer_sc.xml", "composerEditText", "sendButton");
    }

    private void assertLayoutContains(File root, String fileName, String firstNeedle, String secondNeedle) throws Exception {
        File layout = new File(root, "rich-client/src/main/res/layout/" + fileName);
        assertTrue(fileName + " must exist", layout.isFile());
        String xml = read(layout);
        assertTrue(fileName + " must contain " + firstNeedle, xml.contains(firstNeedle));
        assertTrue(fileName + " must contain " + secondNeedle, xml.contains(secondNeedle));
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
