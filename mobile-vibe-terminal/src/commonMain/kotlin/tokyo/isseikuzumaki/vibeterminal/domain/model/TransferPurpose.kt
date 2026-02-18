package tokyo.isseikuzumaki.vibeterminal.domain.model

/**
 * Represents the purpose of a file transfer operation.
 */
enum class TransferPurpose {
    /** Save file to device storage permanently */
    Download,
    /** Prepare file temporarily for sharing via system share sheet */
    Share
}
