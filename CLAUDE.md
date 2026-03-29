# Puzzroom Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-28

## Active Technologies
- Kotlin 2.x (Multiplatform) + Compose Multiplatform, Voyager ScreenModel, Koin DI (005-modifier-keys-handling)
- N/A (no persistence for modifier state — ephemeral UI state) (005-modifier-keys-handling)
- Kotlin 2.x (Multiplatform, `iosArm64`, `iosSimulatorArm64`) + Swift 5.9 (SwiftNIO SSH bridge) + Compose Multiplatform, Voyager, Koin, Room 2.7+ KMP, DataStore KMP, okio (KMP), SwiftNIO SSH (Swift, SPM) (006-ios-support)
- Room KMP + `BundledSQLiteDriver` for connection records; iOS Keychain (Security framework cinterop) for credentials and SSH keys; DataStore for preferences (006-ios-support)

- Markdown documentation, YAML (Maestro flows) + Maestro CLI 1.30+, Mobile Vibe Terminal APK (004-maestro-usage-guide)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Markdown documentation, YAML (Maestro flows)

## Code Style

Markdown documentation, YAML (Maestro flows): Follow standard conventions

## Recent Changes
- 006-ios-support: Added Kotlin 2.x (Multiplatform, `iosArm64`, `iosSimulatorArm64`) + Swift 5.9 (SwiftNIO SSH bridge) + Compose Multiplatform, Voyager, Koin, Room 2.7+ KMP, DataStore KMP, okio (KMP), SwiftNIO SSH (Swift, SPM)
- 005-modifier-keys-handling: Added Kotlin 2.x (Multiplatform) + Compose Multiplatform, Voyager ScreenModel, Koin DI

- 004-maestro-usage-guide: Added Markdown documentation, YAML (Maestro flows) + Maestro CLI 1.30+, Mobile Vibe Terminal APK

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
