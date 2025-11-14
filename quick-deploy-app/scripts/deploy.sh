#!/bin/bash

##############################################################################
# Quick Deploy - APK Deployment Script
#
# This script automates the 4-step deployment process:
# 1. Build APK using Gradle
# 2. Get upload URL from API
# 3. Upload APK to Firebase Storage
# 4. Notify device of new APK
#
# Usage:
#   ./deploy.sh [device-token]
#   or set SECRET_QUICK_DEPLOY_TOKEN environment variable
#
# Exit codes:
#   0 - Success
#   1 - Missing device token
#   2 - Build failed
#   3 - Upload failed
#   4 - Notification failed
##############################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API endpoints
GET_UPLOAD_URL_BASE="https://getuploadurl-o45ehp4r5q-uc.a.run.app/upload"
NOTIFY_URL_BASE="https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/upload"

# Get device token from argument or environment variable
DEVICE_TOKEN="${1:-${SECRET_QUICK_DEPLOY_TOKEN:-}}"

if [ -z "$DEVICE_TOKEN" ]; then
    echo -e "${RED}ERROR: Device token is required${NC}"
    echo "Usage: $0 <device-token>"
    echo "Or set SECRET_QUICK_DEPLOY_TOKEN environment variable"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Quick Deploy - APK Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Device Token: ${DEVICE_TOKEN:0:8}...${DEVICE_TOKEN: -4}"
echo ""

# Get the repository root directory (parent of quick-deploy-app)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
QUICK_DEPLOY_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
REPO_ROOT="$( cd "$QUICK_DEPLOY_DIR/.." && pwd )"

cd "$REPO_ROOT"

##############################################################################
# Step 1: Build APK
##############################################################################
echo -e "${YELLOW}[1/4] Building APK...${NC}"

if [ ! -f "./gradlew" ]; then
    echo -e "${RED}ERROR: gradlew not found in $REPO_ROOT${NC}"
    exit 2
fi

# Clean and build the debug APK
if ./gradlew :quick-deploy-app:clean :quick-deploy-app:assembleDebug --no-daemon --console=plain 2>&1 | tee /tmp/build.log | tail -20; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}ERROR: Build failed${NC}"
    echo "Check /tmp/build.log for details"
    exit 2
fi

# Find the built APK
APK_PATH=$(find "$QUICK_DEPLOY_DIR" -name "*.apk" -path "*/debug/*" -type f | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}ERROR: APK not found after build${NC}"
    echo "Expected location: quick-deploy-app/build/outputs/apk/debug/"
    exit 2
fi

echo -e "${GREEN}✓ APK found: $(basename "$APK_PATH")${NC}"
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "  Size: $APK_SIZE"
echo ""

##############################################################################
# Step 2: Get upload URL
##############################################################################
echo -e "${YELLOW}[2/4] Getting upload URL...${NC}"

UPLOAD_URL_ENDPOINT="${GET_UPLOAD_URL_BASE}/${DEVICE_TOKEN}/url"
echo "Requesting: $UPLOAD_URL_ENDPOINT"

# Use POST request as required by the API
UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$UPLOAD_URL_ENDPOINT")
HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -n 1)
RESPONSE_BODY=$(echo "$UPLOAD_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ne 200 ]; then
    echo -e "${RED}ERROR: Failed to get upload URL (HTTP $HTTP_CODE)${NC}"
    echo "Response: $RESPONSE_BODY"
    exit 3
fi

# Extract the signed URL from JSON response using jq if available, otherwise fallback to grep/sed
if command -v jq &> /dev/null; then
    SIGNED_URL=$(echo "$RESPONSE_BODY" | jq -r '.uploadUrl')
else
    # Fallback for systems without jq
    SIGNED_URL=$(echo "$RESPONSE_BODY" | grep -o '"uploadUrl":"[^"]*"' | sed 's/"uploadUrl":"//;s/"$//')
fi

if [ -z "$SIGNED_URL" ] || [ "$SIGNED_URL" = "null" ]; then
    echo -e "${RED}ERROR: Could not extract signed URL from response${NC}"
    echo "Response: $RESPONSE_BODY"
    exit 3
fi

echo -e "${GREEN}✓ Upload URL obtained${NC}"
echo ""

##############################################################################
# Step 3: Upload APK
##############################################################################
echo -e "${YELLOW}[3/4] Uploading APK to Firebase Storage...${NC}"

# Upload with progress indicator
UPLOAD_RESULT=$(curl -s -w "\n%{http_code}" -X PUT \
    -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary "@$APK_PATH" \
    "$SIGNED_URL")

UPLOAD_HTTP_CODE=$(echo "$UPLOAD_RESULT" | tail -n 1)
UPLOAD_BODY=$(echo "$UPLOAD_RESULT" | sed '$d')

if [ "$UPLOAD_HTTP_CODE" -ne 200 ]; then
    echo -e "${RED}ERROR: Upload failed (HTTP $UPLOAD_HTTP_CODE)${NC}"
    echo "Response: $UPLOAD_BODY"
    exit 3
fi

echo -e "${GREEN}✓ APK uploaded successfully${NC}"
echo ""

##############################################################################
# Step 4: Notify device
##############################################################################
echo -e "${YELLOW}[4/4] Notifying device...${NC}"

NOTIFY_URL="${NOTIFY_URL_BASE}/${DEVICE_TOKEN}/notify"
echo "Notifying: $NOTIFY_URL"

NOTIFY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    "$NOTIFY_URL")

NOTIFY_HTTP_CODE=$(echo "$NOTIFY_RESPONSE" | tail -n 1)
NOTIFY_BODY=$(echo "$NOTIFY_RESPONSE" | sed '$d')

if [ "$NOTIFY_HTTP_CODE" -ne 200 ]; then
    echo -e "${RED}ERROR: Notification failed (HTTP $NOTIFY_HTTP_CODE)${NC}"
    echo "Response: $NOTIFY_BODY"
    exit 4
fi

echo -e "${GREEN}✓ Device notified${NC}"
echo ""

##############################################################################
# Success
##############################################################################
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Deployment completed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Summary:"
echo "  • APK: $(basename "$APK_PATH") ($APK_SIZE)"
echo "  • Device Token: ${DEVICE_TOKEN:0:8}...${DEVICE_TOKEN: -4}"
echo "  • The device should receive a notification shortly"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Check your Android device for a notification"
echo "  2. Tap the notification to download and install"
echo ""

exit 0
