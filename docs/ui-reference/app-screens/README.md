# App Screens Placeholder Tracking

This folder stores in-app screenshots used for `plan-015` visual QA.

Current state: no real screenshots have been exported yet. Keep each pending
screen in this tracker so follow-up capture work is explicit and traceable.

## Capture workflow

1. Run the app on a device or emulator with deterministic sample data.
2. Capture one screenshot per target screen/state.
3. Save files as `<screen-id>--<state>.png` in this folder.
4. Replace the `pending` status in the table below with the filename.
5. Update `docs/ui-reference/comparison-checklist.md` with comparison notes.

## Pending captures

| Screen ID | Expected state | Status | Notes |
| --- | --- | --- | --- |
| training-home | belt connected and primary CTA visible | pending | track against checklist section "Training Home" |
| training-list | one card + floating add button | pending | include visible zone chips |
| training-editor | repeat block + segment list | pending | include buttons at bottom |
| add-segment-dialog | dialog with zone and duration controls | pending | capture modal only if possible |
| select-segments-dialog | multi-select with disabled/enabled OK | pending | include at least 3 segment rows |
| history | list rows and avg BPM column | pending | include bottom navigation |
| settings | max HR, custom zones, zone rows | pending | include at least Z1-Z5 |
| workout-active | gauge, bpm, progress, controls | pending | include color-coded state pill |
| floating-hr-bar | overlay above another app | pending | include zone color and status text |
