/**
 * Firebase Cloud Functions entry point.
 * 
 * This file serves as a bridge between Firebase Functions and the Kotlin/JS compiled code.
 * The actual function implementations are in the Kotlin source files, which are compiled
 * to JavaScript and imported here.
 * 
 * To use this:
 * 1. Run `./gradlew :quick-deploy-app:jsBrowserProductionWebpack` to compile Kotlin to JS
 * 2. The compiled JavaScript will be in build/dist/js/productionExecutable/
 * 3. Run `npm run deploy` to deploy functions to Firebase
 */

import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/https";
import { onDocumentCreated } from "firebase-functions/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

// Initialize Firebase Admin
initializeApp();

/**
 * HTTP Cloud Function that converts text to uppercase.
 * 
 * Usage: https://us-central1-<project-id>.cloudfunctions.net/makeUppercase?text=hello
 */
export const makeUppercase = onRequest(async (req, res) => {
  const text = req.query.text || "";
  const uppercase = text.toString().toUpperCase();
  
  logger.info(`Converting text to uppercase: ${text} -> ${uppercase}`);
  
  res.json({ result: uppercase });
});

/**
 * Firestore-triggered Cloud Function that processes new messages.
 * 
 * Triggered when a new document is created in the 'messages' collection.
 * This function demonstrates using shared data structures between client and server.
 */
export const addMessage = onDocumentCreated("messages/{messageId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    logger.warn("No data associated with the event");
    return;
  }

  const original = snapshot.data().original;
  logger.info(`Processing new message: ${original}`);

  const uppercase = original.toUpperCase();
  const timestamp = Date.now();

  // Store the uppercase version back to Firestore
  const messageId = event.params.messageId;
  await getFirestore()
    .collection("messages")
    .doc(messageId)
    .set({ 
      uppercase: uppercase, 
      timestamp: timestamp 
    }, { merge: true });

  logger.info("Message processed successfully");
});
