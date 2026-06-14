# CyxWatch System Overview (Current V1 State)

CyxWatch is an Android-first local-first privacy observability app. It collects local device signals, converts them to evidence events, and renders risk-aware summaries without uploading user data.

## What is implemented

- App inventory and permission risk context.
- 24-hour usage timeline collection (with permission gating).
- 24-hour app-level network totals (basic mode).
- Opt-in VPN-mode visibility entry point.
- Scoring, alerts, summaries, and retention controls.
- Manual event collection flows with evidence drill-in.

## Core end-to-end flow

```text
Platform collectors
  -> Domain use cases + normalization
  -> Evidence events + repositories
  -> Scoring + alerts + summaries
  -> Compose screens
```

Primary boundaries:

- Platform
  - `AndroidUsageEventCollector`
  - `AndroidAppInventoryRepository`
  - `AndroidNetworkUsageCollector`
  - `CyxWatchVpnService` / `VpnPacketParser`
- Domain
  - `CollectUsageEventsUseCase`
  - `RefreshInstalledAppInventoryUseCase`
  - `CollectNetworkUsageEventsUseCase`
  - `PrivacyScoreCalculator`
  - `EvaluatePrivacyAlertsUseCase`
  - `BuildDailySummaryUseCase`
- Data/settings/repository
  - `UsageAccessConsentRepository`
  - `VpnModeSettingsRepository`
  - `RetentionSettingsRepository`
  - `SharedPrefsNetworkUsageTotalsRepository`
  - `SharedPrefsAppInventorySnapshotRepository`

## Network modes

### Basic mode

When VPN mode is off, network usage is gathered from `NetworkStatsManager` windows and normalized into
`Network usage` events through `AndroidNetworkUsageCollector`.

### Advanced mode (VPN visibility)

Advanced mode is opt-in and explicit:

1. User enables VPN mode from dashboard/settings.
2. App calls `VpnService.prepare(context)`.
3. On success, `CyxWatchVpnService.start(context)` starts the foreground service.
4. The service opens a local TUN interface and reads raw packet headers.
5. `VpnPacketParser` extracts only endpoint metadata:
   - destination IP
   - destination port (when present)
   - protocol label
   - packet length
6. `VpnModeTrafficStore` aggregates destination traffic in memory (bounded top-N snapshot).
7. `AndroidVpnModeNetworkUsageCollector` merges that snapshot into network event evidence as:
   - `collectionMode: "vpn"`
   - `trafficTotals` with `packetsObserved`, `bytesObserved`, `uniqueDestinations`,
     `parsedPackets`, `unparsedPackets`, `captureMode`, `forwardingEnabled`
   - `trafficDestinations` list
   - per-entry `{ bytes, packetCount, avgBytesPerPacket }`

Scope and limitations:

- This V1 service is metadata-only.
- It does not act as a private VPN/tunnel replacement or provide site-level privacy guarantees; it only provides local visibility signals for user audit.
- Packet payloads are not captured.
- Packet forwarding is intentionally not implemented yet (`VpnModeCapabilities.FORWARDING_MODE_SUPPORTED = false`), so this mode does not provide a full transparent tunnel path in this increment.
- The collector reports `captureMode` as `monitor-only` and `forwardingEnabled` as `false` while in this mode.
- A user preference for forwarding mode exists and is persisted. When VPN mode is active, toggling forwarding dispatches a service refresh so the in-memory diagnostics view can reflect the latest requested mode immediately. Forwarding remains disabled until a connectivity-safe transport pipeline is added.
- Service state is persisted in `VpnModeSettingsRepository` on start/stop/revoke.
- Packet parse + aggregate logic in `CyxWatchVpnService` is now isolated in `VpnModePacketProcessor`.
- A new `VpnModePacketForwarder` abstraction resolves packet forwarding strategy during service refresh calls, with `NoopVpnModePacketForwarder` as the V1 implementation.
- Forwarding strategy selection is refreshed on each service start signal so a changed preference can be reflected without restarting the worker thread.

## UI/UX control points

- Dashboard:
  - start/stop advanced network mode
  - collect usage/network inventory evidence
  - score/evidence drill-through
  - retention and deletion controls
- Transparency settings:
  - consent state
  - VPN enabled/disabled timestamps
  - retention window status
  - advanced visibility diagnostics (packet count/byte count/unique destinations)
  - forwarding mode and parsed vs unparsed packet counters
  - forwarding request state (currently not active by default)
  - loaded evidence counts and cleanup actions

## Safety model

- No cloud sync or telemetry upload in V1.
- No packet payload capture.
- Explicit VPN consent and visible state transitions.
- User-controlled retention and immediate loaded-evidence deletion.

## Current file map (advanced network path)

- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/cyxwatch/app/platform/network/CyxWatchVpnService.kt`
- `app/src/main/kotlin/com/cyxwatch/app/platform/network/VpnPacketParser.kt`
- `app/src/main/kotlin/com/cyxwatch/app/platform/network/VpnModeTrafficStore.kt`
- `app/src/main/kotlin/com/cyxwatch/app/platform/network/AndroidVpnModeNetworkUsageCollector.kt`

## Next implementation step

- Add a safe, defined VPN forwarding strategy that preserves connectivity while still capturing endpoint metadata.
