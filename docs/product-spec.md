# Floating HR Training product spec

## Product goal

Floating HR Training is an Android running companion for heart-rate-zone
training. It combines a full-screen workout UI with a floating overlay so the
runner can stay inside a target zone while using maps, music, or other apps.

The differentiator is predictive pacing guidance: the app should warn before
heart rate overshoots a zone, not only after it happens.

## Target users

- Runners who train by heart-rate zones
- Users wearing a BLE heart-rate strap such as Polar H10
- Users doing structured interval workouts with repeated target-zone segments

## Platform assumptions

- Android phone, min SDK 26
- Standard BLE Heart Rate Service device
- Primary reference belt: Polar H10
- Cadence may later come from phone sensors, foot pod, or another BLE device

## Current product state

Implemented today:

- Compose app shell with Training, Verlauf, and Einstellungen areas
- Training list/editor/history/settings prototype screens
- Active workout screen with simulated BPM/cadence telemetry
- Predictive pacing engine with unit tests
- Foreground monitoring service scaffold
- Floating overlay service with draggable zone bar, glow, tone, and vibration
- Basic Sentry SDK integration and training-service telemetry helper

Not implemented yet:

- End-to-end BLE scan/connect state in the UI
- Live BPM flow from BLE into workout UI, foreground service, and overlay
- Real cadence source
- Persistence for settings, plans, or workout history
- Fully editable and persistent training editor
- CI distribution / release workflow

## Product requirements

### Core experience

- Let the runner choose a structured workout and start it quickly
- Show current segment, target zone, BPM, cadence, and pacing guidance
- Support using the app full-screen or as a floating overlay
- Keep alerts glanceable and simple during motion

### Predictive pacing

- Use recent BPM and cadence samples to estimate near-future heart-rate trend
- Emit one of `SpeedUp`, `Maintain`, `SlowDownSoon`, `SlowDownNow`
- Prefer early warnings over late overshoot correction

### BLE heart-rate support

- Scan for nearby heart-rate straps
- Show device name and RSSI
- Connect and receive standard Heart Rate Measurement notifications
- Recover from disconnects, especially for Polar H10

### Data

- Persist lightweight user settings in DataStore
- Persist workouts, plans, and learned telemetry in Room

## Scope boundaries

Current MVP is not intended for:

- Medical diagnosis or medical-grade monitoring
- Cloud sync
- Social sharing
- Advanced coaching plans

The app should eventually include a clear training-only disclaimer.

## Document map

- Implemented screens: `docs/specs/screens.md`
- Implemented floating overlay: `docs/specs/floating-overlay.md`
- Implemented predictive pacing: `docs/specs/predictive-pacing.md`
- Pending implementation plan: `docs/plan.md`
- Additional pacing notes: `docs/pacing-prediction.md`
