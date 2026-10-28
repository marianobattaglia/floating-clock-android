# Proposal

## Why

The current app is declared as Android TV-only and its dashboard is designed for a remote and a wide screen, so it cannot currently provide a genuine phone experience or phone screenshots for the Play listing. Add phone support as a separate presentation path while keeping the existing Android TV interface and behavior intact.

## What Changes

- Keep the app usable on phones and Android TV while preserving its TV launcher entry, banner, and D-pad support; configure Google Play distribution for Android TV only.
- Add a touch-friendly, responsive phone interface for the existing clock, alarm, PRO, and debug-build controls, including portrait and landscape layouts.
- Reuse the existing preferences, overlay service, alarm scheduling, Play Billing entitlement, reviewer access, and Free/PRO rules across both form factors.
- Route Android TV devices to the existing dashboard unchanged; phone-specific layout and interaction work must not replace or restyle that dashboard.
- Retain authentic phone screenshots as validation assets. The phone interface is not a Play distribution target; Play Console distribution is limited to Android TV.

## Capabilities

### New Capabilities

- `phone-experience`: Install and use FloatingClock on Android phones with a phone-appropriate interface and the existing app functionality.

### Modified Capabilities

- None. Existing Android TV, alarm, appearance, localization, and PRO behavior remains the contract; this change adds a phone experience.

## Impact

- Android manifest form-factor declarations and launcher metadata.
- `MainActivity` presentation routing and a new phone-specific Compose interface; current TV dashboard and TV controls remain intact.
- Shared app state and interaction boundaries for clock settings, overlay permissions, alarms, PRO/Billing, reviewer access, and debug-only overrides.
- Compose UI dependencies and localized phone-facing text where needed.
- Verification on a phone and Android TV running API 37, plus authentic phone screenshots as validation assets. Older API levels and additional TV resolutions are outside this change's test scope.
