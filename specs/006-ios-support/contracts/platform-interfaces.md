# Platform Interface Contracts: iOS Support

**Branch**: `006-ios-support` | **Date**: 2026-03-28

This is a mobile KMP project. There are no REST/GraphQL APIs to define. Instead, this document captures the **platform interface contracts** — the `expect/actual` and domain interface boundaries that iOS implementations must satisfy.

---

## Contract 1: SshRepository (iosMain actual)

**Interface**: `commonMain/domain/repository/SshRepository`
**iOS Implementation**: `iosMain/ssh/SshRepositoryIos`

```
connect(host, port, username, password, cols, rows, widthPx, heightPx, startupCommand?) → Result<Unit>
connectWithKey(host, port, username, keyAlias, cols, rows, widthPx, heightPx, startupCommand?) → Result<Unit>
disconnect() → Unit
isConnected() → Boolean
executeCommand(command) → Result<String>
getOutputStream() → Flow<String>          // real-time terminal output
sendInput(input: String) → Unit
downloadFile(remotePath, localFile: Path) → Result<Unit>
downloadFileWithProgress(remotePath, localFile: Path, totalBytes, onProgress) → Result<Unit>
listFiles(remotePath) → Result<List<FileEntry>>
readFileContent(remotePath) → Result<String>
resizeTerminal(cols, rows, widthPx, heightPx) → Unit
```

**Behaviour contracts**:
- `getOutputStream()` must emit output in real time as a cold `Flow`; cancelling collection must not crash.
- `connect*` failures must return `Result.failure` with a user-readable message (never throw).
- `downloadFileWithProgress` must call `onProgress` at regular intervals; final call has `bytesTransferred == totalBytes`.
- All suspend functions must be cancellation-safe.

**Implementation approach** (libssh2 cinterop):
- SSH terminal channel opened via `libssh2_channel_open_session` + `libssh2_channel_request_pty` + `libssh2_channel_shell`.
- Output read loop runs on `Dispatchers.IO`, emitting lines into a `Channel<String>` backed `Flow`.
- SFTP operations use a separate `libssh2_sftp_init` session on the same `LIBSSH2_SESSION` to avoid channel interference.
- Key auth uses `libssh2_userauth_publickey_frommemory` with the private key bytes retrieved from Keychain.

---

## Contract 2: ConnectionRepository (iosMain actual)

**Interface**: `commonMain/domain/repository/ConnectionRepository`
**iOS Implementation**: `iosMain/data/repository/ConnectionRepositoryImpl`
**Backed by**: Room KMP database with `bundledSQLiteDriver`

```
getAllConnections() → Flow<List<SavedConnection>>
getConnectionById(id: Long) → SavedConnection?
insertConnection(connection: SavedConnection) → Long      // returns new id
updateConnection(connection: SavedConnection) → Unit
deleteConnection(connection: SavedConnection) → Unit
updateLastUsed(connectionId: Long) → Unit
getLastActiveConnectionId() → Long?
setLastActiveConnectionId(connectionId: Long?) → Unit
savePassword(connectionId: Long, password: String) → Unit  // stored in Keychain
getPassword(connectionId: Long) → String?                  // retrieved from Keychain
deletePassword(connectionId: Long) → Unit
updateLastFileExplorerPath(connectionId: Long, path: String?) → Unit
getLastFileExplorerPath(connectionId: Long) → String?
```

**Behaviour contracts**:
- `getAllConnections()` returns a `Flow` that emits a new list whenever the database changes.
- `savePassword` / `getPassword` must use iOS Keychain, not the database.
- `deleteConnection` must also call `deletePassword` for that connection.

---

## Contract 3: DatabaseBuilder (expect/actual)

**Expect**: `commonMain/data/database/DatabaseBuilder.kt`

```kotlin
expect fun buildDatabase(): AppDatabase
```

**iOS actual**: `iosMain/data/database/DatabaseBuilder.ios.kt`
```kotlin
actual fun buildDatabase(): AppDatabase =
    Room.databaseBuilder<AppDatabase>(
        name = NSHomeDirectory() + "/Documents/vibeterminal.db",
        factory = { AppDatabase::class.instantiateImpl() }
    ).setDriver(BundledSQLiteDriver())
     .setQueryCoroutineContext(Dispatchers.IO)
     .build()
```

---

## Contract 4: DataStoreBuilder (expect/actual)

**Expect**: `commonMain/data/datastore/DataStoreBuilder.kt`

```kotlin
expect fun createDataStore(): DataStore<Preferences>
```

**iOS actual**: `iosMain/data/datastore/DataStoreBuilder.ios.kt`
```kotlin
actual fun createDataStore(): DataStore<Preferences> =
    createDataStore(
        producePath = { NSHomeDirectory() + "/Documents/vibeterminal_prefs.preferences_pb" }
    )
```

---

## Contract 5: FileDownloader (iosMain actual)

**Interface**: `commonMain/domain/downloader/FileDownloader`
**iOS Implementation**: `iosMain/downloader/FileDownloaderIos`

```
getDownloadDirectory() → Path          // → NSDocumentDirectory/Downloads/
generateUniqueFilename(dir: Path, name: String) → Path
notifyDownloadComplete(file: Path) → Unit   // no-op on iOS; file is in Documents
```

---

## Contract 6: FileSharer (iosMain actual)

**Interface**: `commonMain/domain/sharer/FileSharer`
**iOS Implementation**: `iosMain/sharer/FileSharerIos`

```
getShareCacheDirectory() → Path        // → NSTemporaryDirectory()/share/
shareFile(file: Path) → Unit           // presents UIActivityViewController
cleanupShareCache() → Unit             // delete files older than 1 hour
```

---

## Contract 7: SshKeyProvider (iosMain actual)

**Interface**: `commonMain/security/SshKeyProvider`
**iOS Implementation**: `iosMain/security/SshKeyProviderIos`

```
generateKeyPair(alias: String, type: KeyType) → Result<Unit>   // RSA-4096 or ECDSA-256
getPublicKeyOpenSsh(alias: String) → Result<String>
deleteKeyPair(alias: String) → Unit
listKeyAliases() → List<String>
```

Keys stored in iOS Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`.
