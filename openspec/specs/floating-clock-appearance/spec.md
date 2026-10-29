# floating-clock-appearance Specification

## Purpose

Allow Android TV users to personalize the floating clock's edge distance, overall size, and internal spacing with a remote control, retaining those choices across sessions.

## Requirements

### Requirement: Appearance modal is accessible with a TV remote
While the floating clock is enabled, the app SHALL show a localized **Clock appearance** row. The row SHALL identify that its controls require PRO. A Free user selecting the row SHALL be taken to the PRO destination and SHALL not be able to change the appearance values; a PRO user selecting it SHALL open the existing modal with localized labels for edge distance, overall size, and internal spacing, each with decrease and increase buttons and localized accessible action descriptions. All visible controls SHALL be reachable using only D-pad and Select, show visible focus, and remain accessible on supported TV screen sizes in English, Spanish, Portuguese, and French. The localized **OK** action and Back SHALL dismiss the modal and return focus to its opening row without reverting saved adjustments. The English names used in this specification identify controls semantically rather than requiring English display text.

#### Scenario: Adjust and close using the remote
- **WHEN** a PRO user selects Clock appearance, navigates between all three controls, adjusts values, and presses Back or selects OK
- **THEN** each selected adjustment SHALL be reflected in its displayed value and the modal SHALL close with focus returned to Clock appearance
- **AND** the adjusted values SHALL remain saved

#### Scenario: Free user selects the locked appearance row
- **WHEN** a Free user selects Clock appearance while the floating clock is enabled
- **THEN** the app SHALL open the PRO destination with a clear explanation that appearance controls require PRO
- **AND** it SHALL not open editable appearance controls or change a saved value

#### Scenario: Overlay is disabled
- **WHEN** the floating clock is disabled
- **THEN** the appearance row SHALL remain hidden for both Free and PRO users consistently with the position and format rows
- **AND** previously saved appearance values SHALL remain available for the next successful enable

#### Scenario: Use translated appearance controls
- **WHEN** a PRO user opens the appearance modal in Spanish, Portuguese, or French using only the remote
- **THEN** its title, labels, help, close action, and accessible increase/decrease descriptions SHALL use that language
- **AND** all controls SHALL remain readable and reachable with visible focus and the same ranges, steps, immediate save, and Back behavior

### Requirement: Edge distance adjusts the selected corner's offsets

The system SHALL provide one shared distance for both adjacent edges of the selected corner, from 0 to 128 dp in 4 dp steps, with a default of 16 dp. Increasing it SHALL move the clock inward from those edges; decreasing it SHALL move the clock toward them. The selected corner SHALL remain unchanged, and changing corners SHALL retain the configured distance.

#### Scenario: Use the same distance in each corner
- **WHEN** the user sets edge distance to 24 dp and selects each of the four corners
- **THEN** the clock is anchored 24 dp from the two edges of the selected corner whenever screen space permits

### Requirement: Overall size scales text and background together

The system SHALL provide overall size from 50% to 200% in 10 percentage point steps, with a default of 100%. At a fixed internal spacing setting and when screen space permits, scaling SHALL proportionally change the text and internal spacing so that the complete clock background resizes with its content. The time SHALL remain readable on one line in both 12-hour and 24-hour formats, with seconds on or off.

#### Scenario: Increase and decrease the entire clock
- **WHEN** the user changes overall size from 100% to 150% and then to 50% without changing internal spacing
- **THEN** the text size and internal spacing become respectively 1.5 times and 0.5 times their 100% dimensions, subject to pixel rounding and screen fitting
- **AND** the background encloses the complete time at each size

### Requirement: Internal spacing can change independently of text size

The system SHALL provide internal spacing from 0% to 300% of the original padding in 10 percentage point steps, with a default of 100%. At a fixed overall size, changing this setting SHALL change the spacing between the text and all background edges while retaining the original horizontal-to-vertical padding proportion and leaving text size unchanged, except when screen fitting is required.

#### Scenario: Increase spacing without enlarging the text
- **WHEN** the user increases internal spacing from 100% to 200% at a fixed overall size and there is sufficient screen space
- **THEN** the padding on each side doubles and the background expands around the same-sized text

#### Scenario: Remove configurable padding
- **WHEN** the user decreases internal spacing to 0%
- **THEN** the configurable padding is zero on all sides while the complete text remains visible

### Requirement: Appearance settings persist and update the active overlay

Each accepted adjustment SHALL immediately update the displayed value, persist the three independent settings locally, and refresh an active overlay without requiring the user to toggle it or restart the app. Saved values SHALL be restored when reopening the modal, recreating the app, or recreating/re-enabling the overlay. Existing position, time format, seconds, alarm preferences, and actions SHALL retain their meaning.

#### Scenario: Reopen after recreation
- **WHEN** the user saves non-default values, closes the app, disables and re-enables the overlay, or the app/service is recreated
- **THEN** the next appearance modal and overlay use the same saved values

#### Scenario: Upgrade with no appearance preferences
- **WHEN** the overlay starts with none of the new appearance preferences present
- **THEN** it uses 16 dp edge distance, 100% overall size, and 100% internal spacing
- **AND** it retains the previous appearance of 24 sp text and 24 horizontal/16 vertical physical pixels of explicit padding at those defaults
- **AND** existing preferences are preserved

### Requirement: Bounds and screen fitting prevent unusable geometry

Values SHALL remain within their supported ranges; further increase/decrease at a bound SHALL have no effect. Missing or unreadable values SHALL use their respective defaults, and numeric values outside the range SHALL be clamped. The overlay SHALL remain fully within the available window area, including at the largest size, largest spacing, and longest time format. If requested geometry cannot fit, it SHALL reduce the effective scale uniformly only as needed to fit the content and clamp edge offsets to the remaining available space. Fitting SHALL preserve saved requested values and SHALL be recalculated when format, seconds, position, available display area, or locale-dependent time text changes.

#### Scenario: Reach a control boundary
- **WHEN** the user repeatedly selects decrease at the minimum or increase at the maximum of any control
- **THEN** the displayed, saved, and applied requested value stays at that boundary without wrapping

#### Scenario: Restore invalid settings
- **WHEN** saved settings contain a wrong data type, an out-of-range number, or a missing value
- **THEN** the modal and overlay use the same normalized values without crashing

#### Scenario: Requested geometry exceeds available space
- **WHEN** large size, padding, or distance settings or a display/format change would place part of the clock outside the available area
- **THEN** the complete text and background fit within that area using uniform scale reduction and bounded offsets
- **AND** the saved requested settings and selected corner do not change

#### Scenario: Locale changes the time text width
- **WHEN** the active overlay's locale changes with 12-hour time and seconds enabled at large appearance values
- **THEN** the complete time, including locale-dependent day-period text, fits within the available area in any selected corner
- **AND** saved appearance, corner, seconds, and format values are preserved

### Requirement: Appearance updates respect permissions and overlay lifecycle

Appearance updates SHALL operate on the existing overlay lifecycle on Android API 21 and later, without introducing a new permission requirement or implicitly starting a stopped overlay. Existing enable permission behavior SHALL be preserved, including overlay permission denial and notification permission denial where applicable. A missing overlay SHALL not cause an appearance update to crash or discard saved settings. Stopping the service SHALL release any added receivers, listeners, and callbacks and remove the overlay.

#### Scenario: Enable is denied
- **WHEN** overlay permission is denied on API 23+ or notification permission is denied during the existing enable flow on API 33+
- **THEN** no overlay is started by the appearance feature
- **AND** existing saved appearance values remain intact and the existing permission feedback is preserved

#### Scenario: Overlay stops before an appearance update
- **WHEN** the overlay service has stopped and an appearance update occurs
- **THEN** no window update targets a removed view and no overlay is started implicitly
- **AND** a subsequent successful enable uses the saved settings

#### Scenario: Repeated service cycles across API levels
- **WHEN** the user enables, adjusts, and disables the clock repeatedly on supported Android versions, including API 21–25, API 26+, and API 33+
- **THEN** each active service has one overlay with the saved appearance and stopping it leaves no added registered receiver or pending appearance callback

### Requirement: Alarm behavior survives appearance updates

Updating appearance SHALL preserve the alarm's current sound and blinking state. When an alarm is dismissed through the existing app flow, the overlay SHALL return to its normal background with the selected appearance retained. Appearance settings SHALL affect only the floating clock, leaving the main screen clock's size and spacing unchanged.

#### Scenario: Resize while the alarm is active
- **WHEN** the user adjusts size, edge distance, or spacing while the alarm is ringing and then dismisses the alarm from the app
- **THEN** sound and red blinking continue during adjustment and stop through the existing dismissal flow
- **AND** the overlay keeps the adjusted appearance and normal background afterward
- **AND** the main screen clock retains its existing geometry

### Requirement: Appearance row and modal use the dashboard visual style
The appearance row SHALL use the dashboard's colors, typography, spacing, surfaces, and visible focus treatment and SHALL visibly identify its PRO requirement. Selecting the row SHALL follow the access-dependent behavior defined in **Appearance modal is accessible with a TV remote**. For PRO users, the modal SHALL retain the existing decrease/increase buttons for edge distance (0–128 dp, 4 dp steps, default 16 dp), overall size (50–200%, 10 percentage point steps, default 100%), and internal spacing (0–300%, 10 percentage point steps, default 100%). Existing immediate saving, normalization, active-overlay updates, fitting rules, and disabled-overlay visibility SHALL remain unchanged. A supplementary preview SHALL add no editable controls or intermediate action.

#### Scenario: Adjust appearance after the visual refresh
- **WHEN** a PRO user selects Clock appearance, changes any existing value with its decrease/increase buttons, and closes with OK or Back
- **THEN** the modal SHALL open with the refreshed styling and the same three settings, limits, steps, and defaults
- **AND** accepted adjustments SHALL save and update the active overlay immediately, closing SHALL retain those values, and focus SHALL return to Clock appearance

#### Scenario: Free user sees the locked appearance row
- **WHEN** the floating clock is enabled for a Free user
- **THEN** Clock appearance SHALL be visibly marked as requiring PRO and SHALL remain navigable with the remote
- **AND** selecting it SHALL route to the PRO destination without exposing the appearance controls

#### Scenario: Overlay is disabled
- **WHEN** the floating clock is disabled
- **THEN** the appearance row SHALL remain hidden and its saved values SHALL be unchanged
- **AND** a supplementary preview SHALL not expose a new route to modify those settings

#### Scenario: Compare with the illustrative reference
- **WHEN** a PRO user's appearance row or modal is rendered in the redesigned app
- **THEN** only the existing three appearance controls SHALL be available, without an opacity slider, color selector, or intermediate Edit action
