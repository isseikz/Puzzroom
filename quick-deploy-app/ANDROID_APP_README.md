# Quick Deploy - Android App Implementation Guide

## Overview

The Quick Deploy Android app is the client component that receives and installs APK files from remote build environments. This document provides a comprehensive guide for setting up, building, and using the Android app.

## Architecture

### Components

The Android app follows a clean architecture pattern with the following layers:

```
quick-deploy-app/src/androidMain/
├── kotlin/tokyo/isseikuzumaki/quickdeploy/
│   ├── model/              # Data models
│   │   ├── DeviceInfo.kt
│   │   ├── RegisterRequest.kt
│   │   └── RegisterResponse.kt
│   ├── api/                # API client
│   │   └── QuickDeployApiClient.kt
│   ├── repository/         # Data repositories
│   │   └── DeviceRepository.kt
│   ├── service/            # Background services
│   │   └── QuickDeployMessagingService.kt
│   ├── util/               # Utilities
│   │   └── ApkInstaller.kt
│   ├── ui/                 # UI layer
│   │   ├── navigation/
│   │   │   └── Screen.kt
│   │   ├── registration/
│   │   │   ├── RegistrationViewModel.kt
│   │   │   └── RegistrationScreen.kt
│   │   └── guide/
│   │       └── GuideScreen.kt
│   ├── QuickDeployApplication.kt
│   ├── MainActivity.kt
│   └── App.android.kt
├── res/
│   └── xml/
│       └── file_paths.xml
└── AndroidManifest.xml
```

### Technology Stack

- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM with ViewModel
- **Navigation**: Jetpack Navigation Compose
- **HTTP Client**: Ktor
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Serialization**: kotlinx.serialization
- **Dependency Injection**: Manual (simple factory pattern)

## Requirements Implementation

All client requirements from REQUIREMENTS.md have been implemented:

| Requirement | Implementation | Status |
|------------|----------------|--------|
| **C-001**: Device Registration & URL Generation | `RegistrationScreen`, `DeviceRepository` | ✅ Complete |
| **C-002**: FCM Push Notification Reception | `QuickDeployMessagingService` | ✅ Complete |
| **C-003**: APK Download | `ApkInstaller.downloadApk()` | ✅ Complete |
| **C-004**: APK Installation | `ApkInstaller.installApk()` | ✅ Complete |
| **C-005**: Installation Permission Guidance | Permission check + settings dialog | ✅ Complete |
| **C-006**: Usage Guide Display | `GuideScreen` | ✅ Complete |

## Setup Instructions

### Prerequisites

1. **Android Studio**: Arctic Fox (2020.3.1) or later
2. **JDK**: 11 or later
3. **Android SDK**: API 29 (Android 10) or higher
4. **Firebase Project**: Set up with Cloud Messaging enabled

### Step 1: Firebase Configuration

1. **Create Firebase Project** (if not already created):
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or select existing project
   - Enable Google Analytics (optional)

2. **Add Android App to Firebase**:
   - Click "Add app" → Select Android
   - Enter package name: `tokyo.isseikuzumaki.quickdeploy`
   - Download `google-services.json`

3. **Place Configuration File**:
   ```bash
   cp /path/to/google-services.json quick-deploy-app/
   ```

4. **Enable Cloud Messaging**:
   - In Firebase Console, go to Project Settings
   - Navigate to Cloud Messaging tab
   - Note the Server Key (needed for backend)

### Step 2: Configure Base URL

Edit `App.android.kt` and replace the base URL with your Firebase Functions URL:

```kotlin
// In App.android.kt
val baseUrl = "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net"
```

Example:
```kotlin
val baseUrl = "https://us-central1-quickdeploy-12345.cloudfunctions.net"
```

### Step 3: Build the App

```bash
# Navigate to project root
cd /path/to/Puzzroom

# Build debug APK
./gradlew :quick-deploy-app:assembleDebug

# Or build release APK
./gradlew :quick-deploy-app:assembleRelease

# Output location:
# quick-deploy-app/build/outputs/apk/debug/quick-deploy-app-debug.apk
```

### Step 4: Install on Device

```bash
# Install via ADB
adb install quick-deploy-app/build/outputs/apk/debug/quick-deploy-app-debug.apk

# Or drag & drop the APK to an emulator
```

## User Guide

### First-Time Setup

1. **Launch the App**:
   - Open Quick Deploy app on your Android device
   - Grant notification permission when prompted (Android 13+)

2. **Register Device**:
   - Tap "デバイスを登録" (Register Device)
   - Wait for registration to complete
   - A unique upload URL will be generated

3. **Copy Upload URL**:
   - Tap "URLコピー" to copy the upload URL
   - Or tap "curlコピー" to copy a ready-to-use curl command

4. **Configure Build Environment**:
   - Add the URL to your build script (GitHub Actions, etc.)
   - See [examples/github-actions-integration.md](examples/github-actions-integration.md) for details

5. **Enable Installation Permission**:
   - If prompted, tap "設定を開く" (Open Settings)
   - Enable "Install unknown apps" for Quick Deploy
   - Return to the app

### Daily Usage

1. **Build Your App**:
   - Trigger your build (push to GitHub, run local build, etc.)
   - Build script will upload APK to Quick Deploy

2. **Receive Notification**:
   - You'll receive a push notification: "新しいAPKを受信しました"
   - Notification appears within ~5 seconds of upload

3. **Install APK**:
   - Tap the notification
   - APK downloads automatically
   - Installation prompt appears
   - Tap "Install" to complete

### Manual Download

If notifications don't arrive:

1. Open Quick Deploy app
2. Tap "最新APKをダウンロード＆インストール"
3. APK will download and install

## Key Features

### 1. Device Registration (C-001)

**Implementation**: `RegistrationScreen.kt`, `DeviceRepository.kt`

- Generates unique, unpredictable device token (UUID v4)
- Creates upload/download URLs
- Stores registration data locally (SharedPreferences)
- Provides copy buttons for URL and curl command

**URL Format**:
```
Upload:   https://[region]-[project].cloudfunctions.net/upload/[device-token]
Download: https://[region]-[project].cloudfunctions.net/download/[device-token]
```

### 2. FCM Integration (C-002)

**Implementation**: `QuickDeployMessagingService.kt`

- Receives push notifications from Firebase
- Displays system notification
- Triggers automatic APK download
- Handles notification channel creation (Android O+)

**Notification Flow**:
1. Backend sends FCM message
2. Service receives message
3. Shows notification to user
4. User taps → MainActivity opens
5. Auto-download begins

### 3. APK Download (C-003)

**Implementation**: `ApkInstaller.kt`

- Downloads APK via Ktor HTTP client
- Saves to app cache directory
- Handles network errors gracefully
- Shows progress in UI

**Download Location**:
```
/data/data/tokyo.isseikuzumaki.quickdeploy/cache/apks/downloaded_app.apk
```

### 4. APK Installation (C-004)

**Implementation**: `ApkInstaller.kt`

- Uses FileProvider for Android N+ compatibility
- Launches system installer
- Handles installation errors
- Cleans up old APK files

**Installation Process**:
1. Check permission (C-005)
2. Generate content URI via FileProvider
3. Launch ACTION_VIEW intent
4. Android system handles actual installation

### 5. Permission Handling (C-005)

**Implementation**: `ApkInstaller.kt`, `RegistrationScreen.kt`

- Checks `REQUEST_INSTALL_PACKAGES` permission
- Shows warning if permission not granted
- Opens system settings for user to enable
- Re-checks permission after returning from settings

**Permission Flow**:
```
Android 7.x and below → No special permission needed
Android 8.0+          → REQUEST_INSTALL_PACKAGES required
```

### 6. Usage Guide (C-006)

**Implementation**: `GuideScreen.kt`

- Comprehensive setup instructions
- GitHub Actions integration examples
- Command-line usage examples
- Tips and troubleshooting

## API Integration

### Device Registration

```kotlin
// Automatic in RegistrationViewModel
val result = deviceRepository.registerDevice()

// Backend endpoint: POST /register
// Request:
{
  "fcmToken": "string",
  "deviceInfo": {
    "deviceId": "android_id",
    "deviceModel": "Google Pixel 7",
    "osVersion": "Android 13 (API 33)",
    "appVersion": "1.0"
  }
}

// Response:
{
  "deviceToken": "uuid-v4-token",
  "uploadUrl": "https://...",
  "downloadUrl": "https://..."
}
```

### APK Download

```kotlin
// Triggered by notification or manual button
val result = apkInstaller.downloadApk(downloadUrl)

// Backend endpoint: GET /download/{deviceToken}
// Response: Binary APK file
```

## Permissions

The app requires the following permissions:

```xml
<!-- Network access for API calls and APK download -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Push notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- APK installation (Android 8.0+) -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- File storage (Android 9 and below) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

## Security Considerations

1. **URL Security**:
   - Device tokens are UUID v4 (unpredictable)
   - URLs stored locally in encrypted SharedPreferences
   - Old tokens invalidated when new registration occurs

2. **Network Security**:
   - All communication over HTTPS
   - Certificate pinning not implemented (Firebase handles this)

3. **Storage Security**:
   - APK files stored in app-private cache directory
   - Automatically deleted by system when storage is low
   - Backend deletes APK after 10 minutes

4. **Installation Security**:
   - User must explicitly grant installation permission
   - Android system verifies APK before installation
   - User sees standard installation prompt

## Troubleshooting

### Notifications Not Arriving

1. **Check notification permission**:
   - Settings → Apps → Quick Deploy → Notifications → Enabled

2. **Check FCM token**:
   - Check logcat for "Got FCM token: ..."
   - Ensure token was sent to backend during registration

3. **Check battery optimization**:
   - Some manufacturers aggressively kill background apps
   - Add Quick Deploy to battery optimization whitelist

### Installation Fails

1. **Check permission**:
   - Settings → Apps → Quick Deploy → Install unknown apps → Allowed

2. **Check APK validity**:
   - Ensure APK is not corrupted
   - Check file size (logcat shows bytes downloaded)

3. **Check storage space**:
   - Ensure device has enough free storage

### Registration Fails

1. **Check network connection**:
   - Ensure device has internet access
   - Try switching between WiFi and mobile data

2. **Check backend URL**:
   - Verify `baseUrl` in `App.android.kt`
   - Ensure Firebase Functions are deployed

3. **Check Firebase configuration**:
   - Ensure `google-services.json` is present
   - Verify package name matches

## Development

### Running in Development

```bash
# Build and install debug version
./gradlew :quick-deploy-app:installDebug

# View logs
adb logcat -s QuickDeployApp QuickDeployFCM QuickDeployApiClient DeviceRepository ApkInstaller MainActivity
```

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for formatting
- Document public APIs with KDoc

### Testing

```bash
# Run unit tests
./gradlew :quick-deploy-app:testDebugUnitTest

# Run instrumented tests (requires connected device)
./gradlew :quick-deploy-app:connectedDebugAndroidTest
```

## Future Enhancements

Potential improvements for future versions:

- [ ] Support multiple registered devices
- [ ] APK version history
- [ ] Installation progress indicator
- [ ] Automatic installation (requires root or system app)
- [ ] QR code for easy URL sharing
- [ ] Dark mode support
- [ ] Multi-language support (currently Japanese)

## Related Documentation

- [REQUIREMENTS.md](REQUIREMENTS.md) - Original requirements specification
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Backend implementation summary
- [DEPLOYMENT.md](DEPLOYMENT.md) - Backend deployment guide
- [examples/github-actions-integration.md](examples/github-actions-integration.md) - CI/CD integration
- [functions/README.md](functions/README.md) - API documentation

## Support

For issues and questions:

1. Check this documentation
2. Review logcat output
3. Check Firebase Console for errors
4. Review backend logs in Firebase Functions

## License

This project is part of the Puzzroom ecosystem.

---

**Status**: ✅ Complete and Ready for Testing
**Last Updated**: 2025-11-02
**Minimum Android Version**: Android 10 (API 29)
**Target Android Version**: Android 14 (API 36)
