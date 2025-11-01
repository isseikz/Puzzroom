@file:JsModule("firebase-admin/firestore")
@file:JsNonModule

package tokyo.isseikuzumaki.quickdeploy.functions

/**
 * External declarations for Firebase Admin Firestore SDK.
 */

external fun getFirestore(): Firestore

external interface Firestore {
    fun collection(path: String): CollectionReference
}

external interface CollectionReference {
    fun doc(id: String): DocumentReference
}

external interface DocumentReference {
    fun set(data: dynamic): dynamic
    fun get(): dynamic
}
