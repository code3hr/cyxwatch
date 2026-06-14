# CyxWatch User Cases

## User Profile

Primary user: privacy-aware phone owner who wants to know what apps do when not actively used.

## Use Cases

### UC-01: First-run onboarding
Goal: Get the app running in read-only mode and ask for safe required permissions.

Main flow:
1. User installs and opens CyxWatch.
2. App explains local-first operation and required system steps.
3. User grants Usage Access.
4. App loads available data and opens the dashboard.

Acceptance:
- No crash when permission is denied.
- Clear recovery path when user returns to settings and reopens app.

### UC-02: See today app activity timeline
Goal: Understand which apps were foregrounded and when.

Main flow:
1. Collector reads usage window (default: last 24h).
2. App normalizes and stores timeline events.
3. Dashboard shows today summary and timeline list.

Acceptance:
- Timeline entries include package label and source evidence.
- Repeated screen-off activity is visibly flagged.

### UC-03: Check app risk and permissions
Goal: Review app risk without over-alarming language.

Main flow:
1. User opens app profile.
2. App displays declared permissions and high-risk permission flags.
3. Score reasons explain why an app is considered risky.

Acceptance:
- Every risk label maps to one or more evidences.
- User can open the related timeline slice from each reason.

### UC-04: Review network behavior
Goal: Detect unexpected background traffic.

Main flow:
1. User enables basic network mode.
2. App shows per-app byte totals and top background sessions.
3. User optionally enables VPN mode for endpoint metadata and stronger signals.

Acceptance:
- VPN mode never starts silently.
- VPN mode shows active consent status and one-tap stop action.

### UC-05: Receive meaningful alerts
Goal: Avoid noisy notifications and surface only meaningful risk shifts.

Main flow:
1. Alert rules evaluate nightly summary windows.
2. App triggers alert only when rule threshold and context pass.
3. Notification links to timeline and detailed event evidence.

Acceptance:
- Minimum 1 day of suppression for repeated identical alerts.
- No alerts for routine app updates unless risk delta exists.

### UC-06: Manage data retention
Goal: Keep local history under user control.

Main flow:
1. User opens settings and sets retention window.
2. App prunes old events on schedule.
3. User can run manual delete immediately.

Acceptance:
- Retention applies predictably and confirms deleted ranges.

## Non-Functional Cases

- App must function with low memory devices.
- No hidden background polling in normal mode.
- Battery and privacy expectations remain explicit and user-visible.

## Future User Cases (Post-V1)
- Family activity review.
- Export/share summary for informed decision making.
- Optional cloud-backed policy templates.
