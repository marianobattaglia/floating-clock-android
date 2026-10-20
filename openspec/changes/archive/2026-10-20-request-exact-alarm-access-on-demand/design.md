# Design

## Context

See `proposal.md` for the motivation and `specs/tv-settings-dashboard/spec.md` for the user-visible contract. `FloatingClockSwitch` currently checks exact-alarm access before starting the overlay. `AlarmConfigRow` also checks it in both alarm activation paths, but immediately continues to schedule after launching system settings. `AlarmScheduler` then falls back to `setAlarmClock()`, which is still an exact-alarm API on Android 12+ and can throw without the special access. The README already describes an approximate fallback.

The exact-alarm setting is special app access, not a runtime permission dialog. The system settings activity does not provide a grant result to this flow, so the app must recheck when it resumes. Android's guidance also requires checking `canScheduleExactAlarms()` before scheduling.

## Goals / Non-Goals

**Goals:**

- Keep overlay activation independent from exact-alarm access.
- Complete an alarm activation after the settings round-trip, using the selected time and reporting whether the result is exact or approximate.
- Make the scheduler's result reflect the actual scheduling outcome, including a safe response to permission revocation between the check and API call.
- Preserve current alarm preference keys and TV remote behavior.

**Non-Goals:**

- Change which app feature owns the alarm, the alarm UI, the overlay permission flow, notification permission behavior, or the manifest declarations.
- Add a separate settings page or exactness preference.
- Change alarm recurrence, sound, dismissal, or service behavior.

## Decisions

### Keep exact-alarm access in the alarm activation path

Remove the `needsExactAlarmPermission` check from floating-clock activation. On selection of an idle Alarm row, check `canScheduleExactAlarms()` before opening the configuration modal. If access is already granted, show the modal immediately. If it is absent, retain a pending “show alarm configuration” continuation, launch `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, and show the modal when the activity returns after rechecking access—even if the user denied it. This lets the user configure an approximate alarm while keeping the requested permission step ahead of the modal.

For a direct switch activation with a saved time, retain a pending activation and that time while settings is open; on return, schedule exactly if access was granted or use the inexact fallback otherwise. If the settings intent cannot be launched, continue to the modal for row selection or use the fallback for direct activation. The modal's **ACTIVATE** action rechecks access and schedules without launching settings a second time. On API 21–30, open the modal or schedule directly without the special-access screen. Save the pending continuation in saveable UI state so return from settings and activity recreation do not lose it; preserve D-pad focus on the Alarm row/modal.

Pending activations and the selected hour/minute must survive the Settings round-trip and activity recreation. Consume each continuation once on return. Cancel or Back from the modal clears dialog state and does not schedule an alarm; it does not launch settings again.

An alternative was to request access from **ACTIVATE** inside the modal. The user observed this interrupts alarm configuration after it has already started; gating the modal on row selection matches the desired sequence. If the user returns without granting access, still show the modal and retain the documented approximate fallback.

### Use an inexact API when exact access is unavailable

When `canScheduleExactAlarms()` is true, keep the current exact scheduling behavior. When it is false on API 31+, use `setAndAllowWhileIdle()` for the documented best-effort schedule. Do not use `setAlarmClock()` as a fallback because Android classifies it as exact and guards it with the same access. Catch a `SecurityException` from exact scheduling in case access changes after the check, then retry through the inexact path.

Return a scheduling outcome that distinguishes exact success, approximate success, and failure. Update the enabled preference and selected time only after a successful outcome. Show a localized approximate-timing message after fallback success; retain the existing unavailable-scheduling feedback if both paths fail. The alternative of treating every scheduler call as a boolean success is insufficient because the current method returns `true` without verifying that its fallback is allowed.

### Preserve the existing settings and platform contract

Keep `SCHEDULE_EXACT_ALARM` in the manifest because the alarm still requests precise delivery when the user activates it. Add no dependency or SDK change. Preserve the saved alarm time and enabled preference keys. Localize the approximate-timing feedback in every existing resource catalog and update the README so its best-effort statement matches the implemented API.

## Risks / Trade-offs

- **Approximate alarms can be delayed, especially during Doze** → clearly tell the user when exact access was not granted and use the inexact API intended for alarms during idle mode.
- **Android TV system settings may not expose or resolve the request screen** → catch the unavailable-intent case and continue with the same inexact fallback rather than leaving the activation stuck.
- **The access may be revoked between checking and scheduling** → catch the exact API's `SecurityException`, retry inexactly, and do not persist an enabled state unless a scheduling call succeeds.
- **The activity may be recreated while system settings is open** → save the pending activation and selected time with saveable activity state, consume it only once on resume, and retain normal D-pad/Back focus behavior.

## Migration Plan

No preference migration is needed; existing keys and defaults remain unchanged. New activations use the selected path immediately. Existing alarm cancellation remains valid for the same `PendingIntent`. If implementation changes require replacing an already scheduled pending intent, cancel the prior alarm before rescheduling through the chosen exact or inexact path. Rollback consists of restoring the prior activation flow; no stored data needs conversion.
