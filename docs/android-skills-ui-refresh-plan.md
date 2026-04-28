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
