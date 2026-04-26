# Floating HR Training product spec

## Product goal

Build an Android running heart-rate training app that helps runners stay inside
a target heart-rate zone while using a Bluetooth heart-rate strap such as Polar
H10. The app should work both as a full-screen workout companion and as a
floating overlay while the runner uses other apps.

The key product differentiator is predictive pacing guidance: because heart
rate responds to cadence changes with a delay, alerts should not wait until BPM
has already crossed the target limit. The app records heart rate and cadence,
learns/estimates the response lag, and warns the runner at the right moment so
the final heart-rate response stays inside the target zone.

## Target users

- Runners training by heart-rate zones.
- Users who run with a Bluetooth heart-rate belt and may use maps, music, or
  another running app at the same time.
- Users doing structured interval training with repeated target-zone segments.

## Supported device assumptions

- Android phone, minimum SDK 26.
- BLE heart-rate strap supporting the standard Heart Rate Service.
- Primary reference device: Polar H10.
- Cadence source can later come from phone sensors, a foot pod, or another BLE
  device. Current implementation uses simulated cadence in the workout screen.

## Reference UI direction

The visual target follows the uploaded Strap Zone-style screenshots:

- Dark theme, black or near-black background.
- Rounded dark cards.
- Cyan/blue primary action color.
- Large BPM number in workout mode.
- German labels are used in the current reference UI.
- Bottom navigation:
  - `Training`
  - `Verlauf`
  - `Einstellungen`
- Zone colors:
  - Z1: gray
  - Z2: blue
  - Z3: green
  - Z4: orange/red
  - Z5: purple

Detailed reference notes are preserved in `docs/ui-reference/`.

## Core screens

### Training home

Purpose: choose a workout, connect the belt, and start training.

Required elements:

- App title: `Strap Zone`.
- Training selection card.
- BLE strap connection status.
- Device scan button.
- Detected device cards with heart icon and RSSI.
- Primary bottom action: `Training starten`.

Current status:

- Implemented with sample Polar H10 device data.
- BLE scanning/connection client exists as scaffolding but is not fully wired to
  UI state yet.

### Training list

Purpose: list structured workouts.

Required elements:

- Plan card with:
  - name
  - total duration
  - segment count
  - used zone chips
  - play/start button
- Floating add button.

Current status:

- Implemented with sample `v1` training plan.

### Training editor

Purpose: define structured zone workouts.

Required elements:

- Training name input.
- Optional free-training-after-segment checkbox.
- Segment blocks with repeated segment support.
- Segment add dialog:
  - target zone chips
  - minute/second stepper
  - optional label input
- Segment selection dialog for repeat groups.

Current status:

- Implemented as UI scaffold with sample data.
- Persistence and fully editable state are not implemented yet.

### Active workout

Purpose: show real-time heart rate and pacing guidance.

Required elements:

- Elapsed time.
- Current segment index.
- Alert indicator.
- Semi-circular heart-rate gauge.
- Very large BPM.
- Current target zone.
- Predictive pacing status:
  - speed up
  - maintain
  - slow down soon
  - slow down now
- Cadence and heart-rate slope.
- Segment remaining time and progress bar.
- Pause and stop buttons.

Current status:

- Implemented with simulated BPM/cadence updates.
- Predictive pacing engine is connected to the workout screen.

### History

Purpose: show previous sessions.

Required elements:

- Workout title.
- Date/time.
- Duration.
- Average BPM.

Current status:

- Implemented with sample history rows.
- Room/database persistence is not implemented yet.

### Settings

Purpose: configure zones and training behavior.

Required elements:

- HF Max card with plus/minus controls.
- Custom zone toggle.
- Zone list with BPM ranges.
- Later: floating bar, sound, vibration, and predictive alert sensitivity.

Current status:

- Implemented as UI scaffold.
- Settings persistence is not implemented yet.

## Floating overlay mode

### Activation

Current behavior:

- Starting a workout starts the foreground heart-rate service.
- The app checks `SYSTEM_ALERT_WINDOW`.
- If overlay permission is granted, the floating overlay service starts.
- If permission is missing, Android overlay permission settings are opened.

Future behavior:

- Add an explicit floating-mode setting.
- Allow overlay mode to be enabled/disabled during workout.
- Persist size, position, opacity, and alert style.

### Visual design

The floating overlay is an ambient-light colored zone bar:

```text
[ 127 BPM                         預先減速 ]
[ Z1 | Z2 | Z3 | Z4 | Z5 colored zone bar ]
                         |
              white vertical BPM indicator
```

Rules:

- The bar is split into Z1-Z5 colored BPM sections.
- Non-target zones are dimmed.
- The current target zone has a glow halo.
- Current BPM is represented by a white vertical indicator moving across the
  bar.
- The overlay is dark, semi-transparent, rounded, compact, and draggable.
- The right label shows the current predictive action.

Current status:

- Implemented in `FloatingZoneBarView`.
- Integrated into `FloatingHeartRateService`.
- Still uses incoming intent extras/sample defaults; real-time BLE data flow is
  not yet connected end-to-end.

## Alert model

The app distinguishes current-state and predictive-state alerts:

| State | Meaning | Visual | Sound | Vibration |
| --- | --- | --- | --- | --- |
| Speed up | BPM below target and not rising fast enough | blue glow | short beep | short double pulse |
| Maintain | inside target and projected safe | green glow | none | none |
| Slow down soon | projected BPM will approach/cross upper bound | amber glow | softer warning tone | medium pulse |
| Slow down now | BPM is at/above upper bound | red glow | stronger alert tone | long multi-pulse |

Implementation:

- `PacingPredictionEngine` returns `PacingDecision`.
- Floating overlay maps `PacingDecision` to ambient glow, tone, and vibration.
- Alerts are throttled to avoid repeating continuously.

## Predictive pacing design

Inputs:

- Time-stamped BPM samples.
- Time-stamped cadence samples.
- Current segment target min/max BPM.
- Estimated cadence-to-heart-rate response lag.

Algorithm:

1. Keep a rolling sample window.
2. Calculate heart-rate slope in BPM/minute.
3. Estimate response lag using cadence/heart-rate relationship.
4. Project BPM forward by the estimated lag.
5. Trigger `SlowDownSoon` before the target upper bound is crossed.
6. Trigger `SlowDownNow` when current BPM is already at/above the upper bound.
7. Trigger `SpeedUp` when current BPM is below the target zone and not rising
   fast enough.

Current status:

- Implemented as pure Kotlin in `PacingPredictionEngine`.
- Unit tests cover slow-down prediction, maintain, speed-up, and lag estimation.

## BLE requirements

The app targets standard BLE heart-rate devices:

- Heart Rate Service UUID:
  `0000180D-0000-1000-8000-00805F9B34FB`
- Heart Rate Measurement characteristic UUID:
  `00002A37-0000-1000-8000-00805F9B34FB`

Required capabilities:

- Scan for nearby heart-rate straps.
- Show device name and RSSI.
- Connect/disconnect.
- Subscribe to heart-rate notifications.
- Auto-reconnect to the last used strap.
- Expose live BPM to workout screen, foreground notification, and overlay.

Current status:

- BLE client scaffolding exists in `HeartRateBleClient`.
- Live BLE data is not yet wired into the Compose screen/service state.

## Data and persistence plan

Data to persist:

- User settings:
  - HF Max
  - custom zone ranges
  - alert preferences
  - overlay preferences
- Training plans:
  - segments
  - repeats
  - free-training mode
- Workout sessions:
  - start/end time
  - BPM samples
  - cadence samples
  - zone compliance
  - predictive alert events
- Learned lag profile:
  - per user
  - optionally per zone/intensity range
  - confidence score

Recommended storage:

- DataStore for lightweight settings.
- Room for training plans and workout history.

## Non-goals for current MVP

- Medical diagnosis or medical-grade monitoring.
- Cloud sync.
- Social sharing.
- Advanced coaching plans.

The app should clearly communicate that heart-rate guidance is for training
support, not medical use.

