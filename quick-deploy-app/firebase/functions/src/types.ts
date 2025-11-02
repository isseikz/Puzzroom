/**
 * Type definitions for Quick Deploy API
 */

/**
 * Device registration request payload
 */
export interface RegisterRequest {
  fcmToken: string;
  deviceInfo: {
    deviceId: string;
    deviceModel?: string;
    osVersion?: string;
    appVersion?: string;
  };
}

/**
 * Device registration response
 */
export interface RegisterResponse {
  deviceToken: string;
  uploadUrl: string;
  downloadUrl: string;
}

/**
 * Device document stored in Firestore
 */
export interface DeviceDocument {
  deviceToken: string;
  fcmToken: string;
  deviceInfo: {
    deviceId: string;
    deviceModel?: string;
    osVersion?: string;
    appVersion?: string;
  };
  createdAt: FirebaseFirestore.Timestamp;
  updatedAt: FirebaseFirestore.Timestamp;
  lastApkUploadedAt?: FirebaseFirestore.Timestamp;
}

/**
 * APK upload response
 */
export interface UploadResponse {
  status: string;
  message: string;
  uploadedAt: string;
}

/**
 * Error response
 */
export interface ErrorResponse {
  error: string;
  message: string;
  code?: string;
}
