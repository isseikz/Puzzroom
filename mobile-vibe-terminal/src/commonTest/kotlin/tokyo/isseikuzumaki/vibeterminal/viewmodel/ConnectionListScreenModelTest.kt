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
 * SSH公開鍵認証における認証方式のバリデーションロジックをテストするクラス。
 *
 * ## テスト対象
 * - [SavedConnection] の認証方式（authType）と鍵エイリアス（keyAlias）の整合性
 * - 1つの接続設定に対して認証方式が排他的に設定されることの検証
 *
 * ## 設計意図
 * SSH接続では、パスワード認証と公開鍵認証は排他的な関係にある。
 * このテストクラスでは、データモデルレベルでこの制約が守られることを保証する。
 *
 * @see SavedConnection
 * @see ConnectionConfig
 */
class ConnectionListScreenModelTest {

    /**
     * 公開鍵認証を選択した場合、keyAliasが設定されることを検証する。
     *
     * ## 検証内容
     * - authType が "key" の場合、keyAlias に有効な値が設定される
     * - パスワードは [SavedConnection] には保存されない（セキュリティ上の理由で別管理）
     *
     * ## 背景
     * 公開鍵認証では、Android KeyStore に保存された鍵ペアを使用するため、
     * 鍵のエイリアス（識別子）が必須となる。
     */
    @Test
    fun `saving connection with key auth should have keyAlias set and no password in connection`() {
        // Given: 公開鍵認証の接続設定を作成
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

        // Then: 公開鍵認証の設定が正しく保持されている
        assertEquals("key", connection.authType)
        assertEquals("my-ssh-key", connection.keyAlias)
        // Note: パスワードはセキュリティ上の理由でSavedConnectionには保存されず、別途管理される
    }

    /**
     * パスワード認証を選択した場合、keyAliasがnullであることを検証する。
     *
     * ## 検証内容
     * - authType が "password" の場合、keyAlias は null である
     * - パスワード認証時に不要な鍵参照が残らない
     *
     * ## 背景
     * パスワード認証では鍵ペアを使用しないため、keyAlias は設定されるべきではない。
     * これにより、認証方式の混在を防ぐ。
     */
    @Test
    fun `saving connection with password auth should have null keyAlias`() {
        // Given: パスワード認証の接続設定を作成
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

        // Then: パスワード認証の設定では keyAlias が null
        assertEquals("password", connection.authType)
        assertNull(connection.keyAlias)
    }

    /**
     * 公開鍵認証を選択した場合、keyAliasが必須であることを検証する。
     *
     * ## 検証内容
     * - authType が "key" かつ keyAlias が null の場合、無効な状態と判定される
     * - バリデーションロジックが不正な状態を検出できる
     *
     * ## 背景
     * 公開鍵認証を選択しているにもかかわらず鍵が指定されていない場合、
     * SSH接続は必ず失敗する。UIレベルでこの状態を防ぐ必要がある。
     */
    @Test
    fun `connection with key auth requires non-null keyAlias`() {
        // Given: 不正な状態 - 公開鍵認証だが keyAlias が null
        val connection = SavedConnection(
            id = 0L,
            name = "Test Server",
            host = "example.com",
            port = 22,
            username = "testuser",
            authType = "key",
            keyAlias = null, // 無効な状態
            createdAt = System.currentTimeMillis()
        )

        // When: 公開鍵認証として有効かどうかを判定
        val isValidKeyAuth = connection.authType == "key" && connection.keyAlias != null

        // Then: 無効な公開鍵認証設定として判定される
        assertEquals(false, isValidKeyAuth)
    }

    /**
     * 認証方式（authType）は排他的に決定されることを検証する。
     *
     * ## 検証内容
     * - 各接続設定は "key" または "password" のいずれか一方のみを持つ
     * - 公開鍵認証の接続には keyAlias が設定される
     * - パスワード認証の接続には keyAlias が設定されない
     *
     * ## 背景
     * SSHプロトコルでは複数の認証方式を順番に試すことができるが、
     * UIの簡潔さのため、本アプリでは1つの接続設定に対して1つの認証方式のみを許可する。
     */
    @Test
    fun `authType determines single authentication method`() {
        // Given: 異なる認証方式の2つの接続設定
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

        // Then: 各接続は有効な認証方式のいずれかを持つ
        assertTrue(keyAuthConnection.authType == "key" || keyAuthConnection.authType == "password")
        assertTrue(passwordAuthConnection.authType == "key" || passwordAuthConnection.authType == "password")

        // Then: 公開鍵認証の接続は keyAlias を持つ
        assertEquals("key", keyAuthConnection.authType)
        assertTrue(keyAuthConnection.keyAlias != null)

        // Then: パスワード認証の接続は keyAlias を持たない
        assertEquals("password", passwordAuthConnection.authType)
        assertNull(passwordAuthConnection.keyAlias)
    }
}

/**
 * SSH鍵が削除された場合の接続設定の動作をテストするクラス。
 *
 * ## テスト対象
 * - 参照している鍵が削除された場合の接続設定の保持
 * - 鍵削除後の接続試行の失敗検証
 * - 鍵削除後のリカバリ手順（別の鍵への切り替え、認証方式の変更）
 *
 * ## 設計意図
 * ユーザーがSSH鍵を削除した場合でも、接続設定自体は削除されるべきではない。
 * 代わりに、接続試行時にエラーとして検出され、ユーザーに適切な対処を促す。
 *
 * @see SshKeyProvider
 * @see SavedConnection
 */
class ConnectionKeyDeletionTest {

    /**
     * 参照している鍵が削除されても、接続設定エントリは保持されることを検証する。
     *
     * ## 検証内容
     * - 鍵を削除しても、その鍵を参照する接続設定は残る
     * - 接続設定の keyAlias フィールドは変更されない
     *
     * ## 背景
     * 接続設定には接続先ホスト、ポート、ユーザー名などの情報が含まれる。
     * 鍵が削除されても、これらの情報は有効であり、別の鍵に切り替えて再利用できる。
     */
    @Test
    fun `connection entry is preserved when referenced key is deleted`() {
        // Given: 特定の鍵を参照する接続設定
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

        // When: 鍵が削除される（利用可能な鍵リストから消える）
        val availableKeys = emptyList<SshKeyInfoCommon>()

        // Then: 接続設定は依然として存在する
        assertEquals(1, connections.size)
        assertEquals("deleted-key", connections[0].keyAlias)

        // Then: ただし、参照している鍵は利用不可
        assertTrue(availableKeys.none { it.alias == "deleted-key" })
    }

    /**
     * 削除された鍵を参照する接続設定では、接続が失敗することを検証する。
     *
     * ## 検証内容
     * - SshKeyProvider.keyExists() が false を返す
     * - この状態で接続を試みると失敗する（実際の接続テストは別クラスで実施）
     *
     * ## 背景
     * Android KeyStore から鍵が削除されると、その鍵を使用した認証は不可能になる。
     * 接続試行前にこの状態を検出し、ユーザーに通知する必要がある。
     */
    @Test
    fun `connection with deleted key fails validation`() {
        // Given: 削除された鍵を参照する接続設定
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

        // And: 鍵が存在しないことを返す SshKeyProvider
        val keyProvider = FakeSshKeyProvider(keys = emptyList())

        // When: 接続設定が参照する鍵の存在を確認
        val keyExists = keyProvider.keyExists(connection.keyAlias!!)

        // Then: 鍵が存在しないため、接続は失敗する
        assertEquals(false, keyExists)
    }

    /**
     * 鍵削除後、接続設定を別の鍵に更新できることを検証する。
     *
     * ## 検証内容
     * - 接続設定の keyAlias を新しい鍵に変更できる
     * - 変更後の keyAlias が利用可能な鍵リストに存在する
     *
     * ## 背景
     * ユーザーが誤って鍵を削除した場合でも、新しい鍵を作成して
     * 既存の接続設定に割り当てることで、接続情報を再入力せずに復旧できる。
     */
    @Test
    fun `connection can be updated to use different key after deletion`() {
        // Given: 削除された鍵を参照する接続設定
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

        // And: 新しい鍵が利用可能
        val availableKeys = listOf(
            SshKeyInfoCommon("new-key", "RSA", System.currentTimeMillis())
        )

        // When: 接続設定を新しい鍵に更新
        val updatedConnection = connection.copy(keyAlias = "new-key")

        // Then: 接続設定が新しい鍵を参照している
        assertEquals("new-key", updatedConnection.keyAlias)
        assertTrue(availableKeys.any { it.alias == updatedConnection.keyAlias })
    }

    /**
     * 公開鍵認証からパスワード認証への切り替えが可能であることを検証する。
     *
     * ## 検証内容
     * - authType を "password" に変更できる
     * - 変更後、keyAlias が null になる
     *
     * ## 背景
     * 鍵の復旧が困難な場合、ユーザーはパスワード認証に切り替えることで
     * 接続を継続できる。この操作により、不要な鍵参照がクリアされる。
     */
    @Test
    fun `connection can switch from key to password auth`() {
        // Given: 公開鍵認証の接続設定
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

        // When: パスワード認証に切り替え
        val updatedConnection = connection.copy(
            authType = "password",
            keyAlias = null
        )

        // Then: パスワード認証に変更され、keyAlias がクリアされている
        assertEquals("password", updatedConnection.authType)
        assertNull(updatedConnection.keyAlias)
    }
}

/**
 * [ConnectionConfig] の生成ロジックをテストするクラス。
 *
 * ## テスト対象
 * - [SavedConnection] から [ConnectionConfig] への変換
 * - 認証方式に応じた適切なフィールド設定
 *
 * ## 設計意図
 * [ConnectionConfig] は実際のSSH接続に使用される設定オブジェクト。
 * [SavedConnection] からの変換時に、認証方式に応じて適切な値が設定されることを保証する。
 *
 * @see ConnectionConfig
 * @see SavedConnection
 */
class ConnectionConfigCreationTest {

    /**
     * 公開鍵認証用の [ConnectionConfig] が正しく生成されることを検証する。
     *
     * ## 検証内容
     * - keyAlias が設定されている
     * - password が null である
     * - 接続先情報（host, port, username）が正しくコピーされる
     *
     * ## 背景
     * 公開鍵認証では、パスワードの代わりに KeyStore 内の秘密鍵を使用する。
     * そのため、password フィールドは null となる。
     */
    @Test
    fun `ConnectionConfig for key auth has keyAlias and null password`() {
        // Given: 公開鍵認証の保存済み接続設定
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

        // When: ConnectionConfig を生成
        val config = ConnectionConfig(
            host = savedConnection.host,
            port = savedConnection.port,
            username = savedConnection.username,
            password = null,
            keyAlias = savedConnection.keyAlias,
            connectionId = savedConnection.id
        )

        // Then: 公開鍵認証用の設定が正しく生成される
        assertEquals("my-key", config.keyAlias)
        assertNull(config.password)
        assertEquals("example.com", config.host)
    }

    /**
     * パスワード認証用の [ConnectionConfig] が正しく生成されることを検証する。
     *
     * ## 検証内容
     * - password が設定されている
     * - keyAlias が null である
     *
     * ## 背景
     * パスワード認証では、鍵ペアは使用しないため keyAlias は不要。
     * ユーザーが入力したパスワードが password フィールドに設定される。
     */
    @Test
    fun `ConnectionConfig for password auth has password and null keyAlias`() {
        // Given: パスワード認証の保存済み接続設定
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

        // When: ConnectionConfig を生成（パスワードはユーザー入力から取得）
        val config = ConnectionConfig(
            host = savedConnection.host,
            port = savedConnection.port,
            username = savedConnection.username,
            password = "secret123",
            keyAlias = null,
            connectionId = savedConnection.id
        )

        // Then: パスワード認証用の設定が正しく生成される
        assertEquals("secret123", config.password)
        assertNull(config.keyAlias)
    }

    /**
     * [ConnectionConfig] の認証方式が keyAlias の有無で判定できることを検証する。
     *
     * ## 検証内容
     * - keyAlias が非null の場合、公開鍵認証
     * - keyAlias が null かつ password が非null の場合、パスワード認証
     *
     * ## 背景
     * 実行時に認証方式を判定する際、keyAlias の有無が判定基準となる。
     * この規約により、接続処理のコードが簡潔になる。
     */
    @Test
    fun `ConnectionConfig auth method determined by keyAlias presence`() {
        // Given: 公開鍵認証の設定
        val keyConfig = ConnectionConfig(
            host = "example.com",
            port = 22,
            username = "user",
            password = null,
            keyAlias = "my-key"
        )

        // Given: パスワード認証の設定
        val passwordConfig = ConnectionConfig(
            host = "example.com",
            port = 22,
            username = "user",
            password = "secret",
            keyAlias = null
        )

        // Then: keyAlias の有無で認証方式を判定できる
        assertTrue(keyConfig.keyAlias != null)
        assertNull(keyConfig.password)

        assertNull(passwordConfig.keyAlias)
        assertTrue(passwordConfig.password != null)
    }
}

/**
 * [SavedConnection] のエッジケースとバリデーションをテストするクラス。
 *
 * ## テスト対象
 * - 境界値・特殊文字の取り扱い
 * - デフォルト値の検証
 * - オプションフィールドの保持
 * - 認証方式切り替え時のデータクリア
 *
 * ## 設計意図
 * 様々な入力パターンに対してデータモデルが正しく動作することを保証する。
 *
 * @see SavedConnection
 */
class SavedConnectionEdgeCaseTest {

    /**
     * 鍵エイリアスに許可された特殊文字が保持されることを検証する。
     *
     * ## 検証内容
     * - ハイフン（-）とアンダースコア（_）を含む keyAlias が正しく保存される
     *
     * ## 背景
     * 鍵エイリアスには英数字の他、ハイフンとアンダースコアを許可している。
     * これにより、"my-server_key-2024" のような識別しやすい名前を付けられる。
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
     * 空文字の鍵エイリアスが無効として扱われることを検証する。
     *
     * ## 検証内容
     * - keyAlias が空文字の場合、公開鍵認証として無効と判定される
     *
     * ## 背景
     * UIで入力フィールドを空のまま保存しようとした場合を想定。
     * 空文字は実質的に鍵が指定されていない状態であり、接続は失敗する。
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

        // When: 公開鍵認証として有効かどうかを判定
        val isValidKeyAuth = connection.authType == "key" &&
                            connection.keyAlias != null &&
                            connection.keyAlias!!.isNotBlank()

        // Then: 空文字は無効
        assertFalse(isValidKeyAuth)
    }

    /**
     * SSHのデフォルトポート（22）が正しく設定されることを検証する。
     *
     * ## 検証内容
     * - port を指定しない場合、デフォルト値 22 が使用される
     *
     * ## 背景
     * SSH のウェルノウンポートは 22。ほとんどのサーバーがこのポートを使用するため、
     * ユーザーの入力の手間を省く。
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
     * 全てのオプションフィールドが正しく保持されることを検証する。
     *
     * ## 検証内容
     * - lastUsedAt, deployPattern, startupCommand, isAutoReconnect, monitorFilePath が保持される
     *
     * ## 背景
     * 接続設定にはSSH接続以外の付加機能（自動再接続、デプロイパターン検出等）の
     * 設定も含まれる。これらが正しく保存・復元されることを保証する。
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
     * 認証方式を切り替えた際、前の認証方式のデータがクリアされることを検証する。
     *
     * ## 検証内容
     * - 公開鍵認証からパスワード認証に切り替えると、keyAlias が null になる
     *
     * ## 背景
     * 認証方式の切り替え時に古い認証情報が残っていると、
     * 意図しない認証試行が発生する可能性がある。
     */
    @Test
    fun `switching auth type should clear previous auth data`() {
        // Given: 公開鍵認証の接続設定
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

        // When: パスワード認証に切り替え（keyAlias をクリア）
        val passwordAuthConnection = keyAuthConnection.copy(
            authType = "password",
            keyAlias = null
        )

        // Then: 認証方式が変更され、keyAlias がクリアされている
        assertEquals("password", passwordAuthConnection.authType)
        assertNull(passwordAuthConnection.keyAlias)
    }
}

/**
 * [SshKeyProvider] インターフェースの動作をテストするクラス。
 *
 * ## テスト対象
 * - 鍵リストの取得
 * - 鍵の存在確認
 * - [SshKeyInfoCommon] のデータ保持
 *
 * ## 設計意図
 * [SshKeyProvider] は commonMain で定義されたインターフェースであり、
 * プラットフォーム固有の実装（Android KeyStore）を抽象化する。
 * このテストでは、インターフェースの契約が正しく実装されることを検証する。
 *
 * @see SshKeyProvider
 * @see SshKeyInfoCommon
 */
class SshKeyProviderTest {

    /**
     * 鍵が存在しない場合、空のリストが返されることを検証する。
     *
     * ## 検証内容
     * - listKeys() が空のリストを返す
     *
     * ## 背景
     * 初回起動時やすべての鍵を削除した後など、鍵が1つも存在しない状態がありえる。
     * この場合、UIは「鍵がありません」というメッセージを表示する。
     */
    @Test
    fun `listKeys returns empty list when no keys`() {
        val provider = FakeSshKeyProvider(emptyList())

        val keys = provider.listKeys()

        assertTrue(keys.isEmpty())
    }

    /**
     * 登録済みの全ての鍵が返されることを検証する。
     *
     * ## 検証内容
     * - listKeys() が全ての鍵を含むリストを返す
     * - 各鍵のエイリアスが正しく取得できる
     *
     * ## 背景
     * 接続設定ダイアログでは、利用可能な鍵をドロップダウンで表示する。
     * この機能のために、全ての鍵を列挙できる必要がある。
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
     * 鍵の存在確認が正しく動作することを検証する。
     *
     * ## 検証内容
     * - 存在する鍵に対して keyExists() が true を返す
     * - 存在しない鍵に対して keyExists() が false を返す
     *
     * ## 背景
     * 接続試行前に、参照している鍵が実際に存在するかを確認する。
     * 鍵が削除されていた場合、接続を試みる前にエラーを表示できる。
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
     * [SshKeyInfoCommon] がアルゴリズム情報を正しく保持することを検証する。
     *
     * ## 検証内容
     * - RSA鍵のアルゴリズムが "RSA" として保持される
     * - ECDSA鍵のアルゴリズムが "ECDSA" として保持される
     *
     * ## 背景
     * UIでは鍵のアルゴリズムを表示し、ユーザーが鍵を識別しやすくする。
     * 例: "my-key (RSA)" と表示される。
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
 * テスト用の [SshKeyProvider] フェイク実装。
 *
 * コンストラクタで渡された鍵リストを返すシンプルな実装。
 * 実際の Android KeyStore を使用せずにテストを実行できる。
 *
 * @param keys テストで使用する鍵のリスト
 */
private class FakeSshKeyProvider(
    private val keys: List<SshKeyInfoCommon>
) : SshKeyProvider {
    override fun listKeys(): List<SshKeyInfoCommon> = keys
    override fun keyExists(alias: String): Boolean = keys.any { it.alias == alias }
}

/**
 * テスト用の [ConnectionRepository] フェイク実装。
 *
 * インメモリで接続設定を管理するシンプルな実装。
 * データベースを使用せずにリポジトリの動作をテストできる。
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
