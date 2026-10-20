# Design

## Context

See [proposal.md](proposal.md) for the visual scope. The current `MainScreen` in `MainActivity.kt` places a live clock above the floating-clock toggle, conditional position/format/appearance rows, and an always available alarm row. Existing rows own their dialog state and action callbacks. Position, format, and appearance save accepted changes immediately. The alarm dialog retains its separate time controls and ACTIVATE/CANCEL actions, while selecting a ringing alarm dismisses it.

`FloatingClockAppearanceRow.kt` already provides bounded stepper controls and restores focus to its row when closed. `FloatingClockSettings.kt`, `ClockAppearanceGeometry.kt`, the overlay service, and alarm components define the functionality to preserve. The manifest and dependency catalog establish the current API 21+ environment. Existing unit tests in `ClockAppearanceTest.kt` cover appearance normalization and geometry; there are currently no instrumented test sources.

The AI image is illustrative. Its alternate brand, opacity/color references, or apparent slider controls do not define the app's functionality. All existing appearance requirements remain valid; the appearance delta adds only a visual presentation requirement.

## Goals / Non-Goals

**Goals:** Reorganize and restyle the existing screen and dialogs, add the requested rail and passive previews, and hide the advertising placeholder while preserving current options, action sequences, persistence, and runtime behavior.

**Non-Goals:** New controls or settings, an intermediate Edit action, replacement sliders, separate configuration routes, a new Back sequence, a status overview, state/controller architecture changes, functional bug fixes, or service/permission changes.

## Decisions

### 1. Reorganize one existing screen

Use the existing Compose for TV stack and extract only reusable presentation components where useful: rail, header, row decoration, preview surface, with no visible advertising placeholder. Keep existing rows and dialog owners mounted in a stable composition instead of creating separate Home/Settings copies. Retain their visibility conditions and callback implementations.

Home brings the existing header into view; Settings brings the existing settings area into view and focuses its first available control. These are shortcuts within the same screen, without a navigation stack or separate status screen. Main controls remain directly reachable, and using a rail shortcut does not reset dialog state or settings. Add only the directional links needed to reach the new rail, preserving the existing configuration and screen-level Back semantics.

Proposed arrangement, with illustrative proportions:

```text
+--------+------------------------------------------------------+
| Logo   | FloatingClock                         existing clock |
|        |----------------------------+-------------------------|
| Home   | Show Floating Clock   switch| Supplementary preview   |
|Settings| Clock position       value | updated by the row's    |
|        | Clock format         value | existing OK/tap action  |
|        | Clock appearance     value |                         |
|        | Alarm                switch| Read-only content       |
+--------+------------------------------------------------------+
```

Use a compact rail and proportional content columns. The workspace uses the height previously occupied by the advertising placeholder. Bound the workspace to the remaining height and allow scrolling where needed. Tune spacing at 720p and 1080p, keeping readable text, current controls, and focus borders fully visible. Avoid matching the reference's literal pixel dimensions.

Alternative considered: separate Home and Settings screens with an added overview. Rejected because the approved scope is a visual refresh of existing functionality.

### 2. Supplement current callbacks without replacing them

On row focus acquisition (including focus on a child switch), update only transient preview selection. On the row's existing OK/Select or tap callback, invoke the original action exactly once and retain preview selection. Do not delay or suppress that action. Position, format, appearance, and idle alarm still open their existing dialogs immediately. Floating-clock activation still toggles via the current flow. Selecting a ringing alarm still stops it. Switch targets retain their current callbacks and interaction meaning.

The preview begins with a neutral prompt until a setting receives focus. Moving Up/Down then updates the preview immediately without opening a modal, toggling a switch, or changing preferences. It has no actions, focus targets, permission requests, or preference writes. When a dialog opens, it keeps modal focus; do not add an in-panel editor or an Edit button. Retain dialog modality and placement unless a purely visual adjustment can leave the preview visible without compromising the existing controls. If the modal obscures the underlying preview, the direct configuration flow takes precedence; the preview remains available when the modal closes.

Keep selection in UI memory only. When a selected row becomes hidden, display the neutral preview instead of exposing hidden settings. Refresh the passive preview from current existing values after adjustments and dialog dismissal. Reuse values already held by the screen/rows; expose read-only snapshots or presentation callbacks only where needed. Any new UI-only observation must be lifecycle-scoped and released on disposal. Do not restructure permission launchers, receivers, service-state reconciliation, or clocks as an unrelated refactor.

Alternative considered: first select a preview and then choose Edit. Rejected because it adds a user step and changes existing row actions.

### 3. Preserve every current configuration control

Restyle the existing dialogs rather than rebuilding their interaction models. Position keeps its four corner buttons; format keeps its existing switches; appearance keeps its three decrease/increase control groups; alarm keeps hour/minute controls, ACTIVATE, CANCEL, and its switch behavior.

Appearance remains edge distance 0–128 dp in 4 dp steps with 16 dp default, overall size 50–200% in 10-point steps with 100% default, and internal spacing 0–300% in 10-point steps with 100% default. Reuse current normalization, persistence, broadcasts, and geometry. The actual overlay's rendering, alarm blinking colors, and time updates are unchanged.

Keep modal OK/Back behavior and current focus restoration. Do not introduce Back handlers that traverse the preview, settings region, and rail before exiting. Passive visual elements must not steal focus. Existing controls must remain operable with D-pad and Select after layout changes.

Alternative considered: adopting sliders or opacity/color options from the image. Rejected because the image is visual inspiration and the user explicitly requires the existing modification options.

### 4. Render a passive local preview

Use a bundled landscape photograph provided by the user as the TV background, with the live clock drawn on top. The image is static and available offline, with a centered crop to fill the existing frame without distortion. Depict only existing values. Read the current clock position, format, seconds, and appearance and use a consistent virtual display scale so the miniature communicates their relative effect. Reuse read-only geometry helpers where useful without changing them or fitting the real overlay to the preview bounds.

For enablement and alarm, display current existing values/state without introducing a new runtime-state source or demonstration behavior. Rendering must not start/bind services, create windows, request permissions, play sound, or schedule/cancel an alarm. The original row callback may still perform its existing side effects; distinguish these from passive rendering during review and regression checks.

Alternative considered: a real overlay window or interactive settings inside the preview. Rejected because either would introduce behavior beyond visual presentation.

### 5. Shared visual styling and hidden advertising placeholder

Apply a coherent palette inspired by the image: dark navy/charcoal surfaces, readable primary/secondary text, blue accents, consistent typography, rounded cards, and visible focus borders. Reuse existing launcher branding and English labels. Apply the same visual language to retained dialogs and their existing controls. Restrict theme changes to activity presentation rather than service-rendered overlay appearance.

Do not render the bottom advertising strip, label, border, or empty reserved height. Keep advertising integration as future work; do not add an SDK, network behavior, external link, or manifest requirement.

Alternative considered: redesigning the floating overlay itself or integrating ads now. Rejected because this change concerns the app's screen presentation and a future layout reservation.

## Risks / Trade-offs

- [An existing modal can cover the supplementary preview] → Keep the direct modal flow and its focus intact; let the preview reflect the selection and remain visible after dismissal instead of inserting another editing step.
- [A two-column layout reduces space] → Use bounded scrolling, test 720p/1080p, and prioritize existing controls and readable focus indicators over decorative detail.
- [UI extraction can accidentally invoke an action twice or remount state] → Retain stable owners and callbacks, invoke each action once, and compare each existing row/switch interaction before and after the visual change.
- [The miniature can misrepresent size or corner offsets] → Use one virtual-display scale and compare representative settings against the real overlay without changing its geometry engine.
- [Functional defects may be discovered during visual validation] → Record them as separate findings and report affected checks accurately; do not silently add permission, scheduler, lifecycle, or alarm changes to this scope.

## Migration Plan

1. Restyle/rearrange existing presentation components while keeping controls, callbacks, persisted keys, and services intact.
2. Add passive preview selection on row focus while preserving current row activations and the two rail shortcuts, without adding configuration steps or Back history.
3. Run existing unit tests, debug build, and lint; separately record Android TV visual and functional regression results and unavailable environments.
4. Update README layout descriptions and screenshots while retaining valid existing usage instructions. No data or service migration is required, and rolling back presentation changes leaves stored settings compatible.

### Clock visibility follow-up

Draw the miniature floating clock over the landscape only when the existing floating-enabled state is true. Keep the landscape when disabled. Hide the large fixed header clock while enabled and restore it when disabled. Keep the existing time observation mounted so the preview time and format remain live even when the header text is hidden; add no timer or service action. Alarm preview information remains unchanged.
