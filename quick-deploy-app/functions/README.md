# Quick Deploy Firebase Functions

This directory contains the Firebase Cloud Functions backend for the Quick Deploy APK distribution system.

## Overview

The Quick Deploy backend provides a REST API for APK file distribution from build environments (GitHub Actions, SSH servers) to Android devices via push notifications.

## Architecture

### Components

1. **Firebase Functions** - Serverless HTTP endpoints and scheduled tasks
2. **Firestore Database** - Device token and FCM token storage
3. **Firebase Storage** - Temporary APK file storage
4. **Firebase Cloud Messaging (FCM)** - Push notifications to Android devices

### API Endpoints

#### POST /register
Device registration and URL generation (Requirement A-001)

**Request:**
```json
{
  "fcmToken": "string",
  "deviceInfo": {
    "deviceId": "string",
    "deviceModel": "string (optional)",
    "osVersion": "string (optional)",
    "appVersion": "string (optional)"
  }
}
```

**Response:**
```json
{
  "deviceToken": "uuid-v4-string",
  "uploadUrl": "https://[project].web.app/upload/[deviceToken]",
  "downloadUrl": "https://[project].web.app/download/[deviceToken]"
}
```

#### POST /upload/{deviceToken}
APK upload and notification (Requirements A-002, A-003, A-004)

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Body: APK file (max 100MB)

**Response:**
```json
{
  "status": "success",
  "message": "APK uploaded successfully and notification sent",
  "uploadedAt": "2025-11-02T02:00:00.000Z"
}
```

**Example using curl:**
```bash
curl -X POST \
  -F "file=@app.apk" \
  https://[project].web.app/upload/[deviceToken]
```

#### GET /download/{deviceToken}
APK download (Requirement A-005)

**Request:**
- Method: GET
- Response Type: application/vnd.android.package-archive

**Response:**
Binary APK file stream

### Scheduled Tasks

#### cleanupExpiredApks
Runs every 5 minutes to delete APK files older than 10 minutes.

## Setup

### Prerequisites

- Node.js 20.x or 22.x
- Firebase CLI (`npm install -g firebase-tools`)
- Firebase project with:
  - Cloud Functions enabled
  - Firestore Database enabled
  - Firebase Storage enabled
  - Firebase Cloud Messaging enabled

### Installation

1. Install dependencies:
```bash
cd functions
npm install
```

2. Build TypeScript code:
```bash
npm run build
```

3. Deploy to Firebase:
```bash
firebase deploy --only functions
```

### Development

#### Local Development

Start the Firebase emulator:
```bash
npm run serve
```

#### Build and Watch

Build TypeScript code in watch mode:
```bash
npm run build:watch
```

#### Linting

Run ESLint:
```bash
npm run lint
```

Auto-fix lint issues:
```bash
npm run lint -- --fix
```

### Environment Configuration

The functions automatically detect the Firebase project configuration. No additional environment variables are required for basic operation.

## Security

- All communication is over HTTPS
- Device tokens are UUID v4 (unpredictable and secure)
- APK files are automatically deleted after 10 minutes
- Firestore and Storage are only accessible via Firebase Admin SDK (no client access)

## File Retention Policy

- **APK Files**: Automatically deleted 10 minutes after upload
- **Device Tokens**: Invalidated when a new URL is generated for the same device
- **Old APKs**: Deleted immediately when a new APK is uploaded for the same device

## Monitoring

View function logs:
```bash
npm run logs
```

Or use Firebase Console:
- https://console.firebase.google.com/project/[project-id]/functions

## Error Handling

All endpoints return appropriate HTTP status codes:
- `200` - Success
- `400` - Bad Request (invalid parameters)
- `404` - Not Found (invalid device token or missing APK)
- `405` - Method Not Allowed
- `500` - Internal Server Error

Error response format:
```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "code": "optional-error-code"
}
```

## Performance

- **Target Latency**: Upload to notification delivery < 10 seconds
- **Max File Size**: 100MB per APK
- **Concurrent Requests**: Max 10 instances per function
- **Timeout**: 540 seconds for upload/download functions

## References

- [Quick Deploy Requirements](../REQUIREMENTS.md)
- [Sequence Diagram](../docs/design/SequenceDiagram.md)
- [Firebase Functions Documentation](https://firebase.google.com/docs/functions)
