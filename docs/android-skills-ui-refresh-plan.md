# Android skills UI refresh plan

This plan records how the installed Android skills should guide the next app/UI
refresh passes.

## Installed project skills

Installed from `https://github.com/android/skills` into `.agents/skills/`:

- `edge-to-edge`
- `navigation-3`
- `migrate-xml-views-to-jetpack-compose`
- `agp-9-upgrade`
- `r8-analyzer`
- `play-billing-library-version-upgrade`
- `display-ai-glasses-with-jetpack-compose-glimmer`
- `sentry-cli`

## Relevant guidance for this app

### 1. Edge-to-edge polish

Use `edge-to-edge` first because the app targets SDK 35 and is already Jetpack
Compose based.

- Add `enableEdgeToEdge()` to `MainActivity`.
- Set `android:windowSoftInputMode="adjustResize"` because the training editor
  uses `OutlinedTextField`.
- Audit every `LazyColumn`, bottom button, and bottom navigation item so content
  stays tappable around system bars and IME.

### 2. Navigation/UI structure

Use `navigation-3` as a reference for the next structural refresh, not as an
immediate dependency migration.

- Keep the current three top-level areas: Training, Verlauf, Einstellungen.
- Introduce typed screen state before adding a full navigation dependency.
- Preserve tab state and make dialogs/editors explicit destinations when the UI
  becomes more than prototype state.

### 3. Compose migration skill

The current app is already Compose, so `migrate-xml-views-to-jetpack-compose`
is mainly useful as a checklist if legacy XML UI is added later. Do not add XML
screens during the refresh.

### 4. Build/performance skills

- Use `agp-9-upgrade` only when planning a Gradle/AGP upgrade.
- Use `r8-analyzer` before release hardening or when keep rules are introduced.
- `play-billing-library-version-upgrade` is not relevant until paid features are
  added.

### 5. Floating overlay visual direction

The overlay should remain ambient and glanceable:

- Transparent overlay background so only text, colored zones, glow, and the BPM
  indicator are visible above other apps.
- Heart-rate scale starts at the Z1 minimum BPM instead of zero, so the visible
  bar represents the meaningful training range.
- Keep non-target zones dimmed and target zone glowing.

### 6. Sentry Android integration

Project status:

- Sentry org: `jethroyu`
- Sentry project: `floatig-hr-training`
- Project URL: `https://jethroyu.sentry.io/insights/projects/floatig-hr-training`
- Platform: Android

Use the project-local `sentry-cli` skill for CLI work. Prefer `sentry ... --json`
when gathering data for agent workflows.

Planned integration areas:

1. **Crash and error reporting**
   - Keep the Sentry Android SDK configured through the Android manifest.
   - Replace wizard/demo exception capture with app-specific capture points before
     production builds.
   - Add targeted exception context around BLE connection, foreground service,
     overlay permission, and training session state transitions.

2. **Release and deploy tracking**
   - Use a release value derived from `applicationId`, `versionName`, `versionCode`,
     and git SHA.
   - Create/finalize releases with `sentry release create`, `set-commits`, and
     `finalize`.
   - Record deploys for distribution channels such as local debug, internal test,
     beta, and production.

3. **ProGuard/R8 mapping upload**
   - Use the Sentry Android Gradle plugin for release mapping UUID generation and
     upload.
   - Keep `includeProguardMapping` enabled for release variants.
   - Use environment-provided auth in CI rather than committing token files.

4. **Source context**
   - Keep source context enabled for development/internal builds when useful.
   - Revisit exposure risk before production; source context uploads source code to
     Sentry for stack trace context.

5. **Performance and tracing**
   - Capture app start, screen interaction, foreground service startup, BLE scan /
     connect flow, and workout start/stop timing.
   - Start with conservative sampling outside debug builds.
   - Add manual spans for app-specific operations that auto-instrumentation cannot
     infer, such as heart-rate prediction updates.

6. **Logcat breadcrumbs**
   - Enable warning/error breadcrumbs for diagnostics around BLE, overlay,
     notification, vibration, and service lifecycle issues.
   - Avoid logging sensitive device identifiers beyond what is needed for debugging.

7. **GitHub/repository integration**
   - Connect the GitHub repository in Sentry so `set-commits --auto` can associate
     issues with commits and releases.
   - Until that integration exists, use `sentry release set-commits --local`.

8. **Dashboards**
   - Reuse Sentry mobile dashboards for crash-free sessions, mobile vitals, and
     performance.
   - Add app-specific dashboard widgets after events arrive: crash-free sessions,
     foreground service errors, overlay permission failures, BLE connection errors,
     app start, and workout start latency.

Security notes:

- Do not commit `sentry.properties`; it can contain `auth.token`.
- Prefer `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, and `SENTRY_PROJECT` from local shell
  or CI secrets.
- Treat DSNs as public identifiers, but avoid exposing auth tokens in logs,
  screenshots, docs, or commits.
