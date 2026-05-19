import test from "node:test";
import assert from "node:assert/strict";

import { resolveHomeserver } from "../src/matrix-runtime.js";

test("resolves an Element Web deployment path through config.json", async () => {
  const calls = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    assert.equal(String(url), "https://hermes.314605.xyz/_h314/config.json");
    return {
      ok: true,
      json: async () => ({
        default_server_config: {
          "m.homeserver": {
            base_url: "https://hermes.314605.xyz",
          },
        },
      }),
    };
  };

  try {
    const resolved = await resolveHomeserver("https://hermes.314605.xyz/_h314/");

    assert.equal(resolved.baseUrl, "https://hermes.314605.xyz");
    assert.equal(resolved.source, "element-web-config");
    assert.deepEqual(calls, ["https://hermes.314605.xyz/_h314/config.json"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("uses root .well-known when Element Web config is unavailable", async () => {
  const calls = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    if (String(url).endsWith("/config.json") || String(url).endsWith("/_matrix/client/versions")) {
      return { ok: false, json: async () => ({}) };
    }
    assert.equal(String(url), "https://example.org/.well-known/matrix/client");
    return {
      ok: true,
      json: async () => ({
        "m.homeserver": {
          base_url: "https://matrix.example.org",
        },
      }),
    };
  };

  try {
    const resolved = await resolveHomeserver("example.org/some-web-path/");

    assert.equal(resolved.baseUrl, "https://matrix.example.org");
    assert.equal(resolved.source, "well-known");
    assert.deepEqual(calls, [
      "https://example.org/some-web-path/config.json",
      "https://example.org/some-web-path/_matrix/client/versions",
      "https://example.org/.well-known/matrix/client",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("keeps a direct Matrix base URL when its versions endpoint is valid", async () => {
  const calls = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    if (String(url).endsWith("/config.json")) {
      return { ok: false, json: async () => ({}) };
    }
    assert.equal(String(url), "https://example.org/matrix/_matrix/client/versions");
    return { ok: true, json: async () => ({ versions: ["v1.12"] }) };
  };

  try {
    const resolved = await resolveHomeserver("https://example.org/matrix/");

    assert.equal(resolved.baseUrl, "https://example.org/matrix");
    assert.equal(resolved.source, "direct");
    assert.deepEqual(calls, [
      "https://example.org/matrix/config.json",
      "https://example.org/matrix/_matrix/client/versions",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("keeps a direct Matrix base URL when discovery has no answer", async () => {
  const calls = [];
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    return { ok: false, json: async () => ({}) };
  };

  try {
    const resolved = await resolveHomeserver("https://example.org/matrix/");

    assert.equal(resolved.baseUrl, "https://example.org/matrix");
    assert.equal(resolved.source, "direct");
    assert.deepEqual(calls, [
      "https://example.org/matrix/config.json",
      "https://example.org/matrix/_matrix/client/versions",
      "https://example.org/.well-known/matrix/client",
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
