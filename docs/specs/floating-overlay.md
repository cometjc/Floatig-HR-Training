# Floating overlay spec

## Visual design

The overlay is an ambient zone bar displayed above all other apps.

```
[ 127 BPM                         Slow down soon ]
[ Z1 | Z2 | Z3 | Z4 | Z5  colored zone bar      ]
                        |
             white vertical BPM indicator
```

- Bar split into Z1-Z5 colored BPM sections (scale starts at Z1 min, not zero).
- Non-target zones dimmed; current target zone has a soft glow halo.
- White vertical indicator shows current BPM position on the bar.
- Right label shows current predictive action.
- Dark, semi-transparent, rounded, compact, draggable.
- Default position: top-center of screen.

## Implementation

- `FloatingZoneBarView` — native `View` drawing the bar, zones, glow, and indicator.
- `FloatingHeartRateService` — overlay service; subscribes to the
  `centralize-workout-state` shared workout `StateFlow`, triggers
  sound/vibration, creates/updates the view.

Legacy intent extras still supported as fallback:
- `EXTRA_BPM` — current BPM integer
- `EXTRA_TARGET_ZONE_ID` — target zone (Z1–Z5)
- `EXTRA_DECISION` — pacing decision string

Current defaults before a workout starts (no live BLE data yet): BPM 127,
target Z2, decision Maintain.

## Alert model

| Decision | Meaning | Glow | Sound | Vibration |
|---|---|---|---|---|
| SpeedUp | BPM below target, not rising fast enough | blue | short rising beep | two short pulses |
| Maintain | inside target, projected safe | green | none | light confirmation |
| SlowDownSoon | projected BPM will approach ceiling | amber | softer warning beep pair | medium pulse |
| SlowDownNow | BPM at/above upper bound | red | stronger alarm tone | long multi-pulse |

Alerts are throttled to avoid continuous repetition.

## Activation

1. User taps `Training starten`.
2. `HeartRateForegroundService` starts.
3. `SYSTEM_ALERT_WINDOW` permission checked.
4. If granted → `FloatingHeartRateService` starts.
5. If missing → Android overlay permission settings opened.

Future: explicit floating-mode toggle in settings; allow enable/disable during
workout; persist size, position, opacity, and alert style.
