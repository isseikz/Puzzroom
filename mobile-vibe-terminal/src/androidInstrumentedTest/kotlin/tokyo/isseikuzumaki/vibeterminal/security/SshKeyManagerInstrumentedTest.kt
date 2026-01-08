package tokyo.isseikuzumaki.vibeterminal.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for SshKeyManager.
 * These tests require a real Android device or emulator because
 * Android KeyStore is not available in unit test environments.
 */
@RunWith(AndroidJUnit4::class)
class SshKeyManagerInstrumentedTest {

    private lateinit var sshKeyManager: SshKeyManager
    private val testKeyAlias = "test_key_${System.currentTimeMillis()}"

    @Before
    fun setUp() {
        sshKeyManager = SshKeyManager()
    }

    @After
    fun tearDown() {
        // Clean up test keys
        try {
            sshKeyManager.deleteKey(testKeyAlias)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun generateKeyPair_createsRsaKeyPair() {
        val keyPair = sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        assertNotNull(keyPair)
        assertNotNull(keyPair.public)
        assertNotNull(keyPair.private)
        assertEquals("RSA", keyPair.public.algorithm)
    }

    @Test
    fun generateKeyPair_createsEcdsaKeyPair() {
        val keyPair = sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.ECDSA_P256)

        assertNotNull(keyPair)
        assertNotNull(keyPair.public)
        assertNotNull(keyPair.private)
        assertEquals("EC", keyPair.public.algorithm)
    }

    @Test
    fun getKeyPair_returnsStoredKeyPair() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        val retrievedKeyPair = sshKeyManager.getKeyPair(testKeyAlias)

        assertNotNull(retrievedKeyPair)
        assertNotNull(retrievedKeyPair.public)
        assertNotNull(retrievedKeyPair.private)
    }

    @Test
    fun getKeyPair_returnsNullForNonExistentKey() {
        val keyPair = sshKeyManager.getKeyPair("non_existent_key")

        assertNull(keyPair)
    }

    @Test
    fun keyExists_returnsTrueForExistingKey() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        assertTrue(sshKeyManager.keyExists(testKeyAlias))
    }

    @Test
    fun keyExists_returnsFalseForNonExistentKey() {
        assertFalse(sshKeyManager.keyExists("non_existent_key"))
    }

    @Test
    fun deleteKey_removesKeyFromKeyStore() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)
        assertTrue(sshKeyManager.keyExists(testKeyAlias))

        val result = sshKeyManager.deleteKey(testKeyAlias)

        assertTrue(result)
        assertFalse(sshKeyManager.keyExists(testKeyAlias))
    }

    @Test
    fun deleteKey_returnsFalseForNonExistentKey() {
        val result = sshKeyManager.deleteKey("non_existent_key")

        assertFalse(result)
    }

    @Test
    fun getPublicKeyOpenSSH_returnsValidFormatForRsaKey() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        val publicKey = sshKeyManager.getPublicKeyOpenSSH(testKeyAlias, "test-comment")

        assertNotNull(publicKey)
        assertTrue(publicKey.startsWith("ssh-rsa "))
        assertTrue(publicKey.endsWith(" test-comment"))
    }

    @Test
    fun getPublicKeyOpenSSH_returnsValidFormatForEcdsaKey() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.ECDSA_P256)

        val publicKey = sshKeyManager.getPublicKeyOpenSSH(testKeyAlias, "test-comment")

        assertNotNull(publicKey)
        assertTrue(publicKey.startsWith("ecdsa-sha2-nistp256 "))
        assertTrue(publicKey.endsWith(" test-comment"))
    }

    @Test
    fun getPublicKeyOpenSSH_returnsNullForNonExistentKey() {
        val publicKey = sshKeyManager.getPublicKeyOpenSSH("non_existent_key")

        assertNull(publicKey)
    }

    @Test
    fun listKeys_returnsCreatedKeys() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        val keys = sshKeyManager.listKeys()

        assertTrue(keys.any { it.alias == testKeyAlias })
    }

    @Test
    fun listKeys_returnsCorrectAlgorithmInfo() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        val keys = sshKeyManager.listKeys()
        val testKey = keys.find { it.alias == testKeyAlias }

        assertNotNull(testKey)
        assertEquals("RSA", testKey.algorithm)
    }

    @Test
    fun getKeyAlgorithm_returnsCorrectAlgorithmForRsa() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)

        val algorithm = sshKeyManager.getKeyAlgorithm(testKeyAlias)

        assertEquals(KeyAlgorithm.RSA_4096, algorithm)
    }

    @Test
    fun getKeyAlgorithm_returnsCorrectAlgorithmForEcdsa() {
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.ECDSA_P256)

        val algorithm = sshKeyManager.getKeyAlgorithm(testKeyAlias)

        assertEquals(KeyAlgorithm.ECDSA_P256, algorithm)
    }

    @Test
    fun getKeyAlgorithm_returnsNullForNonExistentKey() {
        val algorithm = sshKeyManager.getKeyAlgorithm("non_existent_key")

        assertNull(algorithm)
    }

    @Test
    fun generateKeyPair_throwsExceptionForExistingAlias() {
        // 最初に鍵を生成
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)
        val originalPublicKey = sshKeyManager.getPublicKeyOpenSSH(testKeyAlias)

        // 同じエイリアスで再度生成しようとすると例外が発生することを確認
        assertFailsWith<IllegalArgumentException> {
            sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.ECDSA_P256)
        }

        // 元の鍵が保持されていることを確認
        assertTrue(sshKeyManager.keyExists(testKeyAlias))
        assertEquals(KeyAlgorithm.RSA_4096, sshKeyManager.getKeyAlgorithm(testKeyAlias))

        // 公開鍵が変更されていないことを確認
        val currentPublicKey = sshKeyManager.getPublicKeyOpenSSH(testKeyAlias)
        assertEquals(originalPublicKey, currentPublicKey)
    }
}
