# Pending implementation plan

All not-yet-implemented work is tracked here in make-target-style dependency
form.

Format: `plan-NNN-name: [dependencies...]`

A task may only begin once all its listed dependencies are complete.

---

plan-001-centralize-workout-state:
	Add a live workout StateFlow owned by the foreground service.
	Replace duplicated simulated state in the UI and overlay with a single shared
	source: connected device, current BPM, current cadence, current segment,
	current prediction, elapsed/remaining time.

plan-002-wire-ble-hr-data: plan-001-centralize-workout-state
	Create an app-level HeartRateBleController owning HeartRateBleClient.
	Connect the scan button to BLE scan state. Allow selecting and connecting a
	device. Feed live HR samples into the StateFlow, foreground notification,
	floating overlay, and workout history recorder. Add reconnection handling for
	Polar H10 disconnections.

plan-003-add-cadence-data: plan-001-centralize-workout-state
	Integrate Android step counter or BLE foot pod as a cadence source. Normalize
	cadence into timestamped TrainingTelemetrySample. Align HR and cadence sampling
	clocks. Record cadence history for lag learning.

plan-004-persist-data: plan-001-centralize-workout-state
	DataStore: max HR, custom zone ranges, alert preferences, overlay preferences.
	Room: workout sessions, HR/cadence samples, learned lag estimates, training
	plans and segments.

plan-005-complete-training-editor: plan-004-persist-data
	Make segment add/edit/delete persistent. Implement repeat-block editing and
	segment selection modal. Add validation: at least one segment, repeat selection
	minimums, duration > 0.

plan-006-improve-prediction: plan-002-wire-ble-hr-data plan-003-add-cadence-data
	Store per-user/per-zone lag from completed workouts. Weight recent samples more
	strongly. Detect cadence changes that haven't yet produced a HR response. Add
	cooldown and hysteresis to prevent alert spam. Add confidence score to UI and
	overlay. Add tests for overshoot prevention cases.

plan-007-overlay-settings: plan-002-wire-ble-hr-data
	Add explicit floating mode toggle in settings. Add size modes (compact /
	standard / large). Add transparency control. Add default-location reset. Add
	per-alert sound/vibration enable toggles.

plan-008-edge-to-edge:
	Add enableEdgeToEdge() to MainActivity. Set
	android:windowSoftInputMode="adjustResize" for training editor. Audit every
	LazyColumn, bottom button, and bottom navigation item for system bar / IME
	inset handling.

plan-009-github-actions-ci:
	On every push and pull request: run testDebugUnitTest, build assembleDebug,
	upload app/build/outputs/apk/debug/app-debug.apk as a GitHub Actions artifact.

plan-010-sentry-releases: plan-009-github-actions-ci
	Derive release as com.cometjc.floatighrtraining@<versionName>+<versionCode>-<shortSha>.
	After successful CI build: sentry release create, set-commits --local (switch
	to --auto once GitHub is connected in Sentry), finalize, record deploy
	internal-test.

plan-011-sentry-telemetry: plan-002-wire-ble-hr-data
	Add targeted Sentry exception capture around BLE connection, foreground service
	startup, overlay permission, and training session state transitions. Add manual
	performance spans for app start, BLE scan/connect, and workout start/stop.
	Enable warning/error logcat breadcrumbs for BLE, overlay, notification,
	vibration, and service lifecycle. Avoid logging sensitive device identifiers.

plan-012-firebase-distribution: plan-010-sentry-releases
	Upload debug/internal APK to Firebase App Distribution after CI tests and
	Sentry release steps pass. Use CI secrets for Firebase credentials and tester
	groups. Deliver install notifications through Firebase instead of ADB.

plan-013-ui-visual-qa: plan-008-edge-to-edge
	Save app screenshots under docs/ui-reference/app-screens/. Compare each screen
	against docs/ui-reference/comparison-checklist.md. Track intentional
	differences in docs/ui-reference/README.md instead of chat history.
