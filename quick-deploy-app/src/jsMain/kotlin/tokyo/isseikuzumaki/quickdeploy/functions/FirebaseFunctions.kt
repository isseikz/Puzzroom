@file:JsModule("firebase-functions")
@file:JsNonModule

package tokyo.isseikuzumaki.quickdeploy.functions

/**
 * External declarations for Firebase Functions SDK.
 * These map to the Firebase Functions Node.js SDK.
 */

external object logger {
    fun log(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

external interface HttpsRequest

external interface HttpsResponse {
    fun send(body: dynamic)
    fun json(obj: dynamic)
    fun status(code: Int): HttpsResponse
}

external object https {
    fun onRequest(handler: (HttpsRequest, HttpsResponse) -> Unit): dynamic
}

external interface DocumentSnapshot {
    val data: dynamic
    fun get(field: String): dynamic
}

external interface QueryDocumentSnapshot : DocumentSnapshot

external interface Change<T> {
    val before: T
    val after: T
}

external object firestore {
    fun onDocumentCreated(path: String, handler: (event: dynamic) -> Unit): dynamic
    fun onDocumentWritten(path: String, handler: (event: dynamic) -> Unit): dynamic
}
