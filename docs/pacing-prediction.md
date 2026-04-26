# Predictive pacing alerts

This app should avoid waiting until heart rate has already crossed the target
zone. Running cadence changes affect heart rate with a delay, so the alert
engine records both streams and predicts when the upper bound will be reached.

## Inputs

- Heart rate samples: timestamp + BPM from BLE Heart Rate Service.
- Cadence samples: timestamp + steps per minute from phone sensors or a foot pod.
- Target zone: current segment min/max BPM.
- Learned lag: seconds between cadence changes and visible heart-rate response.

## Decision model

1. Keep a rolling window of recent sensor samples.
2. Estimate the heart-rate slope in BPM/minute.
3. Estimate whether cadence is still rising.
4. Project heart rate forward by the learned lag.
5. Trigger an early slow-down alert if projected BPM reaches the zone ceiling.

The MVP uses a conservative linear model. Historical workouts can later replace
the fallback lag value with a per-runner/per-zone lag learned from cadence and
heart-rate cross-correlation.

## Alert timing

- `SlowNow`: projected BPM reaches or exceeds the zone max within the learned
  lag window. Use visual red/orange cue, sound, and vibration.
- `PrepareToSlow`: projected BPM is close to the ceiling. Use a softer visual
  cue before the stronger alert.
- `Hold`: current and projected BPM are safely inside the target zone.
- `SpeedUp`: BPM is below the zone minimum and not rising fast enough.

## Current implementation

- `PacingPredictionEngine` is a pure Kotlin module with unit tests.
- The workout screen now shows predictive action, estimated rise rate, cadence,
  and expected time to the zone ceiling.
