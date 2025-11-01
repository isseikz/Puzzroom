# Firebase Functions Setup Guide

## Overview

This directory contains Firebase Cloud Functions implementation for the Quick Deploy App. The functions are implemented using a hybrid approach that allows sharing data structures between the Kotlin Multiplatform client code and the Node.js Firebase Functions.

## Architecture

### Data Structure Sharing

The key feature of this setup is the ability to share data structures between:
- **Android/iOS clients** (Kotlin)
- **Firebase Functions** (JavaScript/Node.js)

Shared data models are defined in:
```
src/commonMain/kotlin/tokyo/isseikuzumaki/quickdeploy/data/
```

These models use `@Serializable` annotation from kotlinx.serialization and can be:
1. Used directly in Android/iOS code
2. Compiled to JavaScript for use in Firebase Functions (optional)

### Current Implementation

The current implementation uses **JavaScript/TypeScript** for Firebase Functions (as recommended in the issue). The Kotlin/JS compilation capability is configured but the actual functions are implemented in JavaScript for simplicity and better Firebase tooling support.

You can choose either approach:

#### Option 1: JavaScript/TypeScript Functions (Current)
- Functions implemented in `functions/index.js`
- Standard Firebase tooling and workflows
- Data structures defined in Kotlin for client, manually mirrored in JS

#### Option 2: Kotlin/JS Functions (Advanced)
- Functions implemented in `src/jsMain/kotlin/.../functions/`
- Requires additional build step to compile Kotlin to JS
- Full type safety and code sharing

## Prerequisites

1. **Node.js**: Version 20 or 22 (already installed)
   ```bash
   node --version  # Should show v20.x or v22.x
   ```

2. **Firebase CLI**: Install globally
   ```bash
   npm install -g firebase-tools
   ```

3. **Firebase Project**: Create a project at https://console.firebase.google.com

## Setup Instructions

### 1. Configure Firebase Project

Edit `.firebaserc` and replace `"your-project-id"` with your actual Firebase project ID:

```json
{
  "projects": {
    "default": "my-actual-project-id"
  }
}
```

### 2. Install Dependencies

```bash
cd functions
npm install
```

### 3. Initialize Firebase (if needed)

If you need to reconfigure Firebase:

```bash
firebase login
firebase init firestore  # Initialize Cloud Firestore
firebase init functions  # Initialize Cloud Functions
```

When prompted:
- Select "Use an existing project" and choose your project
- Select JavaScript or TypeScript
- Install dependencies with npm

### 4. Deploy Functions

Deploy all functions:
```bash
npm run deploy
```

Or deploy specific functions:
```bash
firebase deploy --only functions:makeUppercase
firebase deploy --only functions:addMessage
```

## Available Functions

### 1. makeUppercase (HTTP Function)

Converts text to uppercase via HTTP request.

**Endpoint:**
```
https://us-central1-<project-id>.cloudfunctions.net/makeUppercase?text=hello
```

**Usage:**
```bash
curl "https://us-central1-<project-id>.cloudfunctions.net/makeUppercase?text=hello"
# Response: {"result":"HELLO"}
```

### 2. addMessage (Firestore Trigger)

Automatically processes new messages added to the `messages` collection.

**Trigger:** Document created in `messages/{messageId}`

**Behavior:**
1. Reads the `original` field from the new document
2. Converts it to uppercase
3. Writes back `uppercase` and `timestamp` fields

**Usage:**
```javascript
// Add a document to trigger the function
await firebase.firestore()
  .collection('messages')
  .add({ original: 'hello world' });

// The function will automatically add:
// { uppercase: 'HELLO WORLD', timestamp: 1234567890 }
```

## Local Testing

### Run Functions Emulator

```bash
cd functions
npm run serve
```

This starts the Firebase Emulator Suite on http://localhost:4000

### Test HTTP Function

```bash
curl "http://localhost:5001/<project-id>/us-central1/makeUppercase?text=hello"
```

### Test Firestore Trigger

Use the Emulator UI at http://localhost:4000 to add documents to the `messages` collection.

## Using Kotlin/JS (Optional Advanced Setup)

To use Kotlin/JS compiled functions:

### 1. Build Kotlin/JS Code

```bash
cd /path/to/quick-deploy-app
./gradlew :quick-deploy-app:jsBrowserProductionWebpack
```

This compiles Kotlin code in `src/jsMain/` to JavaScript.

### 2. Copy Compiled Code

```bash
cp -r build/dist/js/productionExecutable/* functions/dist/
```

### 3. Update index.js

Import and use the compiled Kotlin/JS functions:

```javascript
import { makeUppercase, addMessage } from './dist/quick-deploy-app.js';

export { makeUppercase, addMessage };
```

## Project Structure

```
quick-deploy-app/
├── firebase.json              # Firebase configuration
├── .firebaserc               # Firebase project aliases
├── firestore.rules           # Firestore security rules
├── firestore.indexes.json    # Firestore indexes
├── functions/                # Cloud Functions directory
│   ├── package.json         # Node.js dependencies
│   ├── index.js             # Main functions entry point
│   └── .eslintrc.json       # ESLint configuration
├── src/
│   ├── commonMain/kotlin/   # Shared data structures
│   │   └── tokyo/isseikuzumaki/quickdeploy/data/
│   │       └── Message.kt   # Shared Message model
│   └── jsMain/kotlin/       # Kotlin/JS functions (optional)
│       └── tokyo/isseikuzumaki/quickdeploy/functions/
│           ├── CloudFunctions.kt    # Kotlin/JS implementations
│           ├── FirebaseFunctions.kt # Firebase Functions bindings
│           ├── FirebaseAdmin.kt     # Firebase Admin bindings
│           └── FirebaseFirestore.kt # Firestore bindings
└── build.gradle.kts         # Gradle build with JS target

```

## Development Workflow

### For JavaScript Functions (Recommended for Quick Start)

1. Edit `functions/index.js`
2. Test locally: `npm run serve`
3. Deploy: `npm run deploy`

### For Kotlin/JS Functions (Advanced)

1. Edit Kotlin files in `src/jsMain/kotlin/`
2. Define shared models in `src/commonMain/kotlin/`
3. Build: `./gradlew :quick-deploy-app:jsBrowserProductionWebpack`
4. Copy to functions: `cp -r build/dist/js/productionExecutable/* functions/dist/`
5. Update `functions/index.js` to import Kotlin/JS exports
6. Deploy: `cd functions && npm run deploy`

## Shared Data Structures

Example of a shared data structure:

```kotlin
// src/commonMain/kotlin/tokyo/isseikuzumaki/quickdeploy/data/Message.kt
@Serializable
data class Message(
    val text: String,
    val timestamp: Long = 0L
)
```

This can be used:
- In Android/iOS apps directly
- Compiled to TypeScript definitions (with additional tools)
- Mirrored in JavaScript for Firebase Functions

## Monitoring and Logs

### View Logs

```bash
npm run logs
```

Or in Firebase Console:
https://console.firebase.google.com/project/<project-id>/functions/logs

### View Metrics

Firebase Console → Functions → Dashboard

## Security Considerations

1. **Firestore Rules**: Update `firestore.rules` for production
2. **Authentication**: Add Firebase Auth checks in functions
3. **Environment Variables**: Use Firebase config for secrets
   ```bash
   firebase functions:config:set someservice.key="THE API KEY"
   ```

## Troubleshooting

### Functions won't deploy

1. Check Node.js version: `node --version` (should be 20 or 22)
2. Check Firebase CLI: `firebase --version`
3. Clear cache: `rm -rf functions/node_modules && cd functions && npm install`

### Kotlin/JS compilation issues

1. Check Gradle: `./gradlew --version`
2. Clean build: `./gradlew clean`
3. Rebuild: `./gradlew :quick-deploy-app:jsBrowserProductionWebpack`

### Emulator issues

1. Stop all emulators: `firebase emulators:stop`
2. Clear emulator data: `firebase emulators:exec --project <project-id> "echo done"`
3. Restart: `npm run serve`

## References

- [Firebase Functions Documentation](https://firebase.google.com/docs/functions)
- [Kotlin/JS Documentation](https://kotlinlang.org/docs/js-overview.html)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Cloud Firestore Documentation](https://firebase.google.com/docs/firestore)

## Next Steps

1. Replace `"your-project-id"` in `.firebaserc` with your Firebase project ID
2. Run `cd functions && npm install`
3. Test locally with `npm run serve`
4. Deploy to Firebase with `npm run deploy`
5. Update Firestore rules in `firestore.rules` for production security
6. Implement additional functions as needed
