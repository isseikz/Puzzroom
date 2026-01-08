package tokyo.isseikuzumaki.vibeterminal.security

/**
 * Information about a stored SSH key.
 * Common data class for use across platforms.
 */
data class SshKeyInfoCommon(
    val alias: String,
    val algorithm: String,
    val createdAt: Long
)

/**
 * Interface for providing SSH key information.
 * Used for dependency injection in commonMain.
 */
interface SshKeyProvider {
    /**
     * List all available SSH key aliases.
     * @return List of key info objects
     */
    fun listKeys(): List<SshKeyInfoCommon>

    /**
     * Check if a key exists.
     * @param alias The key alias
     * @return true if the key exists
     */
    fun keyExists(alias: String): Boolean
}
