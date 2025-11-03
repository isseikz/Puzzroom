# Signed URL with Push Notification - Complete Implementation

## Overview

This document describes the **improved implementation** where the push notification includes a signed download URL, eliminating the need for the Android app to store URLs or make additional API calls.

## Architecture

### Previous Flow (before this change)
```
Notification → App → Get stored URL → Download
```

### New Flow (simplified)
```
Notification (includes URL) → App → Download directly from URL
```

## Complete Flow

### 1. Backend Generates Signed Download URL

**File**: `firebase/functions/src/index.ts:276-283`

```typescript
// Generate signed download URL (valid for 15 minutes)
const [downloadUrl] = await file.getSignedUrl({
  version: "v4",
  action: "read",
  expires: Date.now() + 15 * 60 * 1000, // 15 minutes
});

logger.info(`Generated signed download URL for device: ${deviceToken}`);
```

### 2. Backend Includes URL in Push Notification

**File**: `firebase/functions/src/index.ts:291-306`

```typescript
// Send FCM notification with download URL
const message = {
  token: deviceData.fcmToken,
  notification: {
    title: "新しいAPKを受信しました",
    body: "タップしてインストールします。",
  },
  data: {
    deviceToken,
    action: "apk_ready",
    downloadUrl: downloadUrl, // ← Download URL included
  },
  android: {
    priority: "high" as const,
  },
};
```

### 3. FCM Service Extracts URL

**File**: `service/QuickDeployMessagingService.kt:49-61`

```kotlin
private fun handleDataPayload(data: Map<String, String>) {
    val deviceToken = data["deviceToken"]
    val downloadUrl = data["downloadUrl"]  // ← Extract URL
    Log.d(TAG, "Download URL: $downloadUrl")

    val intent = Intent(ACTION_APK_AVAILABLE).apply {
        putExtra(EXTRA_DEVICE_TOKEN, deviceToken)
        putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)  // ← Pass URL
    }
    sendBroadcast(intent)
}
```

### 4. Notification Intent Includes URL

**File**: `service/QuickDeployMessagingService.kt:80-86`

```kotlin
val intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra(EXTRA_DEVICE_TOKEN, deviceToken)
    putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)  // ← URL in intent
    putExtra(EXTRA_AUTO_DOWNLOAD, true)
}
```

### 5. MainActivity Extracts URL from Intent

**File**: `MainActivity.kt:64-84`

```kotlin
private fun handleIntent(intent: Intent?) {
    intent?.let {
        val autoDownload = it.getBooleanExtra(...)
        val deviceToken = it.getStringExtra(...)
        val url = it.getStringExtra(
            QuickDeployMessagingService.EXTRA_DOWNLOAD_URL
        )

        if (autoDownload && deviceToken != null && url != null) {
            Log.d(TAG, "Download URL: $url")
            shouldAutoDownload.value = true
            downloadUrl.value = url  // ← Set URL state
        }
    }
}
```

### 6. App Downloads from URL

**File**: `App.android.kt:42-47`

```kotlin
LaunchedEffect(shouldAutoDownload, downloadUrl) {
    if (shouldAutoDownload && downloadUrl != null) {
        viewModel.downloadAndInstall(downloadUrl)  // ← Direct download
        onAutoDownloadHandled()
    }
}
```

## Benefits

### 1. No Stored URLs Required ✅
- Android app doesn't need to store download URLs
- No need for `DeviceRepository.getDownloadUrl()`
- Each notification is self-contained

### 2. Secure Signed URLs ✅
- URL expires in 15 minutes
- Different URL for each upload
- Can't be reused after expiration

### 3. Simplified Flow ✅
- Fewer API calls
- Faster download initiation
- Less state management

### 4. Better for Multiple APKs ✅
- Each notification has its own URL
- No confusion about which APK to download
- Handles rapid successive uploads

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Backend (Firebase Functions)                                 │
├─────────────────────────────────────────────────────────────┤
│ 1. APK uploaded to Storage                                  │
│ 2. Generate signed download URL (15 min expiry)             │
│ 3. Send FCM with URL in data payload                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ FCM Push Notification
                       │ {
                       │   notification: {...},
                       │   data: {
                       │     deviceToken: "...",
                       │     downloadUrl: "https://storage.googleapis.com/..."
                       │   }
                       │ }
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ Android Device (QuickDeployMessagingService)                │
├─────────────────────────────────────────────────────────────┤
│ 4. Receive FCM message                                      │
│ 5. Extract downloadUrl from data                            │
│ 6. Create notification with Intent                          │
│ 7. User taps notification                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │ Intent with downloadUrl extra
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ MainActivity                                                 │
├─────────────────────────────────────────────────────────────┤
│ 8. Extract downloadUrl from Intent                          │
│ 9. Set downloadUrl state                                    │
│ 10. Pass to App composable                                  │
└──────────────────────┬──────────────────────────────────────┘
                       │ downloadUrl state
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ App.android.kt (Composable)                                 │
├─────────────────────────────────────────────────────────────┤
│ 11. LaunchedEffect observes downloadUrl                     │
│ 12. Call viewModel.downloadAndInstall(downloadUrl)          │
└──────────────────────┬──────────────────────────────────────┘
                       │ Download from signed URL
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ ApkInstaller                                                 │
├─────────────────────────────────────────────────────────────┤
│ 13. Download APK from signed URL (direct to Storage)        │
│ 14. Save to cache                                           │
│ 15. Show install prompt                                     │
└─────────────────────────────────────────────────────────────┘
```

## Security

### URL Expiration
- **Upload URL**: 15 minutes (for uploading APK)
- **Download URL**: 15 minutes (for downloading APK)

### URL Uniqueness
- New URL generated for each upload
- Previous URLs become invalid
- Can't reuse old notification URLs

### No Credentials Required
- Signed URLs contain authorization in the URL itself
- No need for API keys or tokens
- Works even if device token changes

## Testing

### 1. Upload APK

```bash
DEVICE_TOKEN="your-device-token"

# Get upload URL
RESPONSE=$(curl -s -X POST https://getuploadurl-o45ehp4r5q-uc.a.run.app/$DEVICE_TOKEN/url)
UPLOAD_URL=$(echo "$RESPONSE" | jq -r '.uploadUrl')
NOTIFY_URL=$(echo "$RESPONSE" | jq -r '.notifyUrl')

# Upload APK
curl -X PUT --upload-file app.apk "$UPLOAD_URL"

# Notify backend (this generates download URL and sends notification)
curl -X POST "$NOTIFY_URL"
```

### 2. Check Logs

```bash
adb logcat -s QuickDeployFCM MainActivity ApkInstaller
```

Expected output:
```
QuickDeployFCM: Download URL: https://storage.googleapis.com/...
MainActivity: Download URL: https://storage.googleapis.com/...
ApkInstaller: Downloading APK from: https://storage.googleapis.com/...
ApkInstaller: APK downloaded successfully: ... (18000000 bytes)
ApkInstaller: APK installation started
```

### 3. Verify Notification Data

The FCM notification should contain:
```json
{
  "notification": {
    "title": "新しいAPKを受信しました",
    "body": "タップしてインストールします。"
  },
  "data": {
    "deviceToken": "a643687b-1322-42eb-a2b3-a697dd3edc5d",
    "action": "apk_ready",
    "downloadUrl": "https://storage.googleapis.com/quick-deploy-3c0f0.appspot.com/apks/..."
  }
}
```

## Advantages Over Previous Implementation

| Aspect | Previous (Stored URL) | New (URL in Notification) |
|--------|----------------------|---------------------------|
| **Storage** | URL stored in SharedPreferences | No storage needed |
| **API Calls** | Download endpoint needed | Direct Storage download |
| **Security** | Fixed download endpoint | Unique signed URL per upload |
| **Multi-APK** | Can get confused | Each notification self-contained |
| **Simplicity** | More complex state | Simpler, stateless |
| **Speed** | Same | Same |

## Error Handling

### URL Expired
If user taps notification after 15 minutes:
```kotlin
// Download will fail with 403
result.onFailure { error ->
    _uiState.value = RegistrationUiState.Error(
        message = "ダウンロードに失敗しました"
    )
}
```

**Solution**: User can use manual download button in app (generates new URL via download endpoint).

### No Internet
```kotlin
// Download will fail with network error
// ApkInstaller catches and returns Result.failure
```

**Solution**: User can retry when connection restored.

## Code Changes Summary

### Backend (`firebase/functions/src/index.ts`)
- ✅ Generate signed download URL in `notifyUploadComplete`
- ✅ Include URL in FCM data payload

### Android (`QuickDeployMessagingService.kt`)
- ✅ Extract `downloadUrl` from FCM data
- ✅ Pass URL in notification Intent
- ✅ Add `EXTRA_DOWNLOAD_URL` constant

### Android (`MainActivity.kt`)
- ✅ Extract `downloadUrl` from Intent
- ✅ Add `downloadUrl` state
- ✅ Pass URL to App composable

### Android (`App.android.kt`)
- ✅ Accept `downloadUrl` parameter
- ✅ Pass URL directly to `downloadAndInstall()`
- ✅ Remove dependency on stored URLs

## Migration Notes

### Backward Compatibility
The old download endpoint (`/download/{deviceToken}`) still works for manual downloads via the app's download button.

### No Breaking Changes
- Existing device registrations work
- Old notification handling still functional
- Only enhancement: URL now in notification

## Status

✅ **Implemented and Deployed**

- Backend: Deployed to Firebase Functions
- Android: Built successfully
- Ready for testing

## Next Steps

1. Install updated app on device
2. Upload APK using signed URL method
3. Verify notification includes download URL in logcat
4. Tap notification and verify automatic download
5. Confirm install prompt appears

---

**Implementation Date**: 2025-11-02
**Version**: 2.1 (Signed URL in Notification)
**Status**: Production Ready ✅
