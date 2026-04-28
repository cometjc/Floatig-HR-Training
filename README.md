# Floating HR Training

Android app prototype for heart-rate-zone training with BLE heart-rate belts,
predictive pacing alerts, and a floating ambient zone bar overlay.

## Build

Requires Android SDK with `platforms;android-35` and `build-tools;35.0.0`.

```bash
ANDROID_HOME=/path/to/android-sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

If a fresh machine lacks the SDK, install `platform-tools`, `platforms;android-35`,
and `build-tools;35.0.0` first. The committed `gradlew` downloads Gradle
automatically; no Gradle installation is needed.

## Architecture

### Entry and UI

- `app/src/main/java/com/cometjc/floatighrtraining/MainActivity.kt` — runtime
  Bluetooth/location/notification permission requests; hosts the Compose app.
- `app/src/main/java/com/cometjc/floatighrtraining/ui/FloatingHrApp.kt` — main
  Compose UI: training home, training list, training editor, segment dialog,
  active workout screen, history screen, settings screen.

### Data and domain

- `app/src/main/java/com/cometjc/floatighrtraining/model/TrainingModels.kt` —
  HR zones, training segments, training plans, workout history, alert state.
- `app/src/main/java/com/cometjc/floatighrtraining/data/SampleRepository.kt` —
  sample training plan, sample device, sample history.
- `app/src/main/java/com/cometjc/floatighrtraining/prediction/PacingPredictionEngine.kt` —
  pure Kotlin prediction engine: HR slope, cadence-to-HR lag estimation, pacing
  decision (`SpeedUp` / `Maintain` / `SlowDownSoon` / `SlowDownNow`).
- `app/src/test/java/com/cometjc/floatighrtraining/prediction/PacingPredictionEngineTest.kt` —
  unit coverage for prediction decisions and lag estimation.

### BLE and services

- `app/src/main/java/com/cometjc/floatighrtraining/ble/HeartRateBleClient.kt` —
  BLE Heart Rate Service scan/connect scaffold; parses standard HR Measurement
  characteristic. Not yet wired into Compose UI or service state.
- `app/src/main/java/com/cometjc/floatighrtraining/service/HeartRateForegroundService.kt` —
  foreground service scaffold for ongoing HR monitoring.
- `app/src/main/java/com/cometjc/floatighrtraining/service/FloatingHeartRateService.kt` —
  overlay service; creates and updates the draggable floating zone bar; triggers
  sound and vibration per pacing decision.
- `app/src/main/java/com/cometjc/floatighrtraining/service/FloatingZoneBarView.kt` —
  native `View` drawing Z1-Z5 colored segments, dimmed non-target zones, glowing
  target zone, white BPM indicator line, BPM and action text.

## Current behavior

### Training start

1. Foreground monitoring service starts.
2. Overlay permission (`SYSTEM_ALERT_WINDOW`) is checked.
3. If granted → floating zone bar service starts; if missing → Android overlay
   permission settings open.
4. Workout screen opens with simulated HR/cadence values.

### Floating overlay

Defaults: BPM `127`, target zone `Z2`, decision `Maintain`. The service accepts
`EXTRA_BPM`, `EXTRA_TARGET_ZONE_ID`, and `EXTRA_DECISION` intent extras to
update the display. Draggable; positioned near top-center.

### Predictive pacing

The active workout screen feeds rolling simulated `TrainingTelemetrySample`
values into `PacingPredictionEngine`, which sorts by elapsed time, calculates HR
slope, estimates cadence-to-HR lag, projects HR forward, and emits a decision.

## Known limitations

- BLE scan/connect is scaffolded but not wired to UI state or service lifecycle.
- Workout screen uses simulated BPM/cadence; no real cadence source integrated.
- Floating overlay uses defaults/intent extras only; no end-to-end BLE data flow.
- No Room/DataStore persistence; training editor edits are not saved.
- Sound/vibration implemented but not user-configurable.
- No screenshot/golden UI tests or medical disclaimer/onboarding.

## Documentation

- Product spec: `docs/specs/product-spec.md`
- Pending plan: `docs/plan.md`
- Implemented specs index: `docs/specs/README.md`
- Implemented screens spec: `docs/specs/screens.md`
- Implemented floating overlay spec: `docs/specs/floating-overlay.md`
- Implemented predictive pacing spec: `docs/specs/predictive-pacing.md`
- Predictive pacing notes: `docs/specs/pacing-prediction.md`
- UI reference notes: `docs/ui-reference/README.md`

## Plan workflow

- `just plan-ready` lists plan items in `docs/plan.md` that currently have no remaining dependencies.
- `just plan-next` remains as a compatibility alias for `just plan-ready`.
- `just plan-done <target-or-keyword>` removes a completed plan item and clears that target from other plan dependencies.
- `just plan-complete <target-or-keyword>` remains as a compatibility alias for `just plan-done`.
- `just plan-done` only succeeds when at least one file under `docs/specs/` both contains the plan keyword and is currently staged or unstaged in git.
