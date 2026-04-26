# Handoff: Floating HR Training Android app

## Branch and PR

- Branch: `cursor/android-hr-training-app-e5ea`
- Base branch: `main`
- PR: created and updated for this branch.

## Repository state

The repository now contains a new Android app module built with:

- Kotlin
- Jetpack Compose
- Android foreground service
- Android overlay window service
- BLE GATT Heart Rate Service scaffolding
- Pure Kotlin predictive pacing engine with unit tests

The app currently builds in this environment when an Android SDK is available.

Verification command used:

```bash
ANDROID_HOME=/tmp/android-sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Note: the cloud machine did not initially include Android SDK tooling, so a
temporary SDK was installed under `/tmp/android-sdk` for verification. This path
is intentionally not committed.

## Implemented files of interest

### Android entry and UI

- `app/src/main/java/com/example/floatinghr/MainActivity.kt`
  - Requests runtime Bluetooth/location/notification permissions.
  - Hosts the Compose app.

- `app/src/main/java/com/example/floatinghr/ui/FloatingHrApp.kt`
  - Main Compose UI.
  - Implements:
    - Training home
    - Training list dialog
    - Training editor dialog
    - Segment dialog
    - Active workout screen
    - History screen
    - Settings screen
  - Active workout screen currently uses simulated BPM/cadence data and displays
    predictive pacing state.

### Data and domain

- `app/src/main/java/com/example/floatinghr/model/TrainingModels.kt`
  - Heart-rate zones
  - Training segments
  - Training plans
  - Workout history
  - Basic alert state

- `app/src/main/java/com/example/floatinghr/data/SampleRepository.kt`
  - Sample training plan, sample device, sample history.

- `app/src/main/java/com/example/floatinghr/prediction/PacingPredictionEngine.kt`
  - Pure Kotlin prediction engine.
  - Estimates HR slope.
  - Estimates cadence-to-HR lag.
  - Predicts whether the runner should speed up, maintain, slow down soon, or
    slow down immediately.

- `app/src/test/java/com/example/floatinghr/prediction/PacingPredictionEngineTest.kt`
  - Unit coverage for prediction decisions and lag estimation.

### BLE and services

- `app/src/main/java/com/example/floatinghr/ble/HeartRateBleClient.kt`
  - BLE Heart Rate Service UUID scan/connect scaffold.
  - Parses standard BLE Heart Rate Measurement characteristic.
  - Not yet wired into the Compose UI/service state.

- `app/src/main/java/com/example/floatinghr/service/HeartRateForegroundService.kt`
  - Foreground service scaffold for ongoing HR monitoring.

- `app/src/main/java/com/example/floatinghr/service/FloatingHeartRateService.kt`
  - Overlay service.
  - Creates/updates draggable floating zone bar.
  - Triggers sound and vibration patterns per pacing decision.

- `app/src/main/java/com/example/floatinghr/service/FloatingZoneBarView.kt`
  - Native Android `View` drawing the floating ambient zone bar.
  - Draws:
    - Z1-Z5 colored segments
    - dimmed non-target zones
    - glowing target zone
    - white BPM indicator line
    - BPM and action text

## Current behavior

### Training start

When the user taps `Training starten`:

1. Foreground monitoring service starts.
2. Overlay permission is checked.
3. If overlay permission is granted, the floating zone bar service starts.
4. If overlay permission is missing, Android overlay permission settings open.
5. Workout screen opens with simulated heart-rate/cadence values.

### Floating overlay

Current default overlay state:

- BPM: `127`
- Target zone: `Z2`
- Decision: maintain/target

The service accepts intent extras:

- `FloatingHeartRateService.EXTRA_BPM`
- `FloatingHeartRateService.EXTRA_TARGET_ZONE_ID`
- `FloatingHeartRateService.EXTRA_DECISION`
- legacy `EXTRA_ZONE` / `EXTRA_STATE` compatibility

The overlay is draggable and displayed near the top center of the screen.

### Predictive pacing

The active workout screen uses a rolling list of simulated
`TrainingTelemetrySample` values.

The prediction engine:

1. Sorts samples by elapsed time.
2. Uses a recent analysis window.
3. Calculates HR slope in BPM/min.
4. Estimates cadence-to-HR delay from historical sample correlation.
5. Projects HR forward by the learned/fallback delay.
6. Emits one of:
   - `SpeedUp`
   - `Maintain`
   - `SlowDownSoon`
   - `SlowDownNow`

## Important limitations

This is an MVP scaffold, not a production-ready running tracker yet.

Known limitations:

- BLE scan/connect is implemented as a client class but is not yet wired to UI
  state or foreground service lifecycle.
- Workout screen uses simulated BPM/cadence.
- Floating overlay receives default/sample values only unless started with
  explicit extras.
- No Room/DataStore persistence yet.
- Training editor dialogs are mostly static UI; they do not persist edits.
- No real cadence source is integrated yet.
- Sound/vibration patterns are implemented, but not user-configurable.
- No screenshot/golden UI tests yet.
- No medical disclaimers/onboarding yet.

## Next development plan

### 1. Wire real BLE heart-rate data

- Create an app-level monitor/controller that owns `HeartRateBleClient`.
- Connect scan button to BLE scan state.
- Allow selecting a device and connecting.
- Feed HR samples into:
  - active workout screen
  - foreground notification
  - floating overlay
  - workout history recorder
- Add reconnection handling for Polar H10 disconnections.

### 2. Add real cadence data

Potential sources:

- Android step counter / accelerometer cadence estimate.
- BLE foot pod if available.
- Manual test mode for cadence simulation.

Implementation notes:

- Normalize cadence into timestamped `TrainingTelemetrySample`.
- Keep HR and cadence sampling clocks aligned.
- Record cadence history for lag learning.

### 3. Centralize live workout state

Add a live workout state holder, likely:

- Foreground service as owner of active workout session.
- Repository or singleton flow for UI consumption.
- StateFlow containing:
  - connected device
  - current BPM
  - current cadence
  - current segment
  - current prediction
  - elapsed/remaining time

This should replace duplicated simulated state in UI/overlay.

### 4. Persist settings and workouts

- DataStore:
  - max HR
  - custom zone ranges
  - overlay settings
  - alert preferences
- Room:
  - workout sessions
  - HR/cadence samples
  - learned lag estimates
  - training plans/segments

### 5. Improve prediction model

The current model is intentionally simple and explainable.

Next improvements:

- Store per-user/per-zone lag from completed workouts.
- Weight recent samples more strongly.
- Detect whether cadence recently changed but HR has not responded yet.
- Avoid alert spam with cooldown and hysteresis.
- Add confidence score to UI/overlay.
- Add tests for overshoot prevention cases.

### 6. Complete training editor

- Make segment add/edit/delete persistent.
- Implement repeat block editing.
- Implement segment selection modal.
- Add validation:
  - at least one segment
  - repeat selection minimums
  - duration > 0

### 7. Floating overlay settings

- Add explicit floating mode toggle in settings.
- Add size modes:
  - compact
  - standard
  - large
- Add transparency control.
- Add default location reset.
- Add per-alert sound/vibration enable toggles.

### 8. UI comparison and visual QA

Reference docs are stored under:

- `docs/ui-reference/README.md`
- `docs/ui-reference/comparison-checklist.md`

When screenshots are available:

- save app screenshots under `docs/ui-reference/app-screens/`
- save source images under `docs/ui-reference/source-images/`
- compare against the checklist.

## Build notes

The committed `gradlew` is a lightweight launcher that downloads Gradle
distribution into `.gradle/wrapper-dists/` if a local Gradle installation is not
available. It does not commit Gradle distribution binaries.

Expected command:

```bash
ANDROID_HOME=/path/to/android-sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

If a fresh cloud machine lacks Android SDK, install:

- `platform-tools`
- `platforms;android-35`
- `build-tools;35.0.0`

## Product docs

- Product spec: `docs/product-spec.md`
- Predictive pacing details: `docs/pacing-prediction.md`
- UI references: `docs/ui-reference/README.md`
- UI comparison checklist: `docs/ui-reference/comparison-checklist.md`
