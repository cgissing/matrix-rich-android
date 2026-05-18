# Matrix Rich Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a debug-buildable Android WebView Matrix client shell with ntfy notification wake support.

**Architecture:** Host Element Web or a compatible static Matrix web client in one Android WebView inside a native mobile shell with top app bar, bottom navigation, push tab, and settings tab. Keep push separate in a foreground ntfy JSON-stream service. Use GitHub Actions for reproducible APK builds.

**Tech Stack:** Android Java, Android WebView, ntfy JSON stream, Gradle Android Plugin, GitHub Actions.

---

### Task 1: Project and CI

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `app/build.gradle`
- Create: `.github/workflows/android.yml`
- Create: `.gitignore`

- [x] Create the Gradle Android project skeleton.
- [x] Add JUnit for JVM unit tests.
- [x] Add GitHub Actions steps for JDK 17, Android SDK, Gradle 8.10.2, unit tests, debug APK assembly, and artifact upload.

### Task 2: ntfy Parser and Endpoint Tests

**Files:**
- Create: `app/src/test/java/io/github/cgissing/matrixrich/NtfyMessageTest.java`
- Create: `app/src/test/java/io/github/cgissing/matrixrich/NtfyEndpointTest.java`
- Create: `app/src/main/java/io/github/cgissing/matrixrich/NtfyMessage.java`
- Create: `app/src/main/java/io/github/cgissing/matrixrich/NtfyEndpoint.java`

- [x] Test parsing displayable ntfy message events.
- [x] Test ignoring `open` and `keepalive` events.
- [x] Test missing optional fields.
- [x] Test ntfy JSON stream URL construction and topic encoding.
- [x] Implement parser and URL helper.

### Task 3: Android WebView Shell

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/io/github/cgissing/matrixrich/MainActivity.java`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/styles.xml`
- Create: `app/src/main/res/drawable/ic_launcher.xml`

- [x] Create a no-XML Activity UI with mobile top app bar, bottom navigation, WebView chat tab, push tab, and settings tab.
- [x] Enable JavaScript, DOM storage, database storage, file chooser, media permission forwarding, and zoom.
- [x] Apply Element mobile-guide bypass cookie and desktop user agent by default.
- [x] Add settings tab for Element Web URL and ntfy values.
- [x] Add `matrixrich://` and `ntfy://` deep-link handling.

### Task 3.1: Element Web Mobile Adapter

**Files:**
- Create: `app/src/main/java/io/github/cgissing/matrixrich/MobileElementAdapter.java`
- Create: `app/src/test/java/io/github/cgissing/matrixrich/MobileElementAdapterTest.java`
- Modify: `app/src/main/java/io/github/cgissing/matrixrich/MainActivity.java`

- [x] Add adapter tests checking LaTeX labs storage, mobile-guide bypass, table CSS, and KaTeX CSS.
- [x] Generate an injected script that installs the viewport, CSS, and Element Web labs setting.
- [x] Inject the script after WebView page load.

### Task 4: ntfy Foreground Push

**Files:**
- Create: `app/src/main/java/io/github/cgissing/matrixrich/NtfyPushService.java`
- Create: `app/src/main/java/io/github/cgissing/matrixrich/BootReceiver.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [x] Add foreground service permissions and service declaration.
- [x] Subscribe to ntfy `/topic/json`.
- [x] Support bearer-token auth.
- [x] Show message notifications.
- [x] Open the WebView on notification tap.
- [x] Restart listener on boot when enabled.

### Task 5: Documentation and Verification

**Files:**
- Create: `README.md`
- Create: `docs/superpowers/specs/2026-05-19-matrix-rich-android-design.md`
- Create: `docs/superpowers/plans/2026-05-19-matrix-rich-android.md`

- [x] Document verified Element Web mobile limitations.
- [x] Document recommended self-hosted Element Web config for LaTeX.
- [x] Document ntfy setup and GitHub Actions APK artifact.
- [ ] Run JVM unit tests and debug APK build in an Android-capable environment.
