# Design

## Context

See [proposal.md](proposal.md) for the release decision and motivation. FloatingClock is a single-module Android TV app with Kotlin, Compose for TV, a floating clock overlay, and a background alarm. It currently uses `minSdk 21` and has no authentication or purchase backend.

This account-profile change is a later phase, not a prerequisite for the first release. The app will first launch free with ads, without subscriptions, sign-in, or collection of account data. The product direction for a later phase is to offer subscriptions that remove ads and unlock enhanced features.

## Goals / Non-Goals

**Goals when this change is resumed:**

- Preserve the requested optional Google account profile and its account details.
- Keep account state independent from clock, overlay, alarm, and saved settings.
- Re-evaluate the least-cost data and authentication architecture before configuring an external service.
- Treat Google Play purchase state as the source of subscription entitlement.

**Non-Goals for the first release and this deferred change:**

- Adding Google sign-in, profile storage, or Firebase configuration to the first release.
- Selling subscriptions, validating purchases, hiding ads, or granting premium features. Those belong in a separate monetization change.
- Requiring an app account to use FloatingClock or to recognize a Google Play purchase.
- Choosing or operating a custom backend before account and subscription requirements are confirmed.

## Decisions

### 1. Separate launch, billing, and account phases

The first version is free with ads and has no account or subscription flow. If subscriptions are added later, create a separate OpenSpec change for Google Play Billing, subscription lifecycle, entitlement behavior, ad removal, and premium features. Test purchases in Play Console test tracks before offering subscriptions publicly; a production release is not required for that testing.

This account-profile change remains available for a later phase. Resume it only if an app account provides a concrete benefit, such as account-linked data or cross-device features. A Firebase account is not required just to query purchases made with the device's Google Play account.

### 2. Re-evaluate authentication and storage before implementation

Firebase Authentication remains a candidate for the requested stable Firebase UID and Google profile. It exposes basic profile information and a server-derived account creation timestamp. Avoid creating a Firestore document solely to duplicate fields already available through Authentication. If separate given/family names or other app-owned profile data are still required, compare the current Firebase options, quotas, and expected usage before choosing a storage service. Do not configure Firebase or promise a no-cost operating profile as part of the first release.

Any future account storage must be user-scoped and must not let an Android client grant subscription entitlement. Keep service-account credentials, private signing keys, and other secrets off the device and out of the repository.

### 3. Keep entitlement separate from identity

The future Profile may display Free or a trusted active entitlement, but a mutable client field is never proof of payment. Google Play Billing owns the purchase lifecycle; ad suppression and premium feature access follow the verified entitlement. Account sign-in and the Google Play billing account may differ, so do not infer purchase ownership from the Firebase UID or email. If a future design needs to associate a purchase with an app account, use Google's supported obfuscated account identifier and validate the association.

The first subscription implementation must decide whether local Play Billing state is sufficient for the product's risk level or whether a secure backend should verify purchase tokens and receive lifecycle notifications. Compare expected operating cost before choosing; never embed backend credentials in the app.

### 4. Re-check Android TV sign-in compatibility when resumed

Keep the app's `minSdk 21` unless a separate product decision changes it. Current Firebase Auth and Firestore Android SDKs require API 23 or higher, and Google's documented Credential Manager flow for Android TV requires API 34 and a compatible Google Play services version. Hiding sign-in on older devices alone does not establish that these dependencies can be added while preserving `minSdk 21`.

When the account phase is resumed, verify dependency manifest requirements and select a sign-in approach that fits the supported TV versions. Do not raise the app's minimum SDK or add a second OAuth flow without updating the proposal and compatibility requirements first.

References: [Firebase Android release notes](https://firebase.google.com/support/release-notes/android), [Credential Manager across form factors](https://developer.android.com/identity/form-factors), [OAuth for TV and limited-input devices](https://developers.google.com/identity/protocols/oauth2/limited-input-device).

### 5. Apply account privacy and deletion requirements only when account creation is enabled

Before enabling account creation in a release, provide the in-app deletion flow, an external deletion request path, a privacy policy, and the required Google Play disclosures. These are prerequisites for the future account release, not for the first anonymous release.

Reference: [Google Play account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111).

## Risks / Trade-offs

- **An account may not add value when Play Billing can restore a purchase.** → Revisit whether cross-device or account-linked behavior is needed before implementing this change.
- **Firebase pricing, quotas, and SDK requirements can change before this work resumes.** → Recheck current terms and dependency manifests before selecting a service or configuring a project.
- **The documented Android TV sign-in path has a higher API requirement than the app's `minSdk`.** → Preserve anonymous app use and decide supported sign-in versions explicitly before implementation.
- **A client-only entitlement check offers less protection against tampered clients than server verification.** → Decide the acceptable risk and operating cost in the separate Billing proposal; never let a profile field grant paid status.
- **Creating app accounts triggers privacy, deletion, and Play Console obligations.** → Complete those release prerequisites before enabling sign-up.

## Migration Plan

1. **First release:** ship the free ad-supported app without login, subscriptions, Firebase profile storage, or account data collection. Keep the existing minimum SDK and anonymous clock/alarm flows.
2. **Monetization phase:** create a separate OpenSpec change for subscription products, Play Billing integration and tests, entitlement state transitions, ad removal, and enhanced features. Select client-only or backend verification after comparing risk and cost.
3. **Account phase, if needed:** resume this change, re-evaluate sign-in compatibility and data storage, then implement the Profile, account lifecycle, privacy, and deletion requirements.
4. **Release checks for account creation:** test remote navigation, supported and unsupported TV versions, sign-in and sign-out, account deletion, and ensure account actions never alter clock or alarm state.

## Open Questions

- Will subscriptions require an account-specific benefit beyond restoring a Google Play purchase?
- Which Android TV API levels must support sign-in, given the app's `minSdk 21`?
- Are the originally requested separate given/family names needed, or are Firebase Auth's available profile fields and creation timestamp sufficient?
- What purchase-verification approach and operating-cost ceiling are acceptable for the future Billing phase?
- If account creation is later enabled, which public deletion URL and support address will be used?
