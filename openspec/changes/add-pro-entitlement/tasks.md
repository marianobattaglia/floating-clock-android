# Tasks

## 1. Billing and entitlement foundation

- [x] 1.1 Add the selected Google Play Billing Library release through `gradle/libs.versions.toml` and `app/build.gradle.kts` without changing `minSdk 21`; verify the dependency resolves, the merged manifest includes the billing permission, and `bash ./gradlew :app:assembleDebug` succeeds.
- [x] 1.2 Implement Play ownership, effective Free/PRO state, purchase-query lifecycle, and a local cache of the last confirmed entitlement. Real restore and temporary Billing outage behavior is covered by task 4.2.
- [x] 1.3 Implement the `pro_lifetime` in-app purchase flow, pending/canceled/error handling, acknowledgement, and Play-localized price display. Real purchase behavior is covered by task 4.2.
- [x] 1.4 Add only new app-private preference keys and preserve existing clock, alarm, and appearance preferences.

## 2. PRO gates and alarm allowance

- [x] 2.1 Gate the existing appearance row and modal using effective entitlement, keeping the existing three controls, values, persistence, and overlay rendering. Device interaction checks are covered by task 4.4.
- [x] 2.2 Enforce the Free rolling 24-hour allowance only after successful exact or approximate scheduling, and keep cancellation and ringing dismissal available. Device interaction checks are covered by task 4.4.
- [x] 2.3 Add localized benefit, locked-feature, cooldown, Billing status/error, and purchase feedback strings to English, Spanish, Portuguese, and French; verify catalog completeness and placeholders with `bash ./gradlew :app:verifyTranslations`.
- [x] 2.4 Update README feature, navigation, alarm-limit, and PRO purchase guidance to match the implementation.

## 3. Navigation and debug controls

- [x] 3.1 Replace the unused Settings rail item with PRO, keep all current settings on Home, add a Debug route only to debug builds, and route Back from secondary destinations to Home. D-pad interaction checks are covered by task 4.4.
- [x] 3.2 Add a debug-only local Free/PRO override isolated from actual Billing ownership. Build-variant checks are covered by task 4.3.
- [x] 3.3 Add a release-only PRO review-code path. Read the plaintext code from ignored local configuration or a secure CI secret, embed only its SHA-256 verifier in the release, provide localized D-pad-accessible entry and invalid/success feedback, persist the review grant separately from Play ownership, and invalidate it when the verifier changes.
- [x] 3.4 Verify focus restoration, visible focus, labels, translated copy, and 720p/1080p layout for Home, PRO, Debug, dialogs, purchase errors, reviewer-code entry, and the cooldown-disabled action using an Android TV emulator or device.

## 4. Play Console and integration verification

- [ ] 4.1 Create the non-consumable `pro_lifetime` product, set its price and regions, add license testers, and publish an internal test track; verify the product details load from Play for the intended package name.
- [ ] 4.2 Test real purchase, pending/canceled purchase, restoration on a second install using the purchasing Play account, and Billing-unavailable behavior through the internal test track; verify entitlement follows Play ownership and no Google sign-in or backend is required.
- [x] 4.3 Run `bash ./gradlew :app:assembleDebug`, `bash ./gradlew :app:assembleRelease`, and `bash ./gradlew :app:lintDebug`; verify release excludes debug controls.
- [x] 4.4 On Android TV, verify overlay enable/disable, Free and PRO appearance access, Free alarm cooldown, PRO alarm reuse, cancellation while scheduled and ringing, exact-alarm permission granted/denied behavior on API 31+, reviewer-code success/failure and separation from Play ownership, and D-pad/Back navigation; record manual device results separately from build/lint results.
- [x] 4.5 Recheck the official Play Billing version acceptance deadline and minSdk compatibility before release; verify the chosen library version remains accepted by Play Console and compatible with API 21, or update the product's minimum API decision before publication.
- [x] 4.6 Install and launch debug and release builds on API 21/22 devices or emulators to confirm the minimum supported API remains functional.
- [ ] 4.7 Add the current release's English reviewer-code steps in Play Console App access and verify the provided code unlocks all PRO-gated features in the reviewed build; update the instructions whenever the release code is rotated.

> **Estado informado:** El usuario confirmó haber completado las verificaciones manuales de 3.4, 4.4 y 4.6. Las tareas 4.1, 4.2 y 4.7 quedan en pausa hasta recibir respuesta de Google Play sobre la exigencia de capturas de pantalla de teléfonos en la ficha.
