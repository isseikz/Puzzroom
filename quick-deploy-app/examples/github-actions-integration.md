# Example: GitHub Actions Integration

This example shows how to integrate Quick Deploy with GitHub Actions to automatically upload APK files after each build.

## Workflow File

Create `.github/workflows/build-and-deploy-apk.yml` in your repository:

```yaml
name: Build and Deploy APK to Quick Deploy

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build Debug APK
      run: ./gradlew assembleDebug
    
    - name: Find APK file
      id: find_apk
      run: |
        APK_PATH=$(find . -name "*.apk" -path "*/build/outputs/apk/debug/*" | head -n 1)
        echo "apk_path=$APK_PATH" >> $GITHUB_OUTPUT
        echo "Found APK: $APK_PATH"
    
    - name: Upload to Quick Deploy
      if: github.event_name == 'push' && steps.find_apk.outputs.apk_path != ''
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_URL }}
      run: |
        curl -X POST \
          -F "file=@${{ steps.find_apk.outputs.apk_path }}" \
          -w "\nHTTP Status: %{http_code}\n" \
          "$QUICK_DEPLOY_URL"
    
    - name: Upload APK as Artifact (Backup)
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: ${{ steps.find_apk.outputs.apk_path }}
        retention-days: 7
```

## Setup Instructions

### 1. Add the Workflow File

Copy the above workflow to `.github/workflows/build-and-deploy-apk.yml` in your Android project repository.

### 2. Get Your Quick Deploy URL

1. Open the Quick Deploy Android app
2. Tap "Register Device" to get your unique upload URL
3. Copy the URL (it will look like: `https://your-project.cloudfunctions.net/upload/your-device-token`)

### 3. Add URL as GitHub Secret

1. Go to your GitHub repository
2. Navigate to **Settings** > **Secrets and variables** > **Actions**
3. Click **New repository secret**
4. Name: `QUICK_DEPLOY_URL`
5. Value: Your upload URL from the Quick Deploy app
6. Click **Add secret**

### 4. Test the Workflow

1. Push a commit to the `main` or `develop` branch
2. Go to **Actions** tab in your GitHub repository
3. Watch the workflow run
4. You should receive a push notification on your device when the APK is ready

## Workflow Explanation

### Trigger Events

```yaml
on:
  push:
    branches: [ main, develop ]  # Triggers on push to these branches
  pull_request:
    branches: [ main ]            # Triggers on PR to main (build only, no deploy)
  workflow_dispatch:              # Allows manual trigger from GitHub UI
```

### Key Steps

1. **Checkout code**: Gets your repository code
2. **Set up JDK**: Installs Java Development Kit
3. **Build APK**: Runs Gradle to build the debug APK
4. **Find APK**: Locates the built APK file
5. **Upload to Quick Deploy**: Sends APK to your device (only on push)
6. **Upload as Artifact**: Backup upload to GitHub Artifacts

### Conditional Upload

The workflow only uploads to Quick Deploy on `push` events (not on pull requests):
```yaml
if: github.event_name == 'push' && steps.find_apk.outputs.apk_path != ''
```

## Advanced Configurations

### Build Multiple Variants

```yaml
    - name: Build Multiple APKs
      run: |
        ./gradlew assembleDebug
        ./gradlew assembleRelease
    
    - name: Upload Debug APK
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_DEBUG_URL }}
      run: |
        curl -X POST -F "file=@app/build/outputs/apk/debug/app-debug.apk" "$QUICK_DEPLOY_URL"
    
    - name: Upload Release APK
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_RELEASE_URL }}
      run: |
        curl -X POST -F "file=@app/build/outputs/apk/release/app-release.apk" "$QUICK_DEPLOY_URL"
```

### Add Build Information

```yaml
    - name: Upload with Build Info
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_URL }}
      run: |
        APK_PATH="${{ steps.find_apk.outputs.apk_path }}"
        BUILD_NUMBER="${{ github.run_number }}"
        COMMIT_SHA="${{ github.sha }}"
        
        echo "Uploading APK - Build: $BUILD_NUMBER, Commit: ${COMMIT_SHA:0:7}"
        curl -X POST \
          -F "file=@$APK_PATH" \
          -H "X-Build-Number: $BUILD_NUMBER" \
          -H "X-Commit-Sha: $COMMIT_SHA" \
          "$QUICK_DEPLOY_URL"
```

### Notify on Failure

```yaml
    - name: Notify on Failure
      if: failure()
      run: |
        echo "Build or deployment failed!"
        # Add your notification logic here (Slack, Discord, etc.)
```

### Upload Only on Specific Branches

```yaml
    - name: Upload to Quick Deploy (Main Branch Only)
      if: github.ref == 'refs/heads/main' && github.event_name == 'push'
      env:
        QUICK_DEPLOY_URL: ${{ secrets.QUICK_DEPLOY_URL }}
      run: |
        curl -X POST -F "file=@${{ steps.find_apk.outputs.apk_path }}" "$QUICK_DEPLOY_URL"
```

## Troubleshooting

### APK Not Found

If the workflow can't find the APK, check your Gradle build output path:
```yaml
    - name: Find APK file (Alternative)
      run: |
        find . -name "*.apk" -type f
        APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
```

### Upload Fails

Check the curl output for errors:
```yaml
    - name: Upload to Quick Deploy (with verbose output)
      run: |
        curl -v -X POST \
          -F "file=@${{ steps.find_apk.outputs.apk_path }}" \
          "$QUICK_DEPLOY_URL"
```

### Secret Not Working

Ensure:
1. Secret name matches exactly: `QUICK_DEPLOY_URL`
2. Secret is added to the correct repository
3. Workflow has access to secrets (check repository settings)

## Security Notes

1. **Never commit the upload URL** to your repository
2. **Use GitHub Secrets** to store sensitive URLs
3. **Limit secret access** to necessary workflows
4. **Rotate URLs** if they are compromised (register new device)

## Alternative: Manual Script

If you prefer to run locally:

```bash
#!/bin/bash
# deploy-apk.sh

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
QUICK_DEPLOY_URL="your-upload-url-here"

# Build APK
./gradlew assembleDebug

# Upload to Quick Deploy
curl -X POST \
  -F "file=@$APK_PATH" \
  -w "\nHTTP Status: %{http_code}\n" \
  "$QUICK_DEPLOY_URL"

echo "APK uploaded successfully!"
```

Make it executable:
```bash
chmod +x deploy-apk.sh
```

Run it:
```bash
./deploy-apk.sh
```

## Next Steps

1. Customize the workflow for your project structure
2. Add additional build steps (tests, lint, etc.)
3. Configure notifications
4. Test the complete workflow

For more examples and documentation:
- [Quick Deploy Documentation](../README.md)
- [Deployment Guide](../DEPLOYMENT.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
