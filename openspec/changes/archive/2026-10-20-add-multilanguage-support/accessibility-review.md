# Accessibility review: localization change

Reviewed on 2026-10-20 against Google's Android accessibility guidance. Scope is the multilingual feature's resource extraction, labels, semantics, and interaction preservation; this is not a full-app accessibility certification.

## Guidance and evidence

| Google guidance | Localization implementation and verification |
| --- | --- |
| Provide meaningful, unique labels for interactive elements | Corner buttons expose their translated corner name. Appearance and alarm adjustment buttons expose complete translated action/target descriptions, rather than only plus/minus glyphs. Catalog verification covers all four languages. |
| Preserve standard component semantics | Existing Compose TV Buttons, Switches, and clickable Surfaces are retained. Instrumentation now asserts button role and click action for all four corner, six appearance, and four alarm adjustment controls. No custom speech engine or duplicated role names were added to translations. |
| Expose relevant state independently of visual styling | Found and corrected a gap in corner buttons: the visual X marker was not accompanied by a selected semantic state. `PositionButton` now exposes `selected`; regression assertions verify the previous corner becomes unselected and the newly chosen corner becomes selected. Existing click behavior and saved IDs are unchanged. |
| Use resource-backed text rather than hardcoded descriptions | The feature uses `stringResource` or current-context `getString` for app-owned labels and feedback. No English-only accessibility descriptions were exempted from translation. Native role/state announcements remain owned by the platform/accessibility service. |
| Preserve accessible interaction and test semantics | Existing tests cover remote activation, adjustment, position selection, modal Back/focus return, and complete dialog control traversal. The preceding eight-case language/resolution matrix passed before the selected-state addition. |
| Complement automation with actual accessibility services | After the selected-state fix, all seven instrumented tests passed with installed TalkBack 14.2.0.740629310 leanback enabled on Android TV API 36, English, 1080p. `dumpsys accessibility` confirmed TalkBack was bound with spoken feedback during execution. TalkBack was restored to its original disabled state afterward. |

Debug application/test APK assembly and translation verification passed after the change. Lint still reports the same seven pre-existing errors; their messages match the pristine baseline. `git diff --check` passes. No accessibility-check framework dependency or Compose upgrade was introduced for this focused review.

## Limits and remaining acceptance

The semantic tests and execution with TalkBack active establish the checked properties and interaction behavior. They do not establish the exact spoken utterances, pronunciation, speech language selection, absence of redundant announcements, or accessibility-focus traversal in every language. The project owner subsequently confirmed manual testing was OK and requested closing both changes with no pending tasks. That acceptance closes the tracked manual work; no per-language spoken utterance log was supplied. Keyboard/D-pad focus assertions are not assertions about TalkBack accessibility focus.

No claim is made that the entire app, overlay, contrast, font scaling, or all supported Android versions have passed an accessibility audit. The later owner-reported manual acceptance closes the full manual acceptance and legacy API/permission tasks; it does not expand the agent-run evidence in this review.

## Official references

- [Google: Principles for improving app accessibility](https://developer.android.com/guide/topics/ui/accessibility/principles): meaningful labels, built-in controls, relevant state, and actions.
- [Google: Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): semantic roles, state, and actions exposed to accessibility services.
- [Google: Compose API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): standard component support and descriptive labels for non-text controls.
- [Google: Accessibility testing in Compose](https://developer.android.com/develop/ui/compose/accessibility/testing): semantic assertions and manual testing with TalkBack complement one another.
