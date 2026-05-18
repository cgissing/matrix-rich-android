# Matrix Rich Android

Matrix Rich Android is a small Android shell for using a web Matrix client with rich LLM-style message rendering, plus an ntfy-based notification wake path.

The first runtime target is Element Web or a compatible static Element Web deployment. Element Web already carries the Matrix web client, E2EE implementation, Markdown rendering, table support through the browser engine, and KaTeX-based math rendering when `feature_latex_maths` is enabled. This Android app does not ask agents to emit Matrix-specific math HTML.

## Current Shape

- A native Android shell provides the mobile layout: top app bar, bottom navigation, a chat runtime tab, a push-status tab, and a settings tab.
- One Android `WebView` hosts the configured Matrix web client inside the chat tab.
- The default URL is `https://app.element.io/`, but this is a fallback.
- A desktop user agent and `element_mobile_redirect_to_guide=false` cookie are applied by default because the official site redirects Android/iOS user agents to its mobile guide.
- After each Element Web page load, the shell injects a small mobile adapter that enables the LaTeX labs flag in local storage, reapplies the mobile-guide bypass, and adds CSS for narrow timelines, tables, code blocks, and KaTeX display blocks.
- A foreground `NtfyPushService` subscribes to `/<topic>/json`, shows Android notifications, and opens the WebView when a notification is tapped.
- `matrixrich://open?url=https%3A%2F%2Fmatrix.to%2F%23%2F...` can wake the app and load a target URL.
- `ntfy://ntfy.example.com/topic` can seed the ntfy server/topic settings.

## Recommended Element Web Deployment

For the rich-text goal, use a self-hosted or pinned Element Web static deployment instead of relying on the public `app.element.io` defaults. A suitable `config.json` should include:

```json
{
  "mobile_guide_toast": false,
  "show_labs_settings": true,
  "features": {
    "feature_latex_maths": true
  }
}
```

This repository includes the same idea as `element-web-config.example.json`.

Why: the official public `app.element.io/config.json` currently does not enable `feature_latex_maths`, and its mobile guide page says the desktop site does not work on mobile. This app bypasses the redirect and wraps the runtime in native mobile navigation, but a pinned/self-hosted Element Web bundle is still the cleaner base for a daily client.

## Formula Performance Note

Element Web formulas are rendered by KaTeX inside the single hosted WebView. It is not one Android `WebView` per formula. The remaining performance risk is normal web layout cost from many KaTeX DOM nodes in a long timeline.

## Privacy Notes

- Do not commit ntfy bearer tokens, homeserver access tokens, Matrix recovery keys, signing keys, or generated APK signing material.
- The sample Element Web config contains only public defaults and feature flags.
- GitHub Actions currently builds an unsigned debug APK artifact. Release signing should use GitHub Actions secrets later, not files committed to the repository.

## ntfy Setup

1. Open **Settings** in the app.
2. Set `ntfy server`, for example `https://ntfy.sh` or your own server.
3. Set `ntfy topic`.
4. Optionally set a bearer token.
5. Enable the foreground push listener.

Publish a test notification:

```bash
curl -d "Hello from Matrix Rich" https://ntfy.sh/YOUR_TOPIC
```

Publish a notification that wakes this app from ntfy or another bridge:

```bash
curl \
  -H "Click: matrixrich://open?url=https%3A%2F%2Fapp.element.io%2F" \
  -d "Open Matrix Rich" \
  https://ntfy.sh/YOUR_TOPIC
```

For Matrix notifications, your homeserver, bot, or gateway still needs to publish to the same ntfy topic. The Android app is the receiver and wake surface.

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

On this Windows workspace, Java/Gradle/ANDROID_HOME were not present during scaffolding, so local APK compilation was intentionally left to GitHub Actions.
