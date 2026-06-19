# CyxWatch Cleanup Ledger

## Purpose

This document tracks cleanup candidates created or exposed during the professional UI/UX implementation phase.
It is not a feature backlog. It is for orphan code, duplicated UI patterns, temporary implementation shortcuts, stale docs, and verification gaps that should be resolved after the current UX pass stabilizes.

## Cleanup Rules

- Keep entries concrete and tied to files.
- Do not clean unrelated code while implementing a focused UX change unless the cleanup is required for correctness.
- Prefer deleting dead code over wrapping it in new abstractions.
- Promote repeated UI patterns into shared components only after at least two screens prove the same shape is needed.
- Close an entry only after code is removed, simplified, or covered by tests.

## Active Cleanup Candidates

### CLEAN-002: Review dashboard component boundaries

- Area: Main dashboard UI
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`
- Issue: `DashboardShell` is carrying more visual structure as monitor UX expands.
- Risk: The file can become hard to reason about as dashboard, alerts, live VPN telemetry, retention, and timelines grow together.
- Cleanup action: Score reason and alert panels are now private composables in `CyxWatchApp.kt`. Continue extracting only stable sections after dashboard UX is accepted; do not split into separate files until ownership is clear.
- Timing: After dashboard UX is accepted.
- Status: `open`

### CLEAN-003: Normalize signal and alert panel styling

- Area: Dashboard and report UI
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/DailySummaryScreen.kt`
- Issue: Dashboard and daily summary now both render permission/behavior signals, but styling is still duplicated.
- Risk: Permission warnings may become visually inconsistent.
- Cleanup action: Dashboard and daily summary now use private panel composables in their owning files. Compare both implementations after polish, then extract a shared panel only if the final states remain equivalent.
- Timing: After alert/report refresh is complete.
- Status: `open`

### CLEAN-004: Verify launch gate persistence and first-run behavior

- Area: Launch gate and settings persistence
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/data/settings/LaunchGateSettingsRepository.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/data/settings/LaunchGateSettingsState.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/LaunchGateScreen.kt`
- Issue: Launch gate is implemented, but full Gradle verification is blocked locally by the wrapper/cache issue.
- Risk: First-run persistence behavior needs build/test confirmation before release.
- Cleanup action: Run unit/instrumentation checks once Gradle works. Remove or adjust any unused launch-gate state fields after validation.
- Timing: Before next release tag.
- Status: `closed`

### CLEAN-007: Review settings component boundaries

- Area: Privacy settings UI
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/ui/TransparencySettingsScreen.kt`
- Issue: Settings now has several private card composables for network visibility, diagnostics, and retention.
- Risk: Parameter lists can grow if settings absorbs more runtime state without a clearer state model.
- Cleanup action: After settings UX is accepted, review whether a narrow `TransparencySettingsUiState` would simplify the screen without hiding behavior.
- Timing: After `CYX-912` is visually stable.
- Status: `open`

### CLEAN-008: Centralize permission classification

- Area: Sensitive-permission policy and UI labels
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/ui/AppProfileScreen.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/domain/PrivacyScoreCalculator.kt`
- Issue: `SENSITIVE_PERMISSIONS` policy is still duplicated between scoring and profile UI.
- Risk: Sensitive-permission taxonomies can diverge across behavior classification and profile indicators.
- Cleanup action: Move to a shared permission policy/formatting utility and replace local helper sets.
- Timing: After current monitor-style implementation lands and screenshots are approved.
- Resolution: Added `SensitivePermissionPolicy` and replaced both scorer/profile lookups with `SensitivePermissionPolicy.isSensitive(...)`.
- Status: `closed`

### CLEAN-005: Keep generated/reference assets out of app source

- Area: Workspace hygiene
- Files:
  - `.gitignore`
  - `Res/`
  - `docs/launch-gate-plan.md`
- Issue: `Res/` contains reference images for UX direction, not Android runtime resources.
- Risk: Large reference assets could accidentally enter app source or release artifacts.
- Cleanup action: Confirm `.gitignore` excludes `Res/`; document which reference images influenced implemented screens.
- Timing: Before commit.
- Status: `closed`
- Resolution: `.gitignore` now excludes `/res/`, `/Res/`, and `/.gradle-work/`, and these references remain in workspace-only docs/assets folders.

### CLEAN-006: Gradle wrapper/cache verification blocker

- Area: Build verification
- Files:
  - `gradlew.bat`
  - `scripts/build-cyxwatch.ps1`
  - `C:\\Users\\chick\\AppData\\Local\\Temp\\cyxwatch-gradle`
  - `C:\\Users\\chick\\AppData\\Local\\Temp\\cyxwatch-rootbuild`
  - `C:\\Users\\chick\\AppData\\Local\\Temp\\cyxwatch-android-home`
- Issue: Local Gradle wrapper execution was blocked by Windows cache/lock errors under restricted cache paths.
- Risk: UI changes could not be compiled or tested during implementation windows.
- Cleanup action:
  - Use a repo-scoped `GRADLE_USER_HOME` and `ANDROID_SDK_HOME`.
  - Added `-CleanBuildState` + `-StopJava` recovery switches to `build-cyxwatch.ps1` for quick reset and rebuild.
- Timing: Before release candidate verification.
- Resolution: `scripts/build-cyxwatch.ps1 -Mode ci -StopJava -CleanBuildState` now recovers from repeated lock contention.
- Status: `closed`

## Closed Cleanup Items

### CLEAN-001: Consolidate scroll navigation controls

- Area: UI reports, evidence screens, settings, and dashboard
- Files:
  - `app/src/main/kotlin/com/cyxwatch/app/ui/ScrollNavigationControls.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/DailySummaryScreen.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/ScoreEvidenceScreen.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/InventoryEvidenceScreen.kt`
  - `app/src/main/kotlin/com/cyxwatch/app/ui/TransparencySettingsScreen.kt`
- Resolution: Added shared `LazyListScrollNavigationControls` and `ScrollNavigationControls`; callers now pass only state and content descriptions.
- Status: `closed`
