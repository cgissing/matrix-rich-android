# SchildiChat UI With Element Web Runtime Design

## Decision

This project must not be a SchildiChat fork with a few renderer patches. SchildiChat Legacy is used as the UI source and interaction reference only. Matrix protocol state, login, sync, E2EE, typing, reactions, verification, and message sending are provided by an Element Web / matrix-js-sdk runtime hosted behind a native bridge.

The existing `matrix-sdk-android` module may stay in the repository while the app is being rebuilt, but it is not the runtime for the new client. New user-visible behavior must not depend on SchildiChat Legacy's Matrix SDK timeline pipeline.

## Goals

- Provide an Android client with SchildiChat-style native UI rather than the Element Web mobile page.
- Let users log in with homeserver URL, username, and password only.
- Support non-root homeserver paths because Element Web can log in to those endpoints.
- Use the Web/JS Matrix stack for E2EE and account/session behavior.
- Render normal LLM-style markdown, tables, fenced code, inline math, and block math without requiring Matrix-specific math message syntax.
- Support typing notifications, reactions, verification events, room list, timeline, and composer as native UI events.
- Support UnifiedPush/ntfy as the push path and wake the JS runtime to sync when possible.
- Build APK artifacts through GitHub Actions without committing private endpoints, usernames, tokens, keystores, or personal data.

## Non-Goals

- Do not embed Element Web as the foreground app UI.
- Do not keep patching SchildiChat Legacy's `matrix-sdk-android` as the answer.
- Do not require agents or senders to emit Matrix-specific math HTML.
- Do not introduce one WebView per formula or one WebView per message.
- Do not hardcode the user's homeserver endpoint in source, docs, tests, or workflows.

## Approaches Considered

### A. Patch SchildiChat Legacy Directly

This is the current wrong direction. It gives the mature SchildiChat UI and features quickly, but it does not create a new client architecture. Missing rendering and homeserver behavior remain tied to old Android SDK assumptions, and the user has no reason to choose it over SchildiChat itself.

### B. Foreground WebView Wrapper

This reuses Element Web's protocol and E2EE behavior, but the Android UI is the same poor mobile web experience the user rejected. It also risks performance problems if the foreground UI is treated as a web page rather than a native timeline.

### C. Native SchildiChat-Style UI Plus Background JS Runtime

This is the selected approach. A single hidden or contained WebView loads a pinned Element Web runtime and exposes a narrow bridge. Native Android screens render login, room list, timeline, composer, reactions, typing, and verification. The runtime sends structured JSON state to the UI and accepts structured commands from the UI.

## Architecture

```text
Native Android UI
  LoginActivity
  HomeActivity / RoomList
  RoomActivity / Timeline / Composer
  Verification and reaction surfaces
        |
        | RuntimeBridge: JSON commands and events
        v
Element Web Runtime WebView
  Element Web / matrix-js-sdk
  IndexedDB / localStorage for E2EE/session state
  Sync, decrypt, send, typing, reactions, verification
        |
        | Matrix Client-Server API
        v
Homeserver

UnifiedPush / ntfy
        |
        v
Native push receiver
        |
        v
Runtime wake + sync request + Android notification update
```

## Runtime Bridge

The bridge is the only boundary between native UI and the JS runtime.

Native commands:

- `runtime.init`: load runtime and report readiness.
- `auth.loginPassword`: homeserver URL, username, password.
- `auth.logout`: clear runtime session.
- `rooms.subscribe`: start room list updates.
- `rooms.open`: select room and subscribe to timeline updates.
- `timeline.paginateBack`: request older events.
- `messages.sendText`: send raw text exactly as typed.
- `typing.set`: send typing true or false for current room.
- `reactions.send`: add a reaction to an event.
- `verification.action`: accept, cancel, compare, or confirm verification steps.
- `push.register`: register or update the Matrix pusher using the UnifiedPush endpoint.
- `sync.once`: ask the runtime to sync after a push wake.

Runtime events:

- `runtime.ready`
- `runtime.error`
- `auth.state`
- `sync.state`
- `rooms.snapshot`
- `timeline.snapshot`
- `timeline.append`
- `typing.update`
- `reactions.update`
- `verification.update`
- `push.registrationState`

All events carry JSON DTOs owned by the new runtime module. Native UI must not consume Matrix SDK Android model classes.

## Rendering

The UI renders message bodies natively from bridge DTOs. The renderer accepts:

- plain body text
- Matrix formatted HTML from the JS runtime when present
- raw markdown body for LLM-style output

The first implementation uses one native renderer instance in the timeline item path, not one WebView per formula. Tables and code blocks use Markwon table/code support. Inline and block math use a native math renderer path or a single shared runtime render service that returns image/span data; it must not create a WebView per formula.

The renderer must prefer sender-authored raw markdown for table/math cases so agents can keep sending normal markdown. It must fall back to plain text if rendering fails.

## UI Reuse Boundary

SchildiChat UI reuse means:

- copy or adapt layouts, colors, dimensions, drawables, room list structure, timeline bubble structure, composer ergonomics, and interaction patterns;
- keep the Android visual hierarchy native;
- replace SchildiChat view models and Matrix SDK dependencies with bridge-backed state stores;
- keep file names and package boundaries clear so reviewers can see which code is copied UI and which code is new runtime.

It does not mean:

- use SchildiChat's Matrix session as the source of truth;
- expose SchildiChat's old timeline factory as the new data pipeline;
- keep debug-only tooling such as LeakCanary in user artifacts.

## Push

The app registers as a UnifiedPush client. ntfy can be used as the distributor. The native push receiver receives wake events, starts the app process if needed, and asks the JS runtime to sync. If Android kills the runtime too aggressively, the app shows a generic notification and finishes sync when opened; this degraded mode must be explicit in logs and settings.

The pusher registration is owned by the JS runtime so that it uses the same Matrix access token and device identity as the E2EE session.

## Security And Privacy

- JS bridge is exposed only to the pinned runtime origin or bundled local runtime asset.
- Bridge methods accept JSON strings and validate command type before dispatch.
- The app never logs passwords, access tokens, device keys, message bodies, or private homeserver endpoints.
- GitHub Actions builds with debug signing only until a release signing path is explicitly configured.
- Private endpoints are test inputs supplied at runtime, not committed fixtures.

## Verification Strategy

CI must build the new app artifact. Unit tests cover DTO parsing, command serialization, bridge dispatch, room/timeline state reduction, and markdown detection. Instrumentation or smoke tests cover WebView runtime startup and the login screen flow.

Manual validation targets:

- login with a custom homeserver path;
- E2EE room decrypts messages;
- room list updates after sync;
- sending text works;
- typing state appears live;
- reactions appear and can be sent;
- verification events surface in native UI;
- raw markdown table and LaTeX render in timeline;
- ntfy/UnifiedPush wake triggers sync or a clear degraded notification.

## First Slice

The first shippable slice is not a full Matrix client. It must prove the new architecture:

1. A new Android app module builds in GitHub Actions.
2. The app shows a native login screen styled after SchildiChat.
3. A single WebView runtime loads Element Web or a pinned local runtime page.
4. Native code can send a command to JS and receive a structured event back.
5. The app can store bridge state and render a SchildiChat-style room list/timeline from sample DTOs.

Only after that slice passes should live Matrix login and E2EE wiring be connected.
