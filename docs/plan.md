# Pending implementation plan

All not-yet-implemented work is tracked here in make-target-style dependency
form.

Format: `plan-NNN-name: [dependencies...]`

A task may only begin once all its listed dependencies are complete.

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

plan-014-firebase-distribution:
	Upload debug/internal APK to Firebase App Distribution after CI tests and
	Sentry release steps pass. Use CI secrets for Firebase credentials and tester
	groups. Deliver install notifications through Firebase instead of ADB.

plan-016-sentry-dashboards:
	After app-specific events arrive in Sentry, create dashboard widgets for
	crash-free sessions, foreground service errors, overlay permission failures,
	BLE connection errors, app start, and workout start latency.
