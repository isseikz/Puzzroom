# NLT (Notification & Location Tracker)

## Overview

NLT is a Kotlin Multiplatform application that monitors notifications from specified apps, captures location data, and stores records in Firebase Firestore for visualization and analysis.

## Features

### Core Features (Implemented)

- **FR 1.1**: Passive notification monitoring via NotificationListenerService
- **FR 1.2**: Notification data capture (package name, title, text, timestamp)
- **FR 1.3**: Target application filtering
- **FR 1.4**: Keyword-based filtering
- **FR 2.1**: Immediate location capture on notification
- **FR 2.2**: FusedLocationProviderClient integration
- **FR 2.3**: 5-second timeout with graceful handling
- **FR 2.4**: Location permission management
- **FR 3.1**: Secure Firestore data persistence
- **FR 3.2**: User-specific private collections
- **Payment Parsing**: Automatic extraction of payment amount and merchant

### Planned Features

- **FR 4.1**: Calendar view with notification markers
- **FR 4.2**: Daily notification timeline
- **FR 4.3**: Map view with location markers
- **FR 4.4**: Marker info windows with notification details
- **FR 4.5**: Map view filtering by app/keyword
- **Authentication**: Firebase Authentication integration
- **Settings**: User preferences for target apps and keywords

## Architecture

### Technology Stack

- **Platform**: Kotlin Multiplatform (Android, iOS)
- **UI**: Compose Multiplatform with shared Atomic Design components
- **Backend**: Firebase Firestore
- **Auth**: Firebase Authentication
- **Location**: Google Play Services Location API (Android)
- **Date/Time**: kotlinx-datetime

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
└── nlt-app/                # NLT application module
    ├── src/commonMain/     # Cross-platform code
    │   └── kotlin/tokyo/isseikuzumaki/nlt/
    │       ├── data/model/ # Data models (NotificationRecord)
    │       ├── domain/     # Business logic (PaymentParser)
    │       └── ui/         # UI components (pages, viewmodel, state)
    │
    └── src/androidMain/    # Android-specific code
        ├── kotlin/tokyo/isseikuzumaki/nlt/
        │   ├── service/    # NotificationListenerService, LocationService
        │   └── data/       # FirestoreRepository
        └── AndroidManifest.xml
```

### Data Model

```kotlin
NotificationRecord(
    id: String,                 // Unique ID
    userId: String,             // Firebase Auth user ID
    packageName: String,        // Source app (e.g., "com.paymentapp")
    title: String?,             // Notification title
    text: String,               // Notification body
    time: Instant?,             // UTC timestamp (Firestore server timestamp)
    latitude: Double?,          // Location latitude (null if unavailable)
    longitude: Double?,         // Location longitude (null if unavailable)
    isParsed: Boolean,          // Payment parsing success flag
    parsedAmount: String?,      // e.g., "1500 JPY"
    parsedMerchant: String?     // e.g., "Starbucks"
)
```

### Firestore Collection Structure

```
/artifacts/nlt_app/users/{userId}/recorded_notifications/{notificationId}
```

## Setup Instructions

### Prerequisites

1. Android Studio with Kotlin Multiplatform support
2. Firebase project with Firestore enabled
3. Android device/emulator with Google Play Services

### Configuration

1. **Firebase Setup**:
   - Create a Firebase project
   - Add Android app to Firebase project
   - Download `google-services.json`
   - Place in `nlt-app/src/androidMain/` directory

2. **Permissions**:
   - Grant notification access: Settings → Apps → NLT → Notification access
   - Grant location permission: Settings → Apps → NLT → Permissions

3. **Build**:
   ```bash
   ./gradlew :nlt-app:assembleDebug
   ```

## Usage

### Notification Monitoring

The NLTNotificationListenerService automatically monitors notifications from configured apps. To configure:

1. Edit target apps in user settings (or SharedPreferences)
2. Edit filter keywords in user settings

Default keywords: "支払い", "決済", "payment", "paid"

### Location Capture

Location is captured immediately when a filtered notification is received:
- Uses high-accuracy mode
- 5-second timeout
- Falls back to null if unavailable

### Data Access

Notification records are stored in Firestore and can be retrieved via:
- `FirestoreRepositoryImpl.getNotificationRecords()` - All records
- `FirestoreRepositoryImpl.getNotificationRecordsWithLocation()` - Records with location only

## Non-Functional Requirements

- **NFR 4.1.1**: Low battery consumption (one-time location requests)
- **NFR 4.1.2**: Fast data persistence (< 500ms to Firestore)
- **NFR 4.2.1**: Secure data storage (user-specific collections)
- **NFR 4.2.2**: Permission management (runtime permission checks)
- **NFR 4.2.3**: HTTPS/TLS (provided by Firebase)

## Development

### Shared UI Components

NLT shares UI components with the main Puzzroom app following Atomic Design principles:

- **Atoms**: AppButton, AppText, AppCard, AppSpacer, etc.
- **Theme**: Warm color palette (terracotta, peach, beige)
- Access via `shared-ui` module

### Testing

```bash
# Run tests
./gradlew :nlt-app:test

# Run Android instrumented tests
./gradlew :nlt-app:connectedAndroidTest
```

## Roadmap

### Phase 1: Core Features ✅ (Completed)
- [x] Notification monitoring
- [x] Location capture
- [x] Firestore persistence
- [x] Payment parsing
- [x] Firebase Authentication
- [x] Basic UI
- [x] Sign-in/Sign-out
- [x] Permissions management UI
- [x] Settings UI

### Phase 2: Visualization (Next)
- [ ] Calendar view
- [ ] Map view
- [ ] Filtering UI

### Phase 3: Advanced Features
- [ ] iOS support
- [ ] Export functionality
- [ ] Analytics

## License

TBD

## Contributors

- Issei Kuzumaki (@isseikz)
