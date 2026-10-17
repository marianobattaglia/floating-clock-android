# Design

## Context

See [proposal.md](proposal.md) for motivation and [the capability delta](specs/floating-clock-appearance/spec.md) for acceptance criteria.

`MainScreen` conditionally displays the position and format rows when `floatingEnabled` is true. Their dialogs save changes immediately in `floating_clock_prefs` and send package-scoped broadcasts. Settings are refreshed on activity resume.

`FloatingClockService` owns a single `TextClock` in a non-focusable, wrap-content overlay window. It currently uses 24 sp text, explicit padding of 24/16/24/16 physical pixels, and 16 dp offsets from the selected corner. Position, format, and seconds receivers update that same view. Alarm blinking changes its background color using a handler. The service unregisters receivers and removes its window on destruction.

This design spans UI, persisted state, and service geometry, so a design artifact is necessary. The manifest and Gradle configuration were inspected; no new component, permission, runtime dependency, or SDK change is needed for this feature.

## Goals / Non-Goals

**Goals:**

- Share defaults, bounds, normalization, and geometry rules between the UI and service.
- Keep preferences as the source of truth and update the existing view on the main thread.
- Preserve default rendering and existing keys/actions across upgrades.
- Make focus traversal and screen fitting deterministic and independently verifiable.

**Non-Goals:**

- Independent horizontal/vertical edge offsets or padding controls, dragging, arbitrary width/height, font/color changes, or resizing the main screen clock.
- A new overlay renderer, settings storage migration, service architecture rewrite, or changes to alarm scheduling/audio.
- A separate preview or reset button. The active overlay provides live feedback; closing the modal retains adjustments.

## Decisions

### 1. One modal with three bounded steppers

Add `FloatingClockAppearanceRow` after `FloatingClockFormatRow`, within the same enabled condition. Use the existing `SettingsRow`, TV Material buttons, `Dialog`, and surface styling with English UI labels to match the app. Proposed labels are `Clock appearance`, `Edge distance`, `Overall size`, and `Internal spacing`.

| Setting | Range | Step | Default | Display |
| --- | --- | --- | --- | --- |
| Edge distance | 0–128 dp | 4 dp | 16 dp | `16 dp` |
| Overall size | 50–200% | 10 percentage points | 100% | `100%` |
| Internal spacing | 0–300% of original padding | 10 percentage points | 100% | `100%` |

These ranges and increments are proposal defaults because the request did not specify them. One edge value affects both adjacent edges of the selected corner. One padding factor affects all sides while retaining their existing proportions.

Each row contains a label, numeric value, `−`, and `+`. Directional navigation traverses the controls predictably; Select performs one step. Buttons remain focusable at bounds and perform a no-op there, avoiding focus loss when a focused button reaches a limit. Provide meaningful accessibility labels, request initial focus inside the modal, and restore focus to the opening row on dismissal. Use bounded scrolling where necessary for the extra settings row and modal on shorter TV layouts.

Persist each change immediately. `OK` and Back only close, consistent with the existing dialogs. Reload values when opening and on resume. Sliders were considered, but explicit buttons make D-pad operation and increments easier to discover. An Apply/Cancel transaction was considered but would differ from existing clock settings.

### 2. Additive, normalized preferences

Define three integer keys in `FloatingClockSettings.kt`: `clock_edge_distance_dp`, `clock_scale_percent`, and `clock_padding_percent`, with constants for defaults and limits and a small shared value model/reader. Preserve all existing keys and broadcasts. Read each setting independently, fall back for missing/wrong-type values, and clamp out-of-range integers before UI display or geometry calculation.

The UI saves the accepted values with `SharedPreferences.apply()` before notifying the service. No schema migration or eager preference overwrite is needed. DataStore was considered but adds migration and dependencies without improving this small feature.

### 3. Re-measure text and padding rather than transform the view

For requested overall scale `s = scalePercent / 100` and padding factor `p = paddingPercent / 100`, calculate:

- Text size: `24 sp × s`.
- Left/right explicit padding: `round(24 px × p × s)`.
- Top/bottom explicit padding: `round(16 px × p × s)`.
- Requested X/Y offsets: `round(edgeDistanceDp × density)` with the current corner gravity.

Retain wrap-content geometry and existing font padding behavior. Setting `textSize` and explicit padding changes actual measured bounds so the background/window encloses the text. `scaleX`/`scaleY` was rejected because visual transforms leave window bounds and anchoring harder to reason about. Fixed window dimensions were rejected because the time width changes with format, seconds, and digits.

Padding remains based on the existing physical pixels so missing preferences exactly preserve the current appearance on every density. Converting those bases to dp in this change would unexpectedly enlarge existing overlays on denser TVs. Percentages expose a useful relative adjustment without presenting legacy pixels as density-independent units. Edge distance remains independent of overall scale.

### 4. Fit geometry without changing requested preferences

Resolve available overlay window bounds using version-guarded window metrics on API 30+ and compatible display bounds on older APIs. Account for the usable area exposed by the window configuration; avoid introducing permission or unrelated inset policy changes.

Measure the longest time representation for the active 12/24-hour and seconds settings, including the applicable AM/PM text and digit widths, using the same text/font metrics. Use this as the fitting envelope so ordinary clock ticks do not resize the component. If it exceeds available width or height, reduce the effective overall scale by the limiting ratio and remeasure with pixel rounding. Apply that same effective scale to both text and padding. Then clamp each requested edge offset to `0..max(0, availableDimension - measuredDimension)`. Requested inset is best effort when space is insufficient; preserving the complete time takes precedence.

Recalculate on appearance, position, format, seconds, and display configuration changes. Keep requested preferences untouched so a larger display or shorter format can use the user's full requested size again. Keep this calculation separate from receiver code for focused geometry tests. Trusting fixed percentage limits alone was rejected because font scale, density, and display dimensions can vary.

### 5. One appearance update action for the existing service

Add `ACTION_CLOCK_APPEARANCE_CHANGED` with a package-scoped sender. The receiver reads the normalized preference snapshot instead of depending on multiple broadcast extras. Register with `RECEIVER_NOT_EXPORTED` on API 33+ and the compatible existing pattern below that version. Do not start the service to deliver an appearance update.

Apply saved appearance during service creation before attaching the window. For a running view, update its text size, padding, and layout parameters in place on the main thread; only call `updateViewLayout` while attached. Reuse the same geometry path from existing position/format/seconds handlers. Preserve the background and blink handler state during geometry changes. Unregister the added receiver and remove any appearance/display callbacks on destruction.

A preference listener was considered but would introduce a second notification mechanism alongside the existing broadcasts. Restarting the service was rejected because it would interrupt the overlay and could disrupt visible alarm blinking.

### 6. Validation stays explicit about its limits

Add focused regression coverage for normalization, proportional geometry, bounds, and fitting using pure calculations where possible. The repository currently has no test sources or unit-test runner dependency; if local JVM tests are introduced, declare the minimal test dependency through the version catalog. Avoid tests that only assert constants or duplicate UI wiring.

Run the Gradle wrapper debug build and lint during implementation. Separately validate D-pad/Back and focus, all corners, min/default/max combinations, 12/24-hour formats with/without seconds, persistence/recreation, permission denial, and alarm blinking/dismissal on Android TV. Include pre-26 compatibility and API 33+ receiver/notification paths when those environments are available; report unavailable environments as limitations.

## Risks / Trade-offs

- [Percent-based spacing is less familiar than dp] → Explain that 100% matches the original spacing and show the live overlay while adjusting.
- [The modal may cover a centered overlay at large insets] → Keep the modal compact and let users close/reopen it without losing changes; no preview dependency is introduced.
- [Mixed historical pixel padding and sp text] → Preserve defaults deliberately and document the formulas; screen fitting covers unusually large font/display settings.
- [An additional row overflows a short TV viewport] → Ensure focus-aware scrolling of affected content and validate on the smallest available TV configuration.
- [Late broadcasts or configuration changes target a detached view] → Guard attachment, keep updates on the main thread, and release added receivers/callbacks at service teardown.
- [Permission helpers already have pre-23 limitations outside this change] → Preserve their current enable flow and scope API 21 coverage to the new settings/rendering path; report existing unrelated failures separately.

## Migration Plan

1. Ship additive preference keys with legacy-compatible defaults and the new modal/update path together.
2. Existing installs render identically until a user changes an appearance setting; no existing preference is rewritten or renamed.
3. Verify reopening, service recreation, and off/on cycles restore the new keys and retain old settings.
4. Rolling back to the prior version ignores the new keys and uses the original fixed appearance. No data conversion or cleanup is required.
