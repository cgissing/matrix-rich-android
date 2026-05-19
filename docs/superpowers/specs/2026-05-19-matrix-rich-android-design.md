# Matrix Rich Android Native UI Design

## Product Boundary

The Android main chat UI is native. It must not show Element Web or any other desktop web Matrix client as the visible room timeline. Browser-side Matrix code can be used as a hidden runtime for protocol and E2EE behavior, but it must only emit data for the native UI to render.

## Native Chat Surface

The app uses a native activity layout with a top app bar, horizontal room rail, scrollable message timeline, composer, push tab, and settings tab. This follows the rough information architecture of Android chat clients: room selection first, conversation content second, composer fixed at the bottom.

## Rich Message Rendering

Message bodies are rendered through Markwon. The renderer enables Markdown, tables, and LaTeX. Single-dollar inline formulas are normalized to the delimiter form expected by the native LaTeX plugin, so senders and agents can keep producing ordinary Markdown math.

## Push Wake

The ntfy foreground service remains independent from the Matrix engine. It listens to a configured ntfy JSON stream, shows native Android notifications, and wakes the native chat surface through `matrixrich://` or regular URL payloads.

## Matrix Runtime

The app uses a hidden local WebView to run a bundled Matrix JS SDK runtime. That runtime owns Matrix login, sync, Rust/WASM E2EE, key backup unlock, and message sending. It emits compact JSON snapshots to Android, and Android maps those snapshots into native room summaries and native timeline messages.

The WebView is not visible, does not load Element Web, and does not render any chat timeline. It exists because the web-side Matrix stack already has the E2EE behavior the user verified in the browser, while the Android-visible UI needs to be rebuilt natively.

## Non-Goals

- Visible Element Web timeline.
- Matrix-specific math message formats.
- Committed credentials, recovery keys, ntfy tokens, or signing keys.
