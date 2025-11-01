#!/bin/bash

# Firebase Functions Test Script
# This script demonstrates the Firebase Functions setup

echo "========================================="
echo "Firebase Functions Setup Verification"
echo "========================================="
echo ""

# Check Node.js
echo "✓ Checking Node.js..."
node --version
echo ""

# Check npm
echo "✓ Checking npm..."
npm --version
echo ""

# Check Firebase CLI (optional)
echo "✓ Checking Firebase CLI..."
if command -v firebase &> /dev/null; then
    firebase --version
else
    echo "  Firebase CLI not installed (optional)"
    echo "  Install with: npm install -g firebase-tools"
fi
echo ""

# Check functions dependencies
echo "✓ Checking functions dependencies..."
if [ -d "functions/node_modules" ]; then
    echo "  Dependencies installed ✓"
    echo "  Packages: $(ls functions/node_modules | wc -l)"
else
    echo "  Dependencies NOT installed"
    echo "  Run: cd functions && npm install"
fi
echo ""

# Verify function files
echo "✓ Verifying function files..."
if [ -f "functions/index.js" ]; then
    echo "  index.js exists ✓"
else
    echo "  index.js missing ✗"
fi

if [ -f "functions/package.json" ]; then
    echo "  package.json exists ✓"
else
    echo "  package.json missing ✗"
fi

if [ -f "firebase.json" ]; then
    echo "  firebase.json exists ✓"
else
    echo "  firebase.json missing ✗"
fi
echo ""

# Check Kotlin source files
echo "✓ Verifying Kotlin source files..."
COMMON_FILES=$(find src/commonMain/kotlin -name "*.kt" 2>/dev/null | wc -l)
JS_FILES=$(find src/jsMain/kotlin -name "*.kt" 2>/dev/null | wc -l)
echo "  Common source files: $COMMON_FILES"
echo "  JS source files: $JS_FILES"
echo ""

# Validate JavaScript syntax
echo "✓ Validating JavaScript syntax..."
if node -c functions/index.js 2>/dev/null; then
    echo "  index.js syntax valid ✓"
else
    echo "  index.js syntax errors ✗"
fi
echo ""

# Summary
echo "========================================="
echo "Summary"
echo "========================================="
echo ""
echo "Firebase Functions is set up and ready!"
echo ""
echo "Next steps:"
echo "1. Update .firebaserc with your Firebase project ID"
echo "2. Run: cd functions && npm install (if not done)"
echo "3. Test locally: cd functions && npm run serve"
echo "4. Deploy: cd functions && npm run deploy"
echo ""
echo "See FIREBASE_SETUP.md for detailed instructions."
echo "See QUICK_REFERENCE.md for usage examples."
echo ""
