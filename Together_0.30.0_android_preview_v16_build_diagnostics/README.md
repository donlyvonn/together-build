# Together 0.30.0 — source foundation (not an APK)

This archive contains an initial Kotlin conversation-linking and timeline-merging module with JUnit tests. It is **not** a complete Android application, a recovered version of 0.29.37, or a functioning messaging connector. Tests are provided but were not executed here because Gradle/Android build tools are not installed in this environment.

## Implemented in source
- Reversible display-thread grouping with explicit recipient confirmation.
- Stable source-aware message identity, chronological ordering, and cautious deduplication.
- Message origin and delivery state models.
- Source capability and synchronization-status models.
- Unit tests for linking, unlinking, chronology, duplicate IDs and same-text cross-service messages.

## Not implemented yet
- Android project scaffolding, UI, persistence and migration.
- Actual Google Messages/RCS or OpenBubbles adapters, permissions and background refresh.
- Attachment viewers and tested exact-thread intents.
- APK compilation/signing. Original Together Preview private key is unavailable.

## Safety
Do not uninstall or clear data for Together 0.29.37. The new project may require a distinct package ID if the original signing key cannot be recovered. Never claim RCS/iMessage history is synchronized unless verified by a real adapter.

## 2026-10-19 implementation milestone
Added `LinkStorage.kt`: versioned, app-private, atomic-file persistence for linked display threads. The storage records only link identifiers, not messages or attachments. `LinkStorageSmoke.kt` verifies save/load, reversible unlink, chronological merge, and source-ID deduplication. Tested with installed Kotlin compiler/JVM (PASS). This is **not** a compiled Android app and does not connect to Google Messages or OpenBubbles. Android UI, permissions, source adapters, integration testing and APK signing remain outstanding. Never uninstall the user's current APK to test this foundation.

Test locally with:
```
kotlinc core/src/main/kotlin/app/together/core/*.kt core/src/test/kotlin/app/together/core/LinkStorageSmoke.kt -include-runtime -d /tmp/together-core-test.jar
java -jar /tmp/together-core-test.jar
```

## Next milestone: non-destructive sync coordinator (2026-10-19)
Added `SyncCoordinator.kt` and `SyncCoordinatorSmoke.kt`. This pure Kotlin layer retains previously observed messages after partial/empty sync responses, merges newly accessible outgoing messages by source IDs, preserves confirmed delivery state when later updates are uncertain, and exposes per-source warnings and capability flags. Smoke test executed successfully with `kotlinc` and JVM. It is **not** a Google Messages/RCS or OpenBubbles connector and cannot retrieve unavailable messages. Data is in-memory until an Android database is implemented; no APK is included.

Run: `kotlinc core/src/main/kotlin/app/together/core/*.kt core/src/test/kotlin/app/together/core/SyncCoordinatorSmoke.kt -include-runtime -d /tmp/together-sync-test.jar && java -jar /tmp/together-sync-test.jar`

## Attachment routing milestone (2026-10-19)
Added `AttachmentRouting.kt` and `AttachmentRoutingSmoke.kt`. The pure Kotlin policy chooses an in-app preview only when a Google Messages media URI is supplied, and chooses exact-thread navigation only when an adapter supplies a verified destination. It otherwise returns a user-visible unavailable reason. Tests passed with `kotlinc`/JVM. Android URI validation, content permissions, viewer UI, actual OpenBubbles intent discovery, and device testing remain outstanding.

Run: `kotlinc core/src/main/kotlin/app/together/core/*.kt core/src/test/kotlin/app/together/core/AttachmentRoutingSmoke.kt -include-runtime -d /tmp/together-attachment-test.jar && java -jar /tmp/together-attachment-test.jar`
