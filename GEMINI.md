# Project Overview

This is a Kotlin Multiplatform project named "Puzzroom" that targets Android, iOS, Web, and Desktop. The project is built with Gradle and uses Compose Multiplatform for the user interface.

The application allows users to create projects, design rooms, and add furniture to them. The core logic is shared across all platforms in the `composeApp` module, which follows a clean architecture pattern with `data`, `di`, `domain`, and `ui` packages.

## Building and Running

The project can be built and run for different platforms using the following Gradle commands:

*   **Android:**
    ```shell
    ./gradlew :composeApp:assembleDebug
    ```
*   **Desktop (JVM):**
    ```shell
    ./gradlew :composeApp:run
    ```
*   **Web (Wasm):**
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
*   **Web (JS):**
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
*   **iOS:**
    Open the `/iosApp` directory in Xcode and run it from there.

## Development Conventions

*   **Architecture:** The project follows a clean architecture pattern, separating data, domain, and UI concerns.
*   **UI:** The UI is built with Compose Multiplatform, allowing for a shared UI across all target platforms.
*   **State Management:** The application uses a combination of `AppState` for temporary UI state and `ProjectViewModel` for persistent data.
*   **Dependencies:** The project uses several libraries, including:
    *   `coil` for image loading.
    *   `kotlinx.serialization` for JSON serialization.
    *   `navigation-compose` for navigation.
    *   `imagepicker` for selecting images.
*   **Testing:** The project uses `kotlinx-coroutines-test` for testing.

## Active Technologies
- Kotlin Multiplatform (KMP) + Compose Multiplatform, Material 3, `voyager` (navigator in TerminalScreen) (003-code-viewer-back-nav)
- Transient UI state in `TerminalScreen` (using `mutableStateOf`) (003-code-viewer-back-nav)

## Recent Changes
- 003-code-viewer-back-nav: Added Kotlin Multiplatform (KMP) + Compose Multiplatform, Material 3, `voyager` (navigator in TerminalScreen)
