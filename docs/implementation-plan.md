# CyxWatch Implementation Plan (MVP)

## Vision

Build an Android-first local-first privacy observability app that helps users understand app behavior through evidence, not fear.

## 12-Week Target Plan

### 1) Foundation (Week 1)
- Initialize Gradle Android app with Kotlin + Compose.
- Add shared CI-ready module boundaries and dependency setup.
- Add core domain models for raw and normalized events.
- Add app entry architecture and baseline telemetry policy (no collection, no upload).
- Add unit test harness for deterministic domain logic.

### 2) Permission and Onboarding Baseline (Week 2)
- Implement Usage Access onboarding flow.
- Persist explicit consent and denial states.
- Gate all usage APIs behind permission status checks.
- Show onboarding copy and recovery path if permission is revoked.

### 3) App Inventory (Weeks 3-4)
- Add installed-app inventory reader and permission declaration extractor.
- Add repository with local caching and update diffing.
- Detect permission deltas on app upgrade and first install.
- Create app profile model with risk flags for known sensitive permissions.

### 4) Usage Collection (Weeks 4-5)
- Add `UsageEventCollector` to capture:
  - foreground app transitions
  - screen on/off and keyguard changes where available
- Normalize usage into `PrivacyEvent` records with concise evidence.
- Add deduplication and backfill for missed windows.

### 5) Network Visibility (Weeks 6-7)
- Add basic network mode using `NetworkStatsManager` app-level usage buckets.
- Add opt-in advanced mode via local `VpnService`.
- Keep advanced mode byte-count and endpoint metadata only.
- Add clear mode indicator and consent screens for users.

### 6) Scoring and Reasoning (Weeks 8-9)
- Implement deterministic rule-based `PrivacyScoreCalculator`.
- Add per-event evidence mapping to human-readable reasons.
- Add per-app and global score dashboards.
- Add threshold config and confidence levels.

### 7) Alerts and Summaries (Weeks 10-11)
- Implement low-noise alert engine with throttling.
- Add daily summary notification workflow.
- Add alert reason details with evidence links.

### 8) Hardening and Release Prep (Week 12)
- Battery impact checks and permission edge-case test pass.
- Add instrumentation tests for onboarding, VPN consent, and permission flow.
- Accessibility and localization pass.
- Prepare MVP release checklist and privacy transparency text.

### 9) UX and Permission Warning Polish (Week 13)
- Improve report readability and scrolling discoverability for long content.
- Show readable app and permission identity in sensitive-permission findings.
- Add unauthorized-permission warning copy and a short notification/action path.
- Build monitor-style reporting layout (status tiles, severity tags, quick action rows).
- Add direct actions from warning cards to score evidence and app profile.
- Add local notification feedback for unauthorized-sensitive permission changes (deduplicated by cooldown).
- Add explicit monitor copy for VPN limitations and consent boundaries inside report views.
- Capture screenshot evidence for report and evidence screen polish before launch.

## Quality Gates

- Every behavior change includes unit tests.
- Any score or alert algorithm change includes scenario tests.
- No change ships without deterministic evidence text.
- No feature stores or uploads user data.

## Milestone Exit Criteria

- Milestone 1 complete:
  - Usage Access flow works end-to-end.
  - Inventory + last 24h usage events load.
  - Local storage stores normalized evidence events.
  - Dashboard renders timeline and top risky apps.
- MVP complete:
  - Local-first privacy score is explainable by event evidence.
  - At least one alert type and one daily summary are usable.
  - Network basic mode is functional; VPN mode remains opt-in and safe.

## Current Execution Mapping

- For actionable, status-tracked stories, use [docs/backlog.md](backlog.md).
