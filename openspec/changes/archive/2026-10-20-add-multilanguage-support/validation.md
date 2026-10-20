# Localization validation

Focused Google-guidance and TalkBack validation is recorded in [accessibility-review.md](accessibility-review.md), including a selected-state semantics fix and seven passing tests with TalkBack active. Manual acceptance was subsequently confirmed by the project owner; see the archive acceptance note below.

Validation performed on 2026-10-20. Implementation is complete. Agent-run API coverage is recorded separately from the owner-reported final manual acceptance.

## Environment and automated checks

- Gradle wrapper 8.13, JBR 21.0.11, existing AGP 8.13.2, SDK Platform 36; no SDK/plugin/runtime dependency upgrades.
- Android TV emulator `emulator-5554`, API 36. Actual system locales were changed through Android Settings, not an app-specific locale override. The connected Chromecast was not modified.
- 1080p: 1920×1080 at 320 dpi. 720p: 1280×720 at the same density, also exercising a smaller logical layout with scrolling.

| Check | Result |
| --- | --- |
| `bash ./gradlew --version` | Passed; Java 21 runs Gradle, JVM target remains 11 |
| `:app:verifyTranslations` | Passed; 63 complete translatable messages in English + Spanish/Portuguese/French, with seven explicit invariant resources |
| `:app:testTranslationVerifier` | Passed; valid fixture accepted and seven malformed fixtures rejected |
| `:app:testDebugUnitTest` | Passed; eight tests: five existing appearance/geometry tests and three locale/identity regressions |
| `:app:assembleDebug` | Debug APK assembled successfully |
| `:app:assembleDebugAndroidTest` | Instrumentation APK assembled successfully |
| `:app:connectedDebugAndroidTest`, selected emulator | Seven tests passed, zero skipped; also ran the earlier six-test suite in Spanish at 1080p before adding full dialog traversal |
| `:app:lintDebug` | Failed on seven existing errors, with 56 warnings and four hints; see comparison below |
| `:app:check --dry-run` | Both translation verification tasks are included in `check` |
| `git diff --check` | Passed for implementation changes |

Configured-context instrumentation verifies `en-US`, `en-GB`, `es-AR`, `es-ES`, `pt-BR`, `pt-PT`, `fr-FR`, `fr-CA`, unsupported `de-DE`/`ja-JP`, and ordered lists `de-DE,fr-FR`, `es-AR,fr-FR`, `en-GB,fr-FR`, `de-DE,ja-JP`. English correctly takes precedence over secondary French. APK configuration inspection confirms only `es`, `fr`, `pt` language alternatives; complete English is unqualified. Other remaining qualifiers are density, display, theme, direction, and API resources.

The XML verifier covers missing/orphan/duplicate keys, empty values, indexed placeholder compatibility, and required plural forms when present. Invariant exceptions are limited to the brand, universal control glyphs, selection markers, separator, and unset-time symbol. All canonical string IDs have production consumers; Kotlin text consumers were audited separately. Brand-only banner artwork remains shared.

## Lint comparison

Ran lint independently on an isolated temporary checkout of baseline commit `d18139b`. Its seven error IDs/messages match the implementation exactly:

- One `NewApi` diagnostic for `Settings.canDrawOverlays` in the existing notification-permission callback in `MainActivity.kt`.
- Six `UnspecifiedRegisterReceiverFlag` diagnostics in existing version-guarded receiver registration branches in `MainActivity.kt` and `FloatingClockService.kt`.

No translation or string-format diagnostics were reported. No lint baseline or suppression was added and no permission/receiver behavior was changed to make lint pass. A combined Gradle invocation consequently exits nonzero at lint even though resource checks, unit tests, and APK assembly complete successfully.

## TV interaction and visual checks

The automated dashboard suite uses the actual device language and exercises remote Select, system Back, visible/reachable controls, position persistence, appearance adjustment/focus return, preview identity, rail actions, child-switch action routing, and traversal of position/format/appearance/alarm dialogs. It uses isolated preferences and does not start an overlay or schedule an alarm just to render a preview. Captures are optional via the instrumentation argument `localizationScreenshots=true` and are stored under the target app's private `files/localization/` directory (export with `adb exec-out run-as com.example.floatingclock tar -cf - files/localization`). Using the target process's private storage fixes the previous optional-capture failure caused by the test package's external directory.

Actual app checks additionally covered:

- Spanish dashboard, position, format, and appearance controls at 1080p; all four corners selected with the remote and their persisted IDs read back.
- 24-hour format without seconds changed to 12-hour with seconds; actual Spanish `p. m.` and French/English day-period text fit the overlay.
- Maximum edge distance 128 dp, overall size 200%, and spacing 300% reached using the existing buttons, with complete text remaining inside the screen; restored 16 dp/100%/100% through those controls.
- French at 1080p and 720p, including scrolling through the appearance modal to its close action with D-pad; Portuguese and English dashboard/alarm flows at 720p. Wider text wraps, and lower rows remain reachable by bounded scrolling.
- Overlay permission was initially unavailable and no overlay was started by preview rendering. Granting overlay permission on the test emulator allowed the existing activation flow.

The language/resolution instrumentation matrix is recorded below. All eight combinations passed seven tests each, including optional screenshot export. [Raw runner results](instrumentation-results/en-720.txt) are saved alongside the other seven locale/resolution result files. These runs supplement the manual device checks; they do not establish legacy API or physical-TV coverage.

| System language | 720p | 1080p |
| --- | --- | --- |
| English, US | 7/7 passed | 7/7 passed |
| Spanish, Spain | 7/7 passed | 7/7 passed |
| French, France | 7/7 passed | 7/7 passed |
| Portuguese, Brazil | 7/7 passed | 7/7 passed |

Representative actual-app captures:

- [Spanish appearance at 1080p](screenshots/es-1080-appearance.png) and [Spanish scrolled appearance at 720p](screenshots/es-720-appearance-scrolled.png).
- [French scrolled appearance at 720p](screenshots/fr-720-appearance.png) and [French appearance at 1080p](screenshots/fr-1080-appearance-scrolled.png).
- [Portuguese dashboard at 720p](screenshots/pt-720-dashboard.png) and [Portuguese appearance at 1080p](screenshots/pt-1080-appearance-scrolled.png).
- [English alarm preview at 720p](screenshots/en-720-alarm-preview.png).

## Active services, audio, and cleanup

- During Spanish → French → Portuguese → English system changes, the overlay retained service record `f6962f`, `lastStartId=1`, and `startForegroundCount=1`. Its notification body/title and channel name changed to the current language; its saved position/appearance/format/seconds remained intact.
- A ringing alarm retained service record `3d791af`, `lastStartId=1`, and `startForegroundCount=1` across Portuguese → English. Its text changed from `O alarme está a tocar.` to `Alarm is ringing.`, while the clock notification changed from `Relógio flutuante` to `Floating Clock`.
- The first alarm's service lifetime was approximately 60 seconds across a late language change; it stopped on the original timeout rather than receiving another minute. Polling introduces roughly two seconds of timing uncertainty.
- Native AudioFlinger diagnostics showed an active track owned by the app process with alarm stream/usage (4), and continuing beep-track activity across the locale change. This verifies the emulator's audio playback path; speaker quality was not evaluated by listening.
- A later alarm fired while the app remained open. Red and normal overlay frames were captured, and selecting the already-focused Alarm row stopped it before auto-stop, cleared `alarm_enabled`, removed the alarm service, and restored the normal overlay.
- Repeated overlay disable/enable/disable/enable/disable cycles produced respectively 0/1/0/1/0 overlay windows and matching service state. The final disabled state left no overlay service/window.
- Instrumentation verifies channel renaming preserves existing ID, importance, sound, and vibration preferences and does not create unused channels. Activity entry/resume synchronizes labels without starting services.

[Red alarm frame](screenshots/alarm-blink-red.png) and [normal blinking phase](screenshots/alarm-blink-normal.png) show the same running alarm.

## Existing behavior issue outside this change

`AlarmConfigRow` initializes its local active-alarm flag to false and only updates it from a broadcast. If the activity is recreated while an alarm is already ringing, it misses the earlier start broadcast; selecting the row can open configuration instead of immediately dismissing the active alarm. This predates localization and was observed during system-language testing. Canceling through the existing configuration controls still invokes the cancellation path. Direct row dismissal was separately verified when the screen was already listening before the alarm fired. This state synchronization issue was not silently changed as part of translation work.

## Remaining coverage and review

- No device execution on API 21–23, API 24–25, API 26–30, or API 31–35. Compatibility branches were reviewed statically, but API 36 checks do not substitute for those device runs. The legacy API/permission task was subsequently closed on owner-reported manual acceptance; this does not extend the agent-run API inventory.
- Exact-alarm denial/unavailable settings and notification-denial permutations were not fully exercised across TV versions; their existing code paths and translations were preserved. The normal exact-alarm flow was tested with access enabled on the emulator.
- Preference keys, position IDs/gravity, actions, channel/notification identities, service types, manifest, SDK versions, alarm scheduling, and audio operations retain their existing meanings. No app language preference, locale picker/config declaration, network access, or runtime dependency was added.
- An IDE deployment-target file changed independently during the session and was left untouched by this implementation.

## Final follow-up review

OpenSpec progress: 20/20 tasks complete after owner-reported manual acceptance.

Rebuilt the instrumentation APK after fixing screenshot storage; translation checks, verifier self-tests, unit tests, and debug assembly succeeded with the installed JBR 21.0.11. Android Studio now bundles JBR 25 on this machine, which fails the current Gradle/Kotlin setup before compilation; selecting the existing JBR 21 resolves that environment issue without project changes. Final lint error messages were compared programmatically with the pristine baseline and remain identical (seven errors). Strict OpenSpec validation and `git diff --check` passed.

Final source review confirms resource-backed UI/toast/accessibility consumers, language-independent routing, unchanged existing preference/action constants and notification IDs, and an unchanged manifest. Asset review found only the invariant FloatingClock branding and language-neutral preview artwork. The IDE deployment-target modification was preserved.

The full manual acceptance is distinct from automated traversal plus visual review. The project owner confirmed both changes were manually tested and OK, and requested archive with no pending tasks; tasks 4.3 and 4.5 were closed on that confirmation. No old emulator images were downloaded, and the physical Chromecast was not changed.

## Owner acceptance for archive

On 2026-10-20, the project owner confirmed manual testing of both redesign and multilingual support was OK and requested archiving without pending tasks. All tracked tasks are closed. Detailed devices, API levels, and spoken utterances were not supplied with that confirmation; the agent-run evidence above remains unchanged. Existing lint findings and the separately documented pre-existing alarm-state issue are not represented as fixed by this acceptance.
