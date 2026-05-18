# Matrix Rich Android Native UI Design

## Product Boundary

The Android main chat UI is native. It must not show Element Web or any other desktop web Matrix client as the visible room timeline. Element Web can remain a reference for Matrix/E2EE behavior, but it is not the product UI.

## Native Chat Surface

The app uses a native activity layout with a top app bar, horizontal room rail, scrollable message timeline, composer, push tab, and settings tab. This follows the rough information architecture of Android chat clients: room selection first, conversation content second, composer fixed at the bottom.

## Rich Message Rendering

Message bodies are rendered through Markwon. The renderer enables Markdown, tables, and LaTeX. Single-dollar inline formulas are normalized to the delimiter form expected by the native LaTeX plugin, so senders and agents can keep producing ordinary Markdown math.

## Push Wake

The ntfy foreground service remains independent from the Matrix engine. It listens to a configured ntfy JSON stream, shows native Android notifications, and wakes the native chat surface through `matrixrich://` or regular URL payloads.

## Runtime Boundary

The current implementation contains the native shell, rich renderer, and push bridge. Live Matrix sync and E2EE should be added behind this native UI boundary. The visible UI should consume room summaries, timeline events, and send results from that runtime instead of embedding a web client.

## Non-Goals

- Visible Element Web timeline.
- Matrix-specific math message formats.
- Committed credentials, recovery keys, ntfy tokens, or signing keys.
