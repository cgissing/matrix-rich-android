# Element Web Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the native Matrix timeline experiment with an Android shell around Element Web, while keeping ntfy wake/notification support in the native layer.

**Architecture:** `MainActivity` becomes a WebView host for Element Web instead of a native Matrix client. Matrix login, E2EE, Markdown, tables, reactions, typing, verification, and LaTeX rendering stay inside Element Web. Android code owns only shell concerns: WebView permissions, reload/settings UI, mobile CSS/JS patch injection, notification permission, and ntfy foreground wake service.

**Tech Stack:** Android Java, WebView, AndroidX WebKit where already present, existing ntfy Java service/tests, GitHub Actions Android build.

---

### Task 1: Add Element Web Shell Configuration

**Files:**
- Modify: `app/src/main/java/io/github/cgissing/matrixrich/AppPrefs.java`
- Create: `app/src/main/java/io/github/cgissing/matrixrich/ElementWebConfig.java`
- Test: `app/src/test/java/io/github/cgissing/matrixrich/ElementWebConfigTest.java`

- [x] Add a persistent Element Web URL setting with default `https://app.element.io/`.
- [x] Add URL normalization that prepends `https://`, trims whitespace, and preserves nested paths.
- [x] Add tests for blank URL, host-only URL, and the user's nested homeserver-style path.

### Task 2: Add Mobile Patch Script

**Files:**
- Create: `app/src/main/java/io/github/cgissing/matrixrich/ElementWebPatch.java`
- Test: `app/src/test/java/io/github/cgissing/matrixrich/ElementWebPatchTest.java`

- [x] Generate one JavaScript string that injects viewport and CSS into the Element Web document.
- [x] Keep the patch idempotent by using stable element ids.
- [x] Include markers for Element Web shell tests: `matrix-rich-mobile-patch`, `feature_latex_maths`, and `mx_MatrixChat`.

### Task 3: Replace MainActivity With Element Web Shell

**Files:**
- Replace: `app/src/main/java/io/github/cgissing/matrixrich/MainActivity.java`

- [x] Create a vertical layout with a small native toolbar and full-height WebView.
- [x] Load the configured Element Web URL on startup.
- [x] Enable JavaScript, DOM storage, database storage, media playback, file chooser, and camera permission bridge for Element Web features.
- [x] Inject `ElementWebPatch.mobilePatchScript()` on page finish.
- [x] Handle `open_url`, `matrixrich://open?url=...`, and `ntfy://...` notification wake intents.
- [x] Keep a native settings dialog for Element Web URL, ntfy server/topic/token, and push enablement.

### Task 4: Preserve Push Wake Contract

**Files:**
- Keep: `app/src/main/java/io/github/cgissing/matrixrich/NtfyPushService.java`
- Keep: `app/src/main/java/io/github/cgissing/matrixrich/NtfyEndpoint.java`
- Keep: `app/src/main/java/io/github/cgissing/matrixrich/NtfyMessage.java`

- [x] Leave the ntfy JSON stream foreground service in place.
- [x] Let notifications open `MainActivity` and pass click URLs into the WebView shell.
- [x] Keep boot restart behavior through `BootReceiver`.

### Task 5: Verification and Release

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/plans/2026-05-19-matrix-rich-android.md`

- [x] Update docs to state that the native timeline experiment is superseded.
- [ ] Run the available local checks.
- [ ] Run privacy scan.
- [ ] Commit as `cgissing`.
- [ ] Push to GitHub.
- [ ] Wait for GitHub Actions to build the debug APK artifact.
