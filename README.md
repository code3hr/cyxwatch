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
- [Security layer](docs/security-layer.md)
- [Security implementation TODO](docs/todo_security.md)
- [Launch-gate onboarding plan](docs/launch-gate-plan.md)

## Product Direction

Tagline: See what your apps are really doing.

CyxWatch should not be positioned as antivirus or spyware removal. The stronger position is real-time app transparency: useful evidence, simple explanations, and low battery impact.

## How CyxWatch Works

- **Local-first signals only:** Usage access, installed app metadata, declared permissions, and network counters are evaluated on-device only.
- **Network visibility modes:**
  - **Basic mode:** `NetworkStatsManager` byte totals by app.
  - **Advanced mode:** explicit user opt-in VPN path with local packet header visibility (endpoint + size only). No content payloads.
- **Risk explainability:** scoring reasons include app name/package context and signal identifiers so users can open supporting evidence quickly.
- **Sensitive-permission alerts:** when sensitive permissions are newly added or found on new install, CYXWatch raises an in-app warning and notification path for direct review.
- **Important limitation:** this is **not** a private VPN/tunnel service. It does not hide traffic or provide end-to-end anonymity; it only increases observability for local audits.

See [docs/system-overview.md](docs/system-overview.md) for the full component flow and data boundaries.

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

## Local Build Commands (reliable, anti-stall)

Use the helper script so the Gradle environment is always initialized the same way:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
powershell -ExecutionPolicy Bypass -File scripts\build-cyxwatch.ps1 -Mode debug
```

Other presets:

- `-Mode test` → `./gradlew test`
- `-Mode lint` → `./gradlew lintDebug`
- `-Mode release` → `./gradlew assembleRelease`
- `-Mode ci` → test + lint + compile debug AndroidTest + both APK builds

`build-cyxwatch.ps1` outputs synced APKs to:
- `app\build\outputs\apk\debug\app-debug.apk`
- `app\build\outputs\apk\release\app-release.apk` or `app-release-unsigned.apk`

If a run gets blocked by stale Gradle/Java processes, add `-StopJava`.
If lock contention keeps recurring (for example `.lck` files in temp caches), add `-CleanBuildState`.

Example hard recovery:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-cyxwatch.ps1 -Mode ci -StopJava -CleanBuildState
```

## Run On Device (Debug Smoke Test)

Use `scripts/run-debug-on-device.ps1` after you have built at least once:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
powershell -ExecutionPolicy Bypass -File scripts\run-debug-on-device.ps1
```

The script:

- Builds `app-debug.apk` (unless `-NoBuild`).
- Installs it on one connected Android device (`-DeviceId` to pick one if multiple).
- Launches `com.cyxwatch.app/.MainActivity`.
- Starts `adb logcat` filtered on `CyxWatch` and shows recent logs only when `-LogLineCount` > 0.

Examples:

```powershell
# full run: build, install, launch, live logs
powershell -ExecutionPolicy Bypass -File scripts\run-debug-on-device.ps1

# use existing build, install and launch to one specific device
powershell -ExecutionPolicy Bypass -File scripts\run-debug-on-device.ps1 -NoBuild -DeviceId emulator-5554

# capture latest 80 logcat lines only
powershell -ExecutionPolicy Bypass -File scripts\run-debug-on-device.ps1 -NoLaunch -LogLineCount 80
```

For a quick startup verification loop (install, launch, running check, startup log capture), use:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-device-smoke.ps1
```

Useful options:

- `-NoBuild` to reuse the already-built APK
- `-NoInstall` / `-NoLaunch` for custom flows
- `-DeviceId <serial>` if more than one device is connected
- `-PostLaunchWaitSeconds 10` to wait longer before log capture
- `-LogTailLines 300` to capture more startup lines

Requirements:

- Android SDK `adb` available in `PATH`.
- A phone/emulator with USB debugging enabled and authorized.
- The app package uses `com.cyxwatch.app`.
