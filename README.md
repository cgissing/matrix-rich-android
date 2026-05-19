# Matrix Rich Android

Matrix Rich Android is an Android-native Matrix client focused on LLM-style rich message reading: Markdown, tables, formulas, Matrix sync/send, and ntfy wake notifications.

The main chat surface is native Android UI. It does not embed Element Web as the visible room timeline.

## Current Shape

- Native top app bar, room rail, message timeline, composer, push tab, and settings tab.
- Password login with the same simple inputs as Element Web: homeserver or Element Web URL, login name, and password.
- Advanced token restore is still available, but user ID, device ID, and access token are hidden until that mode is enabled.
- Element Web deployment URLs such as `https://example.org/element/` are resolved through their `config.json` / Matrix `.well-known` data before the hidden runtime connects.
- A hidden local WebView runs a bundled Matrix JS SDK runtime for Matrix sync/send and Rust/WASM E2EE. This runtime is not a visible chat UI.
- Matrix JS SDK snapshots populate the native room rail and timeline.
- Sending goes through Matrix JS SDK so encrypted rooms are sent through the E2EE stack instead of the raw REST fallback.
- E2EE session verification can be started from Settings. The native UI exposes Matrix JS SDK SAS and QR flows: request or accept verification, compare SAS values, show a QR code for another device, scan another device's QR code, and confirm successful scans.
- Message bodies render through Markwon with table and LaTeX plugins.
- Inline `$...$` formulas are accepted and normalized for the native LaTeX renderer.
- `$$...$$` display formulas are preserved.
- ntfy foreground service subscribes to `/<topic>/json`, shows Android notifications, and wakes the native chat surface.
- `matrixrich://open?url=...` and `ntfy://host/topic` deep links are handled by the Android app.

## What Changed From The First Attempt

The first build exposed Element Web directly inside the main Chats tab. That is not the intended product shape. The visible chat surface is now a native Android layout; the only WebView is a hidden Matrix JS runtime with no Element Web UI loaded into it.

Element Web remains useful as a reference for E2EE behavior and rendering expectations, but it is not the Android main UI. The app now uses the web-side Matrix JS SDK idea as a hidden runtime, while the visible chat surface stays native.

## Matrix Engine Status

This repository now uses a bundled browser-side Matrix runtime for Matrix state and E2EE, while keeping the Android UI native. The intended boundary is:

```text
Hidden Matrix JS SDK + Rust crypto WASM runtime
        |
        v
Native room list + native timeline + native composer
        |
        v
Markwon table/formula renderer
```

The Android UI stays native. The runtime initializes `matrix-js-sdk` Rust crypto through WASM, uses IndexedDB for Matrix and crypto stores, and can use a Matrix recovery/security key to unlock server-side key backup. Without a recovery key, newly logged-in devices may only decrypt messages for which they receive keys after this device starts.

The Settings screen also exposes E2EE verification for the current session. SAS verification is wired end to end. QR verification supports both showing this device's QR code and scanning another device's QR code with the native camera scanner; the Matrix JS SDK still owns the verification protocol state.

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
- The debug app stores Matrix access tokens and optional recovery keys in Android `SharedPreferences`; release hardening should move secrets to encrypted storage.
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
cd web-runtime
npm ci
npm run check
cd ..
gradle testDebugUnitTest assembleDebug
```
