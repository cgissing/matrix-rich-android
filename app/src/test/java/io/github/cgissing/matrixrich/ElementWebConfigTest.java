package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElementWebConfigTest {
    @Test
    public void usesElementWebWhenUrlIsBlank() {
        assertEquals("https://app.element.io/", ElementWebConfig.normalizeElementWebUrl(""));
        assertEquals("https://app.element.io/", ElementWebConfig.normalizeElementWebUrl(null));
    }

    @Test
    public void acceptsHostOnlyElementWebUrl() {
        assertEquals("https://app.element.io/", ElementWebConfig.normalizeElementWebUrl(" app.element.io "));
    }

    @Test
    public void preservesNestedPathsForSelfHostedElementWeb() {
        assertEquals(
                "https://element.example.org/_h314/",
                ElementWebConfig.normalizeElementWebUrl("https://element.example.org/_h314/")
        );
    }

    @Test
    public void preservesLocalDevelopmentHttpUrls() {
        assertEquals(
                "http://127.0.0.1:8080/element/",
                ElementWebConfig.normalizeElementWebUrl("http://127.0.0.1:8080/element")
        );
    }

    @Test
    public void exposesElementWebMobileRedirectBypassCookie() {
        String cookie = ElementWebConfig.mobileRedirectBypassCookie();

        assertTrue(cookie.contains("element_mobile_redirect_to_guide=false"));
        assertTrue(cookie.contains("path=/"));
        assertTrue(cookie.contains("max-age="));
    }

    @Test
    public void identifiesElementMobileGuideAndAppHandoffUrls() {
        assertTrue(ElementWebConfig.isMobileGuideOrAppHandoffUrl("https://app.element.io/mobile_guide/"));
        assertTrue(ElementWebConfig.isMobileGuideOrAppHandoffUrl("https://element.example.org/_h314/mobile_guide/"));
        assertTrue(ElementWebConfig.isMobileGuideOrAppHandoffUrl("https://mobile.element.io/element?account_provider=example.org"));
        assertFalse(ElementWebConfig.isMobileGuideOrAppHandoffUrl("https://app.element.io/"));
        assertFalse(ElementWebConfig.isMobileGuideOrAppHandoffUrl("https://element.example.org/_h314/"));
    }
}
