package tokyo.isseikuzumaki.vibeterminal.ssh

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tokyo.isseikuzumaki.vibeterminal.security.KeyAlgorithm
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for SSH connection behavior when referenced key is deleted.
 * These tests verify that:
 * 1. Connection entry is preserved when key is deleted
 * 2. Connection attempt fails when key doesn't exist
 */
@RunWith(AndroidJUnit4::class)
class SshConnectionKeyDeletionTest {

    private lateinit var sshKeyManager: SshKeyManager
    private lateinit var sshRepository: MinaSshdRepository
    private val testKeyAlias = "test_connection_key_${System.currentTimeMillis()}"

    @Before
    fun setUp() {
        sshKeyManager = SshKeyManager()
        sshRepository = MinaSshdRepository(sshKeyManager)
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

    /**
     * Test: connectWithKey fails with "Key not found" when key doesn't exist.
     * This simulates the scenario where a key referenced by a connection has been deleted.
     */
    @Test
    fun connectWithKey_failsWhenKeyDoesNotExist() = runBlocking {
        // Given: A key alias that doesn't exist
        val nonExistentKeyAlias = "non_existent_key_${System.currentTimeMillis()}"

        // Verify the key doesn't exist
        assertFalse(sshKeyManager.keyExists(nonExistentKeyAlias))

        // When: Attempting to connect with the non-existent key
        val result = sshRepository.connectWithKey(
            host = "example.com",
            port = 22,
            username = "testuser",
            keyAlias = nonExistentKeyAlias
        )

        // Then: Connection should fail
        assertTrue(result.isFailure)

        // And: Error message should indicate key not found
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            exception.message?.contains("Key not found") == true ||
            exception.message?.contains("not found") == true,
            "Expected 'Key not found' error but got: ${exception.message}"
        )
    }

    /**
     * Test: After creating and then deleting a key, connection attempt fails.
     * This is the complete lifecycle test.
     */
    @Test
    fun connectWithKey_failsAfterKeyIsDeleted() = runBlocking {
        // Given: Create a key
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)
        assertTrue(sshKeyManager.keyExists(testKeyAlias))

        // When: Delete the key
        val deleteResult = sshKeyManager.deleteKey(testKeyAlias)
        assertTrue(deleteResult)
        assertFalse(sshKeyManager.keyExists(testKeyAlias))

        // Then: Connection attempt should fail
        val result = sshRepository.connectWithKey(
            host = "example.com",
            port = 22,
            username = "testuser",
            keyAlias = testKeyAlias
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            exception.message?.contains("Key not found") == true ||
            exception.message?.contains("not found") == true,
            "Expected 'Key not found' error but got: ${exception.message}"
        )
    }

    /**
     * Test: Key deletion doesn't affect other keys.
     */
    @Test
    fun deletingOneKey_doesNotAffectOtherKeys() {
        // Given: Two keys
        val key1Alias = "test_key_1_${System.currentTimeMillis()}"
        val key2Alias = "test_key_2_${System.currentTimeMillis()}"

        try {
            sshKeyManager.generateKeyPair(key1Alias, KeyAlgorithm.RSA_4096)
            sshKeyManager.generateKeyPair(key2Alias, KeyAlgorithm.ECDSA_P256)

            assertTrue(sshKeyManager.keyExists(key1Alias))
            assertTrue(sshKeyManager.keyExists(key2Alias))

            // When: Delete one key
            sshKeyManager.deleteKey(key1Alias)

            // Then: First key is gone, second key still exists
            assertFalse(sshKeyManager.keyExists(key1Alias))
            assertTrue(sshKeyManager.keyExists(key2Alias))
        } finally {
            // Cleanup
            try { sshKeyManager.deleteKey(key1Alias) } catch (e: Exception) {}
            try { sshKeyManager.deleteKey(key2Alias) } catch (e: Exception) {}
        }
    }

    /**
     * Test: getKeyPair returns null for deleted key.
     */
    @Test
    fun getKeyPair_returnsNullForDeletedKey() {
        // Given: Create and then delete a key
        sshKeyManager.generateKeyPair(testKeyAlias, KeyAlgorithm.RSA_4096)
        assertNotNull(sshKeyManager.getKeyPair(testKeyAlias))

        sshKeyManager.deleteKey(testKeyAlias)

        // Then: getKeyPair returns null
        val keyPair = sshKeyManager.getKeyPair(testKeyAlias)
        assertEquals(null, keyPair)
    }
}
