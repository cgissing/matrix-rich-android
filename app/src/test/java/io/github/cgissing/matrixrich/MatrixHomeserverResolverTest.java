package io.github.cgissing.matrixrich;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MatrixHomeserverResolverTest {
    @Test
    public void resolvesElementWebDeploymentPathThroughConfigJson() {
        List<String> calls = new ArrayList<>();
        MatrixHomeserverResolver.Resolved resolved = MatrixHomeserverResolver.resolve(
                "https://element.example.org/_h314/",
                url -> {
                    calls.add(url);
                    assertEquals("https://element.example.org/_h314/config.json", url);
                    return "{"
                            + "\"default_server_config\":{"
                            + "\"m.homeserver\":{"
                            + "\"base_url\":\"https://matrix.example.org\""
                            + "}"
                            + "}"
                            + "}";
                }
        );

        assertEquals("https://element.example.org/_h314", resolved.input);
        assertEquals("https://matrix.example.org", resolved.baseUrl);
        assertEquals("element-web-config", resolved.source);
        assertEquals(1, calls.size());
    }

    @Test
    public void usesOriginWellKnownWhenElementConfigIsUnavailable() {
        List<String> calls = new ArrayList<>();
        MatrixHomeserverResolver.Resolved resolved = MatrixHomeserverResolver.resolve(
                "example.org/web/",
                url -> {
                    calls.add(url);
                    if (url.endsWith("/config.json") || url.endsWith("/_matrix/client/versions")) {
                        return "";
                    }
                    assertEquals("https://example.org/.well-known/matrix/client", url);
                    return "{\"m.homeserver\":{\"base_url\":\"https://matrix.example.org\"}}";
                }
        );

        assertEquals("https://matrix.example.org", resolved.baseUrl);
        assertEquals("well-known", resolved.source);
        assertEquals("https://example.org/web/config.json", calls.get(0));
        assertEquals("https://example.org/web/_matrix/client/versions", calls.get(1));
        assertEquals("https://example.org/.well-known/matrix/client", calls.get(2));
    }

    @Test
    public void keepsDirectMatrixBaseWhenVersionsEndpointIsValid() {
        List<String> calls = new ArrayList<>();
        MatrixHomeserverResolver.Resolved resolved = MatrixHomeserverResolver.resolve(
                "https://example.org/matrix/",
                url -> {
                    calls.add(url);
                    if (url.endsWith("/config.json")) {
                        return "";
                    }
                    assertEquals("https://example.org/matrix/_matrix/client/versions", url);
                    return "{\"versions\":[\"v1.12\"]}";
                }
        );

        assertEquals("https://example.org/matrix", resolved.baseUrl);
        assertEquals("direct", resolved.source);
        assertEquals("https://example.org/matrix/config.json", calls.get(0));
        assertEquals("https://example.org/matrix/_matrix/client/versions", calls.get(1));
    }

    @Test
    public void keepsDirectMatrixUrlWhenDiscoveryHasNoAnswer() {
        List<String> calls = new ArrayList<>();
        MatrixHomeserverResolver.Resolved resolved = MatrixHomeserverResolver.resolve(
                "https://example.org/matrix/",
                url -> {
                    calls.add(url);
                    return "";
                }
        );

        assertEquals("https://example.org/matrix", resolved.baseUrl);
        assertEquals("direct", resolved.source);
        assertEquals("https://example.org/matrix/config.json", calls.get(0));
        assertEquals("https://example.org/matrix/_matrix/client/versions", calls.get(1));
        assertEquals("https://example.org/.well-known/matrix/client", calls.get(2));
    }
}
