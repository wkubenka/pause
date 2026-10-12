# CLAUDE.md

## Project

Pause (`com.astute.pause`): a local-only Android app that runs a price-scaled smart-spending checklist before non-essential purchases. Spec: `spec.md`. Plan: `docs/superpowers/plans/2026-10-12-pause-v1.md`. Distributed through Astute Repo (github.com/wkubenka/astute-apps), so releases must stay compatible with its contract (see README "Releasing").

## Commands

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest
    ./gradlew testDebugUnitTest --tests "com.astute.pause.domain.MoneyTest"

Tests are JVM-only (JUnit 4 + Robolectric, SDK 34 via `src/test/resources/robolectric.properties`). There is no androidTest suite.

## Rules that are easy to break

- Money is `Long` cents + currency code. Never `Double`.
- Waits run from `Item.firstConsideredAt`, are advisory, and never block Bought/Skipped.
- The app never recommends Buy/Wait/Skip. Keep copy gentle.
- Room schema is exported to `app/schemas/`. Any entity change needs a version bump + Migration. Never use destructive migration.
- No INTERNET permission. Do not add network libraries.
- Android backup is off (`allowBackup="false"` + `res/xml/data_extraction_rules.xml`). Purchase data must stay off Google; don't re-enable it.
- Inject `java.time.Clock` for anything time-based. Don't call `Instant.now()` directly.
- java.time APIs must exist on API 26 (no `Duration.isPositive`, `toHoursPart`, and so on).
