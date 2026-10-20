# Spec Delta

## Purpose

Define a future optional Google-backed account profile for Android TV users to identify themselves and view their account status. Account access remains independent of FloatingClock's clock, overlay, and alarm features.

## Release applicability

These requirements are deferred and do not apply to the first release. The first version is free with ads and SHALL NOT offer sign-in, create app accounts, collect account profile data, or sell subscriptions. Subscription billing, ad removal, and enhanced paid features SHALL be specified in a separate future change. Revisit this profile capability only if an app account is needed for a concrete user benefit beyond Google Play purchase restoration.

## ADDED Requirements

### Requirement: Google account access is optional

The app SHALL provide a Profile area where a signed-out user can start Google sign-in. Signing in SHALL remain optional; the dashboard, floating clock, alarm, and their settings SHALL remain fully available without an account. The app SHALL NOT create an account or start sign-in without an explicit user action. If the device cannot provide a supported Google sign-in flow, the Profile area SHALL explain that sign-in is unavailable on that device and the rest of the app SHALL continue to work.

#### Scenario: Use the app without an account
- **WHEN** a user opens FloatingClock and does not sign in
- **THEN** the user can configure and use the dashboard, overlay, and alarm as before
- **AND** no authentication prompt blocks or delays those features

#### Scenario: Start Google sign-in
- **WHEN** a signed-out user selects the Google sign-in action in Profile
- **THEN** the app requests only the Google identity information needed for the profile
- **AND** successful first-time authentication creates an app account associated with that Google identity
- **AND** canceling or failing authentication leaves the user signed out and the app usable

#### Scenario: Google sign-in is unavailable
- **WHEN** the device lacks a supported Google sign-in flow or its required services
- **THEN** Profile shows a localized explanation and does not report the user as signed in
- **AND** no unrelated app feature is disabled

### Requirement: Store the account's first-registration profile

For each authenticated account, the system SHALL associate a stable user ID, email address, given name, family name, and first-registration timestamp with that account. The timestamp SHALL represent the first app-account registration, SHALL use a trusted server-derived time, and SHALL NOT change on later sign-ins. Profile data SHALL be scoped to its account so one signed-in user cannot read or modify another user's profile. Missing optional name information from Google SHALL NOT prevent sign-in; the profile SHALL display only the name information that is available.

#### Scenario: Create a profile on first registration
- **WHEN** a user completes Google sign-in for the first time
- **THEN** the app stores the user's stable ID, available name fields, email, and first-registration timestamp
- **AND** the timestamp is not taken from the device's editable clock

#### Scenario: Sign in again
- **WHEN** the same user signs in on a later launch
- **THEN** the app restores the same account ID and first-registration timestamp
- **AND** a repeat sign-in does not create a duplicate account or replace the first-registration timestamp

#### Scenario: Google omits a name field
- **WHEN** Google supplies a profile without a given name or family name
- **THEN** sign-in succeeds and the profile presents the available name information without inventing a value

### Requirement: Show account and subscription status

When signed in, Profile SHALL show the user's available name, email, first-registration date, and subscription status. The status SHALL be **Free** when no verified active entitlement is available and SHALL be **Active** only when a trusted entitlement source reports an active subscription. A client-side action or editable profile field SHALL NOT grant Active status. This change SHALL NOT include subscription purchases, payment verification, or advertising behavior.

#### Scenario: View a free account
- **WHEN** the signed-in account has no verified active entitlement
- **THEN** Profile shows the account details and the **Free** subscription status

#### Scenario: View an active account
- **WHEN** a trusted entitlement source reports an active subscription for the signed-in account
- **THEN** Profile shows the **Active** status for that account
- **AND** the client cannot grant that status by changing its own profile data

#### Scenario: Open Profile while signed out
- **WHEN** a signed-out user opens Profile
- **THEN** Profile shows the Google sign-in action and does not display another user's account information

### Requirement: Sign-out clears the app account session

Profile SHALL offer a sign-out action for an authenticated user. Signing out SHALL clear the app's authenticated session and return Profile to its signed-out state. Signing out SHALL NOT delete the account or change clock preferences, overlay state, alarm schedule, alarm playback, or other app data.

#### Scenario: Sign out from Profile
- **WHEN** the user selects sign out and the operation succeeds
- **THEN** Profile shows the signed-out Google sign-in action
- **AND** the user must explicitly choose an account to sign in again

#### Scenario: Sign out while clock or alarm features are active
- **WHEN** the user signs out while the overlay is enabled or an alarm is configured or ringing
- **THEN** sign-out changes only the account session
- **AND** the existing overlay and alarm behavior continues unchanged

### Requirement: Disclose account data collection

The app SHALL provide an accessible privacy-policy path from Profile. The policy SHALL describe the account data collected (user ID, email, available given and family names, and first-registration timestamp), its use for account display and future entitlement association, its storage in the selected account service, and how to request deletion. The storage service SHALL be selected before implementation. Users SHALL be able to read the policy without signing in. The app SHALL NOT describe account data as being used for advertising in this change.

#### Scenario: Review account data use
- **WHEN** a signed-out or signed-in user opens Profile and selects its privacy-policy link
- **THEN** the policy is available without requiring an app account
- **AND** it describes the collected account fields, their purpose, storage, and deletion path

### Requirement: Delete an account and its associated data

The signed-in user SHALL be able to request deletion of their app account from within Profile. The app SHALL confirm this destructive action, delete the app account and its associated stored profile data on success, and return to the signed-out dashboard state. The app SHALL also provide an externally accessible account-deletion request resource. The external resource SHALL identify FloatingClock and provide a clear way to request deletion of the app account and associated data. If account deletion cannot complete, the app SHALL report the failure and SHALL NOT claim that the account was deleted.

#### Scenario: Delete an account in the app
- **WHEN** the user confirms account deletion and the deletion succeeds
- **THEN** the app deletes the account and associated profile data, signs the user out, and returns to the dashboard
- **AND** the clock, overlay, and alarm remain usable

#### Scenario: Deletion requires recent authentication
- **WHEN** account deletion requires the user to authenticate again
- **THEN** the app offers a Google re-authentication step and retries deletion only after that step succeeds
- **AND** cancellation leaves the account intact and Profile signed in

#### Scenario: Request deletion outside the app
- **WHEN** a user visits the external deletion resource
- **THEN** it provides a clear request path for deleting the FloatingClock account and associated data
- **AND** a submitted request can be fulfilled without requiring access to the TV app

#### Scenario: Account deletion fails
- **WHEN** an account or profile deletion operation fails
- **THEN** the app reports that deletion did not complete and keeps the remaining account state truthful

### Requirement: Profile respects Android TV remote navigation and app localization

Profile actions SHALL be reachable and operable using D-pad and OK/Select, with visible focus and meaningful accessibility labels. Back SHALL close a profile confirmation or return from the Profile area to the dashboard without unexpectedly signing out or changing settings. All Profile text, status values, errors, confirmations, and accessibility labels SHALL follow the app's supported English, Spanish, Portuguese, and French catalogs.

#### Scenario: Navigate Profile with a TV remote
- **WHEN** the user moves focus through Profile, starts or cancels sign-in, or signs out using D-pad and OK/Select
- **THEN** focus remains visible and actions occur only after OK/Select
- **AND** Back returns to the prior app surface without changing the account or app settings

#### Scenario: Change the app's system language
- **WHEN** the system language is English, Spanish, Portuguese, French, or a regional variant of one of them
- **THEN** Profile labels, subscription status, errors, confirmations, and accessibility descriptions use the selected supported language
