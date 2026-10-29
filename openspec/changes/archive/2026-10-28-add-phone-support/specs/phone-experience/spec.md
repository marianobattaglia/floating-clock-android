# Spec Delta

## Purpose

Provides a phone-appropriate way to use FloatingClock while retaining its existing clock, alarm, appearance, and PRO capabilities. Android TV continues to use its current remote-first dashboard and behavior. The app's Google Play distribution is limited to Android TV; phone support remains available for validation and direct use.

## ADDED Requirements

### Requirement: Phones and Android TV can install and launch the app
The app SHALL be installable and launchable on supported Android phones without increasing the existing minimum API level. It SHALL remain discoverable and launchable from the Android TV home screen, including its existing TV banner and Leanback launcher entry. Phone support SHALL NOT make touchscreen hardware mandatory for installation on TV.

#### Scenario: Launch on a phone and a TV
- **WHEN** the same app release is installed on a supported Android phone and an Android TV device
- **THEN** each device SHALL offer the app through its standard launcher
- **AND** the TV SHALL continue to expose the existing Android TV launch point

### Requirement: Phone interface supports touch and compact layouts
On phones, the app SHALL present its clock, alarm, and PRO controls in a layout usable on compact screens in portrait and landscape orientations. All existing settings and actions SHALL be reachable by touch, with visible labels and touch target areas of at least 48 dp. Content SHALL adapt or scroll without horizontal clipping, including when system bars, display cutouts, text scaling, or the software keyboard reduce available space. Phone-owned text SHALL use the existing supported languages and English fallback.

#### Scenario: Use the app in portrait and landscape
- **WHEN** a user opens the app on a phone and changes between portrait and landscape orientations
- **THEN** the phone layout SHALL adapt while keeping all settings, actions, and dialog buttons reachable
- **AND** orientation changes SHALL NOT reset preferences or alter a scheduled alarm or active overlay

#### Scenario: Use translated text and enlarged system text
- **WHEN** a phone uses English, Spanish, Portuguese, or French, including a supported regional locale, and the user has enlarged system text
- **THEN** phone controls and feedback SHALL use the selected supported language or English fallback
- **AND** longer labels SHALL remain readable and actionable without being clipped

### Requirement: Phone controls preserve the app's existing feature and entitlement behavior
The phone interface SHALL expose the existing floating-clock controls, alarm, PRO destination, and applicable build-only access controls. The same saved preferences, Free and PRO gates, alarm allowance, Play purchase and restore behavior, and release reviewer access SHALL apply on phone and Android TV. Debug entitlement controls SHALL remain available only in debug builds. Phone-specific presentation SHALL NOT change or migrate existing settings or purchase state.

#### Scenario: Use Free and PRO features on a phone
- **WHEN** a Free or PRO user opens the phone interface and uses clock appearance, alarm, or PRO controls
- **THEN** the same access rules and results SHALL apply as on Android TV
- **AND** local settings SHALL remain intact, and PRO SHALL be restored when the phone's Google Play account owns the purchase

#### Scenario: Use build-specific access controls on a phone
- **WHEN** a debug or release build is opened on a phone
- **THEN** debug entitlement overrides SHALL appear only in the debug build
- **AND** configured reviewer access SHALL remain a release-only review path separate from Play purchase ownership

### Requirement: Phone overlay and alarm retain permission and lifecycle behavior
The floating clock and alarm SHALL use the existing API-aware permission and scheduling behavior on phones from API 21 onward. A denied or unavailable overlay, notification, or exact-alarm permission SHALL NOT create a false enabled state or crash. The overlay SHALL remain within the available phone display area as its orientation or display configuration changes. Recreating the phone activity or cycling the overlay SHALL NOT create duplicate windows, listeners, receivers, or scheduled alarms.

#### Scenario: Deny phone permissions
- **WHEN** a user denies overlay access on API 23 or later, notification access when requested on API 33 or later, or exact-alarm access on API 31 or later
- **THEN** the app SHALL retain the existing denial or approximate-scheduling behavior
- **AND** the clock or alarm SHALL be reported as active only when the existing service or scheduler confirms success

#### Scenario: Rotate the phone with the overlay enabled
- **WHEN** the phone changes orientation or available display bounds while the floating clock is active
- **THEN** the clock SHALL be repositioned and fitted within the updated display area
- **AND** the service SHALL retain one overlay and the selected settings without restarting unrelated alarm behavior

### Requirement: Android TV presentation and remote behavior remain unchanged
Android TV devices SHALL continue to use the existing TV dashboard, TV controls, visual arrangement, and feature actions. The phone interface SHALL NOT be shown on TV. D-pad, Select, visible focus, and Back behavior SHALL remain available throughout Home, PRO, Debug when present, and existing dialogs.

#### Scenario: Navigate the existing TV dashboard after phone support is added
- **WHEN** a user opens the app on Android TV and navigates the current features with a remote
- **THEN** the existing TV dashboard and its feature behavior SHALL remain unchanged
- **AND** Home, PRO, Debug when present, settings, dialogs, D-pad focus, and Back SHALL remain usable at supported TV sizes

### Requirement: Play distribution is limited to Android TV
The Google Play release for this app SHALL target Android TV only. The phone interface and its screenshots support implementation validation and direct use; they SHALL NOT be treated as a phone form-factor release or phone listing requirement.

#### Scenario: Prepare a Play release
- **WHEN** the release form factors are configured in Play Console
- **THEN** Android TV SHALL be selected for distribution
- **AND** phone form factors SHALL remain excluded from distribution
