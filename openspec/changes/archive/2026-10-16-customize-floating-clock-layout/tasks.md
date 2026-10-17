# Tasks

## 1. Shared appearance preferences and geometry

- [x] 1.1 Add the three appearance keys, defaults, limits, and step definitions plus a normalized value model/reader in `FloatingClockSettings.kt`; verify absent, wrong-type, and out-of-range values resolve independently and existing keys/actions are unchanged.
- [x] 1.2 Extract testable scale/padding/offset and fitting calculations using the formulas in `design.md`; verify 100% reproduces the existing 24 sp and 24/16 px padding, overall scale affects text and padding together, internal spacing alone leaves text size unchanged when fitting is unnecessary, and final offsets remain in bounds.
- [x] 1.3 Add focused local regression tests for normalization, step limits, combined scale/padding, screen fitting and preservation of requested values; add only the necessary test runner dependency through `gradle/libs.versions.toml` and run `bash ./gradlew :app:testDebugUnitTest`, recording any environment blocker rather than reporting unrun tests as passing.

## 2. Apply appearance to the existing overlay

- [x] 2.1 Add the package-scoped appearance update action/sender and service receiver, loading normalized preferences before window attachment and on updates; verify an active overlay updates immediately while updates to a stopped service do not start it.
- [x] 2.2 Apply text size, padding, selected gravity and bounded offsets to the same `TextClock` on the main thread; verify min/default/max settings visually and confirm no geometry update resets the alarm background or blinking state.
- [x] 2.3 Resolve available window bounds with API guards, measure the longest active time format, and recompute fitting on appearance/position/format/seconds/display changes; extend geometry regression cases for small screens and format expansion and verify complete one-line time remains in bounds without modifying requested preferences.
- [x] 2.4 Register/unregister the added receiver and release any geometry/display callbacks, guarding detached-window updates; verify repeated enable/disable cycles have one active overlay and no new callbacks after teardown, with static review of API 21, 26, 30 and 33 branches.

## 3. TV appearance modal and usage documentation

- [x] 3.1 Add `Clock appearance` after the format row within the existing enabled condition, showing a compact current-value summary; verify the row appears/disappears with overlay enablement and all main settings remain reachable with D-pad on a short TV viewport.
- [x] 3.2 Build the three labeled decrease/value/increase controls and `OK` button using existing TV styling, with bounded scrolling as needed; verify D-pad reaches every control, focus is visible, one Select changes one step, and controls remain stable at their limits.
- [x] 3.3 Save each accepted adjustment before broadcasting and reload normalized values on dialog open/activity resume; verify live overlay changes, reopen/recreation persistence, and independent operation of the three settings.
- [x] 3.4 Implement initial modal focus and return focus on `OK`/Back dismissal; verify both close paths retain saved values and return focus to the appearance row without touch input.
- [x] 3.5 Update README features and usage with the appearance row, proposed ranges, 100% meaning, immediate saving, and possible screen-fitting reduction; verify labels and defaults match the delivered modal and review Markdown links.

## 4. Integrated validation

- [x] 4.1 Run `bash ./gradlew :app:assembleDebug` and `bash ./gradlew :app:lintDebug`, and rerun geometry unit tests if integration changed their inputs; report commands/results and SDK/JDK/network blockers separately from device validation.
- [x] 4.2 On Android TV, verify D-pad/Select/Back and visible focus, min/default/max appearance combinations, all four corners, and 12/24-hour formats with seconds on/off; record device/API/density and check complete text/background remain within the screen, including the longest format.
- [x] 4.3 On Android TV, verify saved values after closing/reopening the modal, activity recreation, service recreation and off/on cycles; verify an install without new preferences keeps the prior appearance and existing position/format/alarm settings.
- [x] 4.4 On available API configurations, check overlay permission denial, API 33+ notification denial, stopped-service updates, and repeated service cycles; distinguish API 21–25, API 26+, and API 33+ coverage and document unavailable environments or pre-existing permission-flow failures separately.
- [x] 4.5 On Android TV, schedule and cancel an alarm, trigger it, adjust each appearance setting during sound/red blinking, and dismiss it through the Alarm row; verify scheduling, sound, dismissal, normal background restoration and saved geometry, with the main screen clock's size unchanged.
- [x] 4.6 Review the final diff and record automated checks, actual TV checks, and remaining limitations independently; verify no unrequested manifest/SDK/runtime dependency changes or machine-specific/build files are included.

Implementation and validation evidence: [validation.md](validation.md). Task 4.5 is complete with the user-reported successful alarm check, complementing the emulator checks.
