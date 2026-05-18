package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RichMarkdownRendererTest {
    @Test
    public void normalizesSingleDollarInlineMathForNativeLatexPlugin() {
        assertEquals(
                "Inline $$E = mc^2$$ works",
                RichMarkdownRenderer.normalizeMathDelimiters("Inline $E = mc^2$ works")
        );
    }

    @Test
    public void leavesDisplayMathDelimitersUntouched() {
        assertEquals(
                "$$\\int_0^1 x dx$$",
                RichMarkdownRenderer.normalizeMathDelimiters("$$\\int_0^1 x dx$$")
        );
    }

    @Test
    public void fixtureContainsTableAndLatexCoverage() {
        String fixture = DemoMatrixState.richMessageFixture();

        assertTrue(fixture.contains("| Step | Result |"));
        assertTrue(fixture.contains("$E = mc^2$"));
        assertTrue(fixture.contains("$$\\int_0^1"));
    }
}
