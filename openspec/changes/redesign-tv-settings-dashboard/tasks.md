# Tasks

## 1. Dashboard presentation and rail

- [x] 1.1 Apply shared activity visual tokens for colors, text, spacing, surfaces, icons, and visible focus in `ui/theme` and existing screen composables; verify representative Compose previews retain the current app identity and readable focus states.
- [x] 1.2 Reorganize the existing screen into a logo rail, app-name/live-clock header, left settings column, right preview area, with the bottom advertising placeholder hidden. Keep existing row/dialog owners stable; verify 720p/1080p screenshots show all current controls without overlap or clipped focus borders.
- [x] 1.3 Add only Home and Settings shortcuts to the header and current settings region of that same screen; verify D-pad/OK/tap reaches both shortcuts without a separate screen, status overview, state reset, mandatory rail step, or new Back history.
- [x] 1.4 Update README layout descriptions and advertising-reservation wording; verify the existing configuration instructions still work unchanged and no advertising integration is implied.

## 2. Existing rows and dialogs

- [x] 2.1 Restyle current row surfaces, icons, summaries, and switches while preserving each existing callback and visibility rule. Verify the floating-clock row still toggles, position/format/appearance/idle alarm rows still open their dialogs directly, a ringing alarm row still stops it, and each activation executes its existing action once.
- [x] 2.2 Apply the visual style to existing position, format, appearance, and alarm dialogs without replacing controls or altering save/close/cancel behavior. Verify four position buttons, format switches, three appearance stepper groups with existing ranges/steps/defaults, and existing alarm hour/minute/ACTIVATE/CANCEL controls remain available.
- [x] 2.3 Verify remote access, modal focus, existing OK/Back dismissal, and focus restoration after restyling; explicitly check appearance returns to its row and screen-level Back has no added intermediate steps. Add a focused UI regression test only where it provides useful coverage of altered layout/focus behavior using current test dependencies.
- [x] 2.4 Refresh relevant README screenshots/captions after the visual update; verify existing action names, appearance ranges, persistence explanation, and usage sequence remain accurate without an intermediate Edit action.

## 3. Supplementary previews

- [x] 3.1 Add a passive preview panel with an initial neutral prompt and selection updated when a row or child switch receives focus. Verify Up/Down switches previews without changing settings, and OK/Select/tap still runs the original action once, and previews add no focus target, button, or editing step.
- [x] 3.2 Render existing clock position/format/seconds/appearance and existing enablement/alarm information using read-only values and consistent miniature-display scaling. Verify representative corners, long time formats, and appearance extremes against the real overlay without modifying its geometry helpers, saved values, or runtime rendering.
- [x] 3.3 Refresh preview content after accepted changes and dialog dismissal, reset content for hidden rows, and release any added UI observation on disposal. Verify rendering adds no permission request, service start/binding, window, audio, or alarm scheduling/canceling beyond the selected row's existing action.
- [x] 3.4 Document previews as a supplementary visual element in README; verify the text preserves direct dialog opening and acknowledges that an existing modal can temporarily obscure the underlying preview.

## 4. Integrated regression validation

- [x] 4.1 Run `bash ./gradlew :app:testDebugUnitTest`, `bash ./gradlew :app:assembleDebug`, and `bash ./gradlew :app:lintDebug`; record actual outcomes and any JDK/SDK/network/tool blocker separately from manual validation.
- [x] 4.2 On Android TV, compare the existing and redesigned configuration flows using D-pad/Select/Back, including direct dialog opening, switches, focus restoration, 720p/1080p readability, the passive preview, and the absence of an advertising placeholder. Record screenshots and actual results without physical-device identifiers; mark unavailable checks as pending.
- [x] 4.3 On Android TV, verify unchanged overlay enable/disable, all four corners, 12/24-hour formats and seconds, three appearance control ranges and immediate persistence, hidden dependent rows, and saved values after app/service recreation. Verify the header and actual overlay geometry retain their current independent behavior.
- [ ] 4.4 On Android TV, verify unchanged alarm scheduling/canceling, sound, red blinking, and dismissal through the current alarm row. Compare existing overlay/notification denial and exact-alarm/unavailable-settings behavior where applicable; record pre-existing defects separately without adding functional fixes to this visual change.
- [x] 4.5 Review the implementation diff for presentation-only scope: unchanged preference/action contracts, permission flows, schedulers, service lifecycle, overlay rendering, SDK/dependencies, and manifest. Review minSdk compatibility of any added UI code and document runtime coverage by available API level; distinguish static review from device results.

## 5. Requested visual follow-up

- [x] 5.1 Update the preview on row/child-switch focus; verify Up/Down changes the panel without changing saved values or opening dialogs, while OK still opens the corresponding configuration.
- [x] 5.2 Remove the bottom advertising placeholder and its reserved height; verify it is absent at 1080p and 720p and refresh affected screenshots.
- [x] 5.3 Align README and planning artifacts with these requests; rerun debug build, existing unit tests, lint, and focused Android TV navigation checks, recording actual results and prior limitations.

## 6. Landscape preview background follow-up

- [x] 6.1 Replace the illustrative room/launcher capture with the landscape photograph provided by the user; fill the existing frame with a centered crop without distortion, retaining live clock geometry, configuration, and offline rendering.
- [x] 6.2 Build, compare lint with the existing baseline, and verify the background, D-pad previews, OK/Back, and layout on the 1080p/720p emulator; refresh screenshots.

## 7. Clock visibility follow-up

- [x] 7.1 Draw the miniature clock only when floating clock is enabled, preserve the landscape when disabled, and hide/restore the large header clock inversely while keeping live time observation mounted.
- [x] 7.2 Build and lint, verify disabled/enabled/disabled transitions on Android TV, and refresh 1080p/720p captures while preserving D-pad and OK/Back behavior.
