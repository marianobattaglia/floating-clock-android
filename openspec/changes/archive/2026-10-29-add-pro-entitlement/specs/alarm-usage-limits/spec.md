# Spec Delta

## Purpose

Defines the Free alarm activation allowance and the PRO bypass while preserving the existing one-alarm-at-a-time behavior and ensuring every scheduled or ringing alarm can always be canceled.

## ADDED Requirements

### Requirement: Free users can activate one alarm per rolling 24 hours
The app SHALL allow a Free user one successful transition from no scheduled alarm to a scheduled alarm in each rolling 24-hour period. A successful exact or approximate schedule SHALL start the period. Opening or editing the configuration dialog, changing the selected time, a denied permission, or a failed scheduling attempt SHALL NOT consume the allowance. The allowance SHALL persist across app and device restarts. PRO users SHALL have no activation cooldown, while the existing limit of one scheduled alarm at a time SHALL remain in effect for all users.

#### Scenario: First successful Free activation
- **WHEN** a Free user activates an alarm and exact or approximate scheduling succeeds
- **THEN** the app SHALL schedule the alarm and record the activation time
- **AND** another activation from the disabled state SHALL be unavailable until 24 hours have elapsed

#### Scenario: Failed activation does not consume the allowance
- **WHEN** a Free user's scheduling attempt fails or required access is denied and no alarm is scheduled
- **THEN** the app SHALL leave the alarm disabled and SHALL preserve the unused activation allowance
- **AND** it SHALL retain the existing permission and approximate-timing feedback

#### Scenario: Wait for the Free allowance to renew
- **WHEN** 24 hours have elapsed since the last successful Free activation
- **THEN** the app SHALL allow the user to activate the next alarm
- **AND** the remaining wait time, when applicable, SHALL be presented in localized text that is reachable with D-pad and Select

#### Scenario: PRO user activates alarms
- **WHEN** a PRO user schedules, cancels, or schedules another alarm
- **THEN** the app SHALL not apply the Free cooldown
- **AND** it SHALL continue to maintain at most one scheduled alarm at a time

#### Scenario: Replace the time of an already scheduled alarm
- **WHEN** a Free user changes the time of the existing scheduled alarm while it remains enabled and scheduling succeeds
- **THEN** the app SHALL replace that same alarm without creating a second alarm or moving the original cooldown start time

### Requirement: Alarm cancellation is always available
The app SHALL allow a user of any access level to cancel a scheduled alarm or dismiss a ringing alarm at any time, including during the Free cooldown. Cancellation SHALL stop the existing alarm schedule and sound/blinking behavior as applicable. Canceling SHALL NOT restore or extend the Free activation allowance; a new activation from the disabled state remains subject to the original 24-hour period.

#### Scenario: Cancel a scheduled alarm during cooldown
- **WHEN** a Free user cancels an enabled alarm before it rings
- **THEN** the app SHALL disable and cancel the alarm immediately
- **AND** it SHALL keep the activation time so the user cannot reactivate before the cooldown expires

#### Scenario: Dismiss a ringing alarm during cooldown
- **WHEN** a Free user dismisses an alarm while it is ringing
- **THEN** the app SHALL stop the sound and blinking immediately
- **AND** cancellation SHALL remain available even though the user cannot activate another alarm yet

#### Scenario: Use the alarm on supported Android versions
- **WHEN** alarm scheduling, cancellation, or dismissal occurs on API 21 and later, including versions requiring exact-alarm access
- **THEN** the allowance SHALL count only after the existing scheduler reports a successful exact or approximate schedule
- **AND** permission denial, fallback scheduling, process recreation, and cancellation SHALL not cause a crash or false enabled state
