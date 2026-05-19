# Matrix Rich Android Native UI Plan

This plan is superseded by `docs/superpowers/plans/2026-05-19-element-web-shell.md`.

The native Matrix timeline experiment was abandoned because it recreated too much of Element Web: E2EE session handling, message rendering, reactions, typing indicators, verification, and formula support all had to be mapped from Matrix JS SDK into Android Java. The current direction is an Element Web Android shell with native Android support only for WebView hosting, permissions, mobile patching, and ntfy wake notifications.
