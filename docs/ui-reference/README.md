# UI reference snapshots

This folder preserves the reference context from the uploaded sample screens so the app UI can be compared against it later.

Note: the original uploaded image binaries were not available as files in the workspace, so this document records the observable screens, layout details, and comparison checklist from the provided screenshots.

## Reference screens captured from the uploaded examples

1. Training edit - segment selection dialog
   - Dark background with centered rounded modal.
   - Title: `Segmente auswählen`.
   - Multi-select list of workout segments.
   - Segment labels include `Z2 Ausdauer`, `Z3 Tempo`, `Z4 Schwelle`.
   - Actions: `Abbrechen`, disabled/enabled `OK`.

2. Training edit - add segment dialog
   - Title: `Segment hinzufügen`.
   - Zone selector chips: any zone, Z1, Z2, Z3, Z4, Z5.
   - Duration steppers for minutes and seconds.
   - Optional label text field.
   - Actions: `Abbrechen`, `OK`.

3. Training edit - repeat block
   - Title bar: `Training bearbeiten`, `Speichern`.
   - Training name input: `v1`.
   - Repeated segment card: `3x wiederholen`.
   - Colored vertical markers per zone.
   - Bottom actions: `+ Segment`, `Wiederholen`.

4. Training list
   - Title: `Trainings`.
   - Card for plan `v1`.
   - Shows total duration, segment count, zone chips, and play button.
   - Floating add button at bottom right.

5. History
   - Title: `Verlauf`.
   - Simple list rows with workout name, date/time, duration, and average BPM.
   - Bottom navigation: Training / Verlauf / Einstellungen.

6. Settings
   - Title: `Einstellungen`.
   - HF Max card with plus/minus controls.
   - Custom zones toggle.
   - Zone list with BPM ranges:
     - Z1 Erholung
     - Z2 Ausdauer
     - Z3 Tempo
     - Z4 Schwelle
     - Z5 VO2 Max

7. Training home
   - App title: `Strap Zone`.
   - Training selection card.
   - Bluetooth belt status: connected to Polar H10.
   - Device scan button.
   - Device card with heart icon and RSSI.
   - Primary bottom button: `Training starten`.

8. Active workout
   - Gradient dark blue workout screen.
   - Top row: elapsed time, segment progress, alert icon.
   - Semi-circular heart-rate gauge with needle.
   - Large BPM number.
   - Status pill: in target / too low / too high.
   - Target zone and next segment text.
   - Segment remaining time and progress bar.
   - Bottom controls: `PAUSE`, `STOPP`.

## Visual style checklist

- Dark theme with black or near-black background.
- Card surfaces use rounded corners and slightly lighter dark gray.
- Main accent color is cyan/blue.
- Zone color mapping:
  - Z1: gray
  - Z2: blue
  - Z3: green
  - Z4: orange/red
  - Z5: purple
- Workout screen emphasizes very large BPM text.
- Training-related copy follows the German reference labels for now.
- Floating heart-rate bar should use the same color/status language:
  - too low: prompt to speed up
  - in target: maintain
  - too high: slow down

## Traceable comparison assets

- App screenshot folder: `docs/ui-reference/app-screens/`
- Placeholder tracker (until real captures exist): `docs/ui-reference/app-screens/README.md`
- Checklist with per-area comparison status: `docs/ui-reference/comparison-checklist.md`

If the binary screenshot files are not available yet, keep entries as `pending`
with screen IDs. This keeps the visual QA flow auditable across plan hand-offs.

## Future comparison workflow

When actual screenshots are available from the app:

1. Save app screenshots under `docs/ui-reference/app-screens/`.
2. Save original/reference images under `docs/ui-reference/source-images/` if raw files are provided.
3. Compare each screen against the checklist above.
4. Track intentional differences in this document instead of relying on chat history.
