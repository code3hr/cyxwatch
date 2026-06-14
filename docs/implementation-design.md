# CyxWatch Implementation Design

## Goals

- Local-first architecture.
- Evidence-first UX: every score and alert must point to data.
- Minimal battery and permission surface.

## Architecture at a Glance

Layered architecture with strict boundaries:

1. UI Layer
   - Compose screens and view models.
   - State driven by domain use cases.
2. Domain Layer
   - Scoring, normalization, alert rules, app risk policy.
   - Platform-independent contracts.
3. Data Layer
   - Local persistence (Room or simple SQLite wrapper).
   - Mappers and repositories.
4. Platform Layer
   - Usage access queries and permissions.
   - App package metadata.
   - Network stats + optional VPN transport.

## Core Components

### Data Models
- `PrivacyEvent`
  - `eventId`, `timestamp`, `packageName`, `eventType`, `severity`,
    `source`, `title`, `explanation`, `evidenceJson`
- `AppProfile`
  - app metadata, permissions, install/update timestamps, risk tags
- `PrivacyScore`
  - score value + list of reasons + generation timestamp
- `Alert`
  - ruleId, target app, triggered reason, evidence refs, timestamp

### Repositories
- `AppInventoryRepository`
  - `readInstalledAppProfiles()`
- `AppInventorySnapshotRepository`
  - `readSnapshot()`
  - `writeSnapshot(profiles)`
- `EventStoreRepository`
  - `insert(event)`
  - `queryTimeline(window)`
  - `queryByPackage(packageName)`
  - `pruneBefore(retentionDate)`
- `NetworkStatsRepository`
  - `collectDailyAppData()`
  - `collectVpnSnapshot()`

### Use Cases
- `CollectUsageEventsUseCase`
- `RefreshInstalledAppInventoryUseCase`
- `DetectAppInventoryChangesUseCase`
- `BuildInventoryChangeEventsUseCase`
- `Profile` presentation flow for `AppProfile` metadata + sensitive permission flags
- `CollectNetworkUsageUseCase`
- `CalculatePrivacyScoreUseCase`
- `BuildTimelineUseCase`
- `EvaluateAlertsUseCase`

## Event Flow

1. Collector gathers raw platform signal.
2. Normalizer maps raw signal to canonical `PrivacyEvent`.
3. EventStore persists events and enforces retention.
4. Scoring/alert engines consume events on schedule.
5. UI renders evidence-rich summaries.

## Work Scheduling

- `WorkManager` for periodic summaries and cleanup.
- Foreground service only for VPN mode where user explicitly enabled it.
- Recompute scores on:
  - foreground transitions
  - new network window completion
  - app permission changes

## Data Retention

- Default retention: 14 days.
- Settings should allow 7/14/30 days for V1.
- Hard cap cleanup job runs daily at app start and scheduled intervals.

## Scoring Design

- Deterministic weighted rule set.
- Each decrement has:
  - rule ID
  - delta value
  - explicit reason string
- Scoring output includes all active triggers for explainability.

### Rule Examples
- Background network volume above threshold.
- Active while screen off.
- Sensitive permission set appears atypical for app role.
- Permission set changed after update.

## Alert Design

- Alert rules execute on event windows, not single micro-events.
- Throttle and dedupe alerts:
  - one alert per app per rule per hour.
  - suppress duplicates when evidence has not changed.
- Always include a direct path to timeline context.

## Privacy and Security Controls

- No cloud sync for event store.
- Evidence JSON stores only app identifiers, metrics, and event context.
- No packet payload capture in V1.
- Explicit opt-in and visible state for VPN mode.
- Settings page includes retention and data delete controls.

## Testing Design

- Unit tests:
  - normalization
  - score rules
  - retention cutoff
  - alert thresholds
- Instrumentation:
  - usage permission request flow
  - VPN opt-in and stop path
  - onboarding recovery from denied permissions
- Manual QA:
  - battery behavior with normal use
  - permission edge cases
  - notification noise and timing
