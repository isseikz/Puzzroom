# Quick Deploy MCP Server

An MCP (Model Context Protocol) server that provides tools for deploying Android APKs to devices using the Quick Deploy system.

## Overview

This MCP server wraps the deployment script (`scripts/deploy.sh`) and provides three tools that can be used by AI agents like GitHub Copilot:

1. **deploy_apk** - Deploy the APK to a device
2. **set_device_token** - Save a device token for future deployments
3. **get_device_token** - Retrieve the saved device token

## Installation

1. Install dependencies:
```bash
cd quick-deploy-app/mcp
npm install
```

2. Configure the MCP server in your GitHub Copilot settings (or other MCP-compatible client).

### For GitHub Copilot

Add to your MCP configuration (typically in VS Code settings or `~/.config/github-copilot/config.json`):

```json
{
  "mcpServers": {
    "quick-deploy": {
      "command": "node",
      "args": ["/path/to/Puzzroom/quick-deploy-app/mcp/index.js"],
      "env": {
        "SECRET_QUICK_DEPLOY_TOKEN": "your-device-token-here"
      }
    }
  }
}
```

## Usage

### Option 1: Set Device Token via Environment Variable

Set the `SECRET_QUICK_DEPLOY_TOKEN` environment variable in your MCP configuration:

```json
{
  "mcpServers": {
    "quick-deploy": {
      "command": "node",
      "args": ["/path/to/Puzzroom/quick-deploy-app/mcp/index.js"],
      "env": {
        "SECRET_QUICK_DEPLOY_TOKEN": "7da00b5d-e281-45d2-a4ec-60fd37ddcf6f"
      }
    }
  }
}
```

### Option 2: Save Device Token Using the MCP Tool

From your AI agent (e.g., GitHub Copilot), use the `set_device_token` tool:

```
Please save my device token: 7da00b5d-e281-45d2-a4ec-60fd37ddcf6f
```

This will save the token to `~/.quick-deploy/device-token.txt`.

### Deploying

Once configured, ask your AI agent to deploy:

```
Please deploy the Quick Deploy APK to my device
```

The agent will use the `deploy_apk` tool, which will:
1. Build the APK using Gradle
2. Get the signed upload URL from Firebase
3. Upload the APK to Firebase Storage
4. Notify the device via FCM

## Available Tools

### deploy_apk

Deploys the Quick Deploy Android APK to a device.

**Parameters:**
- `device_token` (optional): The device token UUID. If not provided, will use saved token or environment variable.

**Example:**
```
Deploy the APK to device 7da00b5d-e281-45d2-a4ec-60fd37ddcf6f
```

### set_device_token

Stores a device token for future deployments.

**Parameters:**
- `device_token` (required): The device token UUID to save.

**Example:**
```
Save my device token: 7da00b5d-e281-45d2-a4ec-60fd37ddcf6f
```

### get_device_token

Retrieves the currently saved device token.

**Example:**
```
What is my current device token?
```

## Token Priority

The MCP server checks for device tokens in this order:
1. `device_token` parameter passed to `deploy_apk`
2. `SECRET_QUICK_DEPLOY_TOKEN` environment variable
3. Saved token in `~/.quick-deploy/device-token.txt`

## Error Handling

The MCP server provides clear English error messages that help AI agents understand what went wrong:

- Missing device token
- Build failures
- Upload failures
- Notification failures

Each error includes:
- Success status (true/false)
- Error message
- Additional details (build logs, HTTP responses)
- Exit code (for script execution)

## Development

### Running Locally

```bash
cd quick-deploy-app/mcp
npm install
node index.js
```

The server will run in stdio mode, waiting for MCP protocol messages.

### Testing

You can test the deployment script directly:

```bash
cd quick-deploy-app/scripts
./deploy.sh 7da00b5d-e281-45d2-a4ec-60fd37ddcf6f
```

## Requirements

- Node.js >= 18.0.0
- Java/Gradle (for building Android APK)
- Internet connection (for Firebase APIs)
- Valid device token from Quick Deploy app

## API Endpoints Used

The deployment script uses these Firebase Cloud Functions:
- **Get Upload URL**: `https://getuploadurl-o45ehp4r5q-uc.a.run.app/upload/{deviceToken}/url`
- **Notify Device**: `https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/upload/{deviceToken}/notify`

## Security

- Device tokens are stored locally in `~/.quick-deploy/device-token.txt` with user-only read permissions
- Environment variables can be used for CI/CD pipelines
- All communication with Firebase is over HTTPS
- Device tokens are long, random UUIDs that are difficult to guess

## Troubleshooting

### "No device token provided"
- Set `SECRET_QUICK_DEPLOY_TOKEN` environment variable in MCP config
- Or use `set_device_token` tool to save a token
- Or provide `device_token` parameter when calling `deploy_apk`

### "Build failed"
- Ensure Gradle is properly set up
- Check that you're in the correct repository
- Check `/tmp/build.log` for detailed build errors

### "Upload failed" or "Notification failed"
- Verify your device token is correct
- Check your internet connection
- Verify Firebase Cloud Functions are running

## Related Files

- `../scripts/deploy.sh` - The deployment script wrapped by this MCP server
- `../../.github/workflows/quick-deploy-auto.yml` - GitHub Actions workflow for auto-deployment
- `../REQUIREMENTS.md` - System requirements and specifications
- `../docs/design/SequenceDiagram.md` - System architecture and flow
