# CyxWatch Product Architecture

## Core Principle

CyxWatch should be an observability tool, not a heavy security suite. The app earns trust by showing evidence users can understand while consuming as little battery, CPU, and memory as possible.

The central product loop is:

1. Observe app behavior.
2. Normalize it into simple local events.
3. Explain what happened in human language.
4. Alert only when a meaningful threshold is crossed.

## Platform Reality

Android is the only realistic first-class monitoring platform for the MVP.

Android supports useful but permission-gated observability:

- `UsageStatsManager` can query app usage and events, but most methods require `android.permission.PACKAGE_USAGE_STATS`, and the user must grant usage access in Settings.
- `VpnService` can observe routed IP packets for opt-in local VPN monitoring. Android requires first-run user action, allows only one active VPN at a time, and shows a system-managed VPN notification.
- Background execution is restricted. Apps should use scheduled work for periodic tasks and reserve long-running foreground services for user-visible monitoring modes.

iOS should be treated as a later companion app unless the product changes. A normal iOS app cannot deeply monitor other apps, inspect system-wide traffic, or report arbitrary background activity from other apps.

References:

- Android `UsageStatsManager`: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Android `VpnService`: https://developer.android.com/reference/android/net/VpnService
- Android background restrictions: https://developer.android.com/develop/background-work/background-tasks/bg-work-restrictions

## MVP Modules

### 1. App Inventory

Purpose: build the local list of installed apps and their declared permissions.

Data captured:

- Package name
- App label
- Version
- Install/update time where available
- Declared permissions
- Whether the app is launchable

Output:

- App profile screen
- Permission risk hints
- Baseline data for scoring

### 2. Usage Event Collector

Purpose: collect app activity and screen-state context without continuous polling.

Data captured:

- Foreground app transitions
- Screen interactive/non-interactive events
- Keyguard shown/hidden events where available
- Daily usage aggregates

Output:

- Timeline events
- "Active while screen was off" indicators
- Background activity summary

Important limitation: this does not prove what code ran inside another app. It gives observable usage and system event context.

### 3. Network Observer

Purpose: show network behavior in a privacy-preserving way.

V1 should support two levels:

- Basic mode: app-level data usage from Android network statistics where available.
- Advanced mode: opt-in local VPN service for domain/IP and byte-count visibility.

V1 should not store packet payloads. DNS/domain metadata and byte counts are enough for user-facing transparency.

### 4. Event Store

Purpose: keep a small, queryable local history.

Suggested event model:

```text
event_id
timestamp
package_name
event_type
severity
source
title
explanation
evidence_json
```

Retention should be short by default, such as 7 or 14 days, with user control for longer history.

### 5. Scoring Engine

Purpose: convert observed facts into an understandable privacy score.

V1 scoring should be deterministic, transparent, and local:

- Sensitive permission count
- Permission mismatch against app category
- Background network volume
- Repeated activity while screen is off
- New risky permission after update

Every score must include reasons. A score without evidence is not useful.

### 6. Alerts

Purpose: notify only on meaningful changes.

Initial alert rules:

- App sends unusually high background data.
- App has sensitive permissions uncommon for its category.
- App becomes active repeatedly while the screen is off.
- App permissions change after update.
- New app is installed with high-risk permission set.

Avoid noisy alerts. Daily summaries are often better than real-time interruption.

## User Experience

The app should open directly to the monitoring dashboard:

- Today summary
- Top risky apps
- Recent timeline
- Network activity
- Permission changes

Language should be evidence-based:

- Good: "This app used 42 MB of data while not opened today."
- Avoid: "This app is spying on you."

## Cross-Platform Strategy

The product can be cross-platform at the business and UX layer, but not at the low-level monitoring layer.

Recommended split:

- Android native app owns monitoring.
- Shared design language and copy can later be reused.
- iOS companion can provide privacy education, manual audits, breach alerts, VPN profile integration where allowed, and account/family reporting.

Do not start with Flutter for the Android monitor. The hard part is platform integration, not shared UI rendering.

## First Engineering Milestone

Create an Android Kotlin prototype that:

1. Shows onboarding for Usage Access.
2. Reads installed app inventory.
3. Reads usage events for the last 24 hours after permission is granted.
4. Stores normalized events locally.
5. Renders a simple timeline and per-app summary.

Success criteria:

- Works without root.
- No cloud dependency.
- No continuous background polling.
- Battery impact is negligible during normal mode.
- User can understand why each event appears.
