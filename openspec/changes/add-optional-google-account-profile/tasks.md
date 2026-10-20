# Tasks

> **Estado: diferido.** Estas tareas no pertenecen a la primera versión, que será gratuita con anuncios y sin cuentas ni suscripciones. Mantener todas sin marcar. No comenzar hasta reactivar la fase de cuentas; las suscripciones, la eliminación de anuncios y las funciones premium requieren un cambio OpenSpec separado.

Antes de reactivar estas tareas, confirmar si una cuenta de FloatingClock aporta un beneficio que Google Play Billing por sí solo no cubra. Revalidar compatibilidad con `minSdk 21`, costos vigentes de Firebase y requisitos de privacidad/eliminación.

## 1. Firebase setup and profile-data security

- [ ] 1.1 After reactivation, compare Firebase products, current quotas, and expected usage before creating a project; if selected, register the Android application, enable only required services, and verify the chosen billing plan.
- [ ] 1.2 Select a sign-in/dependency strategy compatible with the supported Android TV API levels; configure resources and network permission; verify `bash ./gradlew :app:assembleDebug`. Do not change `minSdk 21` without a separate product decision.
- [ ] 1.3 Add Firestore rules for owner-only profile access, immutable first-registration time, `free`-only client status writes, and owner deletion; verify Firebase Emulator Suite rules tests reject cross-user reads/writes and client attempts to set `active` or alter registration time.

## 2. Authentication and account lifecycle

- [ ] 2.1 Implement an account repository/state model for Firebase session restoration, Google sign-in on supported Android TV devices, profile creation/recovery, server-derived registration time, and retryable network/provider errors; verify unit/emulator coverage for first registration, repeat sign-in, missing name claims, offline errors, and unsupported TV devices.
- [ ] 2.2 Implement sign-out with Firebase session clearing and Credential Manager credential-state clearing; verify the app asks for an explicit account choice on the next sign-in and retains overlay, alarms, and saved preferences.
- [ ] 2.3 Implement confirmed account deletion with Google re-authentication when required, deletion of the profile and Firebase Auth account, and truthful partial-failure recovery; verify success, cancellation, re-authentication, and each operation failing independently with emulator-backed or fake-repository tests.

## 3. Profile interface, disclosure, and localization

- [ ] 3.1 Add Profile to the side rail and implement its signed-out, loading, signed-in, unsupported-device, and error states in the existing activity; verify D-pad/Select, visible focus, Profile Back behavior, and unchanged Home/Settings, clock, overlay, and alarm behavior.
- [ ] 3.2 Display the available name, email, localized first-registration date, and subscription status; default new accounts to Free and render Active only for a trusted entitlement state; verify status mapping and that Firestore rules prevent a client from granting Active.
- [ ] 3.3 Add every Profile, sign-in, error, privacy, sign-out, and deletion message/accessibility label to English, Spanish, Portuguese, and French; verify `bash ./gradlew :app:verifyTranslations :app:testTranslationVerifier` and inspect translated Profile labels at TV distance.
- [ ] 3.4 Add an account-data privacy policy path and a public account-deletion request resource; configure the publisher's support address and final public URL, link both resources from Profile, and update Google Play Data safety/deletion entries; verify the resources load without sign-in and explain collection, purpose, storage, and deletion for FloatingClock.

## 4. Integration validation

- [ ] 4.1 Run `bash ./gradlew :app:assembleDebug`, `bash ./gradlew :app:lintDebug`, and the translation checks; record automated results separately from device checks.
- [ ] 4.2 On a supported Android TV device, manually verify Google account selection, cancellation, sign-in, persisted session, account switching, sign-out, profile errors, and deletion; repeat remote/focus/Back checks at 720p and 1080p and confirm an older/unsupported TV can still use every clock and alarm feature anonymously.
- [ ] 4.3 Verify sign-out and account deletion while the floating clock and alarm are active; confirm neither action changes existing settings, service state, alarm scheduling, sound, or dismissal behavior.
