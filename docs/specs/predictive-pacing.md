# Predictive pacing spec

## Goal

Keep the runner inside the current heart-rate zone without waiting for BPM to
cross the limit first. Because heart rate reacts to cadence changes with delay,
the app projects near-future BPM and warns early when needed.

## Inputs

- Timestamped heart-rate samples
- Timestamped cadence samples
- Current segment target min/max BPM
- Estimated cadence-to-heart-rate lag

## Decision model

`PacingPredictionEngine` currently follows this flow:

1. Sort the rolling sample window by elapsed time.
2. Estimate heart-rate slope in BPM/minute.
3. Estimate cadence-to-heart-rate lag from recent sample correlation, with a
   fallback lag when data is limited.
4. Project BPM forward by the learned/fallback lag.
5. Emit one pacing decision.

## Decisions

| Decision | Meaning | Current UI copy |
| --- | --- | --- |
| `SpeedUp` | BPM below target and not rising fast enough | `加快步伐` |
| `Maintain` | Current and projected BPM safely inside target | `Im Ziel` / `目標中` |
| `SlowDownSoon` | Projected BPM will approach or cross the upper bound soon | `提前放慢` / `預先減速` |
| `SlowDownNow` | Current BPM already at or above the upper bound | `立即放慢` / `立即減速` |

## Current implementation

- Engine: `app/src/main/java/com/cometjc/floatighrtraining/prediction/PacingPredictionEngine.kt`
- Tests: `app/src/test/java/com/cometjc/floatighrtraining/prediction/PacingPredictionEngineTest.kt`
- Workout UI integration: `app/src/main/java/com/cometjc/floatighrtraining/ui/FloatingHrApp.kt`
- Floating overlay integration: `app/src/main/java/com/cometjc/floatighrtraining/service/FloatingHeartRateService.kt`

Current state:

- `plan-002-wire-ble-hr-data` now feeds live Polar H10 BPM into the active
  workout, foreground notification, and floating overlay.
- `plan-003-add-cadence-data` now feeds phone step-counter cadence into the
  same workout sample timeline used by pacing prediction.
- The model is intentionally simple and explainable.
- Alert cooldown/throttling exists in the overlay service, not yet as a richer
  prediction-policy layer.

## Known gaps

- No persistence of learned lag across workouts
- No confidence score exposed to UI
- No overshoot-prevention tuning based on recorded workouts
