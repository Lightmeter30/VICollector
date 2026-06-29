# Repository Guidelines

## Project Structure & Module Organization

VICollector is a single-module Android project. The Gradle root files live at the repository root: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, and the Gradle wrapper scripts.

- `app/` contains the Android application module.
- `app/src/main/java/com/example/vicollector/` contains Kotlin source code.
- `app/src/main/res/` contains Android resources such as styles.
- `app/src/test/java/` contains local JVM unit tests.
- `docs/superpowers/plans/` contains implementation planning notes.

Keep generated output under `build/` and `app/build/` out of hand-written changes.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from the repository root.

- `./gradlew.bat :app:assembleDebug` builds the debug APK on Windows.
- `./gradlew.bat :app:testDebugUnitTest` runs local JVM unit tests.
- `./gradlew.bat :app:compileDebugKotlin` checks Kotlin compilation quickly.
- `./gradlew.bat clean` removes generated Gradle build output.

For non-Windows shells, use `./gradlew` with the same tasks.

## Coding Style & Naming Conventions

Write application code in Kotlin targeting Java 17. Use four-space indentation, idiomatic Kotlin data classes for models, and explicit names for time units such as `timestampNs` or `durationSec`. Keep package names under `com.example.vicollector`.

Use `PascalCase` for classes and objects, `camelCase` for functions and properties, and `UPPER_SNAKE_CASE` for constants. Prefer small files grouped by responsibility, following the existing `core/model`, `core/error`, `core/constant`, `config`, and `utils` package layout.

## Testing Guidelines

The project currently uses JUnit 4 for local unit tests. Place tests in `app/src/test/java` with names ending in `Test`, for example `CoreModelTest`. Test method names should describe the behavior being verified, such as `imuSampleKeepsRawTimestamp`.

Run `./gradlew.bat :app:testDebugUnitTest` before submitting model, configuration, or utility changes. Add focused tests for new data contracts, constants, and error behavior.

## Commit & Pull Request Guidelines

Use concise imperative commit messages with an optional scope, for example `app: add IMU sample model tests` or `docs: update contributor guide`. Keep each commit focused on one logical change and avoid mixing generated build output with source changes.

Pull requests should include a short description, validation commands run, related issue or plan link when available, and screenshots or screen recordings for UI changes. Call out Android permission, storage, camera, or sensor behavior changes explicitly.

## Security & Configuration Tips

Do not commit local machine paths or SDK settings from `local.properties`. Keep secrets, signing keys, and device-specific capture output outside the repository.
