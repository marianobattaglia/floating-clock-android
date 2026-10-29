# Spec Delta

## MODIFIED Requirements

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
