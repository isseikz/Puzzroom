# Example: Manual and SSH Server Integration

This example shows how to use Quick Deploy from manual builds or SSH servers.

## Quick Start

### 1. Get Your Upload URL

1. Open the Quick Deploy Android app
2. Tap "Register Device"
3. Copy the upload URL shown on screen
4. Optionally, copy the curl command sample

### 2. Upload APK Manually

#### Using curl (Recommended)

```bash
# Basic upload
curl -X POST \
  -F "file=@/path/to/your/app.apk" \
  https://your-project.cloudfunctions.net/upload/your-device-token

# With verbose output
curl -v -X POST \
  -F "file=@/path/to/your/app.apk" \
  https://your-project.cloudfunctions.net/upload/your-device-token

# With progress indicator
curl -X POST \
  -F "file=@/path/to/your/app.apk" \
  --progress-bar \
  https://your-project.cloudfunctions.net/upload/your-device-token
```

#### Using wget

```bash
wget --post-file=/path/to/your/app.apk \
  --header="Content-Type: application/vnd.android.package-archive" \
  https://your-project.cloudfunctions.net/upload/your-device-token
```

#### Using HTTPie (if installed)

```bash
http POST https://your-project.cloudfunctions.net/upload/your-device-token \
  file@/path/to/your/app.apk
```

## Shell Script Examples

### Basic Deployment Script

Create `deploy-to-quick-deploy.sh`:

```bash
#!/bin/bash

# Configuration
QUICK_DEPLOY_URL="https://your-project.cloudfunctions.net/upload/your-device-token"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Build the APK
echo "Building APK..."
./gradlew assembleDebug

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

# Upload to Quick Deploy
echo "Uploading to Quick Deploy..."
response=$(curl -X POST \
  -F "file=@$APK_PATH" \
  -w "%{http_code}" \
  -s \
  "$QUICK_DEPLOY_URL")

# Check response
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ APK uploaded successfully!${NC}"
    echo "Check your device for installation prompt."
else
    echo -e "${RED}✗ Upload failed with status: $response${NC}"
    exit 1
fi
```

Make it executable:
```bash
chmod +x deploy-to-quick-deploy.sh
```

Run it:
```bash
./deploy-to-quick-deploy.sh
```

### Advanced Script with Options

Create `quick-deploy.sh`:

```bash
#!/bin/bash

# Default configuration
QUICK_DEPLOY_URL="${QUICK_DEPLOY_URL:-}"
BUILD_TYPE="debug"
MODULE="app"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Usage function
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -u, --url URL          Quick Deploy upload URL (required)"
    echo "  -b, --build TYPE       Build type: debug or release (default: debug)"
    echo "  -m, --module NAME      Module name (default: app)"
    echo "  -s, --skip-build       Skip build step, upload existing APK"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  QUICK_DEPLOY_URL       Upload URL (alternative to -u flag)"
    echo ""
    echo "Examples:"
    echo "  $0 -u https://example.com/upload/token"
    echo "  $0 -u https://example.com/upload/token -b release"
    echo "  QUICK_DEPLOY_URL=https://example.com/upload/token $0 -s"
    exit 1
}

# Parse arguments
SKIP_BUILD=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -u|--url)
            QUICK_DEPLOY_URL="$2"
            shift 2
            ;;
        -b|--build)
            BUILD_TYPE="$2"
            shift 2
            ;;
        -m|--module)
            MODULE="$2"
            shift 2
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate URL
if [ -z "$QUICK_DEPLOY_URL" ]; then
    echo -e "${RED}Error: Quick Deploy URL is required${NC}"
    echo "Set QUICK_DEPLOY_URL environment variable or use -u flag"
    usage
fi

# Set APK path based on build type
APK_PATH="$MODULE/build/outputs/apk/$BUILD_TYPE/$MODULE-$BUILD_TYPE.apk"

# Build if not skipped
if [ "$SKIP_BUILD" = false ]; then
    echo -e "${YELLOW}Building $BUILD_TYPE APK...${NC}"
    ./gradlew assemble${BUILD_TYPE^}
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
fi

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}APK not found: $APK_PATH${NC}"
    echo "Build the APK first or check the path"
    exit 1
fi

# Get APK size
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)

# Upload to Quick Deploy
echo -e "${YELLOW}Uploading $APK_SIZE APK to Quick Deploy...${NC}"
response=$(curl -X POST \
  -F "file=@$APK_PATH" \
  -w "%{http_code}" \
  -s \
  -o /tmp/quick-deploy-response.txt \
  "$QUICK_DEPLOY_URL")

# Check response
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ APK uploaded successfully!${NC}"
    echo -e "${GREEN}Check your device for installation prompt.${NC}"
    cat /tmp/quick-deploy-response.txt
    rm /tmp/quick-deploy-response.txt
    exit 0
else
    echo -e "${RED}✗ Upload failed with HTTP status: $response${NC}"
    cat /tmp/quick-deploy-response.txt
    rm /tmp/quick-deploy-response.txt
    exit 1
fi
```

Usage:
```bash
# Set URL and deploy
export QUICK_DEPLOY_URL="https://your-url"
./quick-deploy.sh

# Or use command line flag
./quick-deploy.sh -u "https://your-url"

# Build release and deploy
./quick-deploy.sh -u "https://your-url" -b release

# Skip build, upload existing APK
./quick-deploy.sh -u "https://your-url" -s
```

## SSH Server Integration

### Setup on Remote Server

1. SSH into your server:
```bash
ssh user@your-server.com
```

2. Clone your Android project:
```bash
git clone https://github.com/your-username/your-android-app.git
cd your-android-app
```

3. Install dependencies:
```bash
# Install Java if not present
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# Install Android SDK (or use existing)
# Follow: https://developer.android.com/studio#command-tools
```

4. Create deployment script:
```bash
nano deploy.sh
```

Paste the basic or advanced script from above, then:
```bash
chmod +x deploy.sh
```

### Automated Build and Deploy

Create a git hook to deploy on push:

```bash
# In your repository on the server
cd .git/hooks
nano post-receive
```

Add:
```bash
#!/bin/bash

echo "Building and deploying to Quick Deploy..."

# Navigate to repository
cd /path/to/your-android-app

# Pull latest changes
git pull

# Build and deploy
QUICK_DEPLOY_URL="your-upload-url" ./deploy-to-quick-deploy.sh
```

Make it executable:
```bash
chmod +x post-receive
```

### Scheduled Builds (Cron)

Add a cron job for scheduled builds:

```bash
crontab -e
```

Add:
```bash
# Build and deploy every day at 2 AM
0 2 * * * cd /path/to/your-android-app && git pull && QUICK_DEPLOY_URL="your-url" ./deploy.sh
```

## Environment Variable Setup

### Using .env File

Create `.env` file:
```bash
QUICK_DEPLOY_URL=https://your-project.cloudfunctions.net/upload/your-device-token
```

Load in script:
```bash
#!/bin/bash

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

# Rest of your script...
```

**Important**: Add `.env` to `.gitignore` to avoid committing the URL!

### Using Environment Variables Directly

```bash
# Set once in your shell session
export QUICK_DEPLOY_URL="https://your-url"

# Add to ~/.bashrc or ~/.zshrc for persistence
echo 'export QUICK_DEPLOY_URL="https://your-url"' >> ~/.bashrc
source ~/.bashrc
```

## Integration with Other Tools

### Make Task

Add to `Makefile`:
```makefile
.PHONY: deploy-quick

deploy-quick:
	@echo "Building and deploying to Quick Deploy..."
	./gradlew assembleDebug
	curl -X POST -F "file=@app/build/outputs/apk/debug/app-debug.apk" $(QUICK_DEPLOY_URL)
```

Usage:
```bash
make deploy-quick QUICK_DEPLOY_URL=https://your-url
```

### Gradle Task

Add to `build.gradle.kts`:
```kotlin
tasks.register("deployToQuickDeploy") {
    dependsOn("assembleDebug")
    
    doLast {
        val url = System.getenv("QUICK_DEPLOY_URL")
            ?: throw GradleException("QUICK_DEPLOY_URL environment variable not set")
        
        val apkFile = file("build/outputs/apk/debug/app-debug.apk")
        
        exec {
            commandLine("curl", "-X", "POST", "-F", "file=@${apkFile.absolutePath}", url)
        }
    }
}
```

Usage:
```bash
QUICK_DEPLOY_URL=https://your-url ./gradlew deployToQuickDeploy
```

## Troubleshooting

### curl Command Not Found

Install curl:
```bash
# Ubuntu/Debian
sudo apt-get install curl

# macOS (usually pre-installed)
brew install curl
```

### Permission Denied

Make script executable:
```bash
chmod +x deploy.sh
```

### APK Not Found

Check the build output path:
```bash
find . -name "*.apk" -type f
```

### Upload Timeout

Increase curl timeout:
```bash
curl -X POST \
  -F "file=@app.apk" \
  --max-time 300 \
  "$QUICK_DEPLOY_URL"
```

## Security Best Practices

1. **Never commit** upload URLs to version control
2. **Use environment variables** or .env files (gitignored)
3. **Rotate URLs** if exposed or compromised
4. **Limit access** to deployment scripts on shared servers
5. **Use HTTPS** only (enforced by Firebase)

## Next Steps

- Automate your build process
- Set up monitoring for failed builds
- Create deployment logs
- Integrate with your CI/CD pipeline

For more information:
- [Quick Deploy Documentation](../README.md)
- [Deployment Guide](../DEPLOYMENT.md)
- [GitHub Actions Integration](github-actions-integration.md)
