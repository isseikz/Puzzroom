# Quick Deploy - Deployment Guide

This guide explains how to set up and deploy the Quick Deploy system.

## Prerequisites

Before you begin, ensure you have:

1. **Firebase Account**: Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. **Node.js**: Version 20 or 22 installed
3. **Firebase CLI**: Install globally with `npm install -g firebase-tools`
4. **Git**: For version control

## Firebase Project Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Enter project name (e.g., "quick-deploy-puzzroom")
4. Follow the setup wizard
5. Enable Google Analytics (optional)

### 2. Enable Required Firebase Services

In the Firebase Console, enable:

#### Firestore Database
1. Go to "Firestore Database" in the left menu
2. Click "Create database"
3. Choose production mode
4. Select a location (choose one close to your users)

#### Firebase Storage
1. Go to "Storage" in the left menu
2. Click "Get started"
3. Use default security rules (we'll deploy custom rules)
4. Select the same location as Firestore

#### Firebase Cloud Messaging (FCM)
1. Go to "Project Settings" > "Cloud Messaging"
2. Note your Server Key (you'll need this for Android app)
3. FCM is automatically enabled for Firebase projects

### 3. Configure Firebase CLI

1. Login to Firebase:
```bash
firebase login
```

2. Initialize Firebase in the project:
```bash
cd quick-deploy-app
firebase init
```

When prompted:
- Select "Firestore", "Functions", and "Storage"
- Choose your existing project
- Accept default Firestore rules file
- Select TypeScript for Functions
- Use ESLint: Yes
- Install dependencies: Yes
- Accept default Storage rules file

**Note**: The initialization files already exist in the repository, so you can skip this step if you're using the provided configuration.

### 4. Update Firebase Configuration

Edit `.firebaserc` to use your project ID:
```json
{
  "projects": {
    "default": "your-project-id"
  }
}
```

## Deploy Firebase Backend

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Build TypeScript Code

```bash
npm run build
```

### 3. Deploy to Firebase

Deploy all Firebase services:
```bash
cd ..
firebase deploy
```

Or deploy only functions:
```bash
firebase deploy --only functions
```

### 4. Verify Deployment

After deployment, Firebase will show the URLs for your functions:
```
✔  functions[register(us-central1)] https://us-central1-your-project.cloudfunctions.net/register
✔  functions[upload(us-central1)] https://us-central1-your-project.cloudfunctions.net/upload
✔  functions[download(us-central1)] https://us-central1-your-project.cloudfunctions.net/download
```

## Integration with Build Systems

### GitHub Actions Example

Create `.github/workflows/build-and-deploy.yml`:

```yaml
name: Build and Deploy APK

on:
  push:
    branches: [ main, develop ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build APK
      run: ./gradlew assembleDebug
    
    - name: Upload to Quick Deploy
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_URL }}
      run: |
        APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
        curl -X POST \
          -F "file=@$APK_PATH" \
          "$QUICK_DEPLOY_URL"
```

Add your Quick Deploy URL as a repository secret:
1. Go to your GitHub repository
2. Settings > Secrets and variables > Actions
3. Click "New repository secret"
4. Name: `QUICK_DEPLOY_URL`
5. Value: Your upload URL from the Android app

### Manual Upload Example

```bash
# Build your APK
./gradlew assembleDebug

# Upload to Quick Deploy
curl -X POST \
  -F "file=@app/build/outputs/apk/debug/app-debug.apk" \
  https://us-central1-your-project.cloudfunctions.net/upload/YOUR_DEVICE_TOKEN
```

## Android App Setup

### 1. Add Firebase to Android App

1. In Firebase Console, click "Add app" and select Android
2. Enter your app's package name (e.g., `tokyo.isseikuzumaki.quickdeploy`)
3. Download `google-services.json`
4. Place it in `quick-deploy-app/src/androidMain/`

### 2. Add Firebase Dependencies

The necessary Firebase dependencies should already be in `build.gradle.kts`:
```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
```

### 3. Configure FCM

Follow the Android client implementation in Phase 2.

## Monitoring and Maintenance

### View Logs

```bash
# View all function logs
firebase functions:log

# View logs for specific function
firebase functions:log --only register
```

### Monitor Usage

1. Go to Firebase Console
2. Navigate to "Functions" section
3. View invocations, execution time, and errors

### Update Functions

After making changes:
```bash
cd functions
npm run build
firebase deploy --only functions
```

### Database Maintenance

#### View Devices
```bash
firebase firestore:get devices
```

#### Cleanup Old Devices (if needed)
Use Firebase Console or write a cleanup script.

## Security Best Practices

1. **Protect Device Tokens**: Never commit device tokens to version control
2. **Use Secrets**: Store tokens in GitHub Secrets or environment variables
3. **Monitor Usage**: Regularly check Firebase Console for unusual activity
4. **Update Dependencies**: Keep Firebase SDKs up to date
5. **Review Rules**: Periodically review Firestore and Storage security rules

## Troubleshooting

### Function Deployment Fails

```bash
# Check Firebase CLI version
firebase --version

# Update Firebase CLI
npm install -g firebase-tools

# Clear cache and redeploy
npm run build
firebase deploy --only functions --force
```

### APK Upload Fails

1. Check file size (max 100MB)
2. Verify device token is valid
3. Check Firebase Storage quota
4. View function logs for errors

### Push Notifications Not Received

1. Verify FCM is enabled in Firebase Console
2. Check Android app has correct `google-services.json`
3. Ensure device has valid FCM token
4. Check function logs for FCM send errors

## Cost Estimation

Firebase has a free tier (Spark Plan) that should be sufficient for personal use:

- **Functions**: 2M invocations/month free
- **Firestore**: 20K document writes/day free
- **Storage**: 5GB stored free
- **FCM**: Unlimited free messages

For higher usage, upgrade to Blaze Plan (pay-as-you-go).

## Next Steps

1. Deploy the Firebase backend
2. Build and install the Android app
3. Register your device to get an upload URL
4. Integrate the URL with your build system
5. Test the complete workflow

For more information, see:
- [Firebase Functions Documentation](https://firebase.google.com/docs/functions)
- [Quick Deploy API Reference](functions/README.md)
- [Requirements Document](REQUIREMENTS.md)
