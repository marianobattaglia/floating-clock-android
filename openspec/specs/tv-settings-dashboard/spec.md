# tv-settings-dashboard Specification

## Purpose

Present the existing Android TV clock and alarm controls in a cohesive visual dashboard with focus-driven supplementary previews and no visible advertising placeholder, preserving their current functionality and configuration flows.

## Requirements

### Requirement: Dashboard arranges the existing screen visually
The app SHALL present the existing app logo at the top of a left navigation rail containing **Home** and **PRO**, plus **Debug** only in debug builds. The unused **Settings** rail entry SHALL be hidden; the existing settings rows SHALL remain available on the Home dashboard. Selecting Home SHALL show the dashboard and its existing settings region. Selecting PRO SHALL show the PRO access screen. Selecting Debug in a debug build SHALL show the debug entitlement controls. These destinations SHALL not duplicate clock, alarm, purchase, or entitlement state. The app SHALL use cohesive colors, typography, rounded surfaces, icons, spacing, and visible focus inspired by the reference image while retaining its current brand and language.

#### Scenario: Use the rail shortcuts
- **WHEN** the user selects Home, PRO, or Debug with OK/Select or tap
- **THEN** the matching destination SHALL be shown without changing clock settings, alarm state, scheduled services, or actual Play purchase state
- **AND** selecting Debug SHALL be possible only in a debug build

#### Scenario: Settings remain available from Home
- **WHEN** the user selects Home
- **THEN** the current clock, appearance, and alarm rows SHALL remain available subject to their Free/PRO access rules
- **AND** the left rail SHALL not show an unused Settings entry

#### Scenario: Read the header clock
- **WHEN** the user views the redesigned Home dashboard or changes clock format through the existing dialog
- **THEN** the header SHALL display the live time with the existing 12/24-hour and seconds behavior while the floating clock is disabled, and SHALL hide that fixed clock while the floating clock is enabled
- **AND** floating-clock appearance controls SHALL not resize the header clock

### Requirement: Existing settings options and actions remain unchanged
The Home dashboard SHALL retain the current floating-clock enablement, four corner positions, 12/24-hour format, optional seconds, three appearance controls, and alarm controls, with the new Free/PRO access rules applied to appearance and alarm activation. Each row and switch SHALL keep its existing action except where the access rules require navigation to PRO or enforce the Free alarm cooldown. The appearance row SHALL remain visible only while the overlay is enabled; a PRO user's selection SHALL open its existing dialog directly, while a Free user's selection SHALL navigate to the PRO destination. The idle alarm row SHALL retain its existing configuration flow, subject to the Free limit of one successful activation per rolling 24 hours and PRO's unlimited activation allowance. Selecting a ringing alarm row SHALL always dismiss it immediately for all users. Position, format, seconds, and overlay behavior SHALL remain free and unchanged. The redesign SHALL NOT introduce an Edit step, replacement slider, or changed appearance value range, step, default, or persistence timing.

#### Scenario: Open and close a clock configuration dialog
- **WHEN** a Free or PRO user selects position or format with OK/Select or tap and closes the dialog with its existing OK or Back action
- **THEN** the same dialog SHALL open directly and retain its current settings and persistence behavior
- **AND** there SHALL be no intermediate preview action to complete before editing

#### Scenario: Select appearance according to access
- **WHEN** a PRO user selects the appearance row
- **THEN** the existing appearance modal SHALL open directly
- **AND** when a Free user selects that row, the PRO destination SHALL open without exposing editable appearance controls

#### Scenario: Toggle the floating clock
- **WHEN** the user activates the floating-clock row or its existing switch
- **THEN** the current enable/disable and permission flow SHALL be invoked once for that activation
- **AND** the dashboard SHALL reflect the same result as the existing flow

#### Scenario: Configure or dismiss an alarm
- **WHEN** the user selects the alarm row
- **THEN** an idle alarm SHALL open the current configuration dialog, subject to the Free or PRO activation rule
- **AND** a ringing alarm SHALL always be stopped through the current dismissal action
- **AND** opening the dialog or changing its time alone SHALL not consume a Free activation

#### Scenario: Hide dependent rows
- **WHEN** the overlay is disabled
- **THEN** position, format, and appearance rows SHALL be hidden as before, saved values SHALL be retained, and the preview SHALL not offer hidden controls

### Requirement: Previews supplement the existing row action
A row's preview SHALL become active immediately when that row or its child switch receives focus, including Up/Down movement with the remote. Focus movement SHALL NOT invoke the row action, open a dialog, toggle a setting, request permission, or stop an alarm. OK/Select or tap SHALL retain the access-dependent action described by the row. The preview SHALL be read-only and SHALL NOT replace, delay, or suppress that action, take focus from an opened dialog, or add interactive controls. Before any setting receives focus, the panel SHALL show a neutral prompt. Clock previews SHALL use a bundled, offline landscape photograph as the background, centered and cropped to fill the existing frame without distortion. The live clock SHALL be drawn over that static image only while the floating clock is enabled; disabling it SHALL leave the landscape visible without a miniature clock. Clock previews SHALL depict existing position, format, seconds, edge distance, size, and internal spacing values in a bounded simulated display. Alarm and enablement previews SHALL show only information already represented by current controls. Previews SHALL reflect accepted changes and SHALL NOT retain content for a hidden row. Preview rendering itself SHALL NOT persist values, request permissions, create an overlay, play audio, or schedule/cancel an alarm; the row's original action retains its existing side effects.

#### Scenario: Select appearance
- **WHEN** the user moves focus to Appearance and then selects it
- **THEN** focus movement SHALL immediately show the appearance preview without changing a preference
- **AND** a PRO user SHALL directly open the existing appearance modal, while a Free user SHALL be taken to the PRO destination
- **AND** the modal, when opened, SHALL retain focus and there SHALL be no separate Edit action

#### Scenario: Select an enablement row without permission
- **WHEN** the user selects the floating-clock row while required permission is missing
- **THEN** the existing enablement permission flow SHALL still run and the supplementary preview SHALL reflect the resulting state
- **AND** preview rendering SHALL add no independent permission request or service start

#### Scenario: Reflect settings faithfully
- **WHEN** the user changes a corner, time format, seconds, or an available appearance value through its existing control
- **THEN** the supplementary preview SHALL depict the accepted values using a consistent simulated-display scale
- **AND** no geometry or saved value of the real floating clock SHALL be changed by rendering the preview

### Requirement: Visual changes preserve remote and Back behavior
All existing controls and new navigation destinations SHALL remain reachable using D-pad and OK/Select with visible focus and meaningful labels. The Home, PRO, and debug-only Debug rail entries SHALL be reachable without making them a mandatory step before dashboard settings. Passive previews and the header SHALL NOT become focus targets. Opening PRO or Debug SHALL keep navigation state predictable; pressing Back from either destination SHALL return to Home, and pressing Back from Home with no dialog open SHALL retain existing screen-level exit behavior. Existing modal dismissal and focus restoration SHALL be preserved; route navigation SHALL not add an extra Back step through the preview or rail. Layout SHALL keep controls visible at supported TV sizes, including 720p and 1080p, with bounded scrolling where needed.

#### Scenario: Dismiss appearance with the remote
- **WHEN** a PRO user selects Appearance, adjusts its existing controls, and presses Back or OK
- **THEN** the modal SHALL close, adjusted values SHALL remain saved, and focus SHALL return to its opening row as before

#### Scenario: Return from PRO or Debug
- **WHEN** the user presses Back while the PRO destination or debug-only Debug destination is open
- **THEN** the Home dashboard SHALL be shown with usable focus
- **AND** a later Back press from Home with no dialog open SHALL follow existing screen-level behavior

#### Scenario: Exit the main screen
- **WHEN** the user presses Back on Home with no dialog open
- **THEN** existing screen-level Back behavior SHALL be retained without requiring traversal through preview or rail focus targets

#### Scenario: Use a smaller TV display
- **WHEN** any destination is displayed at 720p or 1080p
- **THEN** its rows, purchase actions, dialog controls, and focus indicators SHALL remain readable and reachable without clipping or reserving space for an advertising placeholder

### Requirement: Advertising placeholder remains hidden

The app SHALL NOT render a bottom advertising banner, placeholder label, border, empty reserved area, or focus target. The workspace SHALL use the recovered space. Future advertising integration remains outside this iteration and SHALL NOT introduce an SDK, ad request, tracking, link, permission, or change to the overlay window.

#### Scenario: View the dashboard
- **WHEN** the user opens the dashboard or navigates between settings
- **THEN** no advertising banner or placeholder is visible and no empty bottom strip is reserved for it

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
- **THEN** the existing service/window lifecycle and saved values SHALL be preserved without accumulated UI listeners or callbacks

#### Scenario: Alarm rings during configuration
- **WHEN** an alarm rings while the user adjusts appearance and then dismisses it using the current alarm row
- **THEN** existing sound and red blinking SHALL continue until dismissal and stop through the existing flow
- **AND** the accepted appearance values SHALL be retained
