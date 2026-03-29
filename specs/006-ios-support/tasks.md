# Tasks: iOS Support for Mobile Vibe Terminal

**Input**: Design documents from `/specs/006-ios-support/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Not requested — no test tasks generated.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- All paths are relative to `mobile-vibe-terminal/` unless prefixed with `iosApp/`

---

## Phase 1: Setup

**Purpose**: Worktree isolation and dependency configuration required before any code is written.

- [x] T001 Create worktree and symlink local.properties: `git worktree add -b 006-ios-support worktrees/006-ios-support main && ln -s ../../local.properties worktrees/006-ios-support/local.properties`
- [x] T002 Add `okio` to `commonMain.dependencies` in `mobile-vibe-terminal/build.gradle.kts` (required to replace `java.io.File` across all targets)
- [x] T003 [P] ~~Add prebuilt `libssh2.xcframework` to Xcode~~ — not needed; libssh2/OpenSSL `.a` files are statically linked into the Kotlin framework via cinterop (`staticLibraries` in libssh2.def); Xcode consumes only the KMP-generated framework
- [x] T004 [P] Add libssh2 cinterop Gradle configuration in `mobile-vibe-terminal/build.gradle.kts` under `iosMain` source set: `cinterops { val libssh2 by creating { defFile("src/iosMain/cinterop/libssh2.def"); packageName("libssh2") } }`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before any user story work begins. Resolves the compile-blocking `java.io.File` issue and establishes iOS persistence and security foundations.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T005 Replace `java.io.File` with `okio.Path` in `src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/domain/repository/SshRepository.kt` (update `downloadFile` and `downloadFileWithProgress` signatures)
- [x] T006 Update `src/androidMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ssh/MinaSshdRepository.kt` call sites to pass `file.toPath()` (okio extension) at the two `downloadFile`/`downloadFileWithProgress` invocations
- [x] T007 [P] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/data/database/DatabaseBuilder.ios.kt` using `Room.databaseBuilder` with `BundledSQLiteDriver()` and `NSDocumentDirectory` path
- [x] T008 [P] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/data/datastore/DataStoreBuilder.ios.kt` using `createDataStore` with `NSDocumentDirectory` path
- [x] T009 Create `src/iosMain/cinterop/libssh2.def` cinterop definition file pointing at the libssh2 headers from the xcframework added in T003 (headers path, linkerOpts for libssh2 and libz)
- [x] T010 Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/security/KeychainHelper.kt` — a Kotlin/Native wrapper around the iOS `Security` framework (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`) for storing and retrieving string secrets keyed by service name

**Checkpoint**: iOS target must compile cleanly (`./gradlew :mobile-vibe-terminal:compileKotlinIosSimulatorArm64`) before proceeding.

---

## Phase 3: User Story 1 — SSH Terminal on iPhone/iPad (Priority: P1) 🎯 MVP

**Goal**: A working interactive SSH terminal session on iOS — connect with password or key auth, send input, receive real-time output, resize terminal.

**Independent Test**: Launch app on iOS simulator → add a server connection → connect → run `echo hello` → confirm output appears in terminal. No Android device required.

- [x] T011 [US1] Implement SSH session lifecycle in `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ssh/SshRepositoryIos.kt`: `connect()` and `connectWithKey()` using `libssh2_session_init`, `libssh2_userauth_password`/`libssh2_userauth_publickey_frommemory`, and `libssh2_channel_open_session` + `libssh2_channel_request_pty` + `libssh2_channel_shell`; also `disconnect()` and `isConnected()`
- [x] T012 [US1] Implement terminal I/O in `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ssh/SshRepositoryIos.kt`: `getOutputStream()` as `Flow<String>` backed by `MutableSharedFlow` fed from a coroutine calling `libssh2_channel_read` in a loop; `sendInput()` via `libssh2_channel_write`; `executeCommand()` via one-shot channel; `resizeTerminal()` via `libssh2_channel_request_pty_size_ex`
- [x] T013 [P] [US1] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/security/SshKeyProviderIos.kt`: Keychain-backed implementation storing PEM private keys and OpenSSH public keys; `listKeys()` via NSUserDefaults metadata; `keyExists()` via Keychain lookup
- [x] T014 [US1] Update `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/di/PlatformModule.kt` to bind `SshRepositoryIos` as `SshRepository` and `SshKeyProviderIos` as `SshKeyProvider`; initialize `AppDatabase` via `getRoomDatabase()` and `DataStore` via `createDataStore()` and register in Koin module
- [x] T015 [US1] Audit `src/commonMain/` for Magic Deploy controls — added `expect val isMagicDeploySupported: Boolean` in PlatformCapabilities.kt; gated APK install button in FileExplorerSheet behind it

**Checkpoint**: User Story 1 fully functional — SSH terminal session works on iOS 16 simulator.

---

## Phase 4: User Story 2 — Connection Management and Persistence (Priority: P2)

**Goal**: Saved connections (including credentials) survive app termination and relaunch on iOS.

**Independent Test**: Add a connection with a password → force-quit the app → relaunch → confirm the connection is listed and credentials are pre-filled → connect successfully without re-entering password.

- [x] T016 [US2] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/data/repository/ConnectionRepositoryImpl.kt` backed by Room `AppDatabase` with all CRUD methods using `ServerConnectionDao`
- [x] T017 [US2] Integrate `KeychainHelper` into `ConnectionRepositoryImpl` for `savePassword`, `getPassword`, and `deletePassword` — using `"vibe_password_<connectionId>"` Keychain keys
- [x] T018 [US2] Updated `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/di/PlatformModule.kt` to bind `ConnectionRepositoryImpl` as `ConnectionRepository` (replacing `ConnectionRepositoryStub`)

**Checkpoint**: User Stories 1 AND 2 independently functional — terminal works and connections persist across relaunches.

---

## Phase 5: User Story 3 — Smart File Explorer and Code Peek (Priority: P3)

**Goal**: iOS users can browse remote directories, view file content in Code Peek, and download/share files via the iOS share sheet.

**Independent Test**: With an active SSH session → open File Explorer → navigate directories → tap a text file → confirm Code Peek modal shows content → tap Share → confirm iOS share sheet appears with the file.

- [x] T019 [US3] Implement SFTP operations in `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ssh/SshRepositoryIos.kt`: `listFiles()` via `libssh2_sftp_opendir`/`libssh2_sftp_readdir_ex`; `readFileContent()` and `downloadFile()` via `libssh2_sftp_open_ex`/`libssh2_sftp_read`; `downloadFileWithProgress()` with `onProgress` callbacks
- [x] T020 [P] [US3] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/downloader/FileDownloaderIos.kt` using `NSDocumentDirectory/Downloads/`
- [x] T021 [P] [US3] Implement `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/sharer/FileSharerIos.kt` using `NSCachesDirectory/share/` and `UIActivityViewController`
- [x] T022 [US3] Updated `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/di/PlatformModule.kt` to bind `FileDownloaderIos` and `FileSharerIos`

**Checkpoint**: All three user stories independently functional on iOS simulator.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that span multiple user stories and final build validation.

- [x] T023 [P] Implement real haptic feedback in `src/iosMain/kotlin/tokyo/isseikuzumaki/vibeterminal/util/HapticFeedback.kt` using `UIImpactFeedbackGenerator` and `UINotificationFeedbackGenerator` via UIKit
- [x] T024 Verify the full iOS simulator build passes with no errors or warnings: `./gradlew :mobile-vibe-terminal:iosArm64Binaries :mobile-vibe-terminal:iosSimulatorArm64Binaries`
- [x] T025 [P] Updated `specs/006-ios-support/quickstart.md` with libssh2 setup options (CocoaPods/xcframework), Kotlin 2.3.x requirement for kmp-terminal-input compatibility, and Room migration notes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately; T003 and T004 are parallel
- **Foundational (Phase 2)**: Depends on Phase 1 complete — **BLOCKS all user stories**; T007, T008 are parallel after T005/T006; T009 and T010 are parallel
- **US1 (Phase 3)**: Depends on Phase 2 complete; T013 is parallel with T011/T012
- **US2 (Phase 4)**: Depends on Phase 2 complete; independent of Phase 3 (US1)
- **US3 (Phase 5)**: Depends on Phase 3 (US1) for active SSH session; T020 and T021 are parallel
- **Polish (Phase 6)**: Depends on all desired user stories complete

### User Story Dependencies

- **US1 (P1)**: Requires Phase 2. No dependency on US2 or US3.
- **US2 (P2)**: Requires Phase 2. No dependency on US1 or US3.
- **US3 (P3)**: Requires Phase 3 (US1) — SFTP is built on the same SSH session.

### Within Each User Story

- T011 (session lifecycle) must complete before T012 (terminal I/O) — same file, sequential
- T013 (SshKeyProvider) is independent of T011/T012 — different file, parallel
- T014 (DI wiring) must come after T011, T012, T013
- T016 (ConnectionRepositoryImpl) must come after T010 (KeychainHelper)
- T019 (SFTP) must extend T011 (existing SSH session) — sequential

### Parallel Opportunities

```text
Phase 1:  T001 → T002 → [T003 ∥ T004]
Phase 2:  T005 → T006 → [T007 ∥ T008] → T009 → T010
Phase 3:  T011 → T012 → [T013 ∥ T014(wait T011,T012,T013)] → T015
Phase 4:  T016 → T017 → T018  (can run in parallel with Phase 3 after Phase 2)
Phase 5:  T019 → [T020 ∥ T021] → T022
Phase 6:  [T023 ∥ T024 ∥ T025]
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (SSH Terminal)
4. **STOP AND VALIDATE**: Run app on iOS simulator, open a terminal session, confirm input/output works
5. Ship / demo MVP

### Incremental Delivery

1. Phase 1 + Phase 2 → iOS target compiles cleanly
2. Phase 3 (US1) → SSH terminal works on iPhone/iPad ← **MVP**
3. Phase 4 (US2) → connections persist across relaunches
4. Phase 5 (US3) → file explorer, Code Peek, file sharing
5. Phase 6 → polish + validated build

### Parallel Team Strategy

After Phase 2 completes:
- **Developer A**: Phase 3 (US1 — SSH terminal, libssh2 cinterop)
- **Developer B**: Phase 4 (US2 — Connection management, Room + Keychain)
- US3 starts after Developer A finishes Phase 3

---

## Notes

- `[P]` tasks = different files, no dependency on incomplete sibling tasks
- `[Story]` label maps each task to its user story for traceability
- Magic Deploy (`IosApkInstallerStub`) stays as-is permanently — do not replace or remove
- `desktopMain` is unaffected by the `okio.Path` change (okio supports JVM targets); no desktop tasks needed
- Commit after each checkpoint to keep a clean rollback point
- Total: **25 tasks** across 6 phases
