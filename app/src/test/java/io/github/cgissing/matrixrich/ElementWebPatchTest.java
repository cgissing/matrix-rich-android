package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ElementWebPatchTest {
    @Test
    public void injectsMobileCssAndViewportMarkers() {
        String script = ElementWebPatch.mobilePatchScript();

        assertTrue(script.contains("matrix-rich-mobile-patch"));
        assertTrue(script.contains("matrix-rich-viewport"));
        assertTrue(script.contains("mx_MatrixChat"));
        assertTrue(script.contains("safe-area-inset-bottom"));
    }

    @Test
    public void nudgesElementLabsLatexSettingWithoutParsingMessagesNatively() {
        String script = ElementWebPatch.mobilePatchScript();

        assertTrue(script.contains("feature_latex_maths"));
        assertTrue(script.contains("localStorage"));
    }
}
