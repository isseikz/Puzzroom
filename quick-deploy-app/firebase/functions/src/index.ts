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
const BUCKET_NAME = "gs://quick-deploy-3c0f0.firebasestorage.app";
const NOTIFY_URL = "https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/upload";
const UPLOAD_URL = "https://getuploadurl-o45ehp4r5q-uc.a.run.app/upload";
const DOWNLOAD_URL = "https://download-o45ehp4r5q-uc.a.run.app";
const APK_EXPIRATION_MINUTES = 10;

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

      // Delete storage files associated with old tokens
      const bucket = storage.bucket(BUCKET_NAME);
      const deleteOldFilesPromises = existingDeviceQuery.docs.map(async (doc) => {
        const oldDeviceToken = doc.id;
        const oldApkPath = `${APK_STORAGE_PATH}/${oldDeviceToken}/app.apk`;
        const file = bucket.file(oldApkPath);
        const [exists] = await file.exists();
        if (exists) {
          await file.delete();
          logger.info(`Deleted old APK file for device: ${oldDeviceToken}`);
        }
      });
      await Promise.all(deleteOldFilesPromises);

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

      const uploadUrl = `${UPLOAD_URL}/${deviceToken}`;
      const downloadUrl = `${DOWNLOAD_URL}/${deviceToken}`;

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
 * A-002: Get Signed Upload URL
 *
 * POST /upload/{deviceToken}/url
 *
 * Generates a signed URL for direct upload to Firebase Storage.
 * This allows the build tool to upload APK directly to Storage without going through Functions.
 */
export const getUploadUrl = onRequest(
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
      // Extract device token from path
      const pathParts = req.path.split("/");
      const deviceToken = pathParts[pathParts.length - 2]; // /upload/{token}/url

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

      // Delete old APK if exists
      const bucket = storage.bucket(BUCKET_NAME);
      const oldApkPath = `${APK_STORAGE_PATH}/${deviceToken}/app.apk`;
      const [files] = await bucket.getFiles({prefix: oldApkPath});
      await Promise.all(files.map((file) => file.delete()));
      logger.info(`Deleted old APK files for device: ${deviceToken}`);

      // Generate signed URL for upload (valid for 15 minutes)
      const apkPath = `${APK_STORAGE_PATH}/${deviceToken}/app.apk`;
      const file = bucket.file(apkPath);

      const [signedUrl] = await file.getSignedUrl({
        version: "v4",
        action: "write",
        expires: Date.now() + 15 * 60 * 1000, // 15 minutes
        contentType: "application/vnd.android.package-archive",
      });

      logger.info(`Generated signed upload URL for device: ${deviceToken}`);

      res.status(200).json({
        uploadUrl: signedUrl,
        notifyUrl: `${NOTIFY_URL}/${deviceToken}/notify`,
      });
    } catch (error) {
      logger.error("Error generating upload URL:", error);
      res.status(500).json({
        error: "Internal Server Error",
        message: "Failed to generate upload URL",
      } as ErrorResponse);
    }
  }
);

/**
 * A-003, A-004: Notify Upload Complete and Send Push Notification
 *
 * POST /upload/{deviceToken}/notify
 *
 * Called after APK is uploaded directly to Storage.
 * Sends push notification to the device.
 */
export const notifyUploadComplete = onRequest(
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
      // Extract device token from path
      const pathParts = req.path.split("/");
      const deviceToken = pathParts[pathParts.length - 2]; // /upload/{token}/notify

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

      // Set expiration time on the uploaded file (10 minutes from now)
      const bucket = storage.bucket();
      const apkPath = `${APK_STORAGE_PATH}/${deviceToken}/app.apk`;
      const file = bucket.file(apkPath);

      // Verify file exists
      const [exists] = await file.exists();
      if (!exists) {
        res.status(400).json({
          error: "Bad Request",
          message: "APK file not found. Please upload the file first.",
        } as ErrorResponse);
        return;
      }

      const expirationTime = new Date();
      expirationTime.setMinutes(expirationTime.getMinutes() + APK_EXPIRATION_MINUTES);

      await file.setMetadata({
        metadata: {
          expiresAt: expirationTime.toISOString(),
        },
      });

      // Generate signed download URL (valid for 15 minutes)
      const [downloadUrl] = await file.getSignedUrl({
        version: "v4",
        action: "read",
        expires: Date.now() + 15 * 60 * 1000, // 15 minutes
      });

      logger.info(`Generated signed download URL for device: ${deviceToken}`);

      // Update device document with last upload time
      await db.collection(DEVICES_COLLECTION).doc(deviceToken).update({
        lastApkUploadedAt: Timestamp.now(),
        updatedAt: Timestamp.now(),
      });

      // Send FCM notification with download URL
      const message = {
        token: deviceData.fcmToken,
        notification: {
          title: "新しいAPKを受信しました",
          body: "タップしてインストールします。",
        },
        data: {
          deviceToken,
          action: "apk_ready",
          downloadUrl: downloadUrl, // Include download URL in notification
        },
        android: {
          priority: "high" as const,
        },
      };

      try {
        await messaging.send(message);
        logger.info(`FCM notification sent to device: ${deviceToken} with download URL`);
      } catch (fcmError) {
        logger.error("Failed to send FCM notification:", fcmError);
        // Don't fail the request if notification fails
      }

      const response: UploadResponse = {
        status: "success",
        message: "APK uploaded successfully and notification sent",
        uploadedAt: new Date().toISOString(),
      };

      res.status(200).json(response);
    } catch (error) {
      logger.error("Error in notify function:", error);
      res.status(500).json({
        error: "Internal Server Error",
        message: "Failed to process upload notification",
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
