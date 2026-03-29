package tokyo.isseikuzumaki.vibeterminal.security

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

private const val KEY_PREFIX = "vibe_ssh_"
private const val PREFS_KEY = "ssh_key_aliases"

/**
 * iOS implementation of SshKeyProvider using Keychain for key storage.
 *
 * Private keys are stored in the Keychain as PEM strings.
 * Key metadata (alias, algorithm, createdAt) is persisted in NSUserDefaults.
 *
 * Keychain keys:
 *   "${KEY_PREFIX}${alias}_private" → PEM-encoded private key
 *   "${KEY_PREFIX}${alias}_public"  → OpenSSH public key string
 */
class SshKeyProviderIos : SshKeyProvider {

    override fun listKeys(): List<SshKeyInfoCommon> {
        val defaults = NSUserDefaults.standardUserDefaults
        @Suppress("UNCHECKED_CAST")
        val aliasEntries = (defaults.arrayForKey(PREFS_KEY) as? List<Map<String, Any>>) ?: emptyList()
        return aliasEntries.mapNotNull { entry ->
            val alias = entry["alias"] as? String ?: return@mapNotNull null
            val algorithm = entry["algorithm"] as? String ?: "Unknown"
            val createdAt = (entry["createdAt"] as? Number)?.toLong() ?: 0L
            SshKeyInfoCommon(alias = alias, algorithm = algorithm, createdAt = createdAt)
        }.sortedByDescending { it.createdAt }
    }

    override fun keyExists(alias: String): Boolean {
        return KeychainHelper.load("${KEY_PREFIX}${alias}_private") != null
    }

    /**
     * Store an SSH key pair in the Keychain.
     * @param alias User-friendly name for the key
     * @param privateKeyPem PEM-encoded private key
     * @param publicKeyOpenSsh OpenSSH formatted public key
     * @param algorithm Key algorithm string (e.g. "RSA", "ECDSA")
     */
    fun saveKey(alias: String, privateKeyPem: String, publicKeyOpenSsh: String, algorithm: String) {
        KeychainHelper.save("${KEY_PREFIX}${alias}_private", privateKeyPem)
        KeychainHelper.save("${KEY_PREFIX}${alias}_public", publicKeyOpenSsh)
        registerAlias(alias, algorithm)
    }

    /**
     * Retrieve the PEM-encoded private key for a given alias.
     */
    fun getPrivateKeyPem(alias: String): String? =
        KeychainHelper.load("${KEY_PREFIX}${alias}_private")

    /**
     * Retrieve the OpenSSH public key string for a given alias.
     */
    fun getPublicKeyOpenSsh(alias: String): String? =
        KeychainHelper.load("${KEY_PREFIX}${alias}_public")

    /**
     * Delete a key pair from the Keychain.
     */
    fun deleteKey(alias: String): Boolean {
        val privDeleted = KeychainHelper.delete("${KEY_PREFIX}${alias}_private")
        KeychainHelper.delete("${KEY_PREFIX}${alias}_public")
        unregisterAlias(alias)
        return privDeleted
    }

    // ─── Metadata helpers ────────────────────────────────────────────────────

    private fun registerAlias(alias: String, algorithm: String) {
        val defaults = NSUserDefaults.standardUserDefaults
        @Suppress("UNCHECKED_CAST")
        val current = (defaults.arrayForKey(PREFS_KEY) as? List<Map<String, Any>>)?.toMutableList()
            ?: mutableListOf()

        // Replace if alias already registered
        current.removeAll { (it["alias"] as? String) == alias }
        current.add(
            mapOf(
                "alias" to alias,
                "algorithm" to algorithm,
                "createdAt" to (NSDate().timeIntervalSince1970 * 1000).toLong()
            )
        )
        defaults.setObject(current, PREFS_KEY)
    }

    private fun unregisterAlias(alias: String) {
        val defaults = NSUserDefaults.standardUserDefaults
        @Suppress("UNCHECKED_CAST")
        val current = (defaults.arrayForKey(PREFS_KEY) as? List<Map<String, Any>>)?.toMutableList()
            ?: return
        current.removeAll { (it["alias"] as? String) == alias }
        defaults.setObject(current, PREFS_KEY)
    }
}
