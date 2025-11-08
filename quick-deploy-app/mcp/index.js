#!/usr/bin/env node

/**
 * Quick Deploy MCP Server
 * 
 * This MCP server provides tools for deploying Android APKs to devices
 * using the Quick Deploy system.
 * 
 * Available tools:
 * - deploy_apk: Deploy APK to a device using a device token
 * - set_device_token: Store device token in environment variable
 * - get_device_token: Retrieve stored device token
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { readFile, writeFile, mkdir } from 'fs/promises';
import { existsSync } from 'fs';
import { homedir } from 'os';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Config directory for storing device token
const CONFIG_DIR = join(homedir(), '.quick-deploy');
const TOKEN_FILE = join(CONFIG_DIR, 'device-token.txt');

/**
 * Ensure config directory exists
 */
async function ensureConfigDir() {
  if (!existsSync(CONFIG_DIR)) {
    await mkdir(CONFIG_DIR, { recursive: true });
  }
}

/**
 * Save device token to file
 */
async function saveDeviceToken(token) {
  await ensureConfigDir();
  await writeFile(TOKEN_FILE, token, 'utf-8');
}

/**
 * Load device token from file or environment variable
 */
async function loadDeviceToken() {
  // First check environment variable
  if (process.env.SECRET_QUICK_DEPLOY_TOKEN) {
    return process.env.SECRET_QUICK_DEPLOY_TOKEN;
  }
  
  // Then check saved file
  if (existsSync(TOKEN_FILE)) {
    const token = await readFile(TOKEN_FILE, 'utf-8');
    return token.trim();
  }
  
  return null;
}

/**
 * Execute the deployment script
 */
async function executeDeploy(deviceToken) {
  const scriptPath = join(__dirname, '..', 'scripts', 'deploy.sh');
  
  return new Promise((resolve, reject) => {
    const process = spawn(scriptPath, [deviceToken], {
      env: { ...process.env, SECRET_QUICK_DEPLOY_TOKEN: deviceToken },
      cwd: join(__dirname, '..', '..')
    });
    
    let stdout = '';
    let stderr = '';
    
    process.stdout.on('data', (data) => {
      stdout += data.toString();
    });
    
    process.stderr.on('data', (data) => {
      stderr += data.toString();
    });
    
    process.on('close', (code) => {
      if (code === 0) {
        resolve({
          success: true,
          message: 'Deployment completed successfully',
          output: stdout,
          exitCode: code
        });
      } else {
        reject({
          success: false,
          message: `Deployment failed with exit code ${code}`,
          output: stdout,
          error: stderr,
          exitCode: code
        });
      }
    });
    
    process.on('error', (error) => {
      reject({
        success: false,
        message: `Failed to execute deployment script: ${error.message}`,
        error: error.toString()
      });
    });
  });
}

// Create MCP server
const server = new Server(
  {
    name: 'quick-deploy-mcp-server',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: 'deploy_apk',
        description: 'Deploy the Quick Deploy Android APK to a device. This will build the APK, upload it to Firebase Storage, and notify the target device. The device token can be provided as a parameter or will be read from environment variable SECRET_QUICK_DEPLOY_TOKEN or saved configuration.',
        inputSchema: {
          type: 'object',
          properties: {
            device_token: {
              type: 'string',
              description: 'The device token (UUID) to deploy to. If not provided, will use saved token or SECRET_QUICK_DEPLOY_TOKEN environment variable.',
            },
          },
        },
      },
      {
        name: 'set_device_token',
        description: 'Store a device token for future deployments. This saves the token to ~/.quick-deploy/device-token.txt so you don\'t need to provide it every time.',
        inputSchema: {
          type: 'object',
          properties: {
            device_token: {
              type: 'string',
              description: 'The device token (UUID) to save for future deployments.',
            },
          },
          required: ['device_token'],
        },
      },
      {
        name: 'get_device_token',
        description: 'Retrieve the currently saved device token from environment variable or saved configuration file.',
        inputSchema: {
          type: 'object',
          properties: {},
        },
      },
    ],
  };
});

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  
  try {
    switch (name) {
      case 'deploy_apk': {
        // Get device token from args, saved config, or environment
        let deviceToken = args.device_token;
        
        if (!deviceToken) {
          deviceToken = await loadDeviceToken();
        }
        
        if (!deviceToken) {
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  success: false,
                  error: 'No device token provided. Please provide device_token parameter, set SECRET_QUICK_DEPLOY_TOKEN environment variable, or use set_device_token tool first.',
                }, null, 2),
              },
            ],
          };
        }
        
        try {
          const result = await executeDeploy(deviceToken);
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  success: true,
                  message: result.message,
                  details: result.output,
                  device_token_prefix: deviceToken.substring(0, 8) + '...',
                }, null, 2),
              },
            ],
          };
        } catch (error) {
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  success: false,
                  error: error.message,
                  details: error.output || error.error || '',
                  exit_code: error.exitCode,
                }, null, 2),
              },
            ],
            isError: true,
          };
        }
      }
      
      case 'set_device_token': {
        const token = args.device_token;
        
        if (!token || token.trim().length === 0) {
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  success: false,
                  error: 'Device token cannot be empty',
                }, null, 2),
              },
            ],
            isError: true,
          };
        }
        
        await saveDeviceToken(token.trim());
        
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                success: true,
                message: `Device token saved successfully to ${TOKEN_FILE}`,
                token_prefix: token.substring(0, 8) + '...',
              }, null, 2),
            },
          ],
        };
      }
      
      case 'get_device_token': {
        const token = await loadDeviceToken();
        
        if (!token) {
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify({
                  success: false,
                  message: 'No device token found. Use set_device_token to save one, or set SECRET_QUICK_DEPLOY_TOKEN environment variable.',
                }, null, 2),
              },
            ],
          };
        }
        
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                success: true,
                device_token: token,
                token_prefix: token.substring(0, 8) + '...',
                source: process.env.SECRET_QUICK_DEPLOY_TOKEN ? 'environment' : 'saved_file',
              }, null, 2),
            },
          ],
        };
      }
      
      default:
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                success: false,
                error: `Unknown tool: ${name}`,
              }, null, 2),
            },
          ],
          isError: true,
        };
    }
  } catch (error) {
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            success: false,
            error: error.message,
            stack: error.stack,
          }, null, 2),
        },
      ],
      isError: true,
    };
  }
});

// Start the server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('Quick Deploy MCP Server running on stdio');
}

main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
