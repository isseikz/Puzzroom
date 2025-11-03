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
- **Backend**: Firebase Cloud Functions (Node.js 20, TypeScript)
- **Database**: Firestore
- **Storage**: Firebase Storage
- **Notifications**: Firebase Cloud Messaging (FCM)

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
    ├── functions/          # Firebase Cloud Functions (Backend)
    │   ├── src/
    │   │   ├── index.ts    # Main functions (register, upload, download)
    │   │   └── types.ts    # TypeScript type definitions
    │   ├── package.json    # Node.js dependencies
    │   └── README.md       # Backend documentation
    │
    ├── src/commonMain/     # Cross-platform code
    │   └── kotlin/tokyo/isseikuzumaki/quickdeploy/
    │       └── App.kt      # Main entry point
    │
    ├── src/androidMain/    # Android-specific code
    │   ├── kotlin/tokyo/isseikuzumaki/quickdeploy/
    │   │   └── MainActivity.kt
    │   └── AndroidManifest.xml
    │
    ├── src/iosMain/        # iOS-specific code
    │   └── kotlin/tokyo/isseikuzumaki/quickdeploy/
    │       └── MainViewController.kt
    │
    ├── firebase.json       # Firebase project configuration
    ├── firestore.rules     # Firestore security rules
    └── storage.rules       # Storage security rules
```

## Setup Instructions

### Prerequisites

1. Android Studio with Kotlin Multiplatform support
2. Gradle 8.0+
3. JDK 11+
4. Node.js 20+ (for Firebase Functions)
5. Firebase CLI (`npm install -g firebase-tools`)

### Firebase Backend Setup

1. Install dependencies:
```bash
cd functions
npm install
```

2. Build TypeScript code:
```bash
npm run build
```

3. Deploy to Firebase (requires Firebase project setup):
```bash
firebase login
firebase deploy --only functions
```

For detailed backend setup and API documentation, see [functions/README.md](functions/README.md).

### Android App Build

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
- [Firebase Functions README](functions/README.md) - Backend API documentation
- [Deployment Guide](DEPLOYMENT.md) - Complete deployment and setup guide

### API Endpoints

The Firebase backend provides the following REST API endpoints:

- `POST /register` - Device registration and URL generation
- `POST /upload/{deviceToken}` - APK upload with push notification
- `GET /download/{deviceToken}` - APK download

See [functions/README.md](functions/README.md) for detailed API documentation.

### Integration Examples

- [GitHub Actions Integration](examples/github-actions-integration.md) - Automate APK deployment with GitHub Actions
- [Manual and SSH Server Integration](examples/manual-ssh-integration.md) - Deploy from command line or SSH servers

## Development Status

This is a new module in active development. Features and capabilities will be added incrementally.

### Phase 1: Backend Infrastructure (Completed)
- [x] Basic project structure
- [x] Android and iOS support
- [x] Shared UI integration
- [x] Firebase Functions backend
  - [x] Device registration endpoint (A-001)
  - [x] APK upload endpoint (A-002, A-003)
  - [x] FCM push notification (A-004)
  - [x] APK download endpoint (A-005)
  - [x] Automatic APK cleanup (10-minute expiration)
- [x] Firestore database setup
- [x] Firebase Storage setup

### Phase 2: Android Client (In Progress)
- [ ] Device registration UI
- [ ] URL display and copy functionality
- [ ] FCM integration for push notifications
- [ ] APK download and installation
- [ ] Installation permission handling
- [ ] Usage guide screen

### Phase 3: Integration and Testing (Planned)
- [ ] End-to-end testing
- [ ] GitHub Actions integration example
- [ ] Performance optimization
- [ ] Error handling improvements

## License

TBD

## Contributors

- Issei Kuzumaki (@isseikz)
