package tokyo.isseikuzumaki.vibeterminal.util

/**
 * Utility object for detecting MIME types from file extensions.
 */
object MimeTypeUtil {
    private val mimeTypes = mapOf(
        "txt" to "text/plain",
        "pdf" to "application/pdf",
        "apk" to "application/vnd.android.package-archive",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "zip" to "application/zip",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar",
        "json" to "application/json",
        "xml" to "application/xml",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "kt" to "text/x-kotlin",
        "java" to "text/x-java-source",
        "py" to "text/x-python",
        "sh" to "application/x-sh",
        "bash" to "application/x-sh",
        "md" to "text/markdown",
        "csv" to "text/csv",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "mp4" to "video/mp4",
        "avi" to "video/x-msvideo",
        "mkv" to "video/x-matroska",
        "mov" to "video/quicktime"
    )

    /**
     * Detect MIME type from file extension.
     * @param filename The filename to detect MIME type for
     * @return The detected MIME type or "application/octet-stream" for unknown types
     */
    fun getMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return mimeTypes[extension] ?: "application/octet-stream"
    }
}
