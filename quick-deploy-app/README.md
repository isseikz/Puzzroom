# Quick Deploy

## Overview

Quick Deploy is a streamlined deployment tool designed for the Puzzroom ecosystem. It provides a fast and efficient way to deploy and manage Puzzroom applications.

For detailed system architecture and communication flow, see the [Sequence Diagram](docs/design/SequenceDiagram.md).

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

## Documentation

### Design Documents

- [Sequence Diagram](docs/design/SequenceDiagram.md) - System communication flow between server, API, and client devices
- [Requirements](REQUIREMENTS.md) - Detailed project requirements (Japanese)

## Development Status

This is a new module in active development. Features and capabilities will be added incrementally.

### Phase 1: Foundation (Current)
- [x] Basic project structure
- [x] Android and iOS support
- [x] Shared UI integration
- [ ] Core deployment workflow

### Phase 2: Features (Planned)
- [ ] Deployment automation
- [ ] Configuration management
- [ ] Integration with CI/CD

## License

TBD

## Contributors

- Issei Kuzumaki (@isseikz)
