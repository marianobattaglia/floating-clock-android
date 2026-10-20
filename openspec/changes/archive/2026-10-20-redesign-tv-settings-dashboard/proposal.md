# Proposal

## Why

The existing clock and alarm features already provide the required functionality. This change refreshes their visual presentation using the supplied AI image as layout inspiration, while keeping the current settings, controls, and configuration flows unchanged.

## What Changes

- Introduce a cohesive dashboard presentation: existing app logo at the top of a left rail, only Home and Settings entries, the actual app name and existing live clock in the content header, current settings rows on the left, and supplementary previews on the right.
- Use Home and Settings as shortcuts to the header and current settings area within the same screen. Do not add a separate status overview, duplicate settings screen, or a new navigation history.
- Refresh colors, typography, spacing, icons, cards, focus indicators, and existing dialogs. Preserve the existing app identity and language.
- Update the supplementary preview when a row or its switch receives remote focus, without invoking its action. OK/Select or tap retains the row's current action. Position, format, appearance, and idle alarm rows still open their existing dialogs immediately. The floating-clock row still toggles enablement; selecting a ringing alarm still stops it. There is no intermediate Edit action or additional confirmation.
- Keep every current option, value range, step, default, save/cancel behavior, visibility rule, permission flow, and alarm/overlay behavior. No controls are inferred from the illustrative image.
- Hide the bottom advertising placeholder entirely, including its text and occupied space. Advertising integration remains future work.

## Capabilities

### New Capabilities

- `tv-settings-dashboard`: Visual dashboard shell, rail shortcuts, supplementary previews, visible TV focus, and hidden advertising placeholder, with existing interaction semantics preserved.

### Modified Capabilities

- `floating-clock-appearance`: Add a visual styling requirement for the existing row and modal. Existing functional requirements, direct modal opening, controls, immediate saving, and return to the opening row remain unchanged.

## Impact

- Presentation changes in `MainActivity.kt`, `FloatingClockAppearanceRow.kt`, and `ui/theme/`, with small reusable visual composables as needed. Reuse existing action callbacks and UI state; avoid unrelated state/controller refactors.
- Read existing values for previews without changing persisted keys, broadcasts, geometry calculations, service lifecycle, or alarm scheduling. The preview renderer adds no service or permission action beyond the selected row's existing behavior.
- No planned changes to services, schedulers, manifest, SDK, dependencies, permissions, or error handling. Record any pre-existing functional defect separately rather than expanding this visual change.
- Update README screenshots/layout descriptions while keeping the existing usage steps valid. During implementation, run the existing unit tests, debug build, lint, and separate Android TV regression checks.

## Non-goals

- New settings or replacement controls; color/opacity controls or sliders from the reference; an Edit step; a new Back sequence; profiles/accounts; status overview; advertising SDKs; permission or alarm fixes; service or preference migrations.
