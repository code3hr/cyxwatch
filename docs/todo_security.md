# Security Layer TODO (Implementation Plan)

This file is the implementation backlog for the security layer described in
`docs/security-layer.md`.

Statuses: `todo`, `in_progress`, `blocked`, `done`.

## Phase A - Immediate hardening (P0)

- **SEC-001 - Add encrypted local storage for sensitive state**
  - Scope:
    - Add `androidx.security:security-crypto` dependency.
    - Introduce an encrypted preferences helper in `app/src/main/kotlin/com/cyxwatch/app/data`.
    - Migrate sensitive repos from `SharedPreferences` to encrypted preferences:
      - `UsageAccessConsentRepository`
      - `VpnModeSettingsRepository`
      - `RetentionSettingsRepository`
      - `SharedPrefsAppInventorySnapshotRepository`
      - `SharedPrefsNetworkUsageTotalsRepository`
  - Acceptance:
    - Sensitive consent/configuration values are encrypted at rest with app lock-backed key alias.
    - Existing plain-text preference migration path is safe and backward-compatible.
  - Status: `done` (validated 2026-06-20 with local build)

- **SEC-002 - Tighten notification and intent flow validation**
  - Scope:
    - Validate external `Intent` extras before use in `MainActivity` and any launch path.
    - Accept only expected keys, types, and known rules/packages.
    - Reject malformed or stale extras without mutating app state.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/MainActivity.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`
  - Acceptance:
    - Malformed intent payloads do not crash app.
    - Invalid extras are ignored and logged safely.
  - Status: `todo`

- **SEC-003 - VPN service cleanup hardening**
  - Scope:
    - Ensure `CyxWatchVpnService` calls foreground cleanup and packet stream/resource cleanup on all fail/exit paths.
    - Add defensive checks around VPN interface creation and worker startup.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/CyxWatchVpnService.kt`
  - Acceptance:
    - No leaked file descriptors/threads after rapid start/stop and permission errors.
  - Status: `todo`

## Phase B - Privacy boundary clarity (P1)

- **SEC-004 - Add explicit security/privacy impact docs in onboarding and release notes**
  - Scope:
    - Add explicit copy in onboarding and settings:
      - no packet payload collection
      - no cloud upload
      - local processing only
      - VPN mode is monitoring only, not anonymity tunnel.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui/UsageAccessScreen.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/ui/TransparencySettingsScreen.kt`
    - `docs/release-checklist.md`
  - Acceptance:
    - Copy appears before users can enable VPN mode.
    - Wording is consistent in all public user-facing surfaces.
  - Status: `todo`

- **SEC-005 - Add optional secure-screen mode**
  - Scope:
    - Add user setting to enable secure report screens.
    - Apply/remove `FLAG_SECURE` for screens with sensitive evidence or permissions.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/MainActivity.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/data/settings` (new setting state)
    - report screens in `app/src/main/kotlin/com/cyxwatch/app/ui`
  - Acceptance:
    - Setting is off by default and can be toggled.
    - Secure flag is active only for protected sessions/surfaces.
  - Status: `todo`

## Phase C - Runtime integrity (P2)

- **SEC-006 - Add runtime integrity checks in release builds**
  - Scope:
    - Detect debug-locked runtime conditions (debuggable flag, known tamper flags where low-risk and reliable).
    - Gate high-risk flows (VPN start/permission actions) behind checks.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app` (entry/launcher + service entrypoints)
  - Acceptance:
    - No user-visible behavior change in normal release usage.
    - Logging exists for blocked suspicious state.
  - Status: `todo`

- **SEC-007 - Add crash-safe and permission-recovery tests for security paths**
  - Scope:
    - Unit/instrumented tests for:
      - malformed intent extras
      - revoked usage access handling
      - VPN service start/stop race conditions
      - encrypted preference load/migrate behavior
  - Files:
    - `app/src/test/java/com/cyxwatch/app/...`
    - `app/src/androidTest/java/com/cyxwatch/app/ui/...`
  - Acceptance:
    - Tests fail before fix and pass after.
  - Status: `todo`

## Completion Criteria

- No payload capture or forwarding is enabled without explicit future security review.
- Consent flows and VPN mode are explicitly user-driven and recoverable.
- Sensitive local state is encrypted at rest.
- Unsafe external inputs cannot alter app state without validation.
- Security claims match implemented behavior at all documentation surfaces.
