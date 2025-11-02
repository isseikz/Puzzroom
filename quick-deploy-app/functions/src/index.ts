/**
 * Firebase Cloud Functions for Quick Deploy
 *
 * This module implements the backend API for the Quick Deploy APK distribution system.
 *
 * Endpoints:
 * - POST /register - Device registration and URL generation (A-001)
 * - POST /upload/{deviceToken} - APK upload and notification (A-002, A-003, A-004)
 * - GET /download/{deviceToken} - APK download (A-005)
 */

import {onRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {initializeApp} from "firebase-admin/app";
import {getFirestore, Timestamp} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {getMessaging} from "firebase-admin/messaging";
import {logger} from "firebase-functions";
import busboy from "busboy";
import {v4 as uuidv4} from "uuid";
import {
  RegisterRequest,
  RegisterResponse,
  DeviceDocument,
  UploadResponse,
  ErrorResponse,
} from "./types";

// Initialize Firebase Admin SDK
initializeApp();

const db = getFirestore();
const storage = getStorage();
const messaging = getMessaging();

// Constants
const DEVICES_COLLECTION = "devices";
const APK_STORAGE_PATH = "apks";
const APK_EXPIRATION_MINUTES = 10;
const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

/**
 * A-001: Device Registration and URL Generation
 *
 * POST /register
 *
 * Registers a new device and generates a unique device token.
 * Returns upload and download URLs for the device.
 */
export const register = onRequest(
  {cors: true, maxInstances: 10},
  async (req, res) => {
    // Only allow POST requests
    if (req.method !== "POST") {
      res.status(405).json({
        error: "Method Not Allowed",
        message: "Only POST requests are allowed",
      } as ErrorResponse);
      return;
    }

    try {
      const requestData = req.body as RegisterRequest;

      // Validate request
      if (!requestData.fcmToken || !requestData.deviceInfo?.deviceId) {
        res.status(400).json({
          error: "Bad Request",
          message: "fcmToken and deviceInfo.deviceId are required",
        } as ErrorResponse);
        return;
      }

      // Generate a unique, unpredictable device token (UUID v4)
      const deviceToken = uuidv4();

      // Check if this device already has a token (by deviceId)
      // If so, invalidate the old token
      const existingDeviceQuery = await db.collection(DEVICES_COLLECTION)
        .where("deviceInfo.deviceId", "==", requestData.deviceInfo.deviceId)
        .get();

      // Delete old device tokens for this device
      const batch = db.batch();
      existingDeviceQuery.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });
      await batch.commit();

      // Create device document
      const deviceDoc: DeviceDocument = {
        deviceToken,
        fcmToken: requestData.fcmToken,
        deviceInfo: requestData.deviceInfo,
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now(),
      };

      // Save to Firestore
      await db.collection(DEVICES_COLLECTION).doc(deviceToken).set(deviceDoc);

      logger.info(`Device registered: ${deviceToken}`);

      // Generate URLs
      // Use Cloud Functions URL if FUNCTION_URL is set, otherwise construct from project region
      const baseUrl = process.env.FUNCTION_URL ||
        `https://${process.env.GCLOUD_PROJECT}.cloudfunctions.net`;
      const uploadUrl = `${baseUrl}/upload/${deviceToken}`;
      const downloadUrl = `${baseUrl}/download/${deviceToken}`;

      // Return response
      const response: RegisterResponse = {
        deviceToken,
        uploadUrl,
        downloadUrl,
      };

      res.status(200).json(response);
    } catch (error) {
      logger.error("Error in register function:", error);
      res.status(500).json({
        error: "Internal Server Error",
        message: "Failed to register device",
      } as ErrorResponse);
    }
  }
);

/**
 * A-002, A-003, A-004: APK Upload, Storage, and Notification
 *
 * POST /upload/{deviceToken}
 *
 * Accepts APK file upload via multipart/form-data.
 * Stores the APK in Firebase Storage and sends push notification to the device.
 */
export const upload = onRequest(
  {cors: true, maxInstances: 10, timeoutSeconds: 540},
  async (req, res) => {
    // Only allow POST requests
    if (req.method !== "POST") {
      res.status(405).json({
        error: "Method Not Allowed",
        message: "Only POST requests are allowed",
      } as ErrorResponse);
      return;
    }

    try {
      // Extract device token from path
      const pathParts = req.path.split("/");
      const deviceToken = pathParts[pathParts.length - 1];

      if (!deviceToken) {
        res.status(400).json({
          error: "Bad Request",
          message: "Device token is required in the URL path",
        } as ErrorResponse);
        return;
      }

      // Verify device token exists
      const deviceDoc = await db.collection(DEVICES_COLLECTION).doc(deviceToken).get();
      if (!deviceDoc.exists) {
        res.status(404).json({
          error: "Not Found",
          message: "Invalid device token",
        } as ErrorResponse);
        return;
      }

      const deviceData = deviceDoc.data() as DeviceDocument;

      // Parse multipart form data
      const bb = busboy({headers: req.headers, limits: {fileSize: MAX_FILE_SIZE}});
      let fileProcessed = false;
      let uploadError: Error | null = null;

      bb.on("file", async (
        fieldname: string,
        fileStream: NodeJS.ReadableStream,
        info: {filename: string; encoding: string; mimeType: string}
      ) => {
        const {filename, mimeType} = info;

        logger.info(`Uploading file: ${filename}, type: ${mimeType}`);

        // Validate APK file (accept if filename ends with .apk OR has correct MIME type)
        if (!filename.endsWith(".apk") && mimeType !== "application/vnd.android.package-archive") {
          uploadError = new Error("Only APK files are allowed");
          fileStream.resume();
          return;
        }

        try {
          // Delete old APK if exists
          const bucket = storage.bucket();
          const oldApkPath = `${APK_STORAGE_PATH}/${deviceToken}/`;
          const [files] = await bucket.getFiles({prefix: oldApkPath});

          await Promise.all(files.map((file) => file.delete()));

          // Upload new APK
          const apkPath = `${APK_STORAGE_PATH}/${deviceToken}/app.apk`;
          const file = bucket.file(apkPath);

          const writeStream = file.createWriteStream({
            metadata: {
              contentType: "application/vnd.android.package-archive",
              metadata: {
                uploadedAt: new Date().toISOString(),
                originalFilename: filename,
              },
            },
          });

          fileStream.pipe(writeStream);

          await new Promise((resolve, reject) => {
            writeStream.on("finish", resolve);
            writeStream.on("error", reject);
          });

          // Set expiration time (10 minutes from now)
          const expirationTime = new Date();
          expirationTime.setMinutes(expirationTime.getMinutes() + APK_EXPIRATION_MINUTES);

          await file.setMetadata({
            metadata: {
              expiresAt: expirationTime.toISOString(),
            },
          });

          logger.info(`APK uploaded successfully: ${apkPath}`);

          // Update device document with last upload time
          await db.collection(DEVICES_COLLECTION).doc(deviceToken).update({
            lastApkUploadedAt: Timestamp.now(),
            updatedAt: Timestamp.now(),
          });

          // Send FCM notification
          // Note: Notification messages are in Japanese as per requirements (REQUIREMENTS.md)
          // The target users are Japanese developers
          const message = {
            token: deviceData.fcmToken,
            notification: {
              title: "新しいAPKを受信しました",
              body: "タップしてインストールします。",
            },
            data: {
              deviceToken,
              action: "apk_ready",
            },
            android: {
              priority: "high" as const,
            },
          };

          try {
            await messaging.send(message);
            logger.info(`FCM notification sent to device: ${deviceToken}`);
          } catch (fcmError) {
            logger.error("Failed to send FCM notification:", fcmError);
            // Don't fail the upload if notification fails
          }

          fileProcessed = true;
        } catch (error) {
          logger.error("Error uploading file:", error);
          uploadError = error as Error;
        }
      });

      bb.on("finish", () => {
        if (uploadError) {
          res.status(500).json({
            error: "Upload Failed",
            message: uploadError.message,
          } as ErrorResponse);
          return;
        }

        if (!fileProcessed) {
          res.status(400).json({
            error: "Bad Request",
            message: "No APK file found in the request",
          } as ErrorResponse);
          return;
        }

        const response: UploadResponse = {
          status: "success",
          message: "APK uploaded successfully and notification sent",
          uploadedAt: new Date().toISOString(),
        };

        res.status(200).json(response);
      });

      bb.on("error", (error: Error) => {
        logger.error("Busboy error:", error);
        res.status(500).json({
          error: "Upload Failed",
          message: error.message,
        } as ErrorResponse);
      });

      req.pipe(bb);
    } catch (error) {
      logger.error("Error in upload function:", error);
      res.status(500).json({
        error: "Internal Server Error",
        message: "Failed to upload APK",
      } as ErrorResponse);
    }
  }
);

/**
 * A-005: APK Download
 *
 * GET /download/{deviceToken}
 *
 * Downloads the APK file for the specified device token.
 */
export const download = onRequest(
  {cors: true, maxInstances: 10, timeoutSeconds: 540},
  async (req, res) => {
    // Only allow GET requests
    if (req.method !== "GET") {
      res.status(405).json({
        error: "Method Not Allowed",
        message: "Only GET requests are allowed",
      } as ErrorResponse);
      return;
    }

    try {
      // Extract device token from path
      const pathParts = req.path.split("/");
      const deviceToken = pathParts[pathParts.length - 1];

      if (!deviceToken) {
        res.status(400).json({
          error: "Bad Request",
          message: "Device token is required in the URL path",
        } as ErrorResponse);
        return;
      }

      // Verify device token exists
      const deviceDoc = await db.collection(DEVICES_COLLECTION).doc(deviceToken).get();
      if (!deviceDoc.exists) {
        res.status(404).json({
          error: "Not Found",
          message: "Invalid device token",
        } as ErrorResponse);
        return;
      }

      // Get APK file from storage
      const bucket = storage.bucket();
      const apkPath = `${APK_STORAGE_PATH}/${deviceToken}/app.apk`;
      const file = bucket.file(apkPath);

      // Check if file exists
      const [exists] = await file.exists();
      if (!exists) {
        res.status(404).json({
          error: "Not Found",
          message: "No APK file available for this device",
        } as ErrorResponse);
        return;
      }

      // Set response headers
      res.setHeader("Content-Type", "application/vnd.android.package-archive");
      res.setHeader("Content-Disposition", "attachment; filename=\"app.apk\"");

      // Stream the file to the response
      const readStream = file.createReadStream();

      readStream.on("error", (error) => {
        logger.error("Error streaming file:", error);
        if (!res.headersSent) {
          res.status(500).json({
            error: "Download Failed",
            message: "Failed to download APK file",
          } as ErrorResponse);
        }
      });

      readStream.pipe(res);

      logger.info(`APK download started for device: ${deviceToken}`);
    } catch (error) {
      logger.error("Error in download function:", error);
      if (!res.headersSent) {
        res.status(500).json({
          error: "Internal Server Error",
          message: "Failed to download APK",
        } as ErrorResponse);
      }
    }
  }
);

/**
 * Scheduled function to clean up expired APK files
 *
 * Runs every 5 minutes to delete APK files older than 10 minutes.
 */
export const cleanupExpiredApks = onSchedule(
  {schedule: "*/5 * * * *", timeoutSeconds: 540},
  async () => {
    try {
      logger.info("Starting cleanup of expired APK files");

      const bucket = storage.bucket();
      const [files] = await bucket.getFiles({prefix: APK_STORAGE_PATH});

      const now = new Date();
      let deletedCount = 0;

      for (const file of files) {
        const [metadata] = await file.getMetadata();
        const expiresAt = metadata.metadata?.expiresAt;

        if (expiresAt && typeof expiresAt === "string") {
          const expirationDate = new Date(expiresAt);
          if (now > expirationDate) {
            await file.delete();
            deletedCount++;
            logger.info(`Deleted expired APK: ${file.name}`);
          }
        }
      }

      logger.info(`Cleanup completed. Deleted ${deletedCount} expired APK files.`);
    } catch (error) {
      logger.error("Error in cleanupExpiredApks function:", error);
    }
  }
);
