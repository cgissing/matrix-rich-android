package chat.richclient.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RichMarkdownRendererTest {
    @Test
    public void preprocessesInlineMathWithoutChangingBlockMath() {
        String markdown = "Inline $a^2+b^2=c^2$ and block:\n\n$$\nE = mc^2\n$$";

        String processed = RichMarkdownRenderer.preprocessMarkdown(markdown);

        assertTrue(processed.contains("Inline $$a^2+b^2=c^2$$ and block:"));
        assertTrue(processed.contains("$$\nE = mc^2\n$$"));
    }

    @Test
    public void leavesEscapedDollarAmountsUntouched() {
        String markdown = "Price is \\$5 and formula $x$";

        String processed = RichMarkdownRenderer.preprocessMarkdown(markdown);

        assertEquals("Price is \\$5 and formula $$x$$", processed);
    }
}
