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

plan-003-add-cadence-data:
	Integrate Android step counter or BLE foot pod as a cadence source. Normalize
	cadence into timestamped TrainingTelemetrySample. Align HR and cadence sampling
	clocks. Record cadence history for lag learning.

plan-004-persist-data:
	DataStore: max HR, custom zone ranges, alert preferences, overlay preferences.
	Room: workout sessions, HR/cadence samples, learned lag estimates, training
	plans and segments.

plan-005-complete-training-editor: plan-004-persist-data
	Make segment add/edit/delete persistent. Implement repeat-block editing and
	segment selection modal. Add validation: at least one segment, repeat selection
	minimums, duration > 0.

plan-006-improve-prediction: plan-003-add-cadence-data
	Store per-user/per-zone lag from completed workouts. Weight recent samples more
	strongly. Detect cadence changes that haven't yet produced a HR response. Add
	cooldown and hysteresis to prevent alert spam. Add confidence score to UI and
	overlay. Add tests for overshoot prevention cases.

plan-007-overlay-settings:
	Add explicit floating mode toggle in settings. Add size modes (compact /
	standard / large). Add transparency control. Add default-location reset. Add
	per-alert sound/vibration enable toggles. Preserve the existing ambient
	overlay style instead of switching to a card-like opaque widget.

plan-008-edge-to-edge:
	Add enableEdgeToEdge() to MainActivity. Set
	android:windowSoftInputMode="adjustResize" for training editor. Audit every
	LazyColumn, bottom button, and bottom navigation item for system bar / IME
	inset handling.

plan-009-navigation-structure: plan-008-edge-to-edge
	Keep the current three top-level areas (Training, Verlauf, Einstellungen),
	introduce typed screen state, preserve tab state, and make dialogs/editors
	explicit destinations before considering a full navigation library migration.

plan-010-github-actions-ci:
	On every push and pull request: run testDebugUnitTest, build assembleDebug,
	upload app/build/outputs/apk/debug/app-debug.apk as a GitHub Actions artifact.

plan-011-sentry-releases: plan-010-github-actions-ci
	Derive release as com.cometjc.floatighrtraining@<versionName>+<versionCode>-<shortSha>.
	After successful CI build: sentry release create, set-commits --local (switch
	to --auto once GitHub is connected in Sentry), finalize, record deploy
	internal-test. Keep repository integration as a later improvement and prefer
	CI secret-based auth only.

plan-012-sentry-telemetry:
	Add targeted Sentry exception capture around BLE connection, foreground service
	startup, overlay permission, and training session state transitions. Add manual
	performance spans for app start, BLE scan/connect, and workout start/stop.
	Enable warning/error logcat breadcrumbs for BLE, overlay, notification,
	vibration, and service lifecycle. Avoid logging sensitive device identifiers.
	Reuse the existing Android SDK integration instead of demo-only capture points.

plan-013-sentry-release-hardening: plan-011-sentry-releases
	Review ProGuard/R8 mapping upload for release variants, decide whether source
	context should remain enabled outside internal builds, and document the chosen
	production exposure policy.

plan-014-firebase-distribution: plan-011-sentry-releases
	Upload debug/internal APK to Firebase App Distribution after CI tests and
	Sentry release steps pass. Use CI secrets for Firebase credentials and tester
	groups. Deliver install notifications through Firebase instead of ADB.

plan-015-ui-visual-qa: plan-008-edge-to-edge
	Save app screenshots under docs/ui-reference/app-screens/. Compare each screen
	against docs/ui-reference/comparison-checklist.md. Track intentional
	differences in docs/ui-reference/README.md instead of chat history.

plan-016-sentry-dashboards: plan-012-sentry-telemetry
	After app-specific events arrive in Sentry, create dashboard widgets for
	crash-free sessions, foreground service errors, overlay permission failures,
	BLE connection errors, app start, and workout start latency.
