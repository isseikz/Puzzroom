# Research: iOS Support for Mobile Vibe Terminal

**Branch**: `006-ios-support` | **Date**: 2026-03-28

---

## Decision 1: SSH Client Library for iOS

**Decision**: Use **libssh2 via Kotlin/Native cinterop**.

**Rationale**:
- MINA SSHD is JVM-only — cannot be used on Kotlin/Native (iOS target).
- libssh2 supports both interactive PTY sessions **and** SFTP natively — the full set of operations required by `SshRepository`. SwiftNIO SSH (Apple's library) was initially considered but does not include a built-in SFTP implementation; adding SFTP on top would require implementing the SFTP sub-protocol manually, a significant additional scope.
- Kotlin/Native cinterop with C libraries is well-supported and keeps all SSH/SFTP logic in Kotlin — no Swift layer to maintain or bridge.
- libssh2 ships as a prebuilt `libssh2.xcframework` (available via CocoaPods or direct download), avoiding the need to compile from source.
- Async streaming (terminal output `Flow`) is achieved by running the libssh2 blocking read loop on a background coroutine dispatcher and emitting into a `Channel`.

**Alternatives considered**:
- SwiftNIO SSH: good SSH transport but **no built-in SFTP**; implementing SFTP sub-protocol from scratch was rejected as out-of-scope.
- NMSSH (Obj-C wrapper of libssh2): wraps the same underlying libssh2 but adds an Objective-C layer and requires Swift/Kotlin interop bridge; no benefit over direct cinterop.
- Pure KMP SSH: no mature library exists in the KMP ecosystem as of 2026.

**Implementation approach**:
1. Add prebuilt `libssh2.xcframework` to `iosApp/` (or via CocoaPods `pod 'libssh2'`).
2. Write a Kotlin/Native cinterop definition (`libssh2.def`) pointing at the libssh2 headers.
3. Implement `SshRepositoryIos` in `iosMain` using the cinterop bindings directly, with blocking read loops wrapped in `withContext(Dispatchers.IO)` and output emitted via `Channel<String>`.
4. SFTP operations (`listFiles`, `downloadFile`, `readFileContent`) use `libssh2_sftp_*` functions on a separate SFTP session to avoid interference with the terminal channel — same pattern as the Android MINA implementation.

---

## Decision 2: Replacing `java.io.File` in Domain Interfaces

**Decision**: Replace `java.io.File` with `okio.Path` (from the `okio` multiplatform library) across domain interfaces.

**Rationale**:
- `java.io.File` is a JVM-only type. It compiles for `androidMain` and `desktopMain` but causes compile failures on Kotlin/Native (`iosMain`).
- `okio.Path` is fully supported in Kotlin/Native and works identically on all targets.
- `okio` is already a transitive dependency of many KMP libraries used in the project.
- This is a minimal, targeted change: only the `SshRepository` interface signature needs updating; callers in `androidMain` pass `file.toOkioPath()`.

**Alternatives considered**:
- Custom `expect/actual PlatformFile`: more complex, no benefit over okio.
- `ByteArray` transfer (no local file): would require UI-layer changes and breaks existing Android download flow.

---

## Decision 3: Database Persistence on iOS (Room KMP)

**Decision**: Implement `DatabaseBuilder.ios.kt` using Room KMP's `SQLiteBundledDriver` (bundled SQLite driver for iOS, available since Room 2.7).

**Rationale**:
- Room 2.7+ supports Kotlin Multiplatform via `bundledSQLiteDriver`. KSP processors are already configured for `kspIosArm64` and `kspIosSimulatorArm64` in `build.gradle.kts` — confirming the project expects Room to work on iOS.
- The generated Room code works identically on iOS; only the database builder (file path resolution) is platform-specific.
- iOS stores the database in the app's `NSDocumentDirectory` — the platform-specific part is just resolving the path string.
- No schema changes required; existing Android schema (v6) applies directly.

**Alternatives considered**:
- SQLDelight: would require migrating the entire database definition away from Room — a larger refactor with no clear benefit given Room KMP is already configured.
- CoreData: iOS-only, incompatible with KMP.

---

## Decision 4: Preferences / Secure Storage on iOS

**Decision**: Implement `DataStoreBuilder.ios.kt` using DataStore's `createDataStore` with an `NSDocumentDirectory`-based path. For secure credential storage (passwords, SSH keys), use the iOS **Keychain** via Kotlin/Native cinterop with Apple's `Security` framework.

**Rationale**:
- `DataStore<Preferences>` supports KMP natively; only the file path factory function needs a platform-specific implementation.
- The existing `PasswordEncryptionHelper` (Android Keystore AES/GCM) cannot run on iOS; the iOS Keychain provides equivalent secure-element-backed credential storage without requiring manual encryption.
- Using Keychain directly (via cinterop `Security` framework) avoids adding a new library dependency.

**Alternatives considered**:
- `multiplatform-settings` library (Russhwolf): supports Keychain on iOS, but adds a dependency and wraps preferences — overlaps with existing DataStore usage.
- Saving encrypted bytes in DataStore: increases complexity and doesn't leverage the hardware-backed security the Keychain provides.

---

## Decision 5: File Download and Share on iOS

**Decision**: Implement `FileDownloaderIos` writing to the app's `NSDocumentDirectory`/Downloads folder (visible in Files app), and `FileSharerIos` presenting `UIActivityViewController` via `UIApplication.shared.keyWindow?.rootViewController`.

**Rationale**:
- iOS sandboxed apps write files to their own container; the `Documents/` directory is user-visible in the Files app without special entitlements.
- `UIActivityViewController` is the standard iOS share sheet API.
- These can be implemented fully in `iosMain` Kotlin using `UIKit` cinterop which is available by default in KMP iOS targets.

**Alternatives considered**:
- Writing to `tmp/` only: files not accessible to user after app close.
- iCloud Drive integration: requires additional entitlements, out of scope for v1.

---

## Decision 6: SSH Key Management on iOS

**Decision**: Implement `SshKeyProviderIos` using iOS Keychain for private key storage and `Security.SecKeyGeneratePair` for key generation, exposed via Kotlin/Native cinterop.

**Rationale**:
- iOS Secure Enclave supports RSA and EC key pairs via `SecKeyGeneratePair`.
- Private keys stored in Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` cannot be exported by attackers even with physical access.
- The public key can be exported in OpenSSH format for server configuration.

---

## Decision 7: Magic Deploy and External Display

**Decision**: Both features are **excluded from iOS scope**.

**Rationale**:
- Magic Deploy installs Android APKs — physically impossible on iOS. The `IosApkInstallerStub` is the correct permanent state; no user-visible UI element referencing this feature should appear on iOS.
- External Display Support used Android `Presentation` API and Activity lifecycle management. iOS screen mirroring works automatically at OS level; no custom code needed.

---

## Pre-existing Infrastructure (No Action Needed)

- `iosApp/` Xcode project exists with working `iOSApp.swift` entry point.
- Compose Multiplatform iOS target renders via `ComposeUIViewController` — already functional.
- `BackHandler.ios.kt`, `UrlOpener.ios.kt`, `HapticFeedback.kt` already implemented (minimal).
- Koin `expect fun platformModule(): Module` already declared — only the iOS `actual` needs stub-to-real replacement.
- Voyager, Koin, Compose Multiplatform all support iOS arm64 and simulator targets.
