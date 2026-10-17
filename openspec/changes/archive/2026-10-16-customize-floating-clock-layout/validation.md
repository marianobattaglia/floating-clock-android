# Implementation validation

## Automated checks

- `bash ./gradlew :app:assembleDebug`: passed with the final implementation.
- `bash ./gradlew :app:testDebugUnitTest`: passed, 5 tests, no failures or skipped tests. Coverage includes independent preference fallback/clamping, bounded steps, legacy geometry, combined scale/spacing, per-axis offsets, pixel rounding, small screens, format expansion and preservation of requested values.
- `bash ./gradlew :app:lintDebug`: ran; fails with 7 errors, 56 warnings and 4 hints. The errors correspond to calls already present in HEAD: one `NewApi` finding for `Settings.canDrawOverlays` with minSdk 21 and six `UnspecifiedRegisterReceiverFlag` findings on the existing pre-33 receiver registration branches. The new appearance receiver uses `ContextCompat.registerReceiver` and has no lint error. No lint baseline or suppression was added.
- `openspec validate customize-floating-clock-layout --strict`: passed.
- Diff whitespace and local Markdown links: reviewed. Manifest, SDK versions and runtime dependencies are unchanged; JUnit 4 is the only added test dependency. Local SDK properties, APKs and reports are ignored and are not part of the change.

## Android TV emulator checks

Environment: `Television_1080p`, Android TV API 36, arm64, 1920×1080, density 320 dpi, font scale 1.0. A temporary 800×450 viewport at the same density exercised short layouts and fitting. Original preferences were backed up before testing.

- Opened the appearance row with D-pad/Select; initial focus is on the first decrease button. Traversed all six adjustment buttons and OK with visible focus. Accessibility descriptions identify each increase/decrease action.
- One Select changes one step; tested saturation at all three upper and lower bounds with focus retained. Verified minimum 0 dp / 50% / 0%, default 16 dp / 100% / 100%, and maximum 128 dp / 200% / 300% renderings.
- Back and OK both retain settings and restore focus to the appearance row. Reopening shows saved values. The main settings and the modal scroll to expose offscreen controls in the short viewport.
- Exercised 12/24-hour and seconds controls through the existing dialog. For the complete 16-case corner/format matrix, loaded each format as a saved preference fixture after process recreation, then delivered the existing position action as the app UID. WindowManager frames stayed inside 800×450 for all four corners and all four formats at maximum size and spacing. The complete AM/PM text is visible. Restoring 1920×1080 restores the full requested scale; requested appearance preferences remain unchanged.
- That small-screen check exposed the platform preferred-dialog-width measurement limiting a wrap-content `TextClock`. The final view reports `MEASURED_STATE_TOO_SMALL` when its complete text needs more width, allowing the root to retry measurement without fixed window dimensions. Verified the correction on the emulator. The platform behavior is described in [AOSP ViewRootImpl measurement](https://android.googlesource.com/platform/frameworks/base/+/android12-release/core/java/android/view/ViewRootImpl.java).
- Verified saved non-default values after process/activity recreation, service recreation and three off/on cycles. Each active cycle has one overlay. Sending the appearance action after stopping the overlay does not start a service/window.
- With the three new keys removed, the modal shows the legacy defaults and the service uses the original text and explicit-padding settings. Reading/opening does not eagerly create the keys; position and time-format settings remain intact.
- Denied overlay permission: no overlay starts and saved appearance values remain intact. Restored permission afterward.
- Attempted notification denial using `pm revoke` before and after activity launch. This TV image continues to report `POST_NOTIFICATIONS: granted=true`; notification denial could not be exercised on it. The existing denial branch was reviewed statically.
- Reviewed the new API 21-compatible path and API 26/30/33 guards, receiver teardown, display-listener teardown and detached-window guard. No API 21–25, 26–29 or 30–32 device/image was available for runtime coverage.

## Alarm checks and user confirmation

- Scheduled an alarm through the app; verified its `RTC_WAKEUP`/`ALARM_TRIGGER` entry in AlarmManager and saved enabled/hour/minute values. Cancelled it through the dialog and verified the saved disabled state.
- Delivered `ALARM_TRIGGER` explicitly to the existing `AlarmReceiver` as the app UID. Confirmed `AlarmSoundService` remains active while changing edge distance, size and spacing to 12 dp / 90% / 90%.
- Captured alternating normal/red overlay frames after all three adjustments. Dismissed through the Alarm row; `AlarmSoundService` stopped and the normal background and adjusted geometry remained. The main clock still uses its original 48 sp styling.
- The emulator was started without host audio, and its alarm trigger was delivered manually; audible playback and scheduled delivery were therefore left for user verification.
- **Task 4.5 completed with user-supplied acceptance:** in response to that outstanding check, the user reported: “hice la prueba de la alarma y funciono”. This confirmation complements the emulator checks above. No device/API details or separate new automated run were supplied or inferred.

Only API 36 emulator behavior is claimed as runtime coverage. Build/unit tests and static API review do not establish behavior on older TVs or real hardware. Test preferences and permissions are restored after verification.
