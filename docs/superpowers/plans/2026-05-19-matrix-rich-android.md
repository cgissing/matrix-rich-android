# Matrix Rich Android Native UI Plan

This plan supersedes the first WebView-shell plan.

## Goal

Build an Android-native Matrix client surface with rich Markdown, table, formula rendering, Matrix login/sync/send, and ntfy notification wake support. Element Web may be used as implementation reference, but the visible chat UI must be native Android.

## Architecture

- `MainActivity`: native top app bar, room rail, timeline, composer, push tab, and settings tab.
- `RichMarkdownRenderer`: Markwon-based Markdown renderer with table and LaTeX plugins.
- `HeadlessMatrixRuntime`: invisible Android WebView host for the bundled Matrix JS SDK runtime. It is not a visible chat surface.
- `web-runtime`: browser-side Matrix JS SDK + Rust crypto WASM runtime that handles login, sync, E2EE, and send.
- `MatrixRuntimeMapper`: converts Matrix JS runtime snapshots into native room summaries and timeline messages.
- `MatrixApiClient`: retained REST subset for small non-E2EE helpers and tests, not the primary chat engine.
- `MatrixSyncParser`: retained parser for the REST subset.
- `DemoMatrixState`: temporary local timeline fixtures that exercise Markdown, tables, inline formulas, and display formulas.
- `NtfyPushService`: foreground ntfy JSON stream listener.
- `NtfyEndpoint` and `NtfyMessage`: pure Java push URL/message parsing.

## Implemented Tasks

- [x] Remove the visible WebView chat tab.
- [x] Remove Element Web mobile adapter code and tests.
- [x] Replace the main chat surface with native Android room and timeline views.
- [x] Add native Markdown/table/LaTeX rendering dependencies.
- [x] Normalize single-dollar inline math so agents can keep writing normal Markdown math.
- [x] Add Matrix password/token login.
- [x] Add Matrix `/sync` parsing for native room and message data.
- [x] Add Matrix text-message sending from the native composer.
- [x] Replace the raw REST chat path with a hidden Matrix JS SDK runtime.
- [x] Bundle Matrix JS SDK and Rust crypto WASM into APK assets through GitHub Actions.
- [x] Add optional recovery/security key input for secret-storage/key-backup unlock.
- [x] Add native SAS and QR device-verification controls backed by Matrix JS SDK.
- [x] Keep ntfy foreground listener and deep-link wake handling.
- [x] Update README to describe the native UI boundary.

## Remaining Tasks

- [ ] Add Android instrumentation screenshots for the native layout.
- [ ] Add release signing through GitHub Actions secrets.
