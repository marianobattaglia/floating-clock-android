# Spec Delta

## MODIFIED Requirements

### Requirement: Appearance modal is accessible with a TV remote

While the floating clock is enabled, the app SHALL show a localized `Clock appearance` row with the existing overlay settings. Selecting it SHALL open a modal containing localized labels for edge distance, overall size, and internal spacing values, each with decrease and increase buttons and localized accessible action descriptions. All controls SHALL be reachable using only D-pad and Select, show visible focus, and remain accessible on supported TV screen sizes in English, Spanish, Portuguese, and French. The localized `OK` action and Back SHALL dismiss the modal and return focus to its opening row without reverting saved adjustments. The English names used in this specification identify controls semantically rather than requiring English display text.

#### Scenario: Adjust and close using the remote
- **WHEN** the user selects `Clock appearance`, navigates between all three controls, selects increase or decrease, and presses Back or selects `OK`
- **THEN** each selected adjustment is reflected in its displayed value and the modal closes with focus returned to `Clock appearance`
- **AND** the adjusted values remain saved

#### Scenario: Overlay is disabled
- **WHEN** the floating clock is disabled
- **THEN** the appearance row is hidden consistently with the position and format rows
- **AND** previously saved appearance values remain available for the next successful enable

#### Scenario: Use translated appearance controls
- **WHEN** the user opens the appearance modal in Spanish, Portuguese, or French using only the remote
- **THEN** its title, labels, help, close action, and accessible increase/decrease descriptions use that language
- **AND** all controls remain readable and reachable with visible focus and the same ranges, steps, immediate save, and Back behavior

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
