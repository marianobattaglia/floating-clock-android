# Tasks

## 1. Dual-form-factor routing and shared behavior

- [x] 1.1 Make Leanback optional while retaining optional touchscreen support, both launcher categories, and the TV banner; verify the merged manifest targets both phone and TV and the existing minimum SDK remains 21.
- [x] 1.2 Route Leanback devices to the existing TV dashboard and other phones to a separate phone presentation without changing the TV dashboard tree; verify launches on a phone emulator and Android TV emulator.
- [x] 1.3 Extract only the feature state transitions needed by both presentations, including overlay enablement and alarm scheduling/cooldown, while keeping existing TV controls intact; verify the existing persisted keys, defaults, Free/PRO gates, and service actions remain unchanged.

## 2. Phone interface and feature parity

- [x] 2.1 Add touch-oriented Compose UI dependencies through the version catalog and implement a compact phone Home screen for overlay, clock settings, and alarm; verify navigation and controls in portrait and landscape without clipping.
- [x] 2.2 Implement phone settings dialogs and PRO-gated appearance controls using shared state and existing preferences; verify adjustments persist and Free users cannot edit PRO appearance settings.
- [x] 2.3 Implement the phone PRO screen, Play purchase/restore action, release reviewer-code entry, and debug-only Free/PRO override; verify ownership and review grants remain separate and Debug is absent from release builds.
- [x] 2.4 Add or adapt phone-facing translations and accessibility labels in English, Spanish, Portuguese, and French; verify `bash ./gradlew :app:verifyTranslations` and touch targets are at least 48 dp.

## 3. Phone platform behavior and documentation

- [x] 3.1 Verify overlay permission denial, enable/disable, screen rotation, display-bound fitting, and overlay tap behavior on a phone running API 37; older API device coverage is out of scope for this change.
- [x] 3.2 Verify phone alarm scheduling, cancellation, ringing dismissal, notification denial, exact-alarm granted/denied paths, inexact fallback, Free cooldown, and PRO bypass on API 37.
- [x] 3.3 Update README phone and TV support guidance, including permissions and the separate phone/TV interface; verify documented behavior matches the app and existing setup instructions remain correct.

## 4. Integration and store assets

- [x] 4.1 Run `bash ./gradlew :app:assembleDebug`, `bash ./gradlew :app:assembleRelease`, and `bash ./gradlew :app:lintDebug`; verify both variants build with minSdk 21 and release hides debug-only controls.
- [x] 4.2 Run the Android TV regression on the user's API 37 TV device at its tested resolution with D-pad, Select, and Back across Home, PRO, Debug when present, dialogs, overlay, alarms, and Free/PRO behavior; verify the existing TV presentation and actions are unchanged.
- [x] 4.3 Capture authentic screenshots from the running phone UI as validation assets and keep them separate from TV store assets; phone form-factor distribution is excluded from Play.

## Manual validation report

- 2026-10-28: The user reports satisfactory tests of all phone and TV features, including PRO, on API 37. TV resolution was not recorded; validation is scoped to the tested TV device and resolution. Play distribution is TV-only. Older API levels and additional TV resolutions are not covered by this change's manual test scope.
