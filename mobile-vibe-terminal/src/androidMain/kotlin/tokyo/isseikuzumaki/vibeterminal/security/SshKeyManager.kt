package tokyo.isseikuzumaki.vibeterminal.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * SSH key algorithm types supported by the application.
 */
enum class KeyAlgorithm(val displayName: String, val keySize: Int) {
    RSA_4096("RSA 4096-bit", 4096),
    ECDSA_P256("ECDSA P-256", 256)
}

/**
 * Information about a stored SSH key.
 */
data class SshKeyInfo(
    val alias: String,
    val algorithm: String,
    val createdAt: Long
)

/**
 * Manager class for SSH key pair generation and management using Android KeyStore.
 *
 * Provides functionality to:
 * - Generate RSA 4096 and ECDSA P-256 key pairs
 * - Store keys securely in Android KeyStore
 * - Export public keys in OpenSSH authorized_keys format
 * - Retrieve key pairs for SSH authentication
 */
class SshKeyManager {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val KEY_PREFIX = "vibe_ssh_"
        private const val TAG = "SshKeyManager"
    }

    /**
     * Generate a new SSH key pair and store it in Android KeyStore.
     *
     * @param alias User-friendly name for the key
     * @param algorithm The key algorithm to use (RSA_4096 or ECDSA_P256)
     * @return The generated key pair
     * @throws Exception if key generation fails
     */
    fun generateKeyPair(alias: String, algorithm: KeyAlgorithm): KeyPair {
        val fullAlias = "$KEY_PREFIX$alias"
        Timber.d("$TAG: Generating ${algorithm.displayName} key pair with alias: $fullAlias")

        // Delete existing key if present
        if (keyStore.containsAlias(fullAlias)) {
            keyStore.deleteEntry(fullAlias)
        }

        return when (algorithm) {
            KeyAlgorithm.RSA_4096 -> generateRsaKeyPair(fullAlias)
            KeyAlgorithm.ECDSA_P256 -> generateEcdsaKeyPair(fullAlias)
        }
    }

    private fun generateRsaKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(4096)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setUserAuthenticationRequired(false)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    private fun generateEcdsaKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Get a key pair from the KeyStore.
     *
     * @param alias The key alias (without prefix)
     * @return The key pair, or null if not found
     */
    fun getKeyPair(alias: String): KeyPair? {
        val fullAlias = "$KEY_PREFIX$alias"
        return try {
            val privateKey = keyStore.getKey(fullAlias, null) as? PrivateKey
            val publicKey = keyStore.getCertificate(fullAlias)?.publicKey

            if (privateKey != null && publicKey != null) {
                KeyPair(publicKey, privateKey)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to get key pair for alias: $alias")
            null
        }
    }

    /**
     * Get the public key in OpenSSH authorized_keys format.
     *
     * @param alias The key alias (without prefix)
     * @param comment Optional comment to append (e.g., user@device)
     * @return The public key in OpenSSH format, or null if not found
     */
    fun getPublicKeyOpenSSH(alias: String, comment: String = ""): String? {
        val fullAlias = "$KEY_PREFIX$alias"
        return try {
            val publicKey = keyStore.getCertificate(fullAlias)?.publicKey
            when (publicKey) {
                is RSAPublicKey -> rsaToOpenSSH(publicKey, comment)
                is ECPublicKey -> ecdsaToOpenSSH(publicKey, comment)
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to get public key for alias: $alias")
            null
        }
    }

    /**
     * Convert RSA public key to OpenSSH format.
     */
    private fun rsaToOpenSSH(publicKey: RSAPublicKey, comment: String): String {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Write key type
        writeSSHString(dos, "ssh-rsa")

        // Write public exponent
        writeSSHMpint(dos, publicKey.publicExponent)

        // Write modulus
        writeSSHMpint(dos, publicKey.modulus)

        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return if (comment.isNotBlank()) {
            "ssh-rsa $encoded $comment"
        } else {
            "ssh-rsa $encoded"
        }
    }

    /**
     * Convert ECDSA P-256 public key to OpenSSH format.
     */
    private fun ecdsaToOpenSSH(publicKey: ECPublicKey, comment: String): String {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        val keyType = "ecdsa-sha2-nistp256"
        val curveName = "nistp256"

        // Write key type
        writeSSHString(dos, keyType)

        // Write curve name
        writeSSHString(dos, curveName)

        // Write public point (uncompressed format: 0x04 || x || y)
        val point = publicKey.w
        val x = point.affineX.toByteArray().let { trimLeadingZeros(it) }
        val y = point.affineY.toByteArray().let { trimLeadingZeros(it) }

        // Ensure 32 bytes for each coordinate (P-256)
        val xPadded = padTo32Bytes(x)
        val yPadded = padTo32Bytes(y)

        val pointData = ByteArray(1 + 32 + 32)
        pointData[0] = 0x04 // Uncompressed point indicator
        System.arraycopy(xPadded, 0, pointData, 1, 32)
        System.arraycopy(yPadded, 0, pointData, 33, 32)

        writeSSHBytes(dos, pointData)

        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return if (comment.isNotBlank()) {
            "$keyType $encoded $comment"
        } else {
            "$keyType $encoded"
        }
    }

    private fun writeSSHString(dos: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.US_ASCII)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun writeSSHBytes(dos: DataOutputStream, bytes: ByteArray) {
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun writeSSHMpint(dos: DataOutputStream, value: BigInteger) {
        var bytes = value.toByteArray()
        // SSH mpint format requires no unnecessary leading zeros, but needs a leading zero
        // if the high bit is set (to indicate positive number)
        bytes = trimLeadingZeros(bytes)
        if (bytes.isNotEmpty() && (bytes[0].toInt() and 0x80) != 0) {
            // Need to prepend a zero byte
            val padded = ByteArray(bytes.size + 1)
            System.arraycopy(bytes, 0, padded, 1, bytes.size)
            bytes = padded
        }
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun trimLeadingZeros(bytes: ByteArray): ByteArray {
        var start = 0
        while (start < bytes.size - 1 && bytes[start] == 0.toByte() && (bytes[start + 1].toInt() and 0x80) == 0) {
            start++
        }
        return if (start > 0) bytes.copyOfRange(start, bytes.size) else bytes
    }

    private fun padTo32Bytes(bytes: ByteArray): ByteArray {
        return when {
            bytes.size == 32 -> bytes
            bytes.size < 32 -> {
                val padded = ByteArray(32)
                System.arraycopy(bytes, 0, padded, 32 - bytes.size, bytes.size)
                padded
            }
            else -> bytes.copyOfRange(bytes.size - 32, bytes.size)
        }
    }

    /**
     * List all SSH key aliases stored in the KeyStore.
     *
     * @return List of SshKeyInfo objects
     */
    fun listKeys(): List<SshKeyInfo> {
        val keys = mutableListOf<SshKeyInfo>()
        val aliases = keyStore.aliases()

        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            if (alias.startsWith(KEY_PREFIX)) {
                try {
                    val publicKey = keyStore.getCertificate(alias)?.publicKey
                    val algorithm = when (publicKey) {
                        is RSAPublicKey -> "RSA"
                        is ECPublicKey -> "ECDSA"
                        else -> "Unknown"
                    }

                    // Get creation date from certificate
                    val createdAt = keyStore.getCreationDate(alias)?.time ?: 0L

                    keys.add(SshKeyInfo(
                        alias = alias.removePrefix(KEY_PREFIX),
                        algorithm = algorithm,
                        createdAt = createdAt
                    ))
                } catch (e: Exception) {
                    Timber.w(e, "$TAG: Failed to read key info for alias: $alias")
                }
            }
        }

        return keys.sortedByDescending { it.createdAt }
    }

    /**
     * Delete a key from the KeyStore.
     *
     * @param alias The key alias (without prefix)
     * @return true if deletion was successful
     */
    fun deleteKey(alias: String): Boolean {
        val fullAlias = "$KEY_PREFIX$alias"
        return try {
            if (keyStore.containsAlias(fullAlias)) {
                keyStore.deleteEntry(fullAlias)
                Timber.d("$TAG: Deleted key with alias: $alias")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to delete key: $alias")
            false
        }
    }

    /**
     * Check if a key exists in the KeyStore.
     *
     * @param alias The key alias (without prefix)
     * @return true if the key exists
     */
    fun keyExists(alias: String): Boolean {
        val fullAlias = "$KEY_PREFIX$alias"
        return keyStore.containsAlias(fullAlias)
    }

    /**
     * Get the algorithm type for a stored key.
     *
     * @param alias The key alias (without prefix)
     * @return The KeyAlgorithm, or null if not found
     */
    fun getKeyAlgorithm(alias: String): KeyAlgorithm? {
        val fullAlias = "$KEY_PREFIX$alias"
        return try {
            when (keyStore.getCertificate(fullAlias)?.publicKey) {
                is RSAPublicKey -> KeyAlgorithm.RSA_4096
                is ECPublicKey -> KeyAlgorithm.ECDSA_P256
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
