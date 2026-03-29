# Data Model: iOS Support for Mobile Vibe Terminal

**Branch**: `006-ios-support` | **Date**: 2026-03-28

All entities are shared with Android via `commonMain`. No new persistent entities are introduced; iOS support requires implementing the missing persistence backends for the existing model.

---

## Existing Entities (Shared, No Change)

### SavedConnection

Represents a stored SSH server profile. Persisted in Room database.

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | Long | Primary key, auto-generated |
| `name` | String | User-visible label, non-empty |
| `host` | String | Hostname or IP, non-empty |
| `port` | Int | Default: 22, range 1–65535 |
| `username` | String | Non-empty |
| `authType` | String | Enum: `"password"` or `"key"` |
| `keyAlias` | String? | Null when `authType = "password"` |
| `createdAt` | Long | Unix epoch ms, immutable after creation |
| `lastUsedAt` | Long? | Updated on every connect |
| `deployPattern` | String? | Regex for Magic Deploy (Android-only, nullable on iOS) |
| `startupCommand` | String? | Shell command run on connect |
| `isAutoReconnect` | Boolean | Default: false |
| `monitorFilePath` | String? | File path watched for changes |
| `lastFileExplorerPath` | String? | Restored on next File Explorer open |

**Validation rules**:
- `host` must be non-blank
- `port` must be in 1–65535
- `keyAlias` must reference an existing key in `SshKeyProvider` when `authType = "key"`

---

### FileEntry

Represents a remote filesystem item returned by SFTP `listFiles`. In-memory only; not persisted.

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Filename without path |
| `path` | String | Full remote absolute path |
| `isDirectory` | Boolean | True for directories |
| `size` | Long | Bytes; 0 for directories |
| `modifiedAt` | Long | Unix epoch ms |
| `permissions` | String | Unix permission string (e.g., `rwxr-xr-x`) |

---

### FileTransferState

In-memory state of an active file transfer. Emitted via `Flow` to the UI.

| Field | Type | Description |
|-------|------|-------------|
| `transferId` | String | Unique per transfer operation |
| `remotePath` | String | Source remote path |
| `localPath` | String | Destination local path (platform-specific) |
| `purpose` | TransferPurpose | `DOWNLOAD` or `SHARE` |
| `status` | TransferStatus | `IDLE`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| `bytesTransferred` | Long | Bytes transferred so far |
| `totalBytes` | Long | Total file size |
| `errorMessage` | String? | Non-null only when `status = FAILED` |

---

## New Platform-Specific Constructs (iOS Only)

### PlatformFile (okio.Path replacement)

The existing `SshRepository` interface uses `java.io.File` which is JVM-only. It must be replaced with `okio.Path` (cross-platform).

**Change required**: Update `SshRepository` interface signatures:
- `downloadFile(remotePath: String, localFile: File)` → `downloadFile(remotePath: String, localFile: Path)`
- `downloadFileWithProgress(remotePath, localFile: File, ...)` → `downloadFileWithProgress(remotePath, localFile: Path, ...)`

Android callers are updated to use `file.toOkioPath()` (okio extension).

---

### StoredCredential (Keychain, iOS)

Credentials stored in iOS Keychain. Not a database entity. Keyed by `connectionId`.

| Attribute | Value |
|-----------|-------|
| Service | `"vibeterminal.password.<connectionId>"` |
| Account | Connection username |
| Data | Password string (UTF-8) |
| Accessibility | `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` |

SSH private keys stored similarly:
| Attribute | Value |
|-----------|-------|
| Service | `"vibeterminal.sshkey.<keyAlias>"` |
| Class | `kSecClassKey` |
| Key type | RSA-4096 or EC-256 |

---

## Persistence Backends by Platform

| Concern | Android | iOS |
|---------|---------|-----|
| Connection records | Room (SQLite, `androidMain`) | Room KMP + `bundledSQLiteDriver` (`iosMain`) |
| App preferences | DataStore (`androidMain`) | DataStore + `NSDocumentDirectory` path (`iosMain`) |
| Passwords | DataStore (encrypted) | iOS Keychain via cinterop |
| SSH private keys | Android KeyStore | iOS Keychain via cinterop |
| SSH session state | In-memory | In-memory |
| File transfers | In-memory `Flow` | In-memory `Flow` |
