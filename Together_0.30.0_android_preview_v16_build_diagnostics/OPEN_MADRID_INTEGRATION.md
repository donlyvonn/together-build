# Open Madrid evaluation — Together 0.30.0

Upstream: https://github.com/fenleon/open-madrid (Apache-2.0; review upstream license and code before vendoring).

The 2026-10-17 upstream README reports verified registration/validation, but no proven courier send or receive, incomplete crypto verification, and live activation prerequisites. This project does **not** bundle Apple identities, validation secrets, or Open Madrid source code. It does **not** provide working iMessage sending or receiving.

Added a Kotlin engine capability policy with tests. Open Madrid is explicitly marked experimental and blocked from live send/receive until verified; existing OpenBubbles workflows are not modified. A future integration requires upstream API inspection, license compliance, a device-safe registration flow, end-to-end send/receive tests, and opt-in switching with rollback. Never automatically retry a message across engines after an uncertain send result (duplicate-message risk).

Android compilation and on-device testing are pending.

## v15 inspection
Open Madrid's upstream README as of October 20, 2026 continues to say live courier message send/receive is not implemented/verified. The preview exposes a read-only status dialog for its documented capabilities; no live integration is claimed.
