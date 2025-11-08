#!/bin/bash

##############################################################################
# Quick Deploy - Test Script
#
# This script validates the deployment automation without actually deploying.
# It checks:
# - Script existence and permissions
# - Directory structure
# - Required tools
##############################################################################

set -e
set +e  # Disable exit on error for arithmetic operations

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Quick Deploy - Deployment Automation Tests"
echo "==========================================="
echo ""

PASSED=0
FAILED=0

# Helper functions
pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
}

fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
}

warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
QUICK_DEPLOY_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
REPO_ROOT="$( cd "$QUICK_DEPLOY_DIR/.." && pwd )"

echo "Test 1: Check directory structure"
echo "----------------------------------"

if [ -d "$QUICK_DEPLOY_DIR/scripts" ]; then
    pass "scripts/ directory exists"
else
    fail "scripts/ directory missing"
fi

if [ -d "$QUICK_DEPLOY_DIR/mcp" ]; then
    pass "mcp/ directory exists"
else
    fail "mcp/ directory missing"
fi

if [ -d "$REPO_ROOT/.github/workflows" ]; then
    pass ".github/workflows/ directory exists"
else
    fail ".github/workflows/ directory missing"
fi

echo ""
echo "Test 2: Check file existence"
echo "-----------------------------"

if [ -f "$QUICK_DEPLOY_DIR/scripts/deploy.sh" ]; then
    pass "deploy.sh exists"
else
    fail "deploy.sh missing"
fi

if [ -x "$QUICK_DEPLOY_DIR/scripts/deploy.sh" ]; then
    pass "deploy.sh is executable"
else
    fail "deploy.sh is not executable"
fi

if [ -f "$QUICK_DEPLOY_DIR/mcp/index.js" ]; then
    pass "MCP server index.js exists"
else
    fail "MCP server index.js missing"
fi

if [ -f "$QUICK_DEPLOY_DIR/mcp/package.json" ]; then
    pass "MCP server package.json exists"
else
    fail "MCP server package.json missing"
fi

if [ -f "$QUICK_DEPLOY_DIR/mcp/README.md" ]; then
    pass "MCP server README.md exists"
else
    fail "MCP server README.md missing"
fi

if [ -f "$REPO_ROOT/.github/workflows/quick-deploy-auto.yml" ]; then
    pass "GitHub Actions workflow exists"
else
    fail "GitHub Actions workflow missing"
fi

if [ -f "$QUICK_DEPLOY_DIR/DEPLOYMENT_AUTOMATION.md" ]; then
    pass "DEPLOYMENT_AUTOMATION.md exists"
else
    fail "DEPLOYMENT_AUTOMATION.md missing"
fi

echo ""
echo "Test 3: Check required tools"
echo "-----------------------------"

if command -v bash >/dev/null 2>&1; then
    pass "bash is available"
else
    fail "bash is not available"
fi

if command -v curl >/dev/null 2>&1; then
    pass "curl is available"
else
    fail "curl is not available"
fi

if command -v java >/dev/null 2>&1; then
    pass "java is available"
    java -version 2>&1 | head -1
else
    fail "java is not available"
fi

if [ -f "$REPO_ROOT/gradlew" ]; then
    pass "gradlew exists"
else
    fail "gradlew missing"
fi

if command -v node >/dev/null 2>&1; then
    pass "node is available"
    node --version
else
    warn "node is not available (required for MCP server)"
fi

echo ""
echo "Test 4: Check script syntax"
echo "----------------------------"

if bash -n "$QUICK_DEPLOY_DIR/scripts/deploy.sh" 2>/dev/null; then
    pass "deploy.sh syntax is valid"
else
    fail "deploy.sh has syntax errors"
fi

if command -v node >/dev/null 2>&1; then
    if node -c "$QUICK_DEPLOY_DIR/mcp/index.js" 2>/dev/null; then
        pass "MCP server index.js syntax is valid"
    else
        fail "MCP server index.js has syntax errors"
    fi
else
    warn "Skipping MCP server syntax check (node not available)"
fi

echo ""
echo "Test 5: Check API endpoints structure"
echo "--------------------------------------"

# Check if URLs are defined in deploy.sh
if grep -q "getuploadurl-o45ehp4r5q-uc.a.run.app" "$QUICK_DEPLOY_DIR/scripts/deploy.sh"; then
    pass "Get upload URL endpoint defined"
else
    fail "Get upload URL endpoint not defined"
fi

if grep -q "notifyuploadcomplete-o45ehp4r5q-uc.a.run.app" "$QUICK_DEPLOY_DIR/scripts/deploy.sh"; then
    pass "Notify endpoint defined"
else
    fail "Notify endpoint not defined"
fi

echo ""
echo "Test 6: Check documentation"
echo "---------------------------"

if grep -q "deploy_apk" "$QUICK_DEPLOY_DIR/mcp/README.md"; then
    pass "MCP README documents deploy_apk tool"
else
    fail "MCP README missing deploy_apk documentation"
fi

if grep -q "set_device_token" "$QUICK_DEPLOY_DIR/mcp/README.md"; then
    pass "MCP README documents set_device_token tool"
else
    fail "MCP README missing set_device_token documentation"
fi

if grep -q "get_device_token" "$QUICK_DEPLOY_DIR/mcp/README.md"; then
    pass "MCP README documents get_device_token tool"
else
    fail "MCP README missing get_device_token documentation"
fi

if grep -q "SECRET_QUICK_DEPLOY_TOKEN" "$REPO_ROOT/.github/workflows/quick-deploy-auto.yml"; then
    pass "GitHub Actions workflow uses SECRET_QUICK_DEPLOY_TOKEN"
else
    fail "GitHub Actions workflow doesn't use SECRET_QUICK_DEPLOY_TOKEN"
fi

echo ""
echo "========================================="
echo "Summary:"
echo "  Passed: $PASSED"
echo "  Failed: $FAILED"
echo "========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
