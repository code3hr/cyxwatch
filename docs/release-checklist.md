# CyxWatch MVP Release Checklist

## Pre-release Verification

- [x] Usage Access onboarding path works for grant, deny, and re-enable.
- [x] App inventory loads and updates on install/update (`RefreshInstalledAppInventoryUseCase` exercised in unit/instrumented paths).
- [x] Timeline renders with at least 24h usage data.
- [x] Privacy score visible with reasons.
- [x] Event retention works and manual delete is immediate.

## Security and Privacy

- [x] No network calls for telemetry, uploads, or analytics in code paths.
- [x] VPN mode shows explicit active state and requires user action.
- [x] No payload capture in V1.
- [x] Onboarding and privacy settings explicitly state local-only boundaries:
  - no packet payload collection
  - no cloud upload/sync
  - no on-device payload forwarding
  - VPN visibility is monitoring only, not anonymity tunneling.
- [x] Optional secure-screen mode is available for sensitive screens.

## Testing

- [x] Unit tests cover scoring rules and thresholds.
- [x] Alert throttling behavior covered by unit tests.
- [x] Permission denial and permission recovery flow tested (`UsageAccessScreen` covered).
- [x] Core UI tests cover summary and usage-access states.
- [x] Core UI tests cover profile, score evidence, inventory evidence, and transparency settings accessibility actions.
- [x] Repeated manual collection calls are rate-limited (`CollectionThrottle`, `CollectionThrottleTest`) to reduce battery impact.
- [x] Accessibility labels added to primary actions and evidence-navigation controls.
- [x] VPN opt-in visibility controls are persisted and visible in dashboard/settings.
- [x] VPN opt-in flow now routes through `VpnService.prepare(...)` and starts/stops an actual `VpnService` component on user confirmation.
- [x] VPN service lifecycle updates persisted mode state on stop/revoke to prevent stale enabled-state artifacts.

## Performance and UX

- [x] No visible hidden polling in normal mode.
- [x] Manual collection actions are throttled (8-second minimum interval per action) to avoid accidental rapid re-run overhead.
- [ ] Alert noise not higher than expected threshold for normal usage.
- [x] Accessibility and readability checks completed for core screens (content descriptions + instrumented assertions).

## Release Artifacts

- [x] `Privacy policy` / transparency note exists.
- [ ] Screenshot and short summary text prepared.
- [ ] Final changelog entry prepared.
- [x] GitHub release workflow added for CI lint/test/assemble and tagged release upload.

## CYX-802 Release Readiness Notes

- Added `CollectionThrottle` and wired it into manual collection actions in the dashboard to prevent accidental rapid collection loops.
- Added `CollectionThrottleTest` verifying allow/block windows and per-action isolation.
- Release build command now documented in `docs/build-process.md` for full validation including lint and assemble.
- Remaining manual checks:
  - Alert noise profile review with realistic data.
  - Final usability checks (`TalkBack` smoke run, text scale/large font checks).
