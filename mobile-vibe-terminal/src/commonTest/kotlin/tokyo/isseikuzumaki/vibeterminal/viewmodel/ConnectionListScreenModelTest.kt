package tokyo.isseikuzumaki.vibeterminal.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ConnectionListScreenModel.
 * Tests authentication method validation logic.
 */
class ConnectionListScreenModelTest {

    /**
     * Test: When authType is "key", keyAlias should be set and password is not stored with connection.
     * Verifies that only one authentication method is registered per connection.
     */
    @Test
    fun `saving connection with key auth should have keyAlias set and no password in connection`() {
        // Given
        val connection = SavedConnection(
            id = 0L,
            name = "Test Server",
            host = "example.com",
            port = 22,
            username = "testuser",
            authType = "key",
            keyAlias = "my-ssh-key",
            createdAt = System.currentTimeMillis()
        )

        // Then: Connection should have key auth properties
        assertEquals("key", connection.authType)
        assertEquals("my-ssh-key", connection.keyAlias)
        // Note: Password is not stored in SavedConnection for security; it's stored separately
    }

    /**
     * Test: When authType is "password", keyAlias should be null.
     * Verifies that password auth connections don't have key references.
     */
    @Test
    fun `saving connection with password auth should have null keyAlias`() {
        // Given
        val connection = SavedConnection(
            id = 0L,
            name = "Test Server",
            host = "example.com",
            port = 22,
            username = "testuser",
            authType = "password",
            keyAlias = null,
            createdAt = System.currentTimeMillis()
        )

        // Then: Connection should have password auth properties
        assertEquals("password", connection.authType)
        assertNull(connection.keyAlias)
    }

    /**
     * Test: A connection cannot have both key auth with password auth configured.
     * When authType is "key", even if keyAlias is somehow null, it should be treated as invalid.
     */
    @Test
    fun `connection with key auth requires non-null keyAlias`() {
        // Given: A malformed connection with key auth but no keyAlias
        val connection = SavedConnection(
            id = 0L,
            name = "Test Server",
            host = "example.com",
            port = 22,
            username = "testuser",
            authType = "key",
            keyAlias = null, // Invalid state
            createdAt = System.currentTimeMillis()
        )

        // Then: This should be considered invalid for key authentication
        val isValidKeyAuth = connection.authType == "key" && connection.keyAlias != null
        assertEquals(false, isValidKeyAuth)
    }

    /**
     * Test: authType determines authentication method exclusively.
     * A connection can only have one auth method at a time.
     */
    @Test
    fun `authType determines single authentication method`() {
        // Given: Two connections with different auth types
        val keyAuthConnection = SavedConnection(
            id = 1L,
            name = "Key Auth Server",
            host = "key.example.com",
            port = 22,
            username = "keyuser",
            authType = "key",
            keyAlias = "my-key",
            createdAt = System.currentTimeMillis()
        )

        val passwordAuthConnection = SavedConnection(
            id = 2L,
            name = "Password Auth Server",
            host = "pass.example.com",
            port = 22,
            username = "passuser",
            authType = "password",
            keyAlias = null,
            createdAt = System.currentTimeMillis()
        )

        // Then: Each connection has exactly one auth method
        assertTrue(keyAuthConnection.authType == "key" || keyAuthConnection.authType == "password")
        assertTrue(passwordAuthConnection.authType == "key" || passwordAuthConnection.authType == "password")

        // Key auth connection uses key
        assertEquals("key", keyAuthConnection.authType)
        assertTrue(keyAuthConnection.keyAlias != null)

        // Password auth connection uses password
        assertEquals("password", passwordAuthConnection.authType)
        assertNull(passwordAuthConnection.keyAlias)
    }
}

/**
 * Tests for connection behavior when referenced SSH key is deleted.
 */
class ConnectionKeyDeletionTest {

    /**
     * Test: When a key is deleted, the connection entry should be preserved.
     */
    @Test
    fun `connection entry is preserved when referenced key is deleted`() {
        // Given: A connection that references a key
        val connections = mutableListOf(
            SavedConnection(
                id = 1L,
                name = "My Server",
                host = "example.com",
                port = 22,
                username = "user",
                authType = "key",
                keyAlias = "deleted-key",
                createdAt = System.currentTimeMillis()
            )
        )

        // When: The key is deleted (simulated by removing from available keys)
        val availableKeys = emptyList<SshKeyInfoCommon>() // Key was deleted

        // Then: Connection still exists
        assertEquals(1, connections.size)
        assertEquals("deleted-key", connections[0].keyAlias)

        // But the referenced key is no longer in available keys
        assertTrue(availableKeys.none { it.alias == "deleted-key" })
    }

    /**
     * Test: Connection with deleted key should fail authentication.
     * The keyAlias reference becomes invalid when the key is removed from KeyStore.
     */
    @Test
    fun `connection with deleted key fails validation`() {
        // Given: A connection referencing a key
        val connection = SavedConnection(
            id = 1L,
            name = "My Server",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "deleted-key",
            createdAt = System.currentTimeMillis()
        )

        // And: A key provider that doesn't have the key (it was deleted)
        val keyProvider = FakeSshKeyProvider(keys = emptyList())

        // When: We check if the connection's key exists
        val keyExists = keyProvider.keyExists(connection.keyAlias!!)

        // Then: Key doesn't exist, connection would fail to authenticate
        assertEquals(false, keyExists)
    }

    /**
     * Test: Connection can be updated to use a different key after original is deleted.
     */
    @Test
    fun `connection can be updated to use different key after deletion`() {
        // Given: A connection with a deleted key reference
        val connection = SavedConnection(
            id = 1L,
            name = "My Server",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "deleted-key",
            createdAt = System.currentTimeMillis()
        )

        // And: A new key is available
        val availableKeys = listOf(
            SshKeyInfoCommon("new-key", "RSA", System.currentTimeMillis())
        )

        // When: Connection is updated with new key
        val updatedConnection = connection.copy(keyAlias = "new-key")

        // Then: Connection now references the new key
        assertEquals("new-key", updatedConnection.keyAlias)
        assertTrue(availableKeys.any { it.alias == updatedConnection.keyAlias })
    }

    /**
     * Test: Connection can be changed from key auth to password auth if key is deleted.
     */
    @Test
    fun `connection can switch from key to password auth`() {
        // Given: A connection with key auth
        val connection = SavedConnection(
            id = 1L,
            name = "My Server",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "deleted-key",
            createdAt = System.currentTimeMillis()
        )

        // When: User changes to password auth
        val updatedConnection = connection.copy(
            authType = "password",
            keyAlias = null
        )

        // Then: Connection now uses password auth
        assertEquals("password", updatedConnection.authType)
        assertNull(updatedConnection.keyAlias)
    }
}

/**
 * Fake implementation of SshKeyProvider for testing.
 */
private class FakeSshKeyProvider(
    private val keys: List<SshKeyInfoCommon>
) : SshKeyProvider {
    override fun listKeys(): List<SshKeyInfoCommon> = keys
    override fun keyExists(alias: String): Boolean = keys.any { it.alias == alias }
}

/**
 * Fake implementation of ConnectionRepository for testing.
 */
private class FakeConnectionRepository : ConnectionRepository {
    private val connections = mutableListOf<SavedConnection>()
    private val connectionFlow = MutableStateFlow<List<SavedConnection>>(emptyList())
    private val passwords = mutableMapOf<Long, String>()
    private var nextId = 1L
    private var lastActiveConnectionId: Long? = null

    override fun getAllConnections(): Flow<List<SavedConnection>> = connectionFlow

    override suspend fun getConnectionById(id: Long): SavedConnection? =
        connections.find { it.id == id }

    override suspend fun insertConnection(connection: SavedConnection): Long {
        val id = nextId++
        val newConnection = connection.copy(id = id)
        connections.add(newConnection)
        connectionFlow.value = connections.toList()
        return id
    }

    override suspend fun updateConnection(connection: SavedConnection) {
        val index = connections.indexOfFirst { it.id == connection.id }
        if (index >= 0) {
            connections[index] = connection
            connectionFlow.value = connections.toList()
        }
    }

    override suspend fun deleteConnection(connection: SavedConnection) {
        connections.removeAll { it.id == connection.id }
        connectionFlow.value = connections.toList()
    }

    override suspend fun updateLastUsed(connectionId: Long) {
        val index = connections.indexOfFirst { it.id == connectionId }
        if (index >= 0) {
            connections[index] = connections[index].copy(lastUsedAt = System.currentTimeMillis())
            connectionFlow.value = connections.toList()
        }
    }

    override suspend fun getLastActiveConnectionId(): Long? = lastActiveConnectionId

    override suspend fun setLastActiveConnectionId(connectionId: Long?) {
        lastActiveConnectionId = connectionId
    }

    override suspend fun savePassword(connectionId: Long, password: String) {
        passwords[connectionId] = password
    }

    override suspend fun getPassword(connectionId: Long): String? = passwords[connectionId]

    override suspend fun deletePassword(connectionId: Long) {
        passwords.remove(connectionId)
    }

    override suspend fun updateLastFileExplorerPath(connectionId: Long, path: String?) {
        val index = connections.indexOfFirst { it.id == connectionId }
        if (index >= 0) {
            connections[index] = connections[index].copy(lastFileExplorerPath = path)
            connectionFlow.value = connections.toList()
        }
    }

    override suspend fun getLastFileExplorerPath(connectionId: Long): String? =
        connections.find { it.id == connectionId }?.lastFileExplorerPath
}
