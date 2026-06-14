# CyxWatch Big Picture

## Purpose

CyxWatch is an Android-first, local-first privacy observability app.
The product goal is evidence-driven scoring and alerts, not spyware detection claims.

## What exists today

- Usage Access onboarding screen and state persistence.
- Guarded permission checks for usage access.
- Kotlin domain scoring primitives (PrivacyEvent, PrivacyScore, ScoringRule) with unit tests.
- Compose UI shell with dashboard placeholder and usage-access UX states.

## Architecture at a glance

- app module with clear package layers:
  - com.cyxwatch.app (app shell)
  - com.cyxwatch.app.domain (scoring, models, deterministic rules)
  - com.cyxwatch.app.data (persistence and state repositories)
  - com.cyxwatch.app.platform (platform access adapters)
  - com.cyxwatch.app.ui (Compose UI)
- Long-running and monitoring behavior is planned in separate collectors, repositories,
  and use cases (see implementation design).

## Current milestone focus

- Milestone 1 onboarding + app inventory is active.
- CYX-201 app inventory reader is implemented.
- CYX-202 cross-session install and permission-delta detection is implemented with evidence-event mapping.
- CYX-203 app profile-to-evidence flow is implemented, with clickable sensitive-permission chips opening timeline evidence screens.
- CYX-301 usage timeline work is implemented: foreground/background transitions now flow from `UsageStatsManager` into usage collection status/timeline output.
- CYX-302 screen/lock-state capture is implemented in usage-event normalization (`SCREEN_STATE` with screen on/off + keyguard hints).
- CYX-303 collector hardening is implemented with dedupe and short-gap backfill helpers in usage normalization.
- CYX-401 basic network mode is implemented with 24h app-level network summaries from `NetworkStatsManager` and persisted daily totals.
- CYX-501 scoring rule expansion is implemented with source-aware rules and evidence-linked score reasons.
- CYX-502 scoring UI is implemented on the dashboard with evidence drill-in for score reasons.
- CYX-701 retention controls are implemented for the current in-memory evidence flow, with persisted 7/14/30 day settings and manual prune/delete actions.
- CYX-702 transparency settings are implemented with local-only status, Usage Access consent state, retention controls, and loaded evidence counts.
- Backlog and sequencing are tracked in docs/backlog.md.

## Documentation index

- docs/product-architecture.md - platform constraints and module intent.
- docs/architecture.md - V1 runtime boundaries and flow.
- docs/implementation-plan.md - phased roadmap and milestones.
- docs/implementation-design.md - component design and data flow.
- docs/user-cases.md - user-level scenarios and acceptance criteria.
- docs/build-process.md - local build commands, env setup, and cleanup steps.

## Design principles to keep

- Local-first processing only; no cloud telemetry.
- Explain every score/alert with evidence.
- Optional advanced network visibility via explicit VPN consent only.
- Explicit user controls for retention and data deletion.
