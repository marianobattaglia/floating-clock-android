# Spec Delta

## ADDED Requirements

### Requirement: Appearance row and modal use the dashboard visual style

The existing appearance row and modal SHALL use the dashboard's colors, typography, spacing, surfaces, and visible focus treatment without changing their functional contract. Selecting the row SHALL continue to open the modal directly, and OK or Back SHALL close it and return focus to that row. The modal SHALL retain the existing decrease/increase buttons for edge distance (0–128 dp, 4 dp steps, default 16 dp), overall size (50–200%, 10 percentage point steps, default 100%), and internal spacing (0–300%, 10 percentage point steps, default 100%). Existing immediate saving, normalization, active-overlay updates, fitting rules, and disabled-overlay visibility SHALL remain unchanged. A supplementary preview SHALL add no editable controls or intermediate action. All existing requirements of this capability remain in force.

#### Scenario: Adjust appearance after the visual refresh
- **WHEN** the user selects Clock appearance, changes any existing value with its decrease/increase buttons, and closes with OK or Back
- **THEN** the modal opens directly with refreshed styling and the same three settings, limits, steps, and defaults
- **AND** accepted adjustments save and update the active overlay immediately, closing retains those values, and focus returns to Clock appearance

#### Scenario: Overlay is disabled
- **WHEN** the floating clock is disabled
- **THEN** the appearance row remains hidden as before and its saved values are unchanged
- **AND** a supplementary preview does not expose a new route to modify those settings

#### Scenario: Compare with the illustrative reference
- **WHEN** the appearance row or modal is rendered in the redesigned app
- **THEN** only the existing three appearance controls are available, without an opacity slider, color selector, or intermediate Edit action
