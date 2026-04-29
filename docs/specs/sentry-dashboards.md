# Sentry dashboards spec

This spec captures the operational dashboard setup for `plan-016-sentry-dashboards`.
The goal is to make release health and workout-critical failures visible in one place.

## Dashboard

- Name: `Floating HR Training - Runtime Health`
- Time range default: last 24 hours
- Environment filter default: `internal`, `production` (exclude local dev noise)
- Project filter: `floatig-hr-training-android`

## Widgets

### 1) Crash-free sessions

- Widget type: Crash Free Sessions
- Query:
  - `event.type:error`
  - `!error.type:CanceledException`
- Display:
  - trend line + current percentage
  - split by `environment`

### 2) Foreground service errors

- Widget type: Discover timeseries
- Query:
  - `event.type:error tags.component:foreground-service`
- Y-axis:
  - count of errors
- Breakdown:
  - by `error.type`

### 3) Overlay permission failures

- Widget type: Discover table
- Query:
  - `event.type:error tags.component:overlay-permission`
- Columns:
  - `timestamp`, `title`, `error.type`, `release`, `environment`

### 4) BLE connection errors

- Widget type: Discover timeseries
- Query:
  - `event.type:error tags.component:ble`
- Y-axis:
  - count of errors
- Breakdown:
  - by `tags.ble_stage` (`scan`, `connect`, `disconnect`)

### 5) App start latency

- Widget type: Transaction duration
- Query:
  - `event.type:transaction transaction:app.start`
- Aggregation:
  - p50, p95 duration (ms)

### 6) Workout start latency

- Widget type: Transaction duration
- Query:
  - `event.type:transaction transaction:workout.start`
- Aggregation:
  - p50, p95 duration (ms)

## Alert thresholds

- Crash-free sessions:
  - warn when `< 99.5%` over 24h
  - critical when `< 99.0%` over 24h
- Foreground service errors:
  - warn when `>= 3` per hour
  - critical when `>= 10` per hour
- Overlay permission failures:
  - warn when `>= 5` per day
  - critical when `>= 15` per day
- BLE connection errors:
  - warn when `>= 5` per hour
  - critical when `>= 15` per hour
- App start p95:
  - warn when `> 2500ms`
  - critical when `> 4000ms`
- Workout start p95:
  - warn when `> 3000ms`
  - critical when `> 5000ms`

## Validation flow

1. Confirm telemetry exists in Sentry for each source:
   - `foreground-service`
   - `overlay-permission`
   - `ble`
   - `app.start` transaction
   - `workout.start` transaction
2. Build dashboard widgets with saved queries and keep names aligned with this spec.
3. Verify each widget shows non-empty data for `internal` in the last 24h.
4. Trigger controlled test events from debug builds to validate filters:
   - force BLE scan/connect failure path
   - deny overlay permission and attempt overlay start
   - start/stop workout from cold app start
5. Confirm threshold values in alert rules match this document.
6. Run weekly spot-check:
   - ensure new app release still reports `release` and `environment`
   - ensure widgets remain query-valid after SDK or schema changes
