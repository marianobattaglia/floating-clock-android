# Tasks

## 1. Alarm activation flow

- [x] 1.1 Remove the exact-alarm access check from floating-clock activation and centralize the alarm activation entry points; verify on API 31+ that enabling the overlay does not open exact-alarm settings and that overlay/notification permission behavior still works.
- [x] 1.2 Check exact-alarm access when the idle Alarm row is selected and open settings before the modal only when access is missing; show the modal on return even after denial, and recheck safely at scheduling time. Preserve direct switch activation across settings and activity recreation; verify granted access skips settings, denial retains approximate scheduling, and Back/D-pad focus do not lose or duplicate actions.

## 2. Scheduling outcome and user feedback

- [x] 2.1 Schedule exactly when access is available and use an inexact `setAndAllowWhileIdle()` fallback otherwise; handle a revoked-access `SecurityException`, return exact/approximate/failure outcomes, and update enabled state only on success. Verify exact and fallback scheduling paths and that failure leaves the alarm disabled.
  - Exact and inexact outcomes were exercised on the API 36 emulator. Failure-state handling was reviewed in code; a scheduler failure could not be induced on the emulator.
- [x] 2.2 Add localized approximate-timing feedback to all four resource catalogs and update the README fallback description; verify catalog completeness and placeholders with `bash ./gradlew :app:verifyTranslations :app:testTranslationVerifier` and review the documentation diff.

## 3. Integration validation

- [x] 3.1 Run `bash ./gradlew :app:assembleDebug` and `bash ./gradlew :app:lintDebug`; record actual results and any SDK/JDK/network blocker.
  - `:app:assembleDebug` succeeded. `:app:lintDebug` ran and failed with 7 existing errors: one `NewApi` warning for `Settings.canDrawOverlays` and six `UnspecifiedRegisterReceiverFlag` findings in existing receiver registrations.
- [x] 3.2 On the API 36 Android TV emulator, verify that overlay activation does not open exact-alarm settings; with access absent, selecting Alarm opens settings before the modal and denial returns to the modal; with access granted, the modal opens directly. Verify direct switch activation, exact scheduling, approximate fallback/feedback, and D-pad/Back behavior.
  - Verified exact scheduling via `dumpsys alarm` (`exactAllowReason=permission`) and approximate fallback after denial (nonzero alarm window), with no app crash. The emulator was left with the floating clock enabled, alarm disabled, and exact-alarm access denied.
  - Pending action and dialog values use `rememberSaveable`; forced activity/process recreation during the settings round-trip was not exercised.
- [x] 3.3 Complete the broader Android TV regression matrix: API 21–30, all clock positions and formats, overlay/notification denial behavior, alarm cancel/dismiss, sound and blinking. Record device/API coverage separately from build and lint results.
  - The user reports testing the remaining Android TV regression cases and confirming they work.
