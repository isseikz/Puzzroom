# Google Sign-in Setup Instructions (Credential Manager API)

## Prerequisites

1. Firebase project must be created and configured
2. `google-services.json` must be placed in `nlt-app/src/androidMain/`

## Configuration Steps

### 1. Get Web Client ID from Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **General**
4. Scroll down to **Your apps** section
5. Find your Android app
6. Copy the **Web Client ID** (NOT Android Client ID)
   - This is the "Web application" type OAuth 2.0 client ID

### 2. Update NLTViewModelImpl.kt

Open `nlt-app/src/androidMain/kotlin/tokyo/isseikuzumaki/nlt/ui/viewmodel/NLTViewModelImpl.kt`

Find this line:
```kotlin
private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

Replace `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` with your actual Web Client ID from Firebase Console.

Example:
```kotlin
private const val WEB_CLIENT_ID = "123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com"
```

### 3. Enable Google Sign-in in Firebase

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable **Google** as a sign-in provider
3. Configure the support email if required

## What's Different (Credential Manager API)

This implementation uses the **modern Credential Manager API** instead of the legacy One Tap Sign-in:

- **No activity result handling needed** - Credential Manager handles the UI flow automatically
- **Better UX** - Integrated with Android's system credential picker
- **Future-proof** - Uses the recommended approach by Google
- **Simpler code** - No need for IntentSenderRequest or activity result launchers

## Testing

1. Build and run the app
2. Click "Googleでサインイン" button
3. System credential picker will appear
4. Select your Google account
5. Grant permissions if prompted
6. You should be signed in successfully

## Troubleshooting

### "Developer error" or "Invalid client ID"
- Double-check that you're using the **Web Client ID**, not the Android Client ID
- Make sure the Web Client ID is correctly copied with no extra spaces
- Ensure `google-services.json` is properly configured

### Credential picker doesn't appear
- Check that Google Sign-in is enabled in Firebase Console
- Verify that the app has internet permission in AndroidManifest.xml
- Check Logcat for error messages with tag "NLTViewModel"
- Make sure Credential Manager dependencies are properly added

### "Sign-in failed" error
- Check Logcat for detailed error messages
- Ensure Firebase Authentication is properly set up
- Verify SHA-1 fingerprint is registered in Firebase (if using release build)
- Try clearing app data and signing in again

### GetCredentialException
- This usually indicates configuration issues
- Check that the Web Client ID is correct
- Verify Firebase Authentication is enabled
- Make sure the app is properly registered in Firebase Console
