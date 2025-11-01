# Quick Deploy

## Overview

Quick Deploy is a streamlined deployment tool designed for the Puzzroom ecosystem. It provides a fast and efficient way to deploy and manage Puzzroom applications.

**Firebase Functions Support**: This module includes Firebase Cloud Functions for server-side logic. See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for detailed setup instructions.

## Features

### Planned Features

- Quick deployment workflow
- Configuration management
- Deployment automation
- Integration with Puzzroom ecosystem

## Architecture

### Technology Stack

- **Platform**: Kotlin Multiplatform (Android, iOS)
- **UI**: Compose Multiplatform with shared Atomic Design components
- **Theme**: Warm color palette (shared with main Puzzroom app)

### Module Structure

```
Puzzroom/
├── shared-ui/              # Shared UI components (atoms, molecules, organisms)
│   └── src/commonMain/
│       └── kotlin/tokyo/isseikuzumaki/puzzroom/shared/ui/
│           ├── atoms/      # Basic UI elements (AppButton, AppText, etc.)
│           ├── molecules/  # Simple combinations
│           ├── organisms/  # Complex sections
│           └── theme/      # Warm color theme
│
└── quick-deploy-app/       # Quick Deploy application module
    ├── src/commonMain/     # Cross-platform code
    │   └── kotlin/tokyo/isseikuzumaki/quickdeploy/
    │       └── App.kt      # Main entry point
    │
    ├── src/androidMain/    # Android-specific code
    │   ├── kotlin/tokyo/isseikuzumaki/quickdeploy/
    │   │   └── MainActivity.kt
    │   └── AndroidManifest.xml
    │
    └── src/iosMain/        # iOS-specific code
        └── kotlin/tokyo/isseikuzumaki/quickdeploy/
            └── MainViewController.kt
```

## Setup Instructions

### Prerequisites

1. Android Studio with Kotlin Multiplatform support
2. Gradle 8.0+
3. JDK 11+

### Build

```bash
# Build Android application
./gradlew :quick-deploy-app:assembleDebug

# Run Android application
./gradlew :quick-deploy-app:installDebug
```

### Shared UI Components

Quick Deploy shares UI components with the main Puzzroom app following Atomic Design principles:

- **Atoms**: AppButton, AppText, AppCard, AppSpacer, etc.
- **Theme**: Warm color palette (terracotta, peach, beige)
- Access via `shared-ui` module

## Development Status

This is a new module in active development. Features and capabilities will be added incrementally.

### Phase 1: Foundation (Current)
- [x] Basic project structure
- [x] Android and iOS support
- [x] Shared UI integration
- [x] Firebase Functions setup with Kotlin/JS support
- [x] Shared data structures between client and server
- [ ] Core deployment workflow

### Phase 2: Features (Planned)
- [ ] Deployment automation
- [ ] Configuration management
- [ ] Integration with CI/CD
- [ ] Firebase Cloud Functions implementation

## Firebase Functions

This module includes Firebase Cloud Functions for server-side logic. The implementation supports:

- **Shared Data Structures**: Data models defined in Kotlin can be used on both client (Android/iOS) and server (Firebase Functions)
- **Kotlin/JS Option**: Functions can be written in Kotlin and compiled to JavaScript
- **Standard JavaScript**: Functions can also be written in standard JavaScript/TypeScript

### Quick Start

1. **Setup Firebase**:
   ```bash
   cd quick-deploy-app
   # Edit .firebaserc and add your Firebase project ID
   cd functions
   npm install
   ```

2. **Deploy Functions**:
   ```bash
   npm run deploy
   ```

3. **Local Testing**:
   ```bash
   npm run serve
   ```

See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for complete documentation.

## License

TBD

## Contributors

- Issei Kuzumaki (@isseikz)
