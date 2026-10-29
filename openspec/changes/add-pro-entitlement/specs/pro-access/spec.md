# Spec Delta

## Purpose

Provides a Google Play purchase entitlement for permanent PRO access and a debug-only override so developers can verify Free and PRO behavior without requiring a purchase.

## ADDED Requirements

### Requirement: PRO screen shows the current access state
The app SHALL provide a **PRO** destination in the left navigation rail. The screen SHALL show whether the current installation has Free or PRO access and explain the benefits available in the current release. It SHALL remain usable with D-pad and Select, show visible focus, support Back, and use localized text in English, Spanish, Portuguese, and French. The app SHALL NOT require Google sign-in or collect a user's email, name, or Google account ID to determine the Play purchase entitlement.

#### Scenario: Open PRO without a purchase
- **WHEN** a user with no owned PRO purchase opens the PRO destination after a successful purchase query
- **THEN** the screen SHALL show Free status and offer the one-time PRO purchase action
- **AND** the screen SHALL remain reachable without interrupting the free clock and alarm features

#### Scenario: Open PRO with an owned purchase
- **WHEN** Google Play reports an owned, completed PRO purchase
- **THEN** the screen SHALL show PRO status and the user SHALL receive the PRO access defined by the app

#### Scenario: Purchase status cannot be checked
- **WHEN** the Play Billing service is temporarily unavailable or a purchase query fails
- **THEN** the app SHALL show a localized status-check message and SHALL NOT report a successful new purchase
- **AND** it SHALL preserve the last successfully confirmed status for the current installation when one exists

### Requirement: PRO is a permanent one-time Google Play purchase
The app SHALL offer PRO as a non-consumable one-time Google Play product through the in-app Play purchase flow. A generic Play Store redirect SHALL NOT be used as the purchase mechanism. The app SHALL grant PRO only after a purchase reaches the completed `PURCHASED` state; pending, canceled, failed, or unavailable purchase flows SHALL leave the entitlement unchanged and provide appropriate localized feedback. The app SHALL acknowledge completed purchases as required by Google Play. A successful purchase SHALL be restorable by querying the Play account's owned purchases when the app starts and when it returns to the foreground.

#### Scenario: Complete a PRO purchase
- **WHEN** a Free user selects the PRO purchase action and completes the Google Play purchase flow
- **THEN** the app SHALL grant PRO after confirming the purchase is completed
- **AND** it SHALL acknowledge the non-consumable purchase and refresh the visible status

#### Scenario: Purchase is pending, canceled, or fails
- **WHEN** the user cancels the flow, the payment remains pending, or Google Play returns an error
- **THEN** the app SHALL retain the previous entitlement state and SHALL NOT unlock PRO-only features
- **AND** it SHALL provide localized feedback without disrupting the dashboard

#### Scenario: Restore a previous purchase
- **WHEN** a user installs or opens the app on a device signed into the Google Play account that owns PRO
- **THEN** the app SHALL discover the owned purchase during its purchase query and restore PRO access without a separate app account login

#### Scenario: Purchase flow is unavailable
- **WHEN** Google Play Billing is not available on the device or for the current app installation
- **THEN** the app SHALL explain that purchase is unavailable and keep the user on an accessible screen
- **AND** it SHALL NOT falsely display PRO or navigate to an unrelated generic Play Store page

### Requirement: Debug builds can override the effective entitlement
Only a debug build SHALL show a **Debug** destination in the left rail. That destination SHALL let a developer set the effective access state to Free or PRO for local testing. The override SHALL affect feature gating only; it SHALL NOT create, acknowledge, alter, or represent a Google Play purchase. Release builds SHALL neither show the Debug destination nor honor a persisted debug override.

#### Scenario: Force Free or PRO in a debug build
- **WHEN** a developer selects Free or PRO in the Debug destination of a debug build
- **THEN** the app SHALL immediately apply that effective state to PRO-gated features and status UI
- **AND** the developer SHALL be able to test both entitlement paths without starting a Play purchase

#### Scenario: Install a release build with a prior debug override
- **WHEN** a release build starts on a device that previously stored a debug override
- **THEN** the release build SHALL ignore that override and derive access only from the real Play purchase state
- **AND** it SHALL not expose any Debug navigation item or control

#### Scenario: Debug override is not a purchase
- **WHEN** the debug override is set to PRO
- **THEN** the Play purchase ownership and acknowledgement state SHALL remain unchanged
- **AND** clearing or changing the override SHALL reveal the actual Play entitlement state

### Requirement: Configured release builds provide separate PRO review access
When a review code is configured for a release build, the PRO screen SHALL provide an accessible, localized way to enter it. A valid code SHALL grant local access to PRO-gated features for that installation so reviewers can inspect the full app without purchasing. Review access SHALL remain distinct from actual Google Play ownership: it SHALL NOT change the reported Play purchase status, acknowledge or create a transaction, or write the Play-owned entitlement cache. The grant SHALL persist across process restarts and SHALL be invalidated when a later release is built with a different review-code verifier. When no release code is configured, the review entry point SHALL be absent. The plaintext code SHALL NOT be committed or embedded in the APK; release configuration SHALL embed only its SHA-256 digest. The PRO screen SHALL clearly identify an active review grant as review access, not a purchase. All entry controls and feedback SHALL support D-pad, Select, visible focus, Back, and the supported app languages.

#### Scenario: Reviewer enters the configured code
- **WHEN** a reviewer opens PRO in a release build configured with a review code and submits the matching code
- **THEN** the installation SHALL gain effective PRO access to all PRO-gated features
- **AND** the app SHALL identify the access as review access while leaving the actual Play purchase state unchanged

#### Scenario: Reviewer submits an invalid code
- **WHEN** a user submits a code that does not match the configured release verifier
- **THEN** the app SHALL retain the existing effective access state and show localized invalid-code feedback
- **AND** it SHALL not write or acknowledge a Play purchase

#### Scenario: Release has no reviewer code configured
- **WHEN** a release build is created without a review code
- **THEN** the PRO screen SHALL not show a reviewer-code entry point
- **AND** feature access SHALL continue to depend on actual Play ownership

#### Scenario: Code is rotated in a later release
- **WHEN** a release uses a different review-code verifier from the one used to grant the locally stored review access
- **THEN** the prior review grant SHALL no longer provide PRO access
- **AND** any actual completed Play purchase SHALL continue to provide PRO access

#### Scenario: Review access does not alter debug testing
- **WHEN** the app runs a debug build with a local Free or PRO override
- **THEN** the existing debug override SHALL determine effective access
- **AND** the release review-code mechanism SHALL not alter actual ownership or debug override behavior
