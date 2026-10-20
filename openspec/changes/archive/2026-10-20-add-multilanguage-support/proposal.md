# Proposal

## Why

FloatingClock currently exposes English literals throughout its dashboard, dialogs, accessibility labels, permission feedback, and service notifications. Centralizing these texts and supplying complete translations will make the app usable in English, Spanish, Portuguese, and French and provide a repeatable process for adding languages.

## What Changes

- Follow the Android system language automatically, using native locale/resource matching and English as the fallback when no supported language matches. Regional Spanish, Portuguese, French, and English settings use the corresponding language catalog. No in-app language selector or stored language preference is introduced, as confirmed by the user.
- Move every app-owned user-facing message into Android resources, including dashboard previews, position names, dialog actions, accessibility descriptions, permission/error messages, notifications, and notification channel names. Supply complete English, Spanish, Portuguese, and French catalogs, with documented exceptions for the FloatingClock brand and language-neutral symbols.
- Keep translated labels separate from stable navigation, preview, preference, broadcast, and notification identifiers so changing language cannot change behavior or lose settings.
- Refresh visible UI and active service text after system language changes, preserving clock/alarm preferences, sound, blinking, scheduling, and overlay lifecycle.
- Add repeatable catalog/placeholder checks and a documented workflow for new languages. Validate longer translations with the TV remote and existing clock formats.

## Capabilities

### New Capabilities

- `app-localization`: Automatic Android language selection, English fallback, complete localized app text, runtime refresh, translation completeness, and language-independent behavior.

### Modified Capabilities

- `floating-clock-appearance`: Localized appearance labels and accessible actions, with overlay fitting recalculated for locale-dependent time text while retaining existing control behavior.

## Impact

- Update resources and text consumers in `MainActivity.kt`, `ClockDashboard.kt`, `FloatingClockAppearanceRow.kt`, `FloatingClockSettings.kt`, `FloatingClockService.kt`, and `AlarmSoundService.kt`. Restrict any Gradle changes to resource coverage/verification; no dependency, SDK, or AGP upgrades are planned.
- Preserve existing preferences, position IDs, notification/channel IDs, actions, permissions, foreground service types, and minSdk 21. Android owns permission-dialog and device-settings text; only app-owned feedback is translated.
- Add regression coverage for catalogs, resource resolution, and translated dashboard behavior; retain existing appearance unit tests. Update the English README with language behavior and contributor instructions.
- The ongoing `redesign-tv-settings-dashboard` change preserves the original language within its visual scope. This subsequent change deliberately localizes that implemented dashboard without changing its layout contract or actions; it does not edit or archive that change.

## Non-goals

- An in-app or newly exposed system per-app language picker, accounts/geolocation-based detection, translation services or network calls, regional dialect catalogs, translating project documentation/store listings, alarm/permission fixes, or unrelated dashboard redesign.
