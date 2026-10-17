# Proposal

## Why

The floating clock currently has a fixed size, fixed internal padding, and a fixed distance from the screen edges. TV users need to adjust its readability and placement to suit their screen and viewing distance, and retain those choices between sessions.

## What Changes

- Add a `Clock appearance` settings row alongside the existing position and format rows while the floating clock is enabled.
- Open one modal with three independent decrease/increase controls: distance from the two edges of the selected corner, overall clock scale (background and text together), and internal spacing between text and background edges.
- Show each current value, support the TV remote with visible focus, and close with `OK` or Back. Apply and save each adjustment immediately, matching existing clock dialogs.
- Update the active overlay without restarting its service and restore the settings when it is enabled again or the app/service is recreated.
- Preserve the existing appearance when the new preferences are absent, the four corner choices, time formats, seconds, and alarm blinking.
- Bound adjustments and keep the clock within the available screen area.

## Capabilities

### New Capabilities

- `floating-clock-appearance`: Remote-operable appearance settings, persisted edge distance, overall scale and internal spacing, and their application to the overlay throughout its lifecycle.

### Modified Capabilities

None. The project currently has no main capability specifications.

## Impact

- `MainActivity.kt`: appearance row and modal, preference-backed state and live update notification; ensure the additional row and dialog remain reachable on TV.
- `FloatingClockSettings.kt`: additive preference keys, defaults, limits, shared normalization/geometry rules, and an appearance update action.
- `FloatingClockService.kt`: read appearance preferences on creation, update text/padding and window geometry in place, and release any added receiver or callbacks.
- `README.md`: document the controls, immediate persistence, and interaction between overall scale and padding.
- No new runtime dependencies, SDK changes, permissions, manifest components, or changes to alarm scheduling/audio are expected. A minimal test dependency may be added for geometry regression coverage. Implementation validation includes debug build, lint, and separate Android TV checks.
