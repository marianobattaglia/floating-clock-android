# Proposal

> **Estado: diferido.** Este cambio describe una fase posterior y no forma parte de la primera versión de FloatingClock.

## Why

FloatingClock no necesita una cuenta para su primera versión. La decisión de producto es lanzar una versión gratuita con anuncios, sin inicio de sesión ni suscripciones. Más adelante, si necesitamos sincronizar datos o asociar beneficios entre una cuenta de FloatingClock y compras de Google Play, un perfil Google opcional podrá proporcionar esa identidad. Google Play Billing seguirá siendo la fuente de verdad de las compras; iniciar sesión no será un requisito para usar la app ni para consultar una compra de Play.

## What Changes

Cuando se reactive esta fase futura:

- Add a **Profile** destination to the Android TV side rail, with Google sign-in, account details, subscription status, and sign-out.
- Associate the Firebase user ID, email, available given name and family name, and first-registration timestamp with the app account. Re-evaluate the least-cost storage option before choosing a Firebase product or creating a profile document.
- Show **Free** by default; allow **Active** only when supplied by a trusted entitlement source. Purchase and subscription verification are outside this change.
- Preserve remote navigation and all existing clock and alarm flows when signed out, offline, or when sign-in is unavailable.
- Provide in-app account and data deletion plus an external deletion request path before enabling account creation on Google Play.
- Disclose account data collection and provide an in-app privacy policy path before enabling account creation.
- Add translated profile and account-flow text to the existing English, Spanish, Portuguese, and French catalogs.

### Release boundary

- **First version:** free with ads; no Google sign-in, account profile, subscriptions, or account data collection.
- **Later monetization phase:** subscriptions are intended to remove ads and offer enhanced features. Google Play Billing, entitlement handling, ad suppression, and subscription lifecycle support belong in a separate OpenSpec change and must be tested before subscriptions are offered publicly.
- **Later account phase:** resume this change only if an app account provides a concrete benefit, such as cross-device data or account-linked entitlements. Authentication remains optional.

## Capabilities

### New Capabilities

- `google-account-profile`: Optional Google authentication, stored account profile, subscription label, sign-out, and account deletion.

### Modified Capabilities

- `tv-settings-dashboard`: Add Profile to the existing left rail while preserving the current Home and Settings shortcuts and remote behavior.

## Impact

If resumed, this change will affect `ClockDashboard.kt` and `MainActivity.kt`; the selected authentication/storage dependencies and configuration; localized string catalogs; account deletion and privacy documentation; and Google Play Data safety/deletion setup. No code, Firebase project, account storage, advertising SDK, or purchase flow is added by this deferred change. Existing settings, persisted preferences, overlay and alarm services remain independent of any future account state.
