# Design

## Context

See `proposal.md` for motivation and `specs/phone-experience/spec.md` for the behavior contract. The manifest currently requires `android.software.leanback`, declares touch optional, and already provides both standard and Leanback launcher categories plus the TV banner. `MainActivity` currently renders only the TV dashboard, whose layout and controls use Compose for TV. Clock and alarm preferences, the floating overlay service, alarm scheduler, and Play Billing entitlement are app-wide and do not require a separate account or backend.

The current minimum SDK is 21. Permission behavior differs by Android version: overlay access starts at API 23, exact-alarm access is checked from API 31, and the existing notification permission flow applies from API 33.

## Goals / Non-Goals

**Goals:**

- Add a phone-specific, touch-first interface with the existing app features and shared local/Play state.
- Preserve the current TV dashboard tree, visual layout, D-pad navigation, focus behavior, and existing actions.
- Keep the existing minSdk 21 declaration and API guards, and avoid preference migration or a second app identity. Manual device validation for this change is scoped to API 37; lower API compatibility is not claimed as device-tested.

**Non-Goals:**

- Redesigning the TV dashboard or changing its controls, visual dimensions, focus graph, or remote behavior.
- Adding a separate phone-only package, backend, account system, new ad system, or different Free/PRO rules.
- Designing tablet, Chromebook, foldable-specific, or Android XR experiences beyond layouts that naturally adapt to phone window sizes.

## Decisions

### Declare dual phone and TV availability in one app

Set the Leanback feature to optional while keeping touchscreen optional, both existing launcher categories, and the TV banner. Keep the same application ID and release artifact. This allows the app to run on phones for validation and direct use without removing its Android TV identity. Google Play distribution will be configured for Android TV only; phone form factors will not be released. A separate app ID or product flavor would split installs and Play purchase ownership without a requirement for separate binaries.

### Route by device form factor, not by screen width

Choose the TV presentation when the device reports `PackageManager.FEATURE_LEANBACK`; choose the phone presentation otherwise. Large phones must not accidentally receive the TV rail layout, and Android TV must never receive the compact touch layout solely because of a window-size calculation. The phone presentation may adapt its own content to portrait and landscape window sizes.

### Keep the existing TV presentation as its own path

Retain `ClockDashboard` and its TV Material controls as the Android TV renderer. Add a phone renderer using touch-oriented Compose controls and layouts. Do not replace the TV Material theme or globally restyle shared components as part of this change. Add any regular Compose UI dependency through the version catalog and existing Compose BOM, leaving the TV dependencies in place.

An alternative was to make the current two-column dashboard responsive on narrow screens. That would couple phone layout decisions to the TV focus and layout tree, increasing the chance of changing the TV experience. Separate rendering paths give each form factor a clearer contract.

### Share behavior and state, not duplicated feature rules

Both renderers use the same preferences, overlay service, scheduler, entitlement manager, product ID, and debug/reviewer access boundaries. Extract only state transitions and operations that both renderers need, especially alarm activation, permission return handling, cooldown accounting, and overlay enablement. Keep the current TV controls and defaults while wiring them to any shared operations. Do not duplicate Free/PRO checks or purchase ownership logic in the phone UI.

### Retain existing services and API-specific permission flows

Reuse `FloatingClockService`, `AlarmScheduler`, and `AlarmSoundService`. Do not add a phone-only alarm or overlay implementation unless phone-device verification demonstrates a platform-specific failure. Exercise the existing overlay access screens, notification permission, exact-alarm access, foreground services, display fitting, and orientation changes on phones. Preserve the current API 21–30 scheduling behavior and the API 31+ exact/inexact scheduling path.

### Treat Play Console targeting and screenshots as a separate publishing step

Phone screenshots come from the actual phone presentation and are retained as validation assets; existing TV screenshots remain TV assets. They are not a phone listing requirement because Play distribution is TV-only. Play Console form-factor configuration is a publishing step outside the app code and must select Android TV only.

## Risks / Trade-offs

- **The optional Leanback declaration makes the package eligible for phones →** Keep phone support in the app, but configure the Play release to target Android TV only.
- **Sharing state across two renderers can regress the TV dashboard →** Keep the TV renderer and controls intact, extract only shared behavior, and verify the current TV flows after each shared-state change.
- **Overlay permission screens, background execution, and notification behavior vary across phone OS versions and manufacturers →** Validate the current API 37 paths in this change; retain API guards and minSdk 21, and treat older API behavior as unverified by device testing.
- **TV behavior may vary by display resolution →** Validate the user's API 37 Android TV device at its tested resolution; 720p/1080p matrix coverage is outside this change's scope.

## Migration Plan

1. Implement the phone renderer and shared feature operations without changing persisted preference keys, defaults, product ID, or entitlement storage.
2. Update the manifest for dual availability in the same change as the phone route.
3. Build and lint the app; install it on a phone and TV running API 37. Verify phone permissions and features, then verify TV remote, overlay, alarm, Free/PRO, and debug/reviewer-access behavior on the tested TV device.
4. Keep authentic phone screenshots as QA assets, separate from TV store resources.
5. Configure Google Play distribution for Android TV only. Phone support in the package does not enable phone form-factor distribution.

No persisted-data migration is expected.
