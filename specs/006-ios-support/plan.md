# Implementation Plan: iOS Support for Mobile Vibe Terminal

**Branch**: `006-ios-support` | **Date**: 2026-03-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/006-ios-support/spec.md`

## Summary

Bring Mobile Vibe Terminal to iOS by replacing all iOS stub implementations with real platform-specific code, enabling SSH terminal sessions, connection management, file exploration, and file sharing — while permanently omitting Magic Deploy (APK installation) and Android-specific external display handling. The primary technical challenge is the SSH client: Apache MINA SSHD (used on Android/Desktop) is JVM-only and requires a libssh2 replacement via Kotlin/Native cinterop, which supports both interactive PTY and SFTP natively.

## Technical Context

**Language/Version**: Kotlin 2.x (Multiplatform, `iosArm64`, `iosSimulatorArm64`)
**Primary Dependencies**: Compose Multiplatform, Voyager, Koin, Room 2.7+ KMP, DataStore KMP, okio (KMP), libssh2 (C, via Kotlin/Native cinterop)
**Storage**: Room KMP + `BundledSQLiteDriver` for connection records; iOS Keychain (Security framework cinterop) for credentials and SSH keys; DataStore for preferences
**Testing**: `kotlin.test` (common), `iosSimulatorArm64Test` Gradle task
**Target Platform**: iOS 16+ (arm64 device + arm64 simulator)
**Project Type**: Kotlin Multiplatform mobile (KMM) — single Gradle module `mobile-vibe-terminal` with `iosMain` source set
**Performance Goals**: Terminal input round-trip latency on iOS comparable to Android under equivalent network conditions
**Constraints**: iOS app sandbox; all file I/O within `Documents/` or `tmp/`; no background SSH sessions after iOS suspends app; no APK installation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| 1. Atomic Design Compliance | PASS | No new UI components introduced; existing component hierarchy unchanged |
| 2. Theme Consistency | PASS | No new color values; existing MaterialTheme usage unchanged |
| 3. Platform Independence | PASS (with action) | `java.io.File` in `SshRepository` must be replaced with `okio.Path` before iOS target compiles |
| 4. Unidirectional Data Flow | PASS | iOS implementations are data-layer only; UI layer unchanged |
| 5. Simplicity Check | PASS | Each stub is replaced with the simplest working implementation; no speculative abstractions |
| 6. Worktree Isolation | PASS | Development must occur in `worktrees/006-ios-support/` |
| 7. Clean Architecture Compliance | PASS | New iOS code lives in `data/` and `iosMain/`; domain layer untouched except `okio.Path` signature fix |

**Gate 3 Violation Resolution**: The `java.io.File` import in `SshRepository.kt` (a `commonMain` domain interface) is a JVM-only type that prevents iOS compilation. Replacing it with `okio.Path` (multiplatform) satisfies Platform Independence without changing domain semantics. Android callers require a one-line update: `file.toOkioPath()`.

## Project Structure

### Documentation (this feature)

```text
specs/006-ios-support/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── platform-interfaces.md   # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
mobile-vibe-terminal/src/
├── commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
│   └── domain/repository/SshRepository.kt     ← update: java.io.File → okio.Path
│
├── androidMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
│   └── ssh/MinaSshdRepository.kt              ← update: file.toOkioPath() call sites
│
└── iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
    ├── di/
    │   └── PlatformModule.kt                  ← replace stubs with real impls
    ├── data/
    │   ├── database/
    │   │   └── DatabaseBuilder.ios.kt         ← NEW: Room KMP + BundledSQLiteDriver
    │   ├── datastore/
    │   │   └── DataStoreBuilder.ios.kt        ← NEW: DataStore with NSDocumentDirectory path
    │   └── repository/
    │       └── ConnectionRepositoryImpl.kt    ← replace ConnectionRepositoryStub
    ├── ssh/
    │   └── SshRepositoryIos.kt               ← replace SshRepositoryStub (delegates to Swift)
    ├── security/
    │   ├── KeychainHelper.kt                  ← NEW: Keychain cinterop wrapper
    │   └── SshKeyProviderIos.kt              ← NEW: iOS SSH key management
    ├── downloader/
    │   └── FileDownloaderIos.kt              ← replace FileDownloaderStub
    └── sharer/
        └── FileSharerIos.kt                  ← replace FileSharerStub

iosApp/iosApp/
├── iOSApp.swift                              ← no change needed
└── SSH/
    ├── IosSSHClient.swift                    ← NEW: SwiftNIO SSH implementation
    └── SSHClientProtocol.swift               ← NEW: @objc protocol bridge
```

**Structure Decision**: KMP mobile pattern — no separate `api/` or `backend/`. All shared logic stays in `commonMain`; platform code in `iosMain`. Swift SSH bridge lives in `iosApp/` as it requires Xcode/SPM build.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| libssh2 C cinterop for SSH | MINA SSHD is JVM-only; SwiftNIO SSH has no built-in SFTP; no mature pure-KMP SSH library exists | All alternatives either lack SFTP or require an additional Swift bridge layer |
| okio.Path replacing java.io.File | iOS Kotlin/Native cannot use JVM types in any source set | Custom expect/actual adds complexity with no benefit over the well-established okio multiplatform type |
