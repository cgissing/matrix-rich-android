# Matrix Rich Android Native UI Plan

This plan supersedes the first WebView-shell plan.

## Goal

Build an Android-native Matrix client surface with rich Markdown, table, formula rendering, and ntfy notification wake support. Element Web may be used as implementation reference, but the visible chat UI must be native Android.

## Architecture

- `MainActivity`: native top app bar, room rail, timeline, composer, push tab, and settings tab.
- `RichMarkdownRenderer`: Markwon-based Markdown renderer with table and LaTeX plugins.
- `DemoMatrixState`: temporary local timeline fixtures that exercise Markdown, tables, inline formulas, and display formulas.
- `NtfyPushService`: foreground ntfy JSON stream listener.
- `NtfyEndpoint` and `NtfyMessage`: pure Java push URL/message parsing.

## Implemented Tasks

- [x] Remove the visible WebView chat tab.
- [x] Remove Element Web mobile adapter code and tests.
- [x] Replace the main chat surface with native Android room and timeline views.
- [x] Add native Markdown/table/LaTeX rendering dependencies.
- [x] Normalize single-dollar inline math so agents can keep writing normal Markdown math.
- [x] Keep ntfy foreground listener and deep-link wake handling.
- [x] Update README to describe the native UI boundary.

## Remaining Tasks

- [ ] Wire a real Matrix sync/E2EE runtime behind the native UI.
- [ ] Replace demo room/message fixtures with live room summaries and events.
- [ ] Add Android instrumentation screenshots for the native layout.
- [ ] Add release signing through GitHub Actions secrets.
