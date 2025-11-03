# Auto-Download Flow Implementation

## Overview

This document explains how the automatic APK download and installation works when a push notification is received.

## Complete Flow

### 1. APK Upload (Build Environment)

```bash
# Step 1: Get signed URL
curl -X POST https://getuploadurl-o45ehp4r5q-uc.a.run.app/DEVICE_TOKEN/url

# Response:
# {
#   "uploadUrl": "https://storage.googleapis.com/...",
#   "notifyUrl": "https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/DEVICE_TOKEN/notify"
# }

# Step 2: Upload APK to Storage
curl -X PUT --upload-file app.apk "UPLOAD_URL"

# Step 3: Notify backend
curl -X POST "NOTIFY_URL"
```

### 2. Backend Sends Push Notification

**File**: `firebase/functions/src/index.ts:280-302`

```typescript
// Send FCM notification
const message = {
  token: deviceData.fcmToken,
  notification: {
    title: "新しいAPKを受信しました",
    body: "タップしてインストールします。",
  },
  data: {
    deviceToken,
    action: "apk_ready",
  },
  android: {
    priority: "high" as const,
  },
};

await messaging.send(message);
```

### 3. Device Receives Notification

**File**: `service/QuickDeployMessagingService.kt:25-42`

```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    // Show notification to user
    showNotification(
        title = notification.title ?: "Quick Deploy",
        body = notification.body ?: "新しいAPKを受信しました...",
        deviceToken = message.data["deviceToken"]
    )
}
```

### 4. User Taps Notification

**File**: `service/QuickDeployMessagingService.kt:54-90`

```kotlin
private fun showNotification(...) {
    // Create intent with auto-download flag
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_DEVICE_TOKEN, deviceToken)
        putExtra(EXTRA_AUTO_DOWNLOAD, true)  // ← Auto-download trigger
    }

    val pendingIntent = PendingIntent.getActivity(...)

    val notification = NotificationCompat.Builder(...)
        .setContentIntent(pendingIntent)  // ← Launches MainActivity
        .build()
}
```

### 5. MainActivity Detects Auto-Download Intent

**File**: `MainActivity.kt:59-74`

```kotlin
private fun handleIntent(intent: Intent?) {
    intent?.let {
        val autoDownload = it.getBooleanExtra(
            QuickDeployMessagingService.EXTRA_AUTO_DOWNLOAD,
            false
        )
        val deviceToken = it.getStringExtra(
            QuickDeployMessagingService.EXTRA_DEVICE_TOKEN
        )

        if (autoDownload && deviceToken != null) {
            Log.d(TAG, "Auto-download triggered from notification")
            shouldAutoDownload.value = true  // ← Set flag
        }
    }
}
```

### 6. App Passes Flag to Composable

**File**: `MainActivity.kt:45-50`

```kotlin
setContent {
    App(shouldAutoDownload = shouldAutoDownload.value) {
        // Reset the flag after handling
        shouldAutoDownload.value = false
    }
}
```

### 7. App Triggers Auto-Download

**File**: `App.android.kt:42-47`

```kotlin
LaunchedEffect(shouldAutoDownload) {
    if (shouldAutoDownload) {
        viewModel.autoDownloadFromNotification()
        onAutoDownloadHandled()  // Reset flag
    }
}
```

### 8. ViewModel Downloads and Installs APK

**File**: `RegistrationViewModel.kt:109-116`

```kotlin
fun autoDownloadFromNotification() {
    viewModelScope.launch {
        val downloadUrl = deviceRepository.getDownloadUrl()
        if (downloadUrl != null) {
            downloadAndInstall(downloadUrl)  // ← Triggers download
        }
    }
}
```

**File**: `RegistrationViewModel.kt:88-103`

```kotlin
fun downloadAndInstall(downloadUrl: String) {
    viewModelScope.launch {
        _uiState.value = RegistrationUiState.Downloading

        val result = apkInstaller.downloadAndInstall(downloadUrl)

        result.onSuccess {
            // Return to registered state after installation starts
            loadRegistrationState()
        }.onFailure { error ->
            _uiState.value = RegistrationUiState.Error(...)
        }
    }
}
```

### 9. ApkInstaller Downloads and Shows Install Prompt

**File**: `util/ApkInstaller.kt:95-114`

```kotlin
suspend fun downloadAndInstall(downloadUrl: String): Result<Unit> {
    // Check permission
    if (!canInstallPackages()) {
        return Result.failure(InstallPermissionException(...))
    }

    // Download APK
    val downloadResult = downloadApk(downloadUrl)
    val apkFile = downloadResult.getOrNull()!!

    // Install APK (shows system install prompt)
    installApk(apkFile)
}
```

**File**: `util/ApkInstaller.kt:59-90`

```kotlin
fun installApk(apkFile: File): Result<Unit> {
    val apkUri = FileProvider.getUriForFile(...)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(intent)  // ← System installer opens
}
```

## Complete Timeline

```
[Build Tool]
    │
    ├─1─► Get Upload URL
    ├─2─► Upload APK to Storage
    └─3─► Notify Backend
            │
            ▼
      [Firebase Functions]
            │
            └─4─► Send FCM Push Notification
                    │
                    ▼
              [Android Device]
                    │
                    ├─5─► QuickDeployMessagingService receives FCM
                    ├─6─► Show system notification
                    └─7─► User taps notification
                            │
                            ▼
                      [MainActivity]
                            │
                            ├─8─► Detect EXTRA_AUTO_DOWNLOAD = true
                            └─9─► Set shouldAutoDownload = true
                                    │
                                    ▼
                              [App Composable]
                                    │
                                    └─10─► LaunchedEffect triggers
                                            │
                                            ▼
                                  [RegistrationViewModel]
                                            │
                                            └─11─► autoDownloadFromNotification()
                                                    │
                                                    ▼
                                              [ApkInstaller]
                                                    │
                                                    ├─12─► Download APK
                                                    └─13─► Show install prompt
                                                            │
                                                            ▼
                                                    [User taps "Install"]
                                                            │
                                                            ▼
                                                      APK INSTALLED ✅
```

## Time Breakdown

- **Step 1-3**: Upload (~5 seconds for 18MB APK)
- **Step 4**: FCM delivery (~1-3 seconds)
- **Step 5-7**: User sees and taps notification (~1-2 seconds)
- **Step 8-11**: App processes intent (~< 0.5 seconds)
- **Step 12**: APK download (~3-5 seconds)
- **Step 13**: Install prompt appears (~< 0.5 seconds)

**Total**: ~10-15 seconds from upload to install prompt

## Key Implementation Details

### 1. Auto-Download Flag Propagation

```
Intent → MainActivity → Compose State → LaunchedEffect → ViewModel → Download
```

### 2. State Management

```kotlin
// MainActivity holds the state
private val shouldAutoDownload = mutableStateOf(false)

// Pass to Compose
App(shouldAutoDownload = shouldAutoDownload.value) {
    shouldAutoDownload.value = false  // Reset after handling
}

// LaunchedEffect observes changes
LaunchedEffect(shouldAutoDownload) {
    if (shouldAutoDownload) {
        // Trigger download
    }
}
```

### 3. Permission Handling

If install permission is not granted:

```kotlin
if (!canInstallPackages()) {
    return Result.failure(InstallPermissionException(...))
}
```

The UI shows a warning card with "設定を開く" button to open system settings.

## Testing the Flow

### 1. Upload APK

```bash
# Get device token from app (copy from registration screen)
DEVICE_TOKEN="your-device-token"

# Upload using the new signed URL method
curl -X POST https://getuploadurl-o45ehp4r5q-uc.a.run.app/$DEVICE_TOKEN/url \
  | jq -r '.uploadUrl' > /tmp/upload_url.txt

curl -X POST https://getuploadurl-o45ehp4r5q-uc.a.run.app/$DEVICE_TOKEN/url \
  | jq -r '.notifyUrl' > /tmp/notify_url.txt

curl -X PUT --upload-file app.apk "$(cat /tmp/upload_url.txt)"

curl -X POST "$(cat /tmp/notify_url.txt)"
```

### 2. Observe Logs

```bash
# Watch the complete flow in logcat
adb logcat -s \
  QuickDeployFCM \
  MainActivity \
  ApkInstaller
```

Expected output:
```
QuickDeployFCM: FCM message received
QuickDeployFCM: Notification shown: ...
MainActivity: Auto-download triggered from notification
ApkInstaller: Downloading APK from: ...
ApkInstaller: APK downloaded successfully: ... (18000000 bytes)
ApkInstaller: APK installation started: ...
```

### 3. Verify User Experience

1. ✅ Notification appears within 5 seconds
2. ✅ Tap notification → App opens
3. ✅ "APKをダウンロード中..." appears
4. ✅ Download completes
5. ✅ System install prompt appears
6. ✅ Tap "Install" → App installs

## Troubleshooting

### Notification not appearing
- Check FCM token registration
- Verify notification permission granted
- Check battery optimization settings

### Auto-download not triggering
- Check logcat for "Auto-download triggered" message
- Verify `EXTRA_AUTO_DOWNLOAD` is set in intent
- Ensure app is not killed before download starts

### Install prompt not appearing
- Check install permission (C-005)
- Verify APK downloaded successfully
- Check logcat for errors

## Files Modified

1. `RegistrationViewModel.kt` - Added `autoDownloadFromNotification()`
2. `MainActivity.kt` - Added auto-download intent handling
3. `App.android.kt` - Added LaunchedEffect to trigger download

## Status

✅ **Implemented and Ready for Testing**

The complete auto-download flow is now functional. When a user taps the notification, the app will automatically download and prompt to install the APK.
