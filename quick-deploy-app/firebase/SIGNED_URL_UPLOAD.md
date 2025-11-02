# Signed URL Upload (Recommended)

## Overview

This is the **recommended** method for uploading APK files to Quick Deploy. It uses Firebase Storage signed URLs for direct uploads, which is:

- ✅ **Faster**: APK uploads directly to Storage (no proxy through Functions)
- ✅ **More Reliable**: No multipart parsing issues
- ✅ **More Scalable**: Offloads upload traffic from Functions
- ✅ **Cost Effective**: Reduces Functions execution time

## How It Works

```
┌─────────────┐
│ Build Tool  │
└──────┬──────┘
       │ 1. POST /getUploadUrl/{token}/url
       ├────────────────────────────────────►┌──────────────┐
       │                                      │   Function   │
       │ 2. {uploadUrl, notifyUrl}            │              │
       │◄────────────────────────────────────┤              │
       │                                      └──────────────┘
       │
       │ 3. PUT {uploadUrl} (APK binary)
       ├────────────────────────────────────►┌──────────────┐
       │                                      │   Storage    │
       │ 4. 200 OK                            │              │
       │◄────────────────────────────────────┤              │
       │                                      └──────────────┘
       │
       │ 5. POST {notifyUrl}
       ├────────────────────────────────────►┌──────────────┐
       │                                      │   Function   │
       │ 6. FCM Push Notification             │              │
       │                                      └──────┬───────┘
       │                                             │
       │                                             ▼
       │                                      ┌──────────────┐
       │                                      │    Device    │
       │                                      └──────────────┘
```

## API Endpoints

### 1. Get Upload URL

**Endpoint**: `POST /getUploadUrl/{deviceToken}/url`

**Request**:
```bash
curl -X POST https://us-central1-PROJECT.cloudfunctions.net/getUploadUrl/DEVICE_TOKEN/url
```

**Response**:
```json
{
  "uploadUrl": "https://storage.googleapis.com/...",
  "notifyUrl": "https://us-central1-PROJECT.cloudfunctions.net/notifyUploadComplete/DEVICE_TOKEN/notify"
}
```

### 2. Upload APK to Storage

**Endpoint**: Use the `uploadUrl` from step 1

**Request**:
```bash
curl -X PUT \
  -H "Content-Type: application/vnd.android.package-archive" \
  --upload-file app.apk \
  "UPLOAD_URL_FROM_STEP_1"
```

**Response**: `200 OK` (empty body)

### 3. Notify Upload Complete

**Endpoint**: Use the `notifyUrl` from step 1

**Request**:
```bash
curl -X POST "NOTIFY_URL_FROM_STEP_1"
```

**Response**:
```json
{
  "status": "success",
  "message": "APK uploaded successfully and notification sent",
  "uploadedAt": "2025-11-02T13:00:00.000Z"
}
```

## Usage Examples

### Shell Script

```bash
#!/bin/bash

DEVICE_TOKEN="your-device-token"
APK_FILE="app/build/outputs/apk/debug/app-debug.apk"
BASE_URL="https://us-central1-quick-deploy-3c0f0.cloudfunctions.net"

# Step 1: Get signed URL
RESPONSE=$(curl -s -X POST "$BASE_URL/getUploadUrl/$DEVICE_TOKEN/url")
UPLOAD_URL=$(echo "$RESPONSE" | jq -r '.uploadUrl')
NOTIFY_URL=$(echo "$RESPONSE" | jq -r '.notifyUrl')

# Step 2: Upload APK
curl -X PUT \
  -H "Content-Type: application/vnd.android.package-archive" \
  --upload-file "$APK_FILE" \
  "$UPLOAD_URL"

# Step 3: Notify completion
curl -X POST "$NOTIFY_URL"
```

### GitHub Actions

```yaml
- name: Upload to Quick Deploy
  env:
    DEVICE_TOKEN: ${{ secrets.QUICK_DEPLOY_DEVICE_TOKEN }}
    BASE_URL: https://us-central1-quick-deploy-3c0f0.cloudfunctions.net
  run: |
    # Get signed URL
    RESPONSE=$(curl -s -X POST "$BASE_URL/getUploadUrl/$DEVICE_TOKEN/url")
    UPLOAD_URL=$(echo "$RESPONSE" | jq -r '.uploadUrl')
    NOTIFY_URL=$(echo "$RESPONSE" | jq -r '.notifyUrl')

    # Upload APK
    curl -X PUT \
      -H "Content-Type: application/vnd.android.package-archive" \
      --upload-file app/build/outputs/apk/debug/app-debug.apk \
      "$UPLOAD_URL"

    # Notify completion
    curl -X POST "$NOTIFY_URL"
```

### Python

```python
import requests
import json

DEVICE_TOKEN = "your-device-token"
APK_FILE = "app/build/outputs/apk/debug/app-debug.apk"
BASE_URL = "https://us-central1-quick-deploy-3c0f0.cloudfunctions.net"

# Step 1: Get signed URL
response = requests.post(f"{BASE_URL}/getUploadUrl/{DEVICE_TOKEN}/url")
data = response.json()
upload_url = data["uploadUrl"]
notify_url = data["notifyUrl"]

# Step 2: Upload APK
with open(APK_FILE, "rb") as f:
    requests.put(
        upload_url,
        data=f,
        headers={"Content-Type": "application/vnd.android.package-archive"}
    )

# Step 3: Notify completion
requests.post(notify_url)
```

### Node.js

```javascript
const axios = require('axios');
const fs = require('fs');

const DEVICE_TOKEN = 'your-device-token';
const APK_FILE = 'app/build/outputs/apk/debug/app-debug.apk';
const BASE_URL = 'https://us-central1-quick-deploy-3c0f0.cloudfunctions.net';

async function uploadApk() {
  // Step 1: Get signed URL
  const {data} = await axios.post(
    `${BASE_URL}/getUploadUrl/${DEVICE_TOKEN}/url`
  );

  const {uploadUrl, notifyUrl} = data;

  // Step 2: Upload APK
  const apkData = fs.readFileSync(APK_FILE);
  await axios.put(uploadUrl, apkData, {
    headers: {
      'Content-Type': 'application/vnd.android.package-archive'
    }
  });

  // Step 3: Notify completion
  await axios.post(notifyUrl);

  console.log('✅ APK uploaded successfully!');
}

uploadApk();
```

## Performance Comparison

### Old Method (multipart/form-data)
```
Upload → Functions (parse multipart) → Storage
Time: ~10-15 seconds for 18MB APK
Reliability: ❌ Busboy parsing issues
```

### New Method (signed URL)
```
Upload → Storage (direct)
Time: ~3-5 seconds for 18MB APK
Reliability: ✅ Highly reliable
```

**Speed Improvement**: 2-3x faster
**Reliability**: No parsing issues

## Error Handling

### Invalid Device Token

```bash
$ curl -X POST https://.../getUploadUrl/invalid-token/url
```

Response:
```json
{
  "error": "Not Found",
  "message": "Invalid device token"
}
```

### APK Not Uploaded

If you call notify without uploading the APK:

```bash
$ curl -X POST https://.../notifyUploadComplete/TOKEN/notify
```

Response:
```json
{
  "error": "Bad Request",
  "message": "APK file not found. Please upload the file first."
}
```

### Upload URL Expired

Signed URLs are valid for 15 minutes. If expired:

```bash
$ curl -X PUT --upload-file app.apk "EXPIRED_URL"
```

Response: `403 Forbidden`

**Solution**: Request a new signed URL from step 1.

## Migration from Old Method

### Before (deprecated)

```bash
curl -X POST -F "file=@app.apk" \
  "https://upload-XXX.a.run.app/DEVICE_TOKEN"
```

### After (recommended)

```bash
# 1. Get URL
RESPONSE=$(curl -s -X POST \
  "https://us-central1-PROJECT.cloudfunctions.net/getUploadUrl/DEVICE_TOKEN/url")
UPLOAD_URL=$(echo "$RESPONSE" | jq -r '.uploadUrl')
NOTIFY_URL=$(echo "$RESPONSE" | jq -r '.notifyUrl')

# 2. Upload
curl -X PUT --upload-file app.apk "$UPLOAD_URL"

# 3. Notify
curl -X POST "$NOTIFY_URL"
```

The old endpoint will continue to work but is **deprecated** and may be removed in a future version.

## Security

- **Signed URLs**: Temporary (15 min expiration)
- **Device Token**: Required for all operations
- **Storage**: Private bucket (no public access)
- **Content-Type**: Enforced to APK only

## Troubleshooting

### No notification received

1. Check that step 3 (notify) was called
2. Verify FCM token is registered
3. Check device notification settings

### Upload fails

1. Verify signed URL is not expired (< 15 minutes)
2. Check APK file is valid
3. Ensure Content-Type header is set correctly

### "APK file not found" error

1. Ensure step 2 (upload) completed successfully before step 3
2. Check that you're using the correct device token

## Best Practices

1. **Cache the signed URL**: Use it within 15 minutes
2. **Check upload success**: Verify HTTP 200 response from Storage
3. **Handle errors**: Implement retry logic for network failures
4. **Set timeout**: Upload may take time for large APKs (use 60s+ timeout)

## Complete Example Script

See: [examples/upload-with-signed-url.sh](examples/upload-with-signed-url.sh)

```bash
./examples/upload-with-signed-url.sh YOUR_DEVICE_TOKEN app.apk
```

---

**Status**: ✅ Production Ready
**Version**: 2.0 (Signed URL)
**Recommended**: Yes
