# Matrix Rich Android

Matrix Rich Android is an Android-native Matrix client experiment focused on LLM-style rich message reading: Markdown, tables, formulas, and ntfy wake notifications.

The main chat surface is native Android UI. It does not embed Element Web as the visible room timeline.

## Current Shape

- Native top app bar, room rail, message timeline, composer, push tab, and settings tab.
- Message bodies render through Markwon with table and LaTeX plugins.
- Inline `$...$` formulas are accepted and normalized for the native LaTeX renderer.
- `$$...$$` display formulas are preserved.
- ntfy foreground service subscribes to `/<topic>/json`, shows Android notifications, and wakes the native chat surface.
- `matrixrich://open?url=...` and `ntfy://host/topic` deep links are handled by the Android app.

## What Changed From The First Attempt

The first build exposed Element Web directly inside the main Chats tab. That is not the intended product shape. The visible WebView path has been removed from `MainActivity`; the app now renders a native Android chat layout.

Element Web remains useful as a reference for E2EE behavior and rendering expectations, but it is not the Android main UI.

## Matrix Engine Status

This repository currently has the native Android shell, rich message renderer, and ntfy wake path. The Matrix sync/E2EE engine still needs to be wired behind the native UI. The intended boundary is:

```text
Matrix sync/E2EE runtime
        |
        v
Native room list + native timeline + native composer
        |
        v
Markwon table/formula renderer
```

The Android UI should stay native while the protocol runtime is added behind it.

## ntfy Setup

1. Open **Settings**.
2. Set `ntfy server`, for example `https://ntfy.sh` or your own server.
3. Set `ntfy topic`.
4. Optionally set a bearer token.
5. Enable the foreground push listener.

Publish a test notification:

```bash
curl -d "Hello from Matrix Rich" https://ntfy.sh/YOUR_TOPIC
```

Publish a notification that wakes this app:

```bash
curl \
  -H "Click: matrixrich://open?url=https%3A%2F%2Fmatrix.to%2F%23%2F..." \
  -d "Open Matrix Rich" \
  https://ntfy.sh/YOUR_TOPIC
```

For Matrix notifications, your homeserver, bot, or gateway still needs to publish to the same ntfy topic. The Android app is the receiver and wake surface.

## Privacy Notes

- Do not commit ntfy bearer tokens, homeserver access tokens, Matrix recovery keys, signing keys, or generated APK signing material.
- GitHub Actions builds an unsigned debug APK artifact.
- Release signing should use GitHub Actions secrets later, not files committed to the repository.

## Build

GitHub Actions builds the debug APK on every push, pull request, and manual workflow run:

```text
.github/workflows/android.yml
```

The artifact name is `matrix-rich-debug-apk`.

Local build, when Android SDK and Java are installed:

```bash
gradle testDebugUnitTest assembleDebug
```
