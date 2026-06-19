# Launch Gate ("Login") Screen Implementation Plan

## Clarification

The `Res/login.png` asset is treated as a **first-launch setup page**, not authentication.  
It should appear only on first run and never again after permissions are granted and setup is completed.

## Asset-driven scope (`Res/` folder)

This implementation takes the `Res/` visual pages as the next design baseline:

- `Res/login.png` -> First-launch setup / start gate
- `Res/dashboard.jpg` and `Res/cyxwatch_main.jpg` -> Main dashboard landing variants
- `Res/monitor1.jpg` and `Res/monitor2.jpg` -> Monitoring visual blocks (metrics cards + charts/streams)
- `Res/report.jpg` -> Report and evidence timeline polish
- `Res/alert.jpg` -> Alert/notification and warning UX patterns
- `Res/setting.png` -> Settings and privacy controls flow
- `Res/logo.jpg` -> Brand lockup/iconography for launch and top app header

Current state:

- CYX-910 (first-launch launch gate): **done**
- CYX-911 (dashboard/monitor visual refresh): **done**
- CYX-912 (settings/privacy controls visual refresh): **done**
- CYX-913 (alerts/report visual refresh): **done**

Execution rule:

- Use these assets as copy/layout references for the next UI sprint.
- Keep behavior and wording aligned to the implemented data model:
  - usage access onboarding,
  - privacy controls,
  - monitor dashboard,
  - inventory/profile + permissions evidence,
  - alerts and summary.

## Why we need this

- Current app behavior directly shows the Usage Access screen when permission is missing.
- We already have robust permission state plumbing and privacy copy.
- We need a single onboarding entrypoint that:
  - introduces the app,
  - gives clear actions: **Start monitoring** and **View privacy controls**,
  - then hands off to existing permission/dashboard flows.

## Target behavior

1. First install / first launch:
   - Show Launch Gate screen (on top of current stack).
   - CTA 1: `Start monitoring` (permission-intent path).
   - CTA 2: `View privacy controls` (opens transparency/settings flow).
2. Permission workflow:
   - If Usage Access is granted, proceed to normal app dashboard.
   - If denied, keep user in existing Usage Access recovery screen.
3. Post-setup:
   - Screen is not shown again unless explicit app reset/clear data.
4. If Usage Access is revoked later:
   - Keep existing recovery path (`UsageAccessScreen`) but do not show the full launch gate again.

## Implementation plan (MVP scope)

### Phase 1 — State model

- Add a lightweight launch-state repository:
  - `app/src/main/kotlin/com/cyxwatch/app/data/settings/LaunchGateSettingsState.kt` (data class)
  - `app/src/main/kotlin/com/cyxwatch/app/data/settings/LaunchGateSettingsRepository.kt`
- Persist:
  - `hasCompletedLaunchGate` (Boolean)
  - optional `hasSeenPrivacyControlsFromGate` (Boolean)
- Store in app-private prefs.
- Acceptance:
  - Default false on fresh install.
  - Set true only after user performs an explicit setup completion action.

### Phase 2 — New screen

- Add `app/src/main/kotlin/com/cyxwatch/app/ui/LaunchGateScreen.kt`
- Content:
  - App logo and headline.
  - Two clear actions with visible affordances:
    - Start monitoring
    - View privacy controls
  - Short privacy-first copy matching existing language:
    - local-only processing,
    - no payload collection,
    - no cloud upload,
    - VPN monitoring is not anonymous tunneling.
- Use existing design tokens from current dark monitor-style UI.
- Acceptance:
- `Start monitoring` opens Usage Access settings flow via existing manager.
- `View privacy controls` opens current settings or a dedicated read-only explainer block.

### Phase 3 — Navigation integration

- Update `CyxWatchApp` launch logic:
  - If `!launchGateState.hasCompletedLaunchGate`, render `LaunchGateScreen` first.
  - Otherwise preserve current flow (`UsageAccessScreen` if permission missing, dashboard otherwise).
- Add callback bridges:
  - `onStartMonitoringClick`
  - `onViewPrivacyControlsClick`
- On successful first successful launch action, mark launch gate complete in repo.
- Acceptance:
  - Launch gate only appears once before normal first-use flow.

### Phase 4 — Backwards compatibility path

- If existing users update from older versions:
  - treat `hasCompletedLaunchGate == false` as:
    - show usage access if missing,
    - otherwise bypass to dashboard.
- Ensure no hard block for users already in normal flow.

### Phase 5 — UX/tests hardening

- Add/update UI tests:
  - `LaunchGateScreen` shows two action buttons.
  - Start monitoring route leads to settings open intent flow.
  - Gate is not shown after completion.
- Add semantics labels for both CTAs.
- Add screenshot coverage for first-launch view and privacy controls action.
- Acceptance:
  - No regression to existing onboarding/instrumentation test cases.

## File-level implementation map

- `app/src/main/kotlin/com/cyxwatch/app/CyxWatchApp.kt`:
  - Gate selection logic + callback wiring.
- `app/src/main/kotlin/com/cyxwatch/app/ui/LaunchGateScreen.kt`:
  - New composable UI.
- `app/src/main/kotlin/com/cyxwatch/app/data/settings/LaunchGateSettingsRepository.kt`:
  - New repository/state persistence.
- `app/src/main/kotlin/com/cyxwatch/app/data/settings/...`:
  - Add/adjust shared state access.
- `app/src/androidTest/java/com/cyxwatch/app/ui`:
  - Add launch-gate tests.

## Milestone mapping

- Add to backlog as:
  - **CYX-910: Implement first-launch launch gate (non-auth onboarding).**
- Add to backlog as:
  - **CYX-911: Implement Res-driven monitor/report/dashboard refresh**
  - **CYX-912: Implement Res-driven settings/privacy controls refresh**
  - **CYX-913: Implement Res-driven alerts/reporting refresh**
- Dependencies:
  - Current usage-access gating,
  - Transparency settings screen,
  - Consent state and shared prefs patterns already in use.

## Open decisions

- When should launch gate be considered complete:
  - After permission grant + first dashboard render,
  - or after explicit “Start monitoring” + user has seen privacy controls?  
  Suggested default: complete on first successful monitoring intent action or immediate permission status positive (safer to reduce repeated prompts).
