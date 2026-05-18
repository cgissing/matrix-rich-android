# Matrix Rich Android Design

## Goal

Build an Android client that gives Matrix messages a ChatGPT/Gemini-like rich rendering path for Markdown, tables, and formulas, while supporting ntfy-based notification wake-up without making agents output Matrix-specific markup.

## Verified Constraints

Element Web is a useful runtime reference because E2EE and rich web rendering are already implemented there. It is also not a drop-in mobile client: current Element Web source redirects Android/iOS user agents to `mobile_guide/`, and that page states the desktop site does not work on mobile. The public `app.element.io/config.json` also does not force-enable `feature_latex_maths`.

Formula rendering in Element Web uses KaTeX via React/DOM inside the hosted page. It is not one Android WebView per formula. The realistic performance risk is many KaTeX DOM nodes in one WebView timeline.

## Architecture

The first implementation is an Android native shell with a single WebView and a foreground ntfy listener. The shell uses a mobile-client layout: top app bar, bottom navigation, chat runtime tab, push tab, and settings tab. The WebView loads a configurable Element Web URL inside the chat tab. The app applies a desktop user agent and the Element mobile-guide bypass cookie by default so `app.element.io` remains usable as a fallback. The recommended deployment is a self-hosted or pinned Element Web static bundle with `mobile_guide_toast=false` and `feature_latex_maths=true`.

After Element Web loads, the Android shell injects a small adapter script. The script enables the Element Web LaTeX labs flag in local storage, reapplies the mobile-guide bypass, adds a viewport tag when needed, and installs narrow-screen CSS for timelines, tables, code blocks, and KaTeX display blocks. This does not replace Element Web's app internals, but it reduces the worst mobile layout problems without taking on Matrix sync/E2EE natively.

The ntfy layer is independent from Element Web. It subscribes to the configured ntfy `/topic/json` stream in a foreground service, shows native notifications, and opens the WebView when the user taps the notification. A Matrix homeserver, Hermes gateway, or separate bridge must publish the actual push payload to ntfy.

## Components

- `MainActivity`: hosts native mobile shell UI, WebView, settings tab, push tab, deep-link handling, file chooser, media permission forwarding, and service start/stop.
- `MobileElementAdapter`: generates the Element Web mobile adapter script and CSS.
- `NtfyPushService`: foreground service that streams ntfy JSON events and posts Android notifications.
- `NtfyMessage`: small parser for ntfy JSON lines.
- `NtfyEndpoint`: URL normalizer/encoder for ntfy JSON streams.
- `BootReceiver`: restarts the foreground listener after boot when enabled.
- GitHub Actions workflow: installs Android SDK and Gradle, runs JVM tests, and builds the debug APK artifact.

## Out Of Scope For First Pass

- Rewriting Matrix sync/E2EE natively.
- Forking Element X Android.
- Integrating Huawei/Xiaomi/OPPO/vivo push SDKs.
- Making Element Web itself fully mobile-native.
- Publishing a signed release APK.
