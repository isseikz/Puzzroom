# Puzzroom Agent Instructions

## 🏗️ Architecture
- **KMP Monorepo** with **Atomic Design** UI.
- **UI Hierarchy**: `Atoms` → `Molecules` → `Organisms` → `Templates` → `Pages`.
- **Naming**:
    - Atoms: `App[Element].kt`
    - Molecules: `[Descriptive][Purpose].kt`
    - Organisms: `[Feature][Component].kt`
    - Templates: `[Purpose]Template.kt`
    - Pages: `[Feature]Page.kt`
- **Data Flow**: State Down (ViewModel → UI), Events Up (UI → ViewModel).
- **Theme**: Use `MaterialTheme.colorScheme`. **Do not hardcode colors.**
- **Modules**:
    - `:composeApp`: Main Puzzroom app.
    - `:shared-ui`: Shared component library. **Changes here affect all apps.**
    - `:nlt-app`: NLT application.
    - `:unison-app`, `:quick-deploy-app`, `:mobile-vibe-terminal`: Other applications.

## 🛠️ Commands
- **Run ComposeApp**:
    - Android: `./gradlew :composeApp:assembleDebug`
    - Desktop: `./gradlew :composeApp:run`
    - Web (Wasm): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
    - Web (JS): `./gradlew :composeApp:jsBrowserDevelopmentRun`
- **Testing**: `./gradlew :[module]:test`
- **Coverage**: `./gradlew koverHtmlReport`
- **Syncing shared-ui**: `./gradlew clean build` (if changes aren't reflecting)

## ⚠️ Gotchas
- **NLT App**: Requires `nlt-app/google-services.json` (copy from `.example`).
- **Platform Code**: Use `expect`/`actual` in appropriate source sets (e.g., `androidMain`, `iosMain`).
