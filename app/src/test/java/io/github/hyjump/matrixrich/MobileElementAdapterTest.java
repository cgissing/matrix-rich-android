package io.github.hyjump.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MobileElementAdapterTest {
    @Test
    public void injectionEnablesLatexAndBypassesMobileGuide() {
        String script = MobileElementAdapter.injectionScript();

        assertTrue(script.contains("mx_labs_feature_feature_latex_maths"));
        assertTrue(script.contains("element_mobile_redirect_to_guide=false"));
    }

    @Test
    public void injectionAddsMobileTimelineAndTableCss() {
        String script = MobileElementAdapter.injectionScript();

        assertTrue(script.contains("matrix-rich-mobile-css"));
        assertTrue(script.contains("table"));
        assertTrue(script.contains("katex-display"));
        assertTrue(script.contains("max-width:100vw"));
    }
}
