package tokyo.isseikuzumaki.vibeterminal.domain.model

/**
 * Represents the status of a file transfer operation.
 */
enum class TransferStatus {
    /** Transfer queued but not started */
    Pending,
    /** Transfer actively in progress */
    InProgress,
    /** Transfer finished successfully */
    Completed,
    /** Transfer failed with error */
    Failed,
    /** Transfer cancelled by user */
    Cancelled
}
