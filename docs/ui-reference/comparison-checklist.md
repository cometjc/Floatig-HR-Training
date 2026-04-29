# UI Comparison Checklist

Use this checklist when comparing screenshots of the app against the uploaded reference images.

## Lane3 traceability (`plan-015`)

When real screenshots are not available yet, keep placeholders in
`docs/ui-reference/app-screens/README.md` and mark each section with the
capture status.

| Area | App screenshot | Reference source | Diff status | Follow-up |
| --- | --- | --- | --- | --- |
| Training Home | pending (`training-home`) | uploaded chat reference (German UI) | pending | export screenshot and compare CTA/layout spacing |
| Training List | pending (`training-list`) | uploaded chat reference | pending | verify chips/play icon sizing |
| Training Editor | pending (`training-editor`) | uploaded chat reference | pending | verify repeat card and segment row spacing |
| Add / Select Segment Dialogs | pending (`add-segment-dialog`, `select-segments-dialog`) | uploaded chat reference | pending | compare modal elevation and action states |
| History | pending (`history`) | uploaded chat reference | pending | verify row separators and column alignment |
| Settings | pending (`settings`) | uploaded chat reference | pending | compare zone labels and control paddings |
| Workout Screen | pending (`workout-active`) | uploaded chat reference | pending | compare gauge scale, button sizing, progress bar |
| Floating HR Bar | pending (`floating-hr-bar`) | uploaded chat reference + behavior notes | pending | verify overlay readability on top of another app |

## Global Style

- [ ] Dark / black app background.
- [ ] High-contrast white primary text.
- [ ] Muted gray secondary text.
- [ ] Bright cyan primary action color.
- [ ] Rounded cards with dark gray fill.
- [ ] Large touch targets for workout actions.
- [ ] Bottom navigation on non-workout screens.

## Training Home

- [ ] Large centered app title.
- [ ] Subtitle: choose training and connect strap.
- [ ] Training settings card.
- [ ] Connected Polar H10 row with green connected state.
- [ ] Cyan scan button.
- [ ] Device card with heart icon, device name, RSSI, and chevron.
- [ ] Cyan full-width start training button near bottom.

## Training List

- [ ] Centered title and back affordance.
- [ ] Training card shows name, duration, segment count, zone chips, play button.
- [ ] Floating add button at bottom right.

## Training Editor

- [ ] Name input at top.
- [ ] Free training checkbox.
- [ ] Total duration and segment count.
- [ ] Repeat group card with cyan outline.
- [ ] Segment rows use vertical colored zone bars.
- [ ] Add segment and repeat buttons at bottom.
- [ ] Dialogs use dark rounded surface.

## Add / Select Segment Dialogs

- [ ] Segment selector requires at least two items for repeat.
- [ ] Add segment dialog includes zone chips, minute / second steppers, optional label.
- [ ] Cancel / OK actions are cyan.

## History

- [ ] Centered title.
- [ ] Simple rows separated by thin lines.
- [ ] Left side: workout name, date, duration.
- [ ] Right side: average BPM and label.

## Settings

- [ ] HF Max card with heart icon and plus / minus controls.
- [ ] Custom zones toggle card.
- [ ] Zone list shows Z1-Z5 with BPM ranges.
- [ ] Floating HR Bar setting / description is available.

## Workout Screen

- [ ] Blue-to-black vertical gradient background.
- [ ] Top row: back, elapsed time, segment progress, alert icon, control icon.
- [ ] Semi-circular heart-rate gauge.
- [ ] Very large BPM number.
- [ ] BPM label with letter spacing.
- [ ] Status pill changes color by state.
- [ ] Target zone and next segment labels.
- [ ] Large remaining time.
- [ ] Progress bar.
- [ ] Large Pause and Stop buttons.

## Floating HR Bar

- [ ] Shows heart icon, BPM, zone, and state.
- [ ] Draggable overlay.
- [ ] Visible above other apps after overlay permission.
- [ ] Color/status mapping:
  - Low: cyan / blue, "加快步伐"
  - Target: green, "Im Ziel" or "目標"
  - High: red, "放慢步伐"
