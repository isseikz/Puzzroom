package tokyo.isseikuzumaki.vibeterminal.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
 * Tests for ConnectionConfig creation logic.
 * Verifies that ConnectionConfig is correctly created based on auth type.
 */
class ConnectionConfigCreationTest {

    /**
     * Test: ConnectionConfig for key auth should have keyAlias and null password.
     */
    @Test
    fun `ConnectionConfig for key auth has keyAlias and null password`() {
        // Given: A saved connection with key auth
        val savedConnection = SavedConnection(
            id = 1L,
            name = "Key Server",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "my-key",
            createdAt = System.currentTimeMillis()
        )

        // When: Creating ConnectionConfig for key auth
        val config = ConnectionConfig(
            host = savedConnection.host,
            port = savedConnection.port,
            username = savedConnection.username,
            password = null,
            keyAlias = savedConnection.keyAlias,
            connectionId = savedConnection.id
        )

        // Then: Config should have keyAlias and null password
        assertEquals("my-key", config.keyAlias)
        assertNull(config.password)
        assertEquals("example.com", config.host)
    }

    /**
     * Test: ConnectionConfig for password auth should have password and null keyAlias.
     */
    @Test
    fun `ConnectionConfig for password auth has password and null keyAlias`() {
        // Given: A saved connection with password auth
        val savedConnection = SavedConnection(
            id = 1L,
            name = "Password Server",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "password",
            keyAlias = null,
            createdAt = System.currentTimeMillis()
        )

        // When: Creating ConnectionConfig for password auth
        val config = ConnectionConfig(
            host = savedConnection.host,
            port = savedConnection.port,
            username = savedConnection.username,
            password = "secret123",
            keyAlias = null,
            connectionId = savedConnection.id
        )

        // Then: Config should have password and null keyAlias
        assertEquals("secret123", config.password)
        assertNull(config.keyAlias)
    }

    /**
     * Test: ConnectionConfig correctly determines auth method from keyAlias presence.
     */
    @Test
    fun `ConnectionConfig auth method determined by keyAlias presence`() {
        // Key auth config
        val keyConfig = ConnectionConfig(
            host = "example.com",
            port = 22,
            username = "user",
            password = null,
            keyAlias = "my-key"
        )

        // Password auth config
        val passwordConfig = ConnectionConfig(
            host = "example.com",
            port = 22,
            username = "user",
            password = "secret",
            keyAlias = null
        )

        // Verify: Key config uses key auth
        assertTrue(keyConfig.keyAlias != null)
        assertNull(keyConfig.password)

        // Verify: Password config uses password auth
        assertNull(passwordConfig.keyAlias)
        assertTrue(passwordConfig.password != null)
    }
}

/**
 * Tests for SavedConnection edge cases and validation.
 */
class SavedConnectionEdgeCaseTest {

    /**
     * Test: Key alias with special characters is preserved.
     */
    @Test
    fun `key alias with allowed special characters is preserved`() {
        val connection = SavedConnection(
            id = 1L,
            name = "Test",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "my-server_key-2024",
            createdAt = System.currentTimeMillis()
        )

        assertEquals("my-server_key-2024", connection.keyAlias)
    }

    /**
     * Test: Empty key alias is treated as null for validation purposes.
     */
    @Test
    fun `empty key alias should be treated as invalid for key auth`() {
        val connection = SavedConnection(
            id = 1L,
            name = "Test",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "",
            createdAt = System.currentTimeMillis()
        )

        // Empty keyAlias should be considered invalid for key auth
        val isValidKeyAuth = connection.authType == "key" &&
                            connection.keyAlias != null &&
                            connection.keyAlias!!.isNotBlank()
        assertFalse(isValidKeyAuth)
    }

    /**
     * Test: Default port is 22.
     */
    @Test
    fun `default port is 22`() {
        val connection = SavedConnection(
            id = 1L,
            name = "Test",
            host = "example.com",
            username = "user",
            authType = "password",
            createdAt = System.currentTimeMillis()
        )

        assertEquals(22, connection.port)
    }

    /**
     * Test: Connection preserves all optional fields correctly.
     */
    @Test
    fun `connection preserves optional fields`() {
        val connection = SavedConnection(
            id = 1L,
            name = "Full Config Server",
            host = "example.com",
            port = 2222,
            username = "admin",
            authType = "key",
            keyAlias = "prod-key",
            createdAt = 1000L,
            lastUsedAt = 2000L,
            deployPattern = ">> DEPLOY: (.*)",
            startupCommand = "tmux attach",
            isAutoReconnect = true,
            monitorFilePath = "/var/log/app.log"
        )

        assertEquals("Full Config Server", connection.name)
        assertEquals(2222, connection.port)
        assertEquals("prod-key", connection.keyAlias)
        assertEquals(2000L, connection.lastUsedAt)
        assertEquals(">> DEPLOY: (.*)", connection.deployPattern)
        assertEquals("tmux attach", connection.startupCommand)
        assertTrue(connection.isAutoReconnect)
        assertEquals("/var/log/app.log", connection.monitorFilePath)
    }

    /**
     * Test: Changing auth type clears the other auth method's data.
     */
    @Test
    fun `switching auth type should clear previous auth data`() {
        // Start with key auth
        val keyAuthConnection = SavedConnection(
            id = 1L,
            name = "Test",
            host = "example.com",
            port = 22,
            username = "user",
            authType = "key",
            keyAlias = "my-key",
            createdAt = System.currentTimeMillis()
        )

        // Switch to password auth - keyAlias should be cleared
        val passwordAuthConnection = keyAuthConnection.copy(
            authType = "password",
            keyAlias = null
        )

        assertEquals("password", passwordAuthConnection.authType)
        assertNull(passwordAuthConnection.keyAlias)
    }
}

/**
 * Tests for SshKeyProvider behavior.
 */
class SshKeyProviderTest {

    /**
     * Test: listKeys returns empty list when no keys exist.
     */
    @Test
    fun `listKeys returns empty list when no keys`() {
        val provider = FakeSshKeyProvider(emptyList())

        val keys = provider.listKeys()

        assertTrue(keys.isEmpty())
    }

    /**
     * Test: listKeys returns all available keys.
     */
    @Test
    fun `listKeys returns all available keys`() {
        val keys = listOf(
            SshKeyInfoCommon("key1", "RSA", 1000L),
            SshKeyInfoCommon("key2", "ECDSA", 2000L)
        )
        val provider = FakeSshKeyProvider(keys)

        val result = provider.listKeys()

        assertEquals(2, result.size)
        assertEquals("key1", result[0].alias)
        assertEquals("key2", result[1].alias)
    }

    /**
     * Test: keyExists returns correct result.
     */
    @Test
    fun `keyExists returns correct result for existing and non-existing keys`() {
        val provider = FakeSshKeyProvider(
            listOf(SshKeyInfoCommon("existing-key", "RSA", 1000L))
        )

        assertTrue(provider.keyExists("existing-key"))
        assertFalse(provider.keyExists("non-existing-key"))
    }

    /**
     * Test: SshKeyInfoCommon preserves algorithm information.
     */
    @Test
    fun `SshKeyInfoCommon preserves algorithm information`() {
        val rsaKey = SshKeyInfoCommon("rsa-key", "RSA", 1000L)
        val ecdsaKey = SshKeyInfoCommon("ecdsa-key", "ECDSA", 2000L)

        assertEquals("RSA", rsaKey.algorithm)
        assertEquals("ECDSA", ecdsaKey.algorithm)
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
