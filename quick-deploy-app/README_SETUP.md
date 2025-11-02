# Quick Deploy Android App - Setup Required

## ⚠️ IMPORTANT: Before Building

This Android app requires Firebase configuration to build successfully.

### Required Steps

#### 1. Add Firebase Configuration File

You need to add `google-services.json` to this directory.

**How to get it**:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your Quick Deploy project
3. Go to Project Settings (⚙️ icon)
4. Scroll to "Your apps" section
5. Click Android app (or add one if not exists)
6. Package name: `tokyo.isseikuzumaki.quickdeploy`
7. Click "Download google-services.json"
8. Copy file to: `quick-deploy-app/google-services.json`

**Quick command**:
```bash
# Copy your downloaded file
cp ~/Downloads/google-services.json quick-deploy-app/google-services.json
```

A template file is provided at `google-services.json.template` for reference.

#### 2. Configure Backend URL

Edit `src/androidMain/kotlin/tokyo/isseikuzumaki/quickdeploy/App.android.kt`:

```kotlin
// Line 17: Replace with your actual URL
val baseUrl = "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net"
```

**Example**:
```kotlin
val baseUrl = "https://us-central1-quickdeploy-prod.cloudfunctions.net"
```

### Then Build

```bash
./gradlew :quick-deploy-app:assembleDebug
```

## Need Help?

See detailed guides:
- **[QUICK_START_ANDROID.md](QUICK_START_ANDROID.md)** - Quick setup (10 min)
- **[ANDROID_APP_README.md](ANDROID_APP_README.md)** - Complete documentation
- **[ANDROID_APP_SUMMARY.md](ANDROID_APP_SUMMARY.md)** - Implementation overview

## Files to Configure

- [ ] `google-services.json` - Firebase config (download from console)
- [ ] `App.android.kt` line 17 - Backend URL

Once these are set, you can build and run the app!
