# Spec Delta

These dashboard changes are part of the deferred account-profile phase, not the first release. The first release keeps the current Home and Settings destinations and does not show a Profile destination.

## MODIFIED Requirements

### Requirement: Dashboard arranges the existing screen visually

The app SHALL present the existing app logo at the top of a left rail containing Home, Settings, and Profile; the actual app name and a live clock in the content header while the floating clock is disabled; existing settings rows on the left; and supplementary previews on the right while the dashboard is selected. Home and Settings SHALL retain their existing shortcut behavior. Selecting Profile SHALL open the account profile area. Home, Settings, and Profile SHALL NOT change clock preferences, alarm state, or services merely by receiving focus. The app SHALL use cohesive colors, typography, rounded surfaces, icons, spacing, and visible focus inspired by the reference image while retaining its current brand and language.

#### Scenario: Use the rail shortcuts
- **WHEN** the user selects Home or Settings with OK/Select or tap
- **THEN** the corresponding header or existing settings region is brought into view without changing clock settings, alarm state, or services
- **AND** the same existing configuration controls remain available

#### Scenario: Open Profile from the rail
- **WHEN** the user selects Profile with OK/Select or tap
- **THEN** the account profile area opens and existing clock settings, alarm state, and services remain unchanged
- **AND** selecting Home or Settings returns to the existing dashboard shortcuts and settings behavior

#### Scenario: Read the header clock
- **WHEN** the user views the redesigned screen or changes clock format through the existing dialog
- **THEN** the header displays the live time with the existing 12/24-hour and seconds behavior while the floating clock is disabled, and hides that fixed clock while the floating clock is enabled
- **AND** floating-clock appearance controls do not resize the header clock
