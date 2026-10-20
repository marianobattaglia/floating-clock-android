# Proposal

## Why

Enabling the floating clock currently opens the special exact-alarm access screen, even though the clock does not schedule alarms. This couples two independent features; requesting access when the user enters the Alarm flow keeps overlay activation independent and places the request before alarm configuration begins.

## What Changes

- Remove the exact-alarm access request from floating-clock activation.
- On Android 12 (API 31) and later, check access when the user selects the idle **Alarm** row and request it before showing the configuration modal when it is missing. If access is already granted, show the modal immediately. After returning from system settings, recheck access and then show the modal even if access remains denied, so the user can still use the approximate fallback.
- For direct alarm-switch activation with a saved time, request missing access before scheduling. Check access before every scheduling attempt; if it is already granted, schedule directly. If access is denied or settings are unavailable, preserve the documented best-effort behavior with an inexact alarm and tell the user that delivery may be less precise.
- Keep **ACTIVATE** in the modal from opening the settings screen a second time; the access check happens before the modal, and the scheduler rechecks safely before scheduling.
- Fix the fallback path: `setAlarmClock()` is also an exact-alarm API and requires access; use an inexact API instead and do not report success if scheduling fails.
- Preserve cancellation, saved preferences, sound, and D-pad/Back interaction.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `tv-settings-dashboard`: separate the overlay/notification permissions used by the floating clock from exact-alarm access, and define behavior on alarm activation, return from settings, and access denial.

## Impact

- `MainActivity.kt`: floating-clock activation and the two alarm activation paths.
- `AlarmScheduler.kt`: exact versus inexact scheduling and scheduling failure handling.
- `app/src/main/res/values*/strings.xml` and `README.md`: localized reduced-precision feedback and accurate fallback documentation.
- No permission declaration, dependency, minimum SDK, or service changes are expected. Exact-alarm access remains declared in the manifest and is checked from API 31 onward.
- Android documents that `setExact()`, `setExactAndAllowWhileIdle()`, and `setAlarmClock()` require exact-alarm access for affected apps; `canScheduleExactAlarms()` checks access and `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` opens system settings. Sources: [Schedule exact alarms](https://developer.android.com/develop/background-work/services/alarms) and [Android 14 changes](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms).
