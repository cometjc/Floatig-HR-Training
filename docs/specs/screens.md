# Core screens spec

## Training home

Purpose: choose a workout, connect the heart-rate belt, and start training.

Elements:
- App title: `Strap Zone`
- Training selection card (name, total duration, segment count)
- BLE strap connection status
- Device scan button
- Detected device cards with heart icon and RSSI
- Primary bottom action: `Training starten`

Status: implemented with sample Polar H10 device. BLE client scaffolded;
`plan-002-wire-ble-hr-data` wires Polar BLE scan/connect state into the UI via
an app-level `HeartRateBleController`. The scan button now starts/stops
discovery, detected device rows show RSSI, selecting a row requests a
connection, and the connection card reflects the currently connected Polar H10.

## Training list

Purpose: list structured workouts.

Elements:
- Plan card: name, total duration, segment count, zone chips, play button
- Floating add button

Status: implemented with sample `v1` plan.

## Training editor

Purpose: define structured zone workouts.

Elements:
- Training name input
- Optional free-training-after-segment checkbox
- Segment blocks with repeat support
- Segment add dialog: target zone chips, minute/second stepper, optional label
- Segment selection dialog for repeat groups

Status: UI scaffold with sample data. Persistence and editable state not yet
implemented.

## Active workout

Purpose: show real-time heart rate and pacing guidance.

Elements:
- Elapsed time
- Current segment index
- Alert indicator
- Semi-circular heart-rate gauge with needle
- Very large BPM number
- Current target zone
- Predictive pacing status: speed up / maintain / slow down soon / slow down now
- Cadence and heart-rate slope
- Segment remaining time and progress bar
- Pause and stop buttons

Status: implemented with `centralize-workout-state` via a shared workout
`StateFlow` owned by `HeartRateForegroundService`. The workout screen,
foreground notification, and floating overlay now read the same current BPM,
cadence, current segment, prediction, and elapsed/remaining time.
`plan-002-wire-ble-hr-data` replaces simulated BPM updates with live Polar H10
heart-rate notifications while cadence remains the placeholder source until
`plan-003-add-cadence-data`.

## History

Purpose: show previous sessions.

Elements:
- Workout title, date/time, duration, average BPM

Status: implemented with sample rows. Room persistence not yet implemented.

## Settings

Purpose: configure zones and training behavior.

Elements:
- HF Max card with plus/minus controls
- Custom zone toggle
- Zone list with BPM ranges

Status: UI scaffold. Persistence not yet implemented. Future: floating bar,
sound, vibration, and predictive alert sensitivity controls.
