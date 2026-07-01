package tokyo.isseikuzumaki.vibeterminal.domain.model

/**
 * Represents the state of an active file transfer operation.
 *
 * @param fileEntry The file being transferred
 * @param progress Transfer progress from 0.0 to 1.0
 * @param bytesTransferred Bytes transferred so far
 * @param status Current transfer status
 * @param purpose Whether this is a download or share operation
 * @param localPath Local file path after successful completion
 * @param error Error message if transfer failed
 * @param startedAt Timestamp when transfer started (milliseconds since epoch)
 */
data class FileTransferState(
    val fileEntry: FileEntry,
    val progress: Float = 0f,
    val bytesTransferred: Long = 0L,
    val status: TransferStatus = TransferStatus.Pending,
    val purpose: TransferPurpose,
    val localPath: String? = null,
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis()
) {
    init {
        require(progress in 0f..1f) { "Progress must be between 0.0 and 1.0" }
        require(bytesTransferred >= 0) { "bytesTransferred must be non-negative" }
    }

    /**
     * Check if the transfer is currently active (pending or in progress).
     */
    val isActive: Boolean
        get() = status == TransferStatus.Pending || status == TransferStatus.InProgress

    /**
     * Check if the transfer has finished (completed, failed, or cancelled).
     */
    val isFinished: Boolean
        get() = status == TransferStatus.Completed ||
                status == TransferStatus.Failed ||
                status == TransferStatus.Cancelled
}
