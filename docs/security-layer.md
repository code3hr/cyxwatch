# Security Layer (CyxWatch)

## Purpose

CyxWatch is an on-device observability tool, not a spying or interception product.  
Its security layer must ensure:

- It cannot read packet payloads.
- It cannot silently alter traffic.
- It cannot exfiltrate user device data.
- It only processes sensitive signals with explicit user consent.

## Design Principles

1. Local-first by default  
   All analysis and persistence happen on-device. There is no sync API or backend endpoint in the app.

2. Transparent controls  
   Sensitive permissions are surfaced in onboarding and settings with clear copy about what each grant enables.

3. Least privilege  
   Permissions are limited to what is required for defined functionality:
   - `PACKAGE_USAGE_STATS`
   - `ACCESS_NETWORK_STATE`
   - `FOREGROUND_SERVICE`
   - `POST_NOTIFICATIONS`

4. No payload interception  
   Advanced network mode only captures packet headers/metadata in memory for local counting and trend visibility.

5. Bounded lifetime of advanced telemetry  
   Live VPN traffic metadata is kept in-memory (`VpnModeTrafficStore`) and cleared when VPN stops or app process cycles traffic capture.

## Current Security Controls in the Repository

- Manifest hardening:
  - `android:allowBackup="false"` prevents Android backup extraction.
  - `android:usesCleartextTraffic="false"` blocks cleartext network transport.
  - VPN service is `android:exported="false"` and gated with `android.permission.BIND_VPN_SERVICE`.
  - No `android.permission.INTERNET` permission is requested in the app manifest.

- Consent and gating:
  - Usage queries are routed through `UsageAccessPermissionStateProvider`.
  - Collectors check permission before querying platform sources.
  - VPN mode is explicit opt-in through `CyxWatchVpnService.prepare` flow.

- Service safety:
  - `CyxWatchVpnService` does not forward payloads in V1 (`NoopVpnModePacketForwarder`).
  - `VpnModeCapabilities.FORWARDING_MODE_SUPPORTED = false` prevents packet relay.
  - Packet capture loop is in a foreground service with lifecycle stop hooks (`onDestroy`, `onRevoke`).

- Data handling:
  - Evidence and local state are stored in app-private `SharedPreferences`.
  - No cloud uploads are implemented.
  - No external analytics SDKs are configured in the code base.

## Risk Register (Current)

| Risk | Current status | Mitigation status |
| --- | --- | --- |
| Payload theft through VPN mode | Reduced | Not captured in V1; no forwarder. |
| Hidden traffic manipulation | Reduced | No forwarding path active; service controls toggles and default noop forwarder. |
| Untrusted state tampering | Reduced | Sensitive preference-backed state now uses `EncryptedSharedPreferences` (with backward-compatible migration from plaintext). |
| Crash-driven DoS from collection loops | Medium | Collector reads are guarded by bounds checks and permission checks; service clears state on stop. |
| Abuse via exported components | Low | No exported service/activity beyond launcher activity. |

## Hardening Backlog

Keep this list as the security layer completion set:

- Replace plain `SharedPreferences` with `EncryptedSharedPreferences` for sensitive state (consent, retention, and inventory baselines). (implemented)
- Add tamper-resistant intent handling:
  - Validate external `Intent` extras in `MainActivity` before use.
- Add explicit screen protection toggle:
  - Optional `FLAG_SECURE` for sensitive report screens on devices with screen-share risk.
- Add runtime integrity checks:
  - Detect disabled debug mode / known debugging flags in release builds (where appropriate for threat profile).
- Improve service integrity:
  - Ensure VPN service always calls `stopForeground(...)` and full cleanup on all early failure paths.
- Add a privacy impact section in release notes and onboarding copy:
  - “No payload collection,” “no cloud upload,” “local data only,” “VPN does not provide anonymity.”
- Add negative tests for permission denial and revoked-permission recovery in crash/recovery paths.

## Non-goals for this Layer

- CyxWatch is not a tunnel, not a firewall, not a remote security scanner.
- It should not perform packet modification, TLS interception, or credential capture.

## Evidence Mapping for Trust

When presenting CyxWatch to users, this mapping should be visible in documentation:

- Advanced visibility: `NetworkStatsManager` + optional VPN header metadata
- What is not done: packet content capture, cloud upload, forwarding
- What is done: local-only scoring/evidence and clear explanations tied to on-device data
