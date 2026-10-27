# Codex Guide

## Project and code

FloatingClock is an Android TV app with a floating clock overlay and a background alarm. It has a single `app` module using Kotlin and Jetpack Compose for TV. Dependency versions live in `gradle/libs.versions.toml`; user-facing behavior is documented in `README.md`.

The main source code is in `app/src/main/java/com/marianobattaglia/floatingclock/`:

- `MainActivity.kt`: main screen, settings, and Compose dialogs.
- `FloatingClockService.kt`: foreground service and clock overlay window.
- `AlarmScheduler.kt` and `AlarmReceiver.kt`: alarm scheduling and delivery.
- `AlarmSoundService.kt`: alarm sound and state.
- `FloatingClockSettings.kt`: persisted keys, actions, and positions.
- `ui/theme/`: Compose theme.

Review `app/src/main/AndroidManifest.xml` when changing permissions or components.

## Workflow

- Communicate progress and results in Spanish. Preserve the language and style of existing code and documentation.
- Handle small fixes directly. For new features or broad changes, use OpenSpec to maintain the proposal, design, specifications, and tasks. A request for a proposal alone does not authorize implementation.
- Read the relevant skill before using it. Main workflows: `$openspec-explore`, `$openspec-propose`, `$openspec-apply-change`, and `$openspec-archive-change`. Project context lives in `openspec/config.yaml`.
- Keep changes focused and preserve the user's existing modifications. Update dependencies, Gradle, and the SDK only when required by the task.
- Follow the surrounding Kotlin style, using four-space indentation. Use the version catalog for new dependencies.
- Do not commit `local.properties`, machine-specific paths, credentials, keystores, or build artifacts.

## Requirements to preserve

- The UI must support D-pad navigation, visible focus, selection, and Back without relying on touch input.
- Maintain minSdk 21 compatibility and guard newer APIs by Android version.
- Review overlay, notification, and exact alarm permissions, including denial cases; TV devices may expose different settings.
- Keep the manifest, foreground service types, and service startup behavior consistent.
- Release windows, receivers, callbacks, and audio resources according to their lifecycle.
- Preserve persisted keys and actions; account for migration and consumers if they change.

## Validation

Use the Gradle wrapper. Currently, `gradlew` is not executable; on macOS/Linux, use:

```sh
bash ./gradlew --version
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:lintDebug
```

Android SDK Platform 36 and a JDK compatible with AGP 8.13.2 (17 or newer) are required. The code's JVM target is 11; this is distinct from the JDK running Gradle. Configure the SDK path locally through Android Studio or `local.properties`.

- For Android changes, run the debug build and lint. For documentation changes, review the diff and links; a build is unnecessary.
- There are no test sources in `app/src/test` or `app/src/androidTest`. Add regression tests when they provide useful coverage; do not report tests as passing if they do not exist or were not run.
- For UI, overlay, or alarm changes, validate the affected cases on Android TV: D-pad and Back; enabling/disabling the overlay; all four positions; 12/24-hour formats and seconds; scheduling/canceling alarms; sound, blinking, and dismissal from the app.
- If the SDK, network, permissions, or a device are unavailable, report the blocker. Distinguish builds, static review, and actual TV testing.
- When delivering changes, summarize what changed, the validation performed, and any remaining limitations.
