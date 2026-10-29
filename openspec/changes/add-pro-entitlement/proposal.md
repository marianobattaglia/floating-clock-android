# Proposal

## Why

FloatingClock needs a sustainable freemium model that keeps the core app usable for free while offering a clear, permanent PRO upgrade. The app currently has no purchase or entitlement system, so users need a reliable way to see their access level, buy and restore PRO, and exercise both access states during development.

## What Changes

- Add a PRO screen that displays Free or PRO and offers a one-time, non-consumable Google Play purchase when the user is Free.
- Gate the existing floating-clock appearance controls behind PRO; keep position, format, seconds, overlay, and other current core features free.
- Limit Free users to one successful alarm activation per rolling 24 hours; allow PRO users to activate the existing single alarm without that cooldown. Alarm cancellation remains available at all times.
- Add a Debug destination and local Free/PRO override only in debug builds, so developers can test entitlement behavior without a Play purchase. Release builds must not expose or honor this override.
- Provide a separate, release-only review access code so Google Play reviewers can inspect PRO features without making a purchase; this code must not create or represent a Play purchase.
- Replace the unused Settings shortcut in the left rail with PRO. Keep Home, and show Debug in the rail only in debug builds.
- Keep the app free to install. Do not add subscriptions, Google sign-in, Firebase, a custom backend, or ads in this change. Ad removal may become a PRO benefit only after an ad provider is selected and ads are integrated.

## Capabilities

### New Capabilities

- `pro-access`: Show and restore the user's Free/PRO entitlement, purchase permanent PRO through Google Play, and provide a debug-only entitlement override.
- Reviewer access is a separate local grant for policy review, configured outside the repository and supplied through Play Console's app access instructions; it does not add an app account or backend.
- `alarm-usage-limits`: Enforce the Free alarm activation cooldown while preserving cancellation and the existing single-alarm behavior.

### Modified Capabilities

- `floating-clock-appearance`: Require PRO to access and change the three existing appearance controls while preserving their ranges, persistence, overlay behavior, and accessibility.
- `tv-settings-dashboard`: Replace Settings with PRO in the navigation rail, conditionally add Debug in debug builds, and retain TV remote navigation behavior.

## Impact

- Likely code areas: `MainActivity.kt`, `ClockDashboard.kt`, `AlarmScheduler.kt`, `FloatingClockSettings.kt`, localized strings, and `gradle/libs.versions.toml` / `app/build.gradle.kts` for Google Play Billing.
- External setup required before production sales: configure the one-time PRO product in Play Console and test it through a Play test track. The app will launch Google's in-app billing flow; it will not use a generic Play Store redirect as the purchase mechanism.
- No Firebase project, API key, service-account credential, or backend is required. The Play product ID is a public identifier, not a secret. Client-only billing avoids backend hosting but provides less purchase-fraud and refund protection than server-side verification.
- The reviewer access code is generated and kept outside source control; release builds contain only its SHA-256 verifier. Because review access is client-side, this is an operational review path rather than tamper-proof entitlement security.
- Free alarm cooldown and debug override are local app state. They are usability and QA controls, not server-enforced anti-tampering guarantees.
- All new user-facing text must support English, Spanish, Portuguese, and French, with D-pad, Select, visible focus, and Back behavior suitable for Android TV and minSdk 21.
