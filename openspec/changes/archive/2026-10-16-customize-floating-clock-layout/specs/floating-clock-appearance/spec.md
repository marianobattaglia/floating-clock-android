# Spec Delta

## Purpose

Allow Android TV users to personalize the floating clock's edge distance, overall size, and internal spacing with a remote control, retaining those choices across sessions.

## ADDED Requirements

### Requirement: Appearance modal is accessible with a TV remote

While the floating clock is enabled, the app SHALL show a `Clock appearance` row with the existing overlay settings. Selecting it SHALL open a modal containing labeled edge distance, overall size, and internal spacing values, each with decrease and increase buttons. All controls SHALL be reachable using only D-pad and Select, show visible focus, and remain accessible on supported TV screen sizes. `OK` and Back SHALL dismiss the modal and return focus to its opening row without reverting saved adjustments.

#### Scenario: Adjust and close using the remote
- **WHEN** the user selects `Clock appearance`, navigates between all three controls, selects increase or decrease, and presses Back or selects `OK`
- **THEN** each selected adjustment is reflected in its displayed value and the modal closes with focus returned to `Clock appearance`
- **AND** the adjusted values remain saved

#### Scenario: Overlay is disabled
- **WHEN** the floating clock is disabled
- **THEN** the appearance row is hidden consistently with the position and format rows
- **AND** previously saved appearance values remain available for the next successful enable

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

Values SHALL remain within their supported ranges; further increase/decrease at a bound SHALL have no effect. Missing or unreadable values SHALL use their respective defaults, and numeric values outside the range SHALL be clamped. The overlay SHALL remain fully within the available window area, including at the largest size, largest spacing, and longest time format. If requested geometry cannot fit, it SHALL reduce the effective scale uniformly only as needed to fit the content and clamp edge offsets to the remaining available space. Fitting SHALL preserve saved requested values and SHALL be recalculated when format, seconds, position, or available display area changes.

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
