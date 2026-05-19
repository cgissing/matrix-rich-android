package io.github.cgissing.matrixrich;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
