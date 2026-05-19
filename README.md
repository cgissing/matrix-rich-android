# Matrix Rich Android

Matrix Rich Android is now an Android shell around Element Web. The visible chat experience is Element Web running inside an Android WebView, with a native wrapper for Android permissions, settings, and ntfy wake notifications.

This supersedes the earlier native Matrix timeline experiment. Matrix login, E2EE, room state, Markdown, tables, reactions, typing indicators, verification, and formula rendering stay in Element Web instead of being reimplemented in Android Java.

## Current Shape

- The main screen is a thin Android WebView shell loading Element Web, defaulting to `https://app.element.io/`.
- A native toolbar provides reload and settings.
- Settings let you choose an Element Web URL, for example a self-hosted Element Web deployment such as `https://example.org/element/`.
- The WebView enables JavaScript, DOM storage, IndexedDB-backed browser storage, media permissions, and file upload for Element Web.
- Element Web `config.json` is intercepted and patched to enable `feature_latex_maths` and Labs settings.
- A mobile CSS/JS patch is injected after page load to reduce fixed desktop-width layout problems on Android.
- ntfy foreground service subscribes to `/<topic>/json`, shows Android notifications, and wakes the Element Web shell.
- `matrixrich://open?url=...` and `ntfy://host/topic` deep links are handled by the Android app.

## Boundary

```text
Android shell
  - WebView host
  - file/camera/notification permissions
  - ntfy foreground listener
  - notification click wake
  - mobile CSS/config patch

Element Web
  - Matrix login
  - E2EE
  - sync/send
  - Markdown/tables/formulas
  - reactions/typing/verification
```

Android code should not reparse Matrix timelines or require agents to emit Matrix-specific math message formats. If Element Web can handle a feature, it should stay inside Element Web.

## ntfy Setup

1. Open **Settings** in the Android toolbar.
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

- Do not commit ntfy bearer tokens, Matrix access tokens, recovery keys, signing keys, or generated APK signing material.
- Element Web stores its own browser-side session data inside the Android WebView storage area.
- The debug APK artifact is unsigned for release use.
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
