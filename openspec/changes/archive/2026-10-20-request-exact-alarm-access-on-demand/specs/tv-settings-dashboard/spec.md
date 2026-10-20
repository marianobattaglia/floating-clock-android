# Spec Delta

## MODIFIED Requirements

### Requirement: Runtime behavior remains outside the visual change

The app SHALL preserve existing persisted keys, defaults, unrelated actions, service lifecycle, actual overlay rendering, alarm cancellation, sound, blinking, and permission behavior on supported Android versions. Activating the floating clock SHALL use only the permissions needed by the overlay and its foreground service; it SHALL NOT request exact-alarm access. On Android 12 (API 31) and later, when the user selects the idle **Alarm** row, the app SHALL check exact-alarm access before displaying its configuration modal. If access is missing, it SHALL open the exact-alarm settings screen first; when the app returns, it SHALL recheck access and then display the modal even if the user did not grant access. If access is already granted, it SHALL display the modal without opening system settings. For direct alarm-switch activation with a saved time, the app SHALL request missing access before scheduling and retain the requested action and time while the user visits system settings. Before every scheduling attempt on API 31 or later, the app SHALL check whether exact-alarm access is currently granted. If it is granted, the app SHALL schedule exactly. If scheduling is attempted without access, including after denial or when the settings screen is unavailable, the app SHALL use a working inexact fallback and tell the user that timing may be approximate. A denied or unavailable access request made while opening the configuration modal SHALL NOT schedule an alarm until the user presses **ACTIVATE**. Pressing **ACTIVATE** from the modal SHALL NOT open the settings screen a second time. If exact access is revoked between the check and scheduling, the app SHALL handle the scheduling failure without crashing and SHALL attempt the inexact fallback. If scheduling fails, the alarm SHALL remain disabled and the app SHALL report the failure. Merely moving focus, changing the selected time, canceling, or dismissing a ringing alarm SHALL NOT request exact-alarm access. Android versions below API 31 SHALL NOT request this special access. D-pad, Select, and Back SHALL remain usable through the alarm configuration and return from system settings. Permission denial and unavailable settings screens SHALL not cause a false enabled state. Supplementary previews SHALL remain read-only and SHALL NOT request permissions, schedule/cancel alarms, play audio, or start services. UI resources SHALL be released with their owning lifecycle.

#### Scenario: Enable the floating clock without exact-alarm access
- **WHEN** the user activates the floating clock on a device where exact-alarm access is missing
- **THEN** the app SHALL NOT open the exact-alarm settings screen
- **AND** the existing overlay and notification permission flows SHALL continue to determine whether the clock starts

#### Scenario: Open the alarm configuration without exact-alarm access
- **WHEN** the user selects the idle Alarm row on API 31 or later and exact-alarm access is missing
- **THEN** the app SHALL open exact-alarm settings before showing the configuration modal
- **AND** after returning, it SHALL show the modal and preserve D-pad focus even if access remains denied

#### Scenario: Open the alarm configuration when exact-alarm access is already granted
- **WHEN** the user selects the idle Alarm row on API 31 or later and exact-alarm access is already granted
- **THEN** the app SHALL show the configuration modal without opening system settings

#### Scenario: Activate an alarm from the switch without exact-alarm access
- **WHEN** the user directly enables a previously configured alarm on API 31 or later and exact-alarm access is missing
- **THEN** the app SHALL request the access before scheduling and retain the selected time and pending activation while system settings is open
- **AND** after returning with access granted, it SHALL schedule the selected alarm exactly and reflect the successful activation

#### Scenario: Activate an alarm when exact-alarm access is already granted
- **WHEN** the user activates an alarm with a selected time on API 31 or later and exact-alarm access is already granted
- **THEN** the app SHALL verify the current access and schedule the alarm exactly without opening system settings
- **AND** it SHALL reflect the alarm as enabled only after scheduling succeeds

#### Scenario: Exact-alarm access changes during scheduling
- **WHEN** exact-alarm access is revoked after the app checks it but before exact scheduling completes
- **THEN** the app SHALL handle the scheduling exception without crashing and attempt the inexact fallback
- **AND** it SHALL show approximate-timing feedback if the fallback succeeds, or leave the alarm disabled and report failure if it does not

#### Scenario: Deny access before opening the configuration modal
- **WHEN** the user returns from exact-alarm settings without granting access after selecting the Alarm row
- **THEN** the app SHALL display the configuration modal without scheduling an alarm
- **AND** after the user presses **ACTIVATE**, it SHALL use the inexact fallback and inform the user that timing may be approximate

#### Scenario: Exact-alarm settings are unavailable
- **WHEN** the user selects the Alarm row while exact-alarm access is missing and the device cannot open its settings screen
- **THEN** the app SHALL display the configuration modal without scheduling an alarm
- **AND** if the user later activates the alarm, it SHALL use the inexact fallback and report scheduling failure if that attempt fails

#### Scenario: Deny access during direct switch activation
- **WHEN** the user directly enables a previously configured alarm and returns without granting access, or exact-alarm settings cannot be opened
- **THEN** the app SHALL attempt to schedule the selected alarm through an inexact fallback
- **AND** when that succeeds, it SHALL reflect the alarm as enabled and inform the user that its timing may be approximate
- **AND** when that fails, it SHALL leave the alarm disabled and show the existing scheduling failure feedback

#### Scenario: Activate from the modal after the access check
- **WHEN** the user presses **ACTIVATE** in the configuration modal after entering the Alarm flow
- **THEN** the app SHALL recheck access and schedule exactly if granted or use the inexact fallback if access is missing
- **AND** it SHALL NOT open exact-alarm settings a second time from the modal

#### Scenario: Configure or cancel without activating
- **WHEN** the user changes the time or closes the modal with Back or **CANCEL** without activating the alarm
- **THEN** the app SHALL NOT schedule an alarm or request exact-alarm access again from the modal
- **AND** D-pad focus and Back behavior SHALL remain available and predictable

#### Scenario: Use an Android version below API 31
- **WHEN** the user activates an alarm on API 21 through API 30
- **THEN** the app SHALL schedule it without opening exact-alarm settings
- **AND** the alarm controls, persisted values, sound, blinking, and cancellation SHALL retain their existing behavior

#### Scenario: Deny required permissions
- **WHEN** the existing enablement flow is denied overlay permission on API 23+ or notifications on API 33+
- **THEN** the redesigned UI retains the existing disabled-state feedback and saved values without an additional preview-triggered action

#### Scenario: Exact-alarm access is unavailable
- **WHEN** exact-alarm access is missing on API 31+ and the device cannot open the exact-alarm settings screen
- **THEN** the app SHALL display the alarm configuration modal without scheduling an alarm
- **AND** pressing **ACTIVATE** SHALL use the inexact fallback and report approximate timing when scheduling succeeds

#### Scenario: Recreate the UI or cycle the overlay
- **WHEN** the user recreates the activity or repeatedly enables and disables the overlay on supported API levels
- **THEN** the existing service/window lifecycle and saved values are preserved and the visual additions leave no accumulated UI listeners or callbacks

#### Scenario: Alarm rings during configuration
- **WHEN** an alarm rings while the user adjusts appearance and then dismisses it using the current alarm row
- **THEN** existing sound and red blinking continue until dismissal and stop through the existing flow
- **AND** the accepted appearance values are retained

#### Scenario: Recreate the UI or cycle the overlay
- **WHEN** the user recreates the activity or repeatedly enables and disables the overlay on supported API levels
- **THEN** the existing service/window lifecycle and saved values SHALL be preserved without accumulated UI listeners or callbacks

#### Scenario: Alarm rings during configuration
- **WHEN** an alarm rings while the user adjusts appearance and then dismisses it using the current alarm row
- **THEN** existing sound and red blinking SHALL continue until dismissal and stop through the existing flow
- **AND** the accepted appearance values SHALL be retained
