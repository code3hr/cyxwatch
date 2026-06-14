# CyxWatch

CyxWatch is a lightweight mobile privacy observability app. Its purpose is to show users, in plain language, what apps are doing in the background: app activity, sensitive permission signals, and network behavior.

The first practical target is Android. iOS can share branding, education, account, and reporting experiences later, but Apple sandboxing does not allow an App Store app to deeply monitor other apps.

## Documentation

- [Product architecture (platform constraints)](docs/product-architecture.md)
- [Architecture design (V1 detailed)](docs/architecture.md)
- [System overview (current implementation)](docs/system-overview.md)
- [Implementation plan](docs/implementation-plan.md)
- [Implementation design](docs/implementation-design.md)
- [User cases](docs/user-cases.md)
- [Big picture (one-page)](docs/big-picture.md)
- [Implementation backlog](docs/backlog.md)
- [Release checklist](docs/release-checklist.md)
- [Android tooling baseline](docs/android-tooling-baseline.md)
- [Build process](docs/build-process.md)

## Product Direction

Tagline: See what your apps are really doing.

CyxWatch should not be positioned as antivirus or spyware removal. The stronger position is real-time app transparency: useful evidence, simple explanations, and low battery impact.

## MVP

Version 1 should stay small:

- App activity timeline using Android usage access.
- Screen-on, screen-off, and lock-state context for background activity.
- Per-app network usage and optional local VPN-based traffic observation.
- Permission and risk summary based on installed app declarations and observed behavior.
- Simple privacy score with clear reasons.
- Local-first event history.
- User-controlled alerts for unusual activity.

## Explicit Non-Goals For V1

- No root-only features.
- No antivirus signature scanner.
- No cloud account requirement.
- No deep packet inspection by default.
- No iOS parity claim.
- No always-on heavy polling loop.

## Technical Shape

- Android app: Kotlin and Jetpack Compose.
- Persistence: Room or another small local SQLite wrapper.
- Background work: WorkManager for scheduled summaries; foreground service only when the user enables continuous VPN monitoring.
- Monitoring inputs:
  - `UsageStatsManager` for app usage and device interaction events.
  - Package metadata for declared permissions.
  - Android network stats where available.
  - `VpnService` for opt-in traffic observation.

## Privacy Rules

CyxWatch must be more privacy-preserving than the apps it reports on:

- All observations are local by default.
- No sale or sharing of user telemetry.
- No collection of packet payloads for the MVP.
- Clear consent screens for usage access and VPN mode.
- Plain explanations for every score and alert.

## Build Order

1. Android prototype with onboarding and permission setup.
2. Local event database and app inventory.
3. Usage timeline showing foreground/background/screen-state context.
4. Basic per-app privacy score.
5. Optional VPN monitor for DNS/domain and byte-count visibility.
6. Alert rules and daily summary.

See [docs/product-architecture.md](docs/product-architecture.md) for the detailed MVP architecture and platform constraints.
