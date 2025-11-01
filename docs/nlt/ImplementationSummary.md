# NLT (Notification & Location Tracker) - Implementation Summary

## Overview

This document summarizes the implementation of the NLT application within the Puzzroom monorepo. NLT is a Kotlin Multiplatform application designed to monitor notifications from specified apps, capture location data, and persist records for analysis and visualization.

## Implementation Status

### ✅ Completed Features

#### Core Architecture
- [x] KMP project structure with Android target
- [x] Shared UI module for atomic design components
- [x] Gradle build configuration with required dependencies
- [x] Firebase integration setup (Firestore, Auth, Google Services)
- [x] kotlinx-datetime for cross-platform date/time handling

#### Data Layer (FR 3.0)
- [x] **NotificationRecord** data model with:
  - User and notification metadata
  - Location coordinates
  - Parsed payment information
  - Utility methods (getSummaryText, hasLocation)
- [x] **FirestoreRepositoryImpl** with:
  - Secure user-specific collection paths
  - Save operation with server timestamps
  - Real-time data retrieval with Flow
  - Location-filtered queries
- [x] Firestore collection structure: `/artifacts/nlt_app/users/{userId}/recorded_notifications`

#### Domain Layer
- [x] **PaymentParser** with:
  - Multi-format amount extraction (JPY, USD, yen symbol, etc.)
  - Merchant name extraction (multiple patterns)
  - Support for Japanese and English notifications
  - Comprehensive unit tests

#### Service Layer (Android)
- [x] **NLTNotificationListenerService** implementing:
  - FR 1.1: Passive notification monitoring
  - FR 1.2: Notification data capture
  - FR 1.3: Target application filtering
  - FR 1.4: Keyword-based filtering
  - Asynchronous processing with coroutines
  - Integration with location and Firestore services
  
- [x] **LocationServiceImpl** implementing:
  - FR 2.1: Immediate location capture on notification
  - FR 2.2: FusedLocationProviderClient integration
  - FR 2.3: 5-second timeout with graceful handling
  - FR 2.4: Runtime permission checking
  - NFR 4.1.1: Low battery consumption (one-time requests)

#### UI Layer
- [x] Basic MainActivity with placeholder UI
- [x] Shared UI components from Puzzroom (atoms, theme)
- [x] Warm color theme integration

#### Configuration
- [x] AndroidManifest with required permissions
- [x] Service declaration for NotificationListenerService
- [x] Location and internet permissions

#### Documentation
- [x] Comprehensive README for NLT module
- [x] README for shared-ui module
- [x] Code documentation (KDoc comments)
- [x] Setup instructions
- [x] Architecture diagrams in comments

#### Testing
- [x] Unit tests for PaymentParser (11 test cases)
- [x] Unit tests for NotificationRecord (8 test cases)
- [x] Test coverage for core business logic

### 🚧 Pending Features

#### Authentication
- [ ] Firebase Authentication integration
- [ ] User sign-in/sign-out UI
- [ ] User ID management throughout the app

#### Settings & Configuration
- [ ] User preferences UI for target apps
- [ ] Keyword filter configuration UI
- [ ] Permission request dialogs
- [ ] Notification access settings redirect

#### Visualization (FR 4.0)
- [ ] Calendar view with notification markers (FR 4.1)
- [ ] Daily notification timeline (FR 4.2)
- [ ] Map view with location markers (FR 4.3)
- [ ] Marker info windows (FR 4.4)
- [ ] Map filtering by app/keyword (FR 4.5)

#### iOS Support
- [ ] iOS notification monitoring implementation
- [ ] iOS location services implementation
- [ ] iOS-specific UI components

#### Additional Features
- [ ] Data export functionality
- [ ] Analytics and insights
- [ ] Notification statistics
- [ ] Search and filter in historical data

## Architecture Details

### Module Structure

```
Puzzroom/
├── shared-ui/                      # Shared UI components library
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/shared/ui/
│       ├── atoms/                  # AppButton, AppText, AppCard, etc.
│       ├── molecules/              # (To be populated)
│       ├── organisms/              # (To be populated)
│       └── theme/                  # PuzzroomTheme, Color, Typography
│
├── nlt-app/                        # NLT application
│   ├── build.gradle.kts
│   ├── google-services.json.example
│   ├── src/
│   │   ├── commonMain/kotlin/tokyo/isseikuzumaki/nlt/
│   │   │   ├── data/model/         # NotificationRecord
│   │   │   ├── domain/parser/      # PaymentParser
│   │   │   └── ui/                 # (To be populated)
│   │   │
│   │   ├── commonTest/kotlin/      # Unit tests
│   │   │   ├── data/model/         # NotificationRecordTest
│   │   │   └── domain/parser/      # PaymentParserTest
│   │   │
│   │   └── androidMain/
│   │       ├── AndroidManifest.xml
│   │       └── kotlin/tokyo/isseikuzumaki/nlt/
│   │           ├── MainActivity.kt
│   │           ├── service/        # NotificationListener, LocationService
│   │           └── data/repository/# FirestoreRepository
│   │
│   └── README.md
│
└── composeApp/                     # Original Puzzroom app
    └── ...
```

### Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Language | Kotlin | 2.2.20 | Cross-platform development |
| UI Framework | Compose Multiplatform | 1.9.0 | Cross-platform UI |
| Build Tool | Gradle | 8.14.3 | Build automation |
| Android Plugin | AGP | 8.11.2 | Android build support |
| Backend | Firebase Firestore | 33.7.0 (BOM) | Cloud data storage |
| Auth | Firebase Auth | 33.7.0 (BOM) | User authentication |
| Location | Play Services Location | 21.3.0 | Android location services |
| Date/Time | kotlinx-datetime | 0.6.1 | Cross-platform date/time |
| Serialization | kotlinx-serialization | 1.9.0 | Data serialization |

### Data Flow

```
Notification Received
        ↓
NLTNotificationListenerService
        ↓
    [Filter]
    ├─ Package name filter (FR 1.3)
    └─ Keyword filter (FR 1.4)
        ↓
    [Process]
    ├─ Parse payment info (PaymentParser)
    ├─ Capture location (LocationService, 5s timeout)
    └─ Create NotificationRecord
        ↓
FirestoreRepository
        ↓
    [Save]
    └─ /artifacts/nlt_app/users/{userId}/recorded_notifications/{id}
        ↓
    [Real-time sync to UI via Flow]
```

### Security Considerations

1. **User Data Privacy** (NFR 4.2.1):
   - All data stored in user-specific private collections
   - Collection path: `/artifacts/nlt_app/users/{userId}/...`
   - Requires Firebase Auth for access control

2. **Permissions** (NFR 4.2.2):
   - Notification access: User must explicitly grant
   - Location access: Runtime permission required
   - Background location: Optional, for enhanced accuracy

3. **Network Security** (NFR 4.2.3):
   - HTTPS/TLS enforced by Firebase SDK
   - No custom network code required

### Performance Optimizations

1. **Battery Efficiency** (NFR 4.1.1):
   - One-time location requests (no continuous tracking)
   - 5-second timeout to prevent long-running operations
   - Coroutines for non-blocking async operations

2. **Data Persistence** (NFR 4.1.2):
   - Async Firestore writes (< 500ms typical)
   - Server-side timestamps to reduce client processing
   - Efficient query indexing with composite indexes

## Known Limitations

1. **Network Dependency**: Build requires network access to download Firebase and Play Services dependencies
2. **iOS Implementation**: Not yet implemented (Android-only currently)
3. **Firebase Configuration**: Requires manual `google-services.json` setup
4. **UI Implementation**: Visualization features (calendar, map) not yet implemented
5. **Authentication**: Currently returns empty user ID (needs Firebase Auth integration)

## Next Steps

### Priority 1: Core Functionality
1. Implement Firebase Authentication
2. Add permission request dialogs
3. Create settings UI for target apps and keywords
4. Test end-to-end notification capture flow

### Priority 2: Visualization
1. Implement calendar view with kotlinx-datetime
2. Implement map view with Google Maps or kmp-maps
3. Add filtering and search functionality
4. Create data export feature

### Priority 3: iOS Support
1. Implement iOS notification monitoring
2. Implement iOS location services
3. Create iOS-specific UI components
4. Test cross-platform data sync

## References

- Design Document: Embedded in GitHub issue
- Architecture: Kotlin Multiplatform + Compose Multiplatform
- Design Pattern: Atomic Design (shared-ui module)

## Contributors

- Implementation: GitHub Copilot
- Project Owner: @isseikz
