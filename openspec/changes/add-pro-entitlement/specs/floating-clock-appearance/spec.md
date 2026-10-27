# Spec Delta

## MODIFIED Requirements

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
