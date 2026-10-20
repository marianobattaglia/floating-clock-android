# app-localization Specification

## Purpose

Make every app-owned user-facing text available in English, Spanish, Portuguese, and French, automatically following the Android system language with English fallback and preserving TV clock and alarm behavior.

## Requirements

### Requirement: Language follows Android system preferences automatically

The app SHALL use the Android system language configuration to select English (`en`), Spanish (`es`), Portuguese (`pt`), or French (`fr`), including regional variants of those languages. On API 21–23 it SHALL use the configured locale; on API 24+ it SHALL follow Android's ordered locale matching. When no supported locale matches, all app-owned natural-language text SHALL fall back to English. Selection SHALL work offline without account, location, network, or extra permissions. The app SHALL NOT offer an in-app language selector, persist a language override, or expose a new per-app language picker in system settings in this change.

#### Scenario: Supported regional language
- **WHEN** the user launches the app with system language `es-AR`, `es-ES`, `pt-BR`, `pt-PT`, `fr-FR`, `fr-CA`, `en-US`, or `en-GB`
- **THEN** app-owned text uses the corresponding supported language consistently
- **AND** no language selection step or network connection is required

#### Scenario: Unsupported language falls back to English
- **WHEN** the system uses only an unsupported language such as German or Japanese
- **THEN** app-owned text is English and all controls remain usable

#### Scenario: Multiple system languages
- **WHEN** a device on API 24+ has ordered language preferences `de-DE,fr-FR`, `es-AR,fr-FR`, or `en-GB,fr-FR`
- **THEN** the app displays French, Spanish, or English respectively according to native matching
- **AND** English is used if no supported preference matches

### Requirement: All app-owned text is translated completely

The app SHALL provide complete, meaningful translations in all four supported languages for dashboard navigation, titles, subtitles, hints, previews, status values, four position names, format descriptions, dialogs, buttons, appearance values/help, permission and failure feedback, foreground notifications, and notification channel names. Accessibility labels for existing interactive controls SHALL use the same language, including semantic descriptions that identify increase/decrease actions and their target. Parameterized messages SHALL retain their complete meaning and dynamic values without requiring English word order. The FloatingClock brand and documented language-neutral symbols/units SHALL remain invariant. System-owned permission dialogs/settings and external text SHALL remain under Android's control.

#### Scenario: Explore every app surface
- **WHEN** a user opens every row, dialog, and preview and starts the overlay and alarm in each supported language
- **THEN** all app-owned natural-language text, including service notification titles/bodies and channel names, uses that language
- **AND** the brand, time values, and language-neutral symbols remain valid

#### Scenario: Accessible adjustment descriptions
- **WHEN** accessibility services inspect the appearance controls in Spanish, Portuguese, or French
- **THEN** each increase/decrease description includes the translated action and target without English fragments

#### Scenario: Render formatted values
- **WHEN** appearance summaries or alarm time values are rendered in any supported language
- **THEN** values, units, punctuation, and any grammatical text form a complete readable message without missing or mismatched substitution arguments

### Requirement: Language changes refresh running presentation safely

After a system language change, the foreground or resumed app SHALL display current localized text, including after Android recreates the activity. Active overlay and alarm services SHALL refresh their app-owned notification text and existing channel names on a locale configuration update; already-created channels SHALL also be synchronized on the next app launch without starting an inactive service. An active clock SHALL refresh locale-sensitive time text and fitting. Refresh SHALL NOT schedule, cancel, start, or stop an alarm or overlay, reset alarm playback/auto-stop timing, change channel preferences, or duplicate a window, receiver, or callback. Services starting without an activity SHALL use the current system language. Stopped services SHALL remain stopped.

#### Scenario: Change language while services are active
- **WHEN** the user changes the system from English to French while the overlay and a ringing alarm are active
- **THEN** after the configuration update their app-owned notifications and existing channel names use French, and the app uses French when resumed
- **AND** the existing sound, blinking, auto-stop deadline, saved values, and single overlay continue without restarting playback or services

#### Scenario: Scheduled alarm fires without an activity
- **WHEN** an alarm is scheduled in English, the system changes to Portuguese, and it fires while the activity is closed
- **THEN** the alarm service displays Portuguese notification text and retains the scheduled trigger behavior

#### Scenario: Change language with services stopped
- **WHEN** language changes while both services are stopped and the app next opens
- **THEN** UI and any already-created notification channel names use the current language
- **AND** neither service is started to perform translation refresh

#### Scenario: Repeated configuration and lifecycle changes
- **WHEN** the app and services experience repeated locale updates, activity recreation, and enable/disable cycles on API 21+ including notification channels on API 26+
- **THEN** no removed view is updated and stopping a service releases its window, callbacks, and receivers as before

### Requirement: Translation preserves existing settings and TV interaction

Translation SHALL preserve all existing preference keys/values, selected corners, alarm trigger times, explicit 12/24-hour and seconds choices, value bounds, service actions, and permission behavior. English fallback SHALL NOT force US regional conventions, a different time zone, or a new time format. Navigation, focus selection, icons, preview association, and actions SHALL remain independent of displayed wording. D-pad, Select, visible focus, dialog dismissal/focus return, and screen-level Back SHALL remain usable in every language at 720p and 1080p. Longer translations SHALL NOT hide required controls or their meaning.

#### Scenario: Navigate a translated dashboard
- **WHEN** the user moves between each row, its switch, and Home/Settings shortcuts in any supported language and presses Select
- **THEN** the correct preview, icon, and original action remain associated with each control
- **AND** passive focus movement does not toggle settings, open dialogs, or dismiss alarms

#### Scenario: Upgrade and change language
- **WHEN** an existing installation with saved position, format, seconds, appearance, and alarm settings upgrades or changes language
- **THEN** the same settings and scheduled alarm remain valid without preference migration
- **AND** explicit clock-format choices remain authoritative and alarm entry stays in its existing 24-hour format

#### Scenario: Use long translations with the remote
- **WHEN** the user opens each dialog in French or Portuguese on a 720p or 1080p TV and navigates using D-pad and Select
- **THEN** actionable labels, values, and focus indicators are readable and reachable
- **AND** Back and existing close actions preserve save behavior and focus return without extra navigation steps

#### Scenario: Denied or unavailable permissions
- **WHEN** overlay permission is denied on API 23+, notifications are denied on API 33+, or exact-alarm access/settings are unavailable on API 31+
- **THEN** any feedback supplied by the app uses the selected language
- **AND** the existing permission/scheduling behavior is preserved without a new permission request, settings flow, or translation-triggered service start

### Requirement: Supported translation catalogs remain complete as the app grows

Every app-owned translatable message SHALL have a non-empty entry in English and every supported translated catalog. A repeatable automated check SHALL fail on missing or orphaned keys, incompatible placeholder signatures, and incomplete plural forms if introduced. Exceptions for invariant content SHALL be explicit and limited; ordinary English prose SHALL NOT be exempted to bypass translation. Adding a supported language SHALL include complete translations and the same validation without changing existing feature behavior. The English runtime fallback SHALL remain available as a safety net, but SHALL NOT count as a completed translation.

#### Scenario: Add a new user message
- **WHEN** a contributor adds an English message and omits its French translation or changes a substitution type in Portuguese
- **THEN** automated verification reports the affected message/catalog and fails before the change is accepted

#### Scenario: Extend language coverage
- **WHEN** a contributor adds another supported language with a complete catalog
- **THEN** the same completeness checks cover that language and its system locale can resolve the new translation without changes to existing clock/alarm behavior
