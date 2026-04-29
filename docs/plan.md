# Pending implementation plan

All not-yet-implemented work is tracked here in make-target-style dependency
form.

Format: `plan-NNN-name: [dependencies...]`

A task may only begin once all its listed dependencies are complete.

PLD direct directives (optional, per plan body line):

- `@pld ownership: pathA/,pathB/`
- `@pld required: <cmd1>,<cmd2>`
- `@pld advisory: <cmd1>,<cmd2>`
- `@pld worktree: .worktrees/<branch-name>`

`just pld-bootstrap <execution>` reads dependency-free plans from this file and
directly maps each ready plan target into one PLD lane in `.pld/executor.sqlite`
without requiring intermediate lane-plan markdown conversion.

`just pld-run <execution>` currently runs preflight + bootstrap only (it does
not auto-integrate). Final merge/cleanup still goes through `pld_integrate`
after lanes are reported as `READY_TO_COMMIT`/`DONE`.

Planning notes:

- This repo is already Jetpack Compose based, so UI work should prefer Compose
  patterns and should not add new XML screens.
- For UI polish, prioritize edge-to-edge and inset correctness before larger
  navigation refactors.
- Overlay follow-up work should preserve the current ambient direction:
  transparent background, meaningful BPM scale starting near Z1, dimmed
  non-target zones, and a glowing active target zone.
- Sentry work should use environment-provided credentials only. Do not commit
  `sentry.properties` or auth tokens.

---
