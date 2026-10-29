# Design

## Context

The app is a single-module Android TV application with `minSdk 21`. `MainActivity` owns the settings composables, `ClockDashboard` renders the current Home dashboard and left rail, `AlarmScheduler` reports exact, approximate, or failed scheduling, and `FloatingClockSettings` defines existing local preferences. There is no billing dependency, app account, Firebase project, or backend. See `proposal.md` for the product scope and the change specs for user-visible contracts.

## Goals / Non-Goals

**Goals:**
- Keep the app free to install and preserve API 21 compatibility.
- Make PRO ownership, Free alarm limits, and debug testing flow through one entitlement state.
- Keep purchase secrets and signing credentials out of the app source and repository.

**Non-Goals:**
- Google sign-in, Firebase user records, feature flags, server-side purchase verification, subscriptions, ad integration, and cross-platform account linking.
- Adding multiple or repeating alarms.

## Decisions

### Use Google Play Billing for a permanent, non-consumable entitlement

Use an in-app purchase flow for the public product ID `pro_lifetime`. The PRO screen obtains localized product details from Google Play and starts the Play purchase UI from the user's action. On app start, foreground return, and purchase callback, query owned purchases; grant PRO only for the completed product purchase, acknowledge it, and keep the last confirmed entitlement locally as a cache during temporary Play service failures. A successful query reconciles the cache. Do not store the user's email, name, Google account ID, or purchase token as an app account. Do not implement a generic Play Store redirect for in-app purchase.

The integration is client-only to avoid maintaining a backend. Google recommends backend purchase processing for stronger security, so this choice accepts weaker protection against modified clients and delayed recognition of refunds/revocations. Re-query owned purchases whenever Play Billing is available. If the product is later used to protect substantial value, add server-side verification before broadening the entitlement model.

### Preserve minSdk 21 with the compatible Billing Library release

Do not raise `minSdk`. The build showed that Billing Library 8.2.1 declares API 23. Google Play's release notes identify API 21 support through 8.0.0 and raise the minimum to 23 in 8.1.0. Use Billing 8.0.0, which also includes automatic Billing service reconnection, and revisit the library/API-level trade-off before the Billing 8 update acceptance deadline of August 31, 2027. Google Play's current release notes and deprecation schedule are the authority for the version used at implementation time. See [Billing Library release notes](https://developer.android.com/google/play/billing/release-notes) and [version deprecation schedule](https://developer.android.com/google/play/billing/deprecation-faq).

### Separate actual ownership from the debug override

Represent actual Play ownership and the effective feature-gating state separately. In debug builds, a local override selects effective Free or PRO for QA; it never creates or changes an actual purchase. Guard both the Debug destination and all override reads/writes with the debug build flag. Release builds always use the Play entitlement, even if the same installation has a stale debug preference.

### Provide a separate review access code in configured release builds

Google Play review must be able to inspect PRO features without asking reviewers to purchase them. Add a release-only code entry point on the PRO screen when a code is configured. The code grants a local review entitlement for feature access; it SHALL NOT change the actual Play purchase state, acknowledgement state, or cached purchase ownership. Debug builds continue to use the existing debug override.

Keep the plaintext code in the ignored project-local `local.properties` file (or a secure CI secret store), never in source, README, or version control. The build computes SHA-256 and embeds only the digest as a release `BuildConfig` value. Generate a high-entropy code for each review build. Save the grant separately from Play entitlement and bind it to the configured digest so rotating the code in a later build invalidates grants made with the previous code. If no code is configured, hide the release entry point. The PRO screen SHALL identify that review access is active and does not represent a purchase. Supply the code and short English steps in Play Console's app access instructions.

This is a client-side review affordance, not a secure entitlement system: a modified APK can still bypass local feature checks, and anyone who obtains the private review instructions can use the code. Rotating the code requires building and submitting a new release; no server or immediate revocation is provided.

### Apply entitlement at feature boundaries

Keep appearance controls and the alarm's scheduling path as the points where access is checked, rather than duplicating paywall logic across the overlay service. Show the appearance row with a PRO indicator when the overlay is enabled; selecting it while Free routes to the PRO screen, while PRO opens the existing modal. The overlay renderer continues reading the same appearance preferences and is not coupled to Billing.

For Free alarm use, persist the time of the last successful disabled-to-scheduled activation in app-private preferences. Update it only after `AlarmScheduler` reports exact or approximate success. Failed/denied schedules do not consume the allowance. A user can always cancel or dismiss; cancellation does not clear the timestamp. Replacing the time of an alarm that remains enabled replaces the same scheduled alarm without resetting the original cooldown. The existing single-alarm model and API 21–30 versus API 31+ permission behavior remain in force.

### Keep configuration values separate from credentials

The product ID is a public Play Console identifier and may be kept as ordinary app configuration; it is not a secret. Play Billing does not require an API key or service-account credential embedded in the APK. Signing keys remain local or in a secure CI secret store and must not be committed. No Firebase Console setup is needed for this scope. Play Console product configuration, pricing, license testers, and an internal test track are deployment prerequisites for real purchase-flow tests.

### Preserve TV navigation and localization

Keep Home as the dashboard route and add PRO plus a debug-only Debug route in the rail. Back from PRO or Debug returns to Home; Back from Home and dialog behavior remain unchanged. New labels, status, cooldown feedback, purchase errors, and focus/accessibility descriptions must be added to all four existing string catalogs. Use D-pad, Select, focus restoration, and visible focus in each destination.

## Risks / Trade-offs

- **Client-only entitlement is easier to tamper with and cannot receive real-time revocation events →** Re-query purchases on app start/foreground return, confirm only completed ownership, acknowledge purchases, and document the limitation; add a backend only if the value/risk later justifies its cost.
- **A local alarm cooldown can be bypassed by clearing app data or changing the device clock →** Treat it as a light freemium limit, not a fraud-proof server rule. A future backend is needed for stronger enforcement.
- **A purchase dependency may be unavailable on devices or installs outside Google Play →** Keep free features usable, show an accessible localized explanation, and test the unavailability path.
- **Billing 8.0.0 is not the newest library and has a future support deadline →** Keep its version centralized in the catalog and revisit before August 31, 2027; a later library may require changing the minimum supported API to 23.
- **A release build could inherit local debug preferences →** Ignore debug keys entirely unless the build is debug, and verify both variants.
- **A reviewer code gives local PRO access to anyone who obtains it →** Keep a high-entropy plaintext code outside the repository, share it only through Play Console review instructions, store grants separately from purchases, and rotate the code with a new release when needed. This does not replace server-side entitlement verification.

## Migration Plan

1. Add only new entitlement and alarm-cooldown preference keys; leave all existing clock/alarm keys and defaults intact.
2. Add the Billing dependency through the version catalog and verify the merged manifest and API 21 build path.
3. Configure `pro_lifetime` in Play Console and test a real purchase with an internal test track/license tester before enabling it to production users.
4. Roll out after purchase restoration, pending/canceled flows, debug override isolation, alarm permission-denied cases, and Android TV remote navigation have been checked.
5. If the purchase UI must be temporarily disabled, preserve purchase querying and PRO access for existing owners; disable new purchase initiation without discarding entitlement state.

## Open Questions

- The final price and available regions will be chosen in Play Console. The app SHALL display the localized price returned by Google Play rather than hard-coding a currency amount.
