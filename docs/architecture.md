# CyxWatch Architecture (V1)

## Scope

CyxWatch V1 focuses on Android app observability with local-first processing.
iOS is out of scope for deep monitoring and can be a future companion.

## Architecture Layers

### 1) Presentation
- Compose screens for dashboard, app profile, alerts, and settings.
- ViewModels coordinate state and expose immutable UI models.

### 2) Domain
- Rules and orchestrators:
  - scoring
  - alert policy
  - event normalization
  - retention rules
- No Android framework calls here.

### 3) Data
- Local store for events and app profiles.
- Repositories for query and mutation contracts.

### 4) Platform Integration
- Collectors for platform APIs:
  - Usage event queries
  - package manager metadata
  - network stats
  - optional VPN service

## Runtime Data Flow

1. Platform collectors generate raw signals.
2. Normalizer creates canonical `PrivacyEvent` objects.
3. Repository writes to local store and emits updates.
4. Scoring and alert use cases run on scheduled windows.
5. UI reads derived state and renders evidence and actions.

## Module Boundaries (Proposed Package Layout)

- `com.cyxwatch.app.ui`
- `com.cyxwatch.app.domain`
- `com.cyxwatch.app.data`
- `com.cyxwatch.app.platform`

## Key Decisions

- Use WorkManager for periodic summaries and cleanups.
- Use foreground service only in opt-in VPN mode.
- Store evidence JSON as compact structured text per event.
- Keep app inventory snapshots for permission diffing.

## API and Permission Model

- Usage Access: required for app timeline and screen context.
- VPN mode: explicit opt-in required and user-visible.
- Accessibility: targeted screen-reader and touch-target review is part of release readiness before freeze.
- Package visibility and network permissions handled via user-facing settings and manifest declaration.

## Persistence and Retention

- Default retention: 14 days.
- Manual controls for longer windows with explicit note on storage and risk.
- Daily prune job ensures bounded store growth.

## Failure Modes

- Missing permission: degrade gracefully and show explicit steps.
- VPN unavailable: fallback to basic network mode.
- Incomplete event windows: mark incomplete and retry with bounded backoff.

## Security and Privacy Requirements

- No telemetry upload by default.
- No packet payload collection in V1.
- Consent and consent-state must be represented in onboarding and settings.
- Local data deletion and retention controls required by design.

## Relation to Existing Planning Documents

This architecture aligns with:
- `README.md` for product direction and MVP scope.
- `docs/product-architecture.md` for platform constraints and first milestone.
