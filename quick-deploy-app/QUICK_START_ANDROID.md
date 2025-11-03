# Quick Start Guide - Android App

This guide will get you up and running with the Quick Deploy Android app in under 10 minutes.

## Prerequisites

- [ ] Android Studio installed
- [ ] Firebase project created with Cloud Messaging enabled
- [ ] Backend (Firebase Functions) deployed
- [ ] Android device or emulator running Android 10+

## Setup Steps

### 1. Get Firebase Configuration (2 minutes)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your Quick Deploy project
3. Click ⚙️ → Project Settings
4. Scroll to "Your apps" section
5. Click "Add app" → Android (if not already added)
6. Enter package name: `tokyo.isseikuzumaki.quickdeploy`
7. Download `google-services.json`
8. Place file in `quick-deploy-app/` directory:
   ```bash
   cp ~/Downloads/google-services.json quick-deploy-app/
   ```

### 2. Configure Base URL (1 minute)

Edit `quick-deploy-app/src/androidMain/kotlin/tokyo/isseikuzumaki/quickdeploy/App.android.kt`:

```kotlin
// Line 17: Replace with your Firebase Functions URL
val baseUrl = "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net"
```

To find your URL:
1. Open Firebase Console
2. Go to Functions tab
3. Copy the domain from any function URL
4. Format: `https://us-central1-your-project.cloudfunctions.net`

### 3. Build the App (2 minutes)

```bash
# From project root
./gradlew :quick-deploy-app:assembleDebug
```

APK will be created at:
```
quick-deploy-app/build/outputs/apk/debug/quick-deploy-app-debug.apk
```

### 4. Install on Device (1 minute)

**Option A: Using ADB**
```bash
adb install quick-deploy-app/build/outputs/apk/debug/quick-deploy-app-debug.apk
```

**Option B: Using Android Studio**
1. Open project in Android Studio
2. Select `quick-deploy-app` configuration
3. Click Run ▶️

**Option C: Manual Install**
1. Copy APK to device
2. Open file manager
3. Tap APK and install

### 5. First Run Setup (3 minutes)

1. **Launch the App**
   - Open "Quick Deploy" from app drawer
   - Grant notification permission when prompted

2. **Register Device**
   - Tap "デバイスを登録" (Register Device)
   - Wait 2-3 seconds for registration

3. **Copy Upload URL**
   - Tap "curlコピー" to copy curl command
   - Or tap "URLコピー" to copy just the URL

4. **Enable Installation Permission**
   - If warning appears, tap "設定を開く"
   - Toggle on "Install unknown apps"
   - Return to app

5. **Save URL to Build Environment**
   - Add to GitHub Secrets, or
   - Save in your local build script

## Test the Setup

### Quick Test with Curl

```bash
# Use the curl command you copied earlier
curl -X POST -F "file=@/path/to/test.apk" "YOUR_UPLOAD_URL"
```

Within 5 seconds:
1. ✅ Push notification appears on device
2. ✅ Tap notification
3. ✅ APK downloads and install prompt shows
4. ✅ Tap Install

### GitHub Actions Test

Add to your `.github/workflows/android.yml`:

```yaml
- name: Upload to Quick Deploy
  env:
    QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_URL }}
  run: |
    curl -X POST \
      -F "file=@app/build/outputs/apk/debug/app-debug.apk" \
      "$QUICK_DEPLOY_URL"
```

Then push to trigger the workflow.

## Troubleshooting

### Build Fails: "google-services.json not found"

**Solution**: Ensure `google-services.json` is in `quick-deploy-app/` directory
```bash
ls -la quick-deploy-app/google-services.json
```

### Registration Fails: "Failed to register device"

**Possible causes**:
1. Wrong base URL → Check `App.android.kt`
2. Backend not deployed → Run `firebase deploy`
3. No internet → Check device connectivity

**Check logs**:
```bash
adb logcat -s DeviceRepository QuickDeployApiClient
```

### Notification Not Received

**Check**:
1. Notification permission granted
2. FCM token generated (check logcat)
3. Device not in battery saver mode

**Manual download**:
- Open app
- Tap "最新APKをダウンロード＆インストール"

### Installation Permission Denied

**Solution**:
1. Open Android Settings
2. Apps → Quick Deploy
3. Install unknown apps → Enable

## Next Steps

- ✅ Read [ANDROID_APP_README.md](ANDROID_APP_README.md) for detailed documentation
- ✅ Check [examples/github-actions-integration.md](examples/github-actions-integration.md) for CI/CD setup
- ✅ Review [REQUIREMENTS.md](REQUIREMENTS.md) for feature details

## Getting Help

**View logs**:
```bash
adb logcat -s QuickDeployApp QuickDeployFCM MainActivity
```

**Common log tags**:
- `QuickDeployApp` - Application lifecycle
- `QuickDeployFCM` - Push notifications
- `DeviceRepository` - Device registration
- `QuickDeployApiClient` - API calls
- `ApkInstaller` - APK download/install

**Check backend**:
```bash
firebase functions:log
```

---

**Setup Time**: ~10 minutes
**Status**: Ready to Use ✅
