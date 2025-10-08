package tokyo.isseikuzumaki.puzzroom.data

/**
 * データ永続化に関するエラー
 */
sealed class PersistenceError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    // ローカルストレージエラー
    class IOError(cause: Throwable) : PersistenceError("IO error occurred", cause)
    class SerializationError(cause: Throwable) : PersistenceError("Serialization error occurred", cause)
    class NotFoundError(val id: String) : PersistenceError("Project not found: $id")
    class CorruptedDataError(val fileName: String, cause: Throwable? = null) :
        PersistenceError("Corrupted data in file: $fileName", cause)

    // クラウド同期エラー
    class NetworkError(cause: Throwable) : PersistenceError("Network error occurred", cause)
    class SyncError(cause: Throwable) : PersistenceError("Sync error occurred", cause)
    class UnauthorizedError(message: String) : PersistenceError("Unauthorized: $message")
    class QuotaExceededError(val currentSize: Long, val maxSize: Long) :
        PersistenceError("Quota exceeded: $currentSize / $maxSize")
}

typealias PersistenceResult<T> = Result<T>
