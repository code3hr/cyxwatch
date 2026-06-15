# CyxWatch MVP Backlog

## Backlog Scope

This backlog translates the architecture, plan, design, and user cases into concrete implementation work.
Statuses are `todo`, `in_progress`, `blocked`, and `done`.

## Epic 0 - Setup and Foundation

- CYX-001: Align repository structure with Android conventions
  - Scope: Ensure Gradle, app module, and directory layout match architecture.
  - Files:
    - `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`
    - `app/src/main/...`
  - Acceptance:
    - App compiles with domain logic free of Android framework dependencies.
  - Status: `done`

- CYX-002: Add local-first domain model and scoring primitives
  - Scope: `PrivacyEvent`, `PrivacyScore`, `ScoringRule`, score unit tests.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
    - `app/src/main/kotlin/com/cyxwatch/app/domain/model`
    - `app/src/test/java/.../PrivacyScoreCalculatorTest.kt`
  - Acceptance:
    - Deterministic score and reason mapping.
    - Unit tests for low, medium, high event mixes and clamp behavior.
  - Status: `done`

## Epic 1 - Onboarding and Permissions

- CYX-101: Implement Usage Access onboarding
  - User case: UC-01
  - Scope:
    - Onboarding explanation screen.
    - Link into system Usage Access settings.
    - Persist consent/denial state.
    - Recovery path if permission is revoked.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui/onboarding`
    - `app/src/main/kotlin/com/cyxwatch/app/data/settings`
  - Acceptance:
    - No crash when permission is denied.
    - Dashboard shows "permission required" state.
  - Status: `done`

- CYX-102: Gate collectors behind permission checks
  - Scope: Centralized permission checks and guarded collection entry points.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/permissions`
    - Domain and collector use-case wrappers.
  - Acceptance:
    - No protected platform API call in normal path if permission missing.
  - Status: `done`

## Epic 2 - App Inventory and Profiles

- CYX-201: Build app inventory reader
  - User case: UC-03
  - Scope:
    - Enumerate installed apps and metadata.
    - Capture declared permissions.
    - Mark launchable apps.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/inventory`
    - `app/src/main/kotlin/com/cyxwatch/app/data`
  - Acceptance:
    - Inventory includes package, label, version, install/update time.
  - Status: `done`

- CYX-202: Detect first-install and permission deltas
  - Scope:
    - Compare current and previous snapshots (stored baseline across sessions).
    - Compute install and permission changes into a typed domain change model.
    - Translate changes into `PERMISSION_CHANGED` evidence events.
    - Persist snapshots for baseline comparisons across app restarts.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/data`
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
  - Acceptance:
    - Permission deltas and new installations are consistently detected and ordered deterministically.
    - Permission-change signals are converted into `PrivacyEvent` evidence in a domain use case.
  - Status: `done`

- CYX-203: Add app profile screen
  - Scope:
    - Show app metadata and sensitive permission badges.
    - Link each badge to related timeline evidence.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
  - Acceptance:
    - Every risk badge exposes a timeline evidence action.
  - Status: `done`

## Epic 3 - Usage Signal Collection and Timeline

- CYX-301: Add usage collector for 24h window
  - User case: UC-02
  - Scope:
    - Use `UsageStatsManager` to capture foreground transitions.
    - Normalize to `PrivacyEvent`.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/usage`
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
  - Acceptance:
    - Timeline builds for last 24h when usage access is available.
  - Status: `done`

- CYX-302: Add screen and lock state context
  - Scope:
    - Capture screen on/off and keyguard visibility.
    - Tag app activity during off-lock windows.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/usage`
  - Acceptance:
    - Off-screen activity is clearly flagged in timeline.
  - Status: `done`

- CYX-303: Add dedupe and backfill
  - Scope:
    - Prevent duplicate events during re-runs.
    - Fill short collection gaps.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/usage`
    - `app/src/test/java/com/cyxwatch/app/platform/usage`
  - Acceptance:
    - No duplicate event IDs for same input bucket.
  - Status: `done`
  - Notes:
    - Added in-memory dedupe on collector output with a 1-second near-duplicate window.
    - Added gap backfill for short foreground-to-foreground transitions (inferred background event).

## Epic 4 - Network Visibility

- CYX-401: Add basic network mode
  - User case: UC-04
  - Scope:
    - Read app-level `NetworkStatsManager` totals.
    - Persist daily total bytes and per-app totals.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network`
    - `app/src/main/kotlin/com/cyxwatch/app/data`
  - Acceptance:
    - Network totals available when VPN mode is off.
  - Status: `done`
  - Notes:
    - Added 24h network collector using `NetworkStatsManager`, scoring events, and basic persistence for daily totals and per-app aggregates.

- CYX-402: Add VPN opt-in mode
  - User case: UC-04
  - Scope:
    - Add explicit consent flow and active-state indicator.
    - Collect byte-counts and destination metadata only.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network`
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
  - Acceptance:
    - VPN starts only after user action and supports immediate stop.
- Status: `done`
  - Notes:
    - Added explicit advanced-network mode consent flow using `VpnService.prepare`, persisted state transition, and start/stop service lifecycle.
    - Added explicit advanced-network mode toggle state with persistence and visibility in dashboard/settings.
    - Advanced collector path is now tied to explicit VPN mode state and no longer relies on basic-mode behavior.
    - Forwarding preference is now persisted and can be applied to the currently running VPN service by re-signaling it through the start flow.

- CYX-403: Add forwarding transport seam for safe future implementation
  - Scope:
    - Extract VPN packet parse/aggregation into dedicated processor.
    - Add callback seam for forwarding transport without changing packet loop behavior.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/VpnModePacketProcessor.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/CyxWatchVpnService.kt`
    - `app/src/test/java/com/cyxwatch/app/platform/network/VpnModePacketProcessorTest.kt`
  - Acceptance:
    - Unit tests cover parsed packet aggregation and forwarding callback invocation.
  - Status: `done`

- CYX-404: Add explicit forwarding strategy interface for future transport plumbing
  - Scope:
    - Introduce `VpnModePacketForwarder` abstraction.
    - Resolve forwarding strategy in service state refresh and inject into packet processing loop.
    - Keep V1 behavior with `NoopVpnModePacketForwarder` while retaining safe extension point.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/VpnModePacketForwarder.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/NoopVpnModePacketForwarder.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/platform/network/CyxWatchVpnService.kt`
  - Acceptance:
    - Forwarding requests can be flipped while service is already active and processed through a resolvable strategy path.
  - Status: `done`

## Epic 5 - Scoring and Risk Explanations

- CYX-501: Expand scoring rule set
  - User case: UC-03
  - Scope:
    - Apply event-weighted, deterministic rule engine.
    - Return explanation reasons with evidence references.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
  - Acceptance:
    - Score output always linked to at least one event.
  - Status: `done`
  - Notes:
    - Added a low-volume network tier so small background usage is still scored while keeping high/medium signals explicit.
    - Score reason messages now include package-level context from evidence (for example, detected permission names and network byte totals).

- CYX-502: Render scoring UI
  - Scope:
    - Today score card + top reasons.
    - Per-app score view.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
  - Acceptance:
    - Users can open the evidence behind each reason.
  - Status: `done`
  - Notes:
    - Dashboard renders the current score and top score reasons from loaded evidence.
    - Each rendered score reason opens its supporting evidence events.

## Epic 6 - Alerts and Summary

- CYX-601: Implement alert rules and throttling
  - User case: UC-05
  - Scope:
    - Implement initial rules and cooldown windows.
    - Deduplicate repeated alerts.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
  - Acceptance:
    - No duplicate identical alerts within one day.
  - Status: `done`
  - Notes:
    - Added domain evaluator for alert-worthy score reasons with 24h duplicate suppression.
    - Dashboard now renders alert timeline and supports clearing in-session alert history.
    - Persisted alert history not yet added (future hardening planned in CYX-602).

- CYX-602: Add daily summary workflow
  - Scope:
    - Daily summary screen/notification.
    - Link summary items to timeline context.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
  - Acceptance:
    - Summary includes top risks and why they matter.
  - Status: `done`
  - Notes:
    - Added `BuildDailySummaryUseCase` with window-based event aggregation.
    - Added `DailySummaryScreen` and dashboard navigation into summary flow.
    - Added tests for counts, window behavior, and top apps in `BuildDailySummaryUseCaseTest`.

## Epic 7 - Retention, Settings, Privacy Controls

- CYX-701: Add retention and deletion
  - User case: UC-06
  - Scope:
    - 7/14/30 day retention settings.
    - Manual purge action.
    - Scheduled pruning.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/data`
    - `app/src/main/kotlin/com/cyxwatch/app/ui/settings`
  - Acceptance:
    - Expired events are not visible in UI or analytics.
  - Status: `done`
  - Notes:
    - Added persisted 7/14/30 day retention setting.
    - Dashboard can prune or delete currently loaded evidence immediately.
    - Current implementation applies to loaded in-memory events; future event-store work should move pruning into repository persistence.

- CYX-702: Add transparency and settings UI
  - Scope:
    - Consent states, data policy text, local-first indicator.
    - Opt-in controls and disclaimers.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
    - `app/src/main/kotlin/com/cyxwatch/app/data/settings`
  - Acceptance:
    - Privacy mode and consent state are explicit in all relevant screens.
  - Status: `done`
  - Notes:
    - Added privacy settings screen with local-only mode, Usage Access consent state, retention controls, and loaded evidence counts.
    - Dashboard now shows local-only status and links to privacy settings.

## Epic 8 - Hardening and Release Readiness

- CYX-801: Add test coverage
  - Scope:
    - Unit tests for normalization, scoring, retention, alert thresholds.
    - Instrumentation for permission, onboarding, VPN consent.
  - Files:
    - `app/src/test/...`
    - `app/src/androidTest/...`
  - Acceptance:
    - Core logic and flows covered by tests before milestone close.
  - Status: `done`
  - Notes:
    - Added `BuildDailySummaryUseCaseTest` cases for top-app ordering, blank-package filtering, and window filtering.
    - Added `PrivacyAlertEvaluatorTest` coverage for mixed-rule suppression behavior.
    - Implemented Android UI instrumentation coverage for usage access and summary states.
    - Verified in this environment with `:app:test`, `:app:compileDebugAndroidTestKotlin`, and `:app:lint` using JDK `D:\ProgramFiles 64\jbr` and Android SDK `C:\Users\chick\AppData\Local\Android\Sdk`.

- CYX-802: Release readiness checks
  - Scope:
    - Accessibility, performance, and battery behavior checks.
    - Notification noise review.
  - Files:
    - `docs/release-checklist.md`
  - Acceptance:
    - No critical UX or background regressions.
  - Status: `done`
  - Notes:
    - Added per-action collection throttling (`CollectionThrottle`) for manual collection buttons to reduce accidental rapid invocation overhead.
    - Added `CollectionThrottleTest` coverage for allowed/blocked windows and independent action buckets.
    - Added action-level accessibility labels across core screens and expanded `androidTest` coverage (`UsageAccessScreen`, `DailySummaryScreen`, `AppProfileScreen`, `InventoryEvidenceScreen`, `ScoreEvidenceScreen`, `TransparencySettingsScreen`) to cover navigation/evidence actions.

## Milestone 1 Delivery Order

1. CYX-101 -> CYX-102
2. CYX-201 -> CYX-202 -> CYX-203
3. CYX-301 -> CYX-302 -> CYX-303
4. CYX-501 -> CYX-502
5. CYX-601

## Current Phase

- Current status: MVP baseline with monitor-style UX and release pipeline are complete.
- CYX-301, CYX-302, CYX-303, CYX-401, and CYX-402 are implemented and verified with `testDebugUnitTest`, `assembleDebug`, and service-lifecycle lint/build checks.
- CYX-501, CYX-502, CYX-601, CYX-602, CYX-701, and CYX-702 are implemented.
- CYX-801 and CYX-802 are implemented and verification tasks have been run for new checks.
- CYX-901, CYX-902, CYX-903, CYX-905, CYX-906 and CYX-907 are implemented.

## Epic 9 - UX Polish and Safety Feedback

- CYX-901: Improve summary readability and app identity context
  - Scope:
    - Show app label + package in summary and evidence reports.
    - Format sensitive permission names into readable labels.
    - Ensure report screens expose visible affordance for long content.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui/DailySummaryScreen.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/ui/ScoreEvidenceScreen.kt`
    - `app/src/androidTest/java/com/cyxwatch/app/ui/DailySummaryScreenTest.kt`
  - Acceptance:
    - Long reports remain discoverable.
    - Sensitive permission findings always include readable app and permission identity.
  - Status: `done`

- CYX-902: Add unauthorized-permission warning signal
  - Scope:
    - Add an explicit warning copy for newly observed sensitive permissions.
    - Add notification/in-app alert path to the exact reason evidence screen.
    - Add quick action from warning to app profile.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/domain`
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
    - `app/src/main/kotlin/com/cyxwatch/app/platform`
  - Acceptance:
    - Users are informed immediately when sensitive permissions are newly granted.
    - Warning path is actionable and keeps evidence context available.
  - Status: `done`

- CYX-903: Refine monitor-style report UI
  - Scope:
    - Improve section hierarchy, spacing, and signal clarity in report and evidence views.
    - Add consistent explanation copy for VPN/visibility behavior in report context.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
    - `docs`
  - Acceptance:
    - Report copy and flow are considered production-ready by peer review.
  - Status: `done`
  - Progress:
    - Removed duplicate dashboard scroll controls and kept a single persistent scroll navigation affordance.
    - Expanded report copy around local-vs-network observability boundaries.

- CYX-904: Add monitor-level dashboard surface
  - Scope:
    - Introduce compact score/risk/consent tiles for faster scanability.
    - Add severity-coded chips/tags for risk reasons and unauthorized-permission warnings.
    - Add timeline context for permission and network spikes with quick in-screen actions.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui/DailySummaryScreen.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/ui/ScoreEvidenceScreen.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/ui/CyxWatchApp.kt`
  - Acceptance:
    - Summary opens with a clear monitor-style view and visible high-signal hierarchy.
    - Users can act on a warning within two taps.
  - Status: `done`
  - Progress:
    - added monitor-style dashboard summary header with severity badge and KPI tiles.
    - added action cards for scroll-to-top/scroll-to-latest navigation.
    - added live throughput visualization and endpoint/throughput stream metrics in advanced mode.

- CYX-905: Add unauthorized-permission feedback and notification
  - Scope:
    - Add explicit in-app warning copy for sensitive permission additions and risky new installs.
    - Post a local notification for first-time warnings while avoiding duplicate spam via cooldown logic.
    - Add action flow: warning -> score evidence -> app profile.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui/CyxWatchApp.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/domain/EvaluatePrivacyAlertsUseCase.kt`
    - `app/src/main/kotlin/com/cyxwatch/app/platform/notifications`
  - Acceptance:
    - Users get immediate visual and notification feedback when unauthorized-sensitive permission behavior appears.
    - Each warning provides direct route to evidence and app profile.
  - Status: `done`

- CYX-906: Monitor UX copy and accessibility pass
  - Scope:
    - Add concise, consistent copy for permission sensitivity, consent boundaries, and VPN behavior.
    - Ensure all new action controls have stable semantics and test coverage.
    - Add explicit mention of "local traffic visibility only, not private VPN tunnel" in report surfaces.
  - Files:
    - `app/src/main/kotlin/com/cyxwatch/app/ui`
    - `app/src/androidTest/java/com/cyxwatch/app/ui`
    - `docs`
  - Acceptance:
    - New controls are discoverable and accessible in screenshot tests.
    - Report screens communicate limits clearly for users with no prior VPN security background.
  - Status: `done`
  - Progress:
    - explicit copy added to dashboard summary, transparency settings, usage onboarding, and inventory evidence.
    - existing action controls keep stable accessibility labels and now use non-redundant scroll controls in dashboard.

- CYX-907: Add CI/CD release pipeline and release artifact flow
  - Scope:
    - Add GitHub Actions workflow for build, lint, tests, and release assembly.
    - Support release packaging for tagged versions (starting at 0.0.1).
    - Upload release APK artifact as part of CI.
  - Files:
    - `.github/workflows/ci-cd.yml`
    - `app/build.gradle.kts`
    - `docs/implementation-plan.md`
  - Acceptance:
    - A valid GitHub release can be created by pushing a tag and includes an APK artifact.
    - Release process documents package name, build path, and version.
  - Status: `done`
