# Validation

Date: 2026-10-19

## Automated and static checks

- Gradle wrapper: 8.13; Gradle runs on JDK 21, with the existing JVM target unchanged.
- `bash ./gradlew :app:testDebugUnitTest`: passed, 5 tests in `ClockAppearanceTest`, zero failures/errors. No instrumented test suite was added; UI checks below use ADB input and UI hierarchy inspection on an actual emulator.
- `bash ./gradlew :app:assembleDebug`: passed; the final APK was installed and launched on the TV emulator.
- `bash ./gradlew :app:lintDebug`: failed with 7 errors, 56 warnings, and 4 hints. A separate temporary export of `HEAD` was built and linted with the same commands. Issue IDs, messages, severities, and counts are identical; no lint issue was introduced by this change. Existing errors are one `NewApi` overlay-permission call and six `UnspecifiedRegisterReceiverFlag` findings in existing receiver code. No baseline, suppression, or functional fix was introduced.
- `git diff --check`: passed.
- Existing overlay-toggle, position-save, and appearance-save callback bodies were compared against `HEAD`, ignoring whitespace and the added read-only preview-selection callback; the original actions match.
- Services, scheduler, receiver, preferences/actions, geometry helpers, manifest, SDK/dependency configuration, and build configuration are unchanged.
- README local image links verified. Screenshots contain only emulator UI, without physical-device identifiers.

## Initial Android TV emulator checks (before the focus/banner follow-up)

Environment: existing Android TV AVD, API 36. Tests targeted only the emulator. The connected physical device was not used. Runtime coverage on API 21–35 and 37+ is not claimed.

- Installed the unchanged `HEAD` APK temporarily and compared direct opening of position, format, appearance, and alarm dialogs with the redesigned APK. All retained their existing controls. Appearance OK returned focus to its row and kept adjusted values; the final app's Back path also restored its opening row.
- Verified Home and Settings with D-pad/Select. Settings focuses the first existing row within the same screen. Focusing a setting alone leaves the preview unchanged; selecting it updates the passive preview and invokes the original action.
- Verified overlay enablement from its row and from its switch, one toggle per activation, hiding dependent rows on disable, and existing saved values across APK replacement and activity recreation. Denying overlay access opened the existing TV permission screen and kept the saved enabled flag false; returning left the app usable, and access was restored afterward.
- Verified four corner selections; 12/24-hour switching; seconds shown/hidden; accepted format reflected in the header and preview.
- Verified appearance changes save immediately and survive modal dismissal. Exercised upper bounds 128 dp / 200% / 300% and lower size/spacing bounds 50% / 0%, then restored 16 dp / 100% / 100% using the existing controls. Existing unit tests additionally cover range saturation, normalization, and fitting.
- Verified alarm dialog hour/minute buttons and existing CANCEL action with D-pad; a scheduled alarm test is recorded below.
- Screenshots reviewed at 1920×1080 and 1280×720 (TV density adjusted to 213 for 720p). All five rows and the passive bottom reservation fit. Appearance controls and OK are visible and reachable in the bounded modal. The temporary display overrides were reset afterward.
- Verified taps on Home/Settings and configuration rows invoke their same actions, and child switch taps toggle once without also activating the parent row. The new presentation forwards taps to the existing callback because this TV Material Surface version handles D-pad/accessibility activation separately from pointer input. Back exited directly without intermediate rail/preview steps; reopening retained the accepted appearance value.
- Final dashboard preview uses the existing header clock's formatted time via a read-only callback, without a second timer. The preview reads settings through a disposable listener; it has no preference writes, broadcast sends, permission launchers, services, audio calls, or focus targets.

## Captures

- [Dashboard, 1080p](../../../docs/images/dashboard.png)
- [Position dialog](../../../docs/images/position-dialog.png)
- [Format dialog](../../../docs/images/format-dialog.png)
- [Appearance dialog](../../../docs/images/appearance-dialog.png)
- [Alarm dialog](../../../docs/images/alarm-dialog.png)
- [Dashboard, 720p](../../../docs/images/dashboard-720p.png)
- [Appearance dialog, 720p](../../../docs/images/appearance-dialog-720p.png)

## Remaining runtime checks

- With exact-alarm app-op access denied on API 36, both the original HEAD APK and the visual APK reached ACTIVATE in the existing alarm dialog and completed activation without exceptions; the saved alarm-enabled state became true in both. Exact-alarm access was then restored.
- A real alarm was scheduled with the existing hour/minute controls and ACTIVATE. AlarmManager delivered it at the chosen minute and started the existing foreground `AlarmSoundService`. The overlay's red alarm state was captured. While the alarm was active, an appearance adjustment saved successfully; selecting the alarm row then stopped the sound service, cleared enabled state, and retained the appearance value. The existing CANCEL action was also checked separately.
- Audible output is **not verified**: the emulator was launched with host audio disabled and its playback monitor did not provide an active alarm player to verify actual sound. A physical-TV listening check is still required; task 4.4 remains unchecked for this reason. No sound-service changes were made.
- Notification-denial coverage is not claimed: after the revoke attempt this TV emulator still reported `POST_NOTIFICATIONS` as granted. The existing permission callback is unchanged; a reproducible denied state must be checked on a suitable device.
- Other Android API levels remain untested at runtime. Static review and the unchanged service code do not establish successful execution on those versions.

## Final state

- Implementation and available checks: 23/24 tasks complete. Task 4.4 remains open for audible sound and a reproducible denied-notification state on a suitable TV/device.
- Final visual APK remains available in the normal debug build output. No test alarm was left scheduled; the emulator's initial preference values were restored and the test emulator was shut down. The physical device was not used.
- The change is not archived. Complete the remaining runtime validation before closing it.

## Focus preview and hidden-banner follow-up

These checks supersede the initial preview-activation and advertising-placeholder behavior described above.

- Moving down through Position, Format, Appearance, and Alarm immediately updated the right-hand preview without pressing OK. UI hierarchy assertions confirmed the active row and matching panel title, no dialog appeared, and the entire saved-preference XML stayed unchanged throughout focus movement.
- OK still opened each existing configuration directly. Back closed the dialog and restored focus to its opening row. No configuration callback was added to focus events.
- Removed the complete advertising placeholder, including its label, border, and reserved height. Reviewed refreshed dashboard screenshots at 1080p and 720p; the content occupies the recovered space and no advertising text is rendered.
- Refreshed the dashboard and all documented dialog screenshots, plus the 720p appearance screenshot. README and planning artifacts describe focus-driven previews and hidden advertising.
- Debug APK built successfully; the five existing unit tests passed. Lint still reports the same 7 errors, 56 warnings, and 4 hints as the original baseline, with no new findings. A transient Kotlin daemon startup message recovered automatically and did not prevent compilation or packaging.
- Emulator display overrides and saved app preferences were restored after the checks. The remaining sound/notification-denial validation in task 4.4 is unchanged.

## Initial Android TV background follow-up (superseded by the landscape below)

- Replaced the locally drawn room with `drawable-nodpi/android_tv_preview_background.png`, a static screenshot of the Android TV emulator launcher Apps screen. No account was used; the physical device was not accessed. The PNG has no textual or EXIF metadata and is bundled for offline rendering.
- The live clock is still drawn over the image with the same time, geometry, position, and appearance values. Preview focus, existing actions, preferences, services, and dependencies are unchanged.
- `:app:assembleDebug` passed. `:app:lintDebug` reports the same 7 errors, 56 warnings, and 4 hints as the original HEAD baseline; issue IDs, messages, severities, and counts match exactly.
- Installed the APK and reviewed refreshed 1080p/720p screenshots. Up/Down updated all previews without opening dialogs or changing saved preferences. OK opened the original dialogs; Back returned focus to the opening row. No advertising placeholder appeared.
- Refreshed the dashboard and dialog screenshots. Emulator display overrides and original saved preferences were restored; the test emulator was shut down. Existing sound/notification-denial limitations in task 4.4 remain open.

## User-provided landscape follow-up

- Replaced the launcher screenshot with the user's landscape photograph in `drawable-nodpi/preview_landscape.jpg`. Removed the superseded launcher asset. The provided JPEG is 1920x1080 and has no EXIF metadata.
- Render the original image with a centered source crop to fill the existing preview frame without stretching. The clock drawing and its position, geometry, time, and saved settings are unchanged. No permission or dependency was added; the image remains available offline.
- `:app:assembleDebug` passed. `:app:lintDebug` retains exactly the same issue IDs, messages, severities, and counts as the original baseline (7 errors, 56 warnings, 4 hints).
- Installed and visually reviewed the landscape preview on the API 36 Android TV emulator at 1080p and 720p. D-pad focus changed each preview without modifying saved values; OK opened each existing dialog and Back restored focus. Refreshed all documented screenshots. Restored emulator preferences and display overrides, then shut down the emulator. No physical device was used.

## Clock visibility follow-up

- The miniature clock is drawn only while the floating clock is enabled; when disabled, its preview retains the landscape without a clock. The large fixed header clock uses the inverse visibility. Its existing time/format observation remains mounted and continues supplying the preview; no additional timer, preference key, permission, or service action was introduced.
- `:app:assembleDebug` passed. `:app:lintDebug` retains the same 7 errors, 56 warnings, and 4 hints as the original HEAD baseline (IDs, messages, severities, and counts compared).
- On the API 36 Android TV emulator, verified the disabled/enabled/disabled transition: the header is present when disabled, absent when enabled, and restored after disabling. Reviewed the landscape-only disabled preview and live-clock enabled preview.
- Captures: [disabled](../../../docs/images/dashboard-clock-disabled.png), [enabled](../../../docs/images/dashboard-clock-enabled.png).
- D-pad configuration navigation, original OK dialogs and Back focus restoration passed. Reviewed the updated 1080p/720p dashboard and refreshed all documented dialogs. Original emulator preferences and display overrides were restored; the test emulator was shut down. Physical-device testing and the existing sound/notification-denial limitations remain unchanged.
- Follow-up correction: read the formatted time during composition before forwarding it through `SideEffect`. This keeps Compose subscribed to the ticking state even when the header Text is not rendered, so activity recreation and format/seconds updates continue to reach the passive canvas. Rebuilt the APK and repeated lint comparison after this correction.
- After recreating the activity at 720p with the floating clock enabled, verified the preview shows the current time instead of the placeholder. Enabled seconds through the existing format dialog and inspected captures three seconds apart: the miniature time advanced while the large header remained hidden. Original preferences were restored after the check.
