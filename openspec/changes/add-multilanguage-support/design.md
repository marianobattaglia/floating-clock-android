# Design

## Context

See [proposal.md](proposal.md) for motivation and scope. The current implementation has only `app_name` in `res/values/strings.xml`; most displayed messages live in Kotlin. `MainActivity` extends `ComponentActivity` and uses Compose. `FloatingClockPosition` embeds English labels, `DashboardSetting.forTitle` maps English display text to behavior, and rail navigation compares `Home`/`Settings` strings. Blind extraction of strings would break those associations.

Both services build literal notifications and channels at startup. `FloatingClockService.onConfigurationChanged` currently refits the clock but does not refresh notifications. `AlarmSoundService` has no configuration-change refresh. The main clock and overlay width estimation use `Locale.getDefault()`. Existing appearance/geometry regression tests exist in `app/src/test/java/.../ClockAppearanceTest.kt`; there are no current instrumentation tests. This observed test state is newer than the no-tests statement in OpenSpec's context.

The existing main specification covers appearance. The active dashboard change's language-preservation constraint applies to that visual work; this localization change explicitly supersedes the language constraint only. Its navigation, preview, and action contracts remain applicable, and its artifacts are left untouched.

## Goals / Non-Goals

**Goals:** Use platform resource selection across API 21+, centralize translation authoring, make behavior independent of wording, and update service presentation without restarting operational work.

**Non-Goals:** A custom locale engine, a translation backend, an activity-base-class migration, a stored language preference, new runtime dependencies, or introducing language-setting UI. Use language-wide catalogs with broadly understandable wording; separate regional dialects can be added later.

## Decisions

### 1. Android resources are the translation source of truth

Use this catalog structure:

| Resource path | Role |
| --- | --- |
| `app/src/main/res/values/strings.xml` | Complete canonical English messages and invariant resources |
| `app/src/main/res/values-es/strings.xml` | Complete Spanish translations |
| `app/src/main/res/values-pt/strings.xml` | Complete Portuguese translations, shared by Brazil and Portugal |
| `app/src/main/res/values-fr/strings.xml` | Complete French translations |

Use semantic snake_case names grouped by purpose, such as `dashboard_preview_title`, `clock_position_top_left`, `alarm_notification_title`, and `permission_overlay_denied`. Deduplicate truly equivalent messages, not texts with different contexts that merely match in English. Add translator comments describing context and substitutions. Use positional placeholders and full message templates; use plurals only when a quantity actually changes grammar. Never concatenate translated word fragments such as `Decrease ` plus a label. Prefer a complete action/target message, or dedicated action descriptions where grammar demands it. Parameterized values such as `dp`, percentages, and summaries also belong in resources even when their symbols stay unchanged.

Resolve text with Compose `stringResource`/`pluralStringResource` in composition and current-context `getString`/`getQuantityString` in callbacks, services, and other Android code. Keep resource IDs in models, not eagerly resolved global strings. Mark only the brand and truly invariant content as `translatable="false"`, with comments; normal prose and `OK`-style actions still receive deliberate localized entries. Developer-only action strings, preference IDs, icon keys, time patterns, and logs are not translated.

Alternative considered: Kotlin dictionaries or JSON catalogs with a locale manager. Rejected because they duplicate native matching and tooling and create extra Compose/service refresh work. Runtime translation APIs also add network and availability requirements to a fully local app.

### 2. The system configuration owns language selection

The user confirmed automatic system selection, English fallback, and no in-app picker. Let Android resolve resource variants from each current context's `Resources.Configuration`; do not choose language from country, time zone, IP, accounts, or a saved preference. API 21–23 use the single configured locale, while API 24+ support ordered language lists. Generic `es`, `pt`, and `fr` qualifiers cover regional variants without exhaustive region folders. English stays in unqualified `values` so an unsupported language always has complete fallback resources.

Do not call `Locale.setDefault`, override base contexts, or mutate the system language. No `AppCompatDelegate.setApplicationLocales`, `LocaleManager` picker integration, `android:localeConfig`, or generated locale-config declaration is introduced. Basic automatic localization needs none of these. This keeps the selected experience consistent on TV versions where per-app language settings do not exist. A future explicit language-selection feature can evaluate automatic `LocaleConfig` generation and AppCompat independently.

Restrict packaged dependency language resources to the four supported languages using the current AGP resource-configuration facility if necessary for native matching; record and test the packaged locale set. No plugin or version upgrade is needed. In particular, test English before French in an ordered list as well as unsupported-primary/supported-secondary cases; a catalog check alone cannot prove resource resolution.

For non-resource regional formatting, read the context configuration through the existing AndroidX configuration compatibility APIs rather than treating the process-global locale as the language authority. Text fallback and regional formatting are distinct: German system preferences can show English prose while retaining regional numeric/time conventions. Saved 12/24-hour and seconds values remain authoritative, and alarm entry remains `HH:MM` in 24-hour form. Patterns and day-period names are handled by Android/JDK locale formatting rather than translated literal patterns. Use the same locale and formatting assumptions for clock rendering and width estimation; locale changes invalidate cached formatters/probes. Keep the existing time zone and time source.

Alternative considered: English on every first launch with manual selection. Rejected by the user's choice. Per-app language UI is deferred because it adds a new product flow and compatibility work without being needed for automatic translation.

### 3. Stable identity is independent of presentation

Replace `DashboardSetting` title/subtitle strings with resource IDs and pass a stable setting identity explicitly through main settings rows to preview selection and focus logic. Remove `forTitle` and any translated-title comparison. Use stable rail entries (enum or sealed values) with separate label resources and icon identity instead of comparing `Home`/`Settings` display values. Retain optional identity for dialog-internal rows that do not participate in dashboard previews.

Replace `FloatingClockPosition.label` with a label resource ID or an external mapping; retain every enum entry, persisted `id`, gravity, and `fromId` behavior. Other machine identifiers, including service broadcasts, channel IDs (`FloatingClockChannel`, `AlarmChannel`), and notification IDs, remain untouched. Localized strings are never stored as settings or used as command selectors.

Alternative considered: look up the translated title to recover a setting. Rejected because wording changes and duplicate translations would still affect behavior. This refactor is limited to the existing presentation-to-identity coupling.

### 4. Refresh presentation within existing lifecycles

Compose resources participate in configuration updates; leave normal activity recreation enabled instead of intercepting `locale` in the manifest. Include configuration/locale in any remembered localized data and clock-effect keys. Do not cache translated text in enum/static values. Preserve existing saved settings and dialog save/cancel behavior across the existing lifecycle; a locale-triggered recreation does not introduce a new requirement to retain unsaved dialog drafts.

Extract notification construction from operational start actions in each service so configuration refresh can build localized content and notify using the same active notification ID. Set refresh notifications to avoid alerting again. Never call the alarm start path, reset its auto-stop handler, reschedule an alarm, or restart a foreground service solely to change text.

On `Service.onConfigurationChanged`, after the superclass update, use the current service resources to refresh notification text. For the overlay, refresh the TextClock's locale-dependent rendering and recompute its widest-time probe/fitting while retaining the attached window, corner, appearance, and blinking state. If the platform view needs rebinding to refresh its cached locale, keep it inside the current window lifecycle and ensure at most one attached overlay. Guard updates against destroyed/uninitialized views.

For API 26+, a small shared channel-label synchronization function updates the names of channels that already exist, using their existing channel objects/settings and IDs. Call it at activity creation/resume and from service configuration/start paths. It must not create a channel for an unused feature from the activity, reset importance/sound/user preferences, or delete/recreate channel identity. Services retain their existing first-use channel creation. Updating stopped services' old channels waits until the next app entry; no new background component is justified. All new callbacks/listeners, if needed, remain owned and released by their existing component.

Alternative considered: restarting services on locale changes. Rejected because it risks alarm audio continuity, deadlines, blinking, window duplication, and foreground-start restrictions.

### 5. Cover every app-owned surface and verify completeness

Use the following implementation inventory to prevent visible English fragments:

| Area | Text coverage and special handling |
| --- | --- |
| `MainActivity.kt` | Setting/dialog titles, position/format labels, buttons, toasts, alarm controls and formatted values |
| `ClockDashboard.kt` | Rail labels, tagline, setting subtitles, preview prompts/status/value labels, summaries; stable setting/rail identity |
| `FloatingClockAppearanceRow.kt` | Titles, help, parameterized units/summary, close button, increase/decrease accessibility semantics |
| `FloatingClockSettings.kt` | Four position labels without changing persisted IDs |
| `FloatingClockService.kt` | Notification/channel names, time locale, locale-dependent fitting |
| `AlarmSoundService.kt` | Notification/channel names, configuration refresh isolated from audio/deadline |
| Manifest/resources/assets | Keep resource-backed app label; inspect image text, retaining the FloatingClock brand; no hidden advertising text is made visible |

Android permission and Settings screens are external surfaces; translate the app's messages around them without changing their existing behavior. Do not add new error handling/flows as an incidental localization fix. Audit visible assets for embedded prose and localize any such app-owned text; invariant brand artwork can remain shared.

Add a Gradle-invoked `:app:verifyTranslations` check using XML-aware parsing without a new third-party library. Discover supported language catalogs under the chosen `values-<language>` convention, require the initial `es`/`pt`/`fr` catalogs, and compare them to canonical translatable English keys. Reject missing/orphan/duplicate keys, empty entries, mismatched indexed substitution signatures (including literal percent handling), and incomplete applicable plural quantities if any are introduced. Keep narrow explicit invariant exemptions. Wire this task into `check` and document the direct command so catalog coverage cannot be silently replaced by runtime fallback. XML parsing also checks escaped apostrophes, accents, and nested markup through Android resource compilation; do not use regex-only XML parsing.

Use Android lint for format/translation diagnostics and review Kotlin text consumers explicitly: catalog equality does not detect every hardcoded Compose literal. Add device/instrumented resource-resolution checks with configured contexts for the language/region/fallback matrix, and translated dashboard interaction checks for preview/action identity. Use existing Compose test dependencies; configure only necessary instrumentation runner support through the version catalog if required. Keep catalog checks and existing appearance tests runnable without a device. Verify layout and active service updates separately on Android TV.

To add another language later: add a complete generic catalog with the same semantic IDs, extend any packaged resource-language restriction, run the same catalog/build/lint checks, and add the language to resource/device validation and README coverage. Existing feature logic does not change. Do not introduce a translation platform until catalog scale justifies it.

## Risks / Trade-offs

- Longer French/Portuguese labels can exceed the narrow rail and trailing-value areas → permit suitable wrapping/space adjustments within the existing layout and check 720p/1080p focus visibility. Never make controls inaccessible to fit text.
- Literal-to-resource replacement can break title-based preview routing → convert identity coupling first and verify every row/switch and both rail actions in translated UI.
- Locale updates can leave old notification/channel text → synchronize existing channels on app entry and service configuration changes, without altering channel behavior or restarting audio.
- `TextClock` and width measurement may disagree after a locale update → refresh both from the current configuration and test long 12-hour text at maximum appearance settings in all corners.
- English fallback can hide untranslated entries → fail the catalog check on missing translations and audit actual surfaces; runtime safety does not establish completeness.
- Translations may be grammatically correct yet awkward on TV → review complete messages in context, especially Portuguese shared across regions and accessible action wording.
- OEM TVs differ in language availability, notification display, and permission settings → use configured-context tests for unavailable regional variants and record actual TV/API coverage separately from build/static checks.

## Migration Plan

Ship resource extraction, complete catalogs, stable identity changes, and runtime refresh together after verification. No preference/schema migration is needed: all persisted keys and values keep their meaning, and scheduled alarms remain unchanged. English installations retain equivalent wording; supported non-English system configurations automatically show their translation on next entry. If rollback is needed, revert the localization implementation as a unit; unchanged persisted IDs and notification/channel identities remain compatible with the previous app version.

## References

- [Android localization guidance](https://developer.android.com/guide/topics/resources/localization): resource catalogs, complete defaults, context for translators, and device verification.
- [Android language and locale resolution](https://developer.android.com/guide/topics/resources/multilingual-support): regional/ordered locale matching and restricting supported packaged languages.
- [Compose resource access](https://developer.android.com/develop/ui/compose/resources): localized string and plural access.
- [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages): evaluated separately from automatic system localization; deferred picker/configuration options.
- [NotificationManager channel updates](https://developer.android.com/reference/android/app/NotificationManager#createNotificationChannel(android.app.NotificationChannel)): updating channel names when locale changes while retaining channel identity/settings.
