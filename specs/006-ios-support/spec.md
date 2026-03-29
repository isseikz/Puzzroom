# Feature Specification: iOS Support for Mobile Vibe Terminal

**Feature Branch**: `006-ios-support`
**Created**: 2026-03-28
**Status**: Draft
**Input**: User description: "iOS support based on features for Android but some feature is unavailable for iOS IMO for example Magic deploy and external display support"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - SSH Terminal on iPhone/iPad (Priority: P1)

An iOS user launches Mobile Vibe Terminal on their iPhone or iPad, adds a server connection, and opens an interactive SSH terminal session. They can type commands, see output, and use the fixed key row for control characters—exactly as on Android.

**Why this priority**: SSH terminal is the core value proposition of the app. Without it, there is no MVP on iOS.

**Independent Test**: Can be fully tested by connecting to a live SSH server from an iOS device and running commands, delivering a working SSH terminal experience with no Android dependency.

**Acceptance Scenarios**:

1. **Given** an iOS user has the app installed and no saved connections, **When** they tap "Add Connection" and enter valid SSH credentials, **Then** the connection is saved and visible in the connection list.
2. **Given** a saved connection exists, **When** the user taps it, **Then** an SSH terminal session opens and the user can type and receive output.
3. **Given** an active SSH session, **When** the user taps keys in the fixed key row (e.g., Tab, Ctrl, Esc), **Then** the corresponding control characters are sent to the remote server.
4. **Given** an active SSH session, **When** the user presses the hardware keyboard or on-screen keyboard, **Then** keystrokes are correctly forwarded to the remote shell.

---

### User Story 2 - Connection Management and Persistence on iOS (Priority: P2)

An iOS user can save, edit, and delete server connections. Saved connections (including credentials stored securely) persist across app launches.

**Why this priority**: Without persistent connections, users must re-enter credentials every session, making the app impractical.

**Independent Test**: Can be fully tested by saving a connection, force-quitting the app, reopening it, and confirming the connection still appears and can connect.

**Acceptance Scenarios**:

1. **Given** the user creates a connection with a password, **When** they relaunch the app, **Then** the connection and its stored credentials are still available.
2. **Given** a saved connection, **When** the user edits or deletes it, **Then** changes are reflected immediately and persist after relaunch.
3. **Given** a connection using SSH key authentication, **When** the user selects a stored key, **Then** the connection authenticates without a password prompt.

---

### User Story 3 - Smart File Explorer and Code Peek on iOS (Priority: P3)

An iOS user with an active SSH session can browse remote directories, view file contents in a modal overlay (Code Peek), and download files to local storage or share them via iOS share sheet.

**Why this priority**: File browsing and viewing enhance the terminal workflow for iOS users, though the SSH terminal alone is a usable product.

**Independent Test**: Can be fully tested independently by opening the file explorer, navigating directories, opening a file in Code Peek, and sharing/downloading a file.

**Acceptance Scenarios**:

1. **Given** an active SSH session, **When** the user opens the file explorer, **Then** the home directory is listed and they can navigate subdirectories.
2. **Given** a file is selected in the explorer, **When** the user taps "View," **Then** the file content is shown in a modal Code Peek overlay.
3. **Given** a file in the explorer, **When** the user taps "Download" or "Share," **Then** the file is transferred and the iOS share sheet is presented to save or send it.
4. **Given** the user has previously opened the explorer and navigated to a path, **When** they reopen the explorer in the same session, **Then** the last-visited directory is restored.

---

### Edge Cases

- What happens when an SSH connection drops mid-session on iOS?
- How does the app behave when the user backgrounds the app during an active SSH session (iOS suspends background network)?
- What happens when a file download is interrupted by a network loss?
- How does the app handle iOS keychain access denial when retrieving stored credentials?
- What is shown to the user when they attempt to use a feature that is Android-only (e.g., any deep-link or notification that references Magic Deploy)?

## Requirements *(mandatory)*

### Functional Requirements

**Core SSH Terminal**

- **FR-001**: Users MUST be able to add, edit, and delete SSH server connections on iOS.
- **FR-002**: Users MUST be able to connect to an SSH server using password authentication on iOS.
- **FR-003**: Users MUST be able to connect to an SSH server using SSH key authentication on iOS.
- **FR-004**: The SSH terminal MUST display remote shell output in real time on iOS.
- **FR-005**: Users MUST be able to send keyboard input, including control characters via the fixed key row, to the remote shell on iOS.
- **FR-006**: The terminal MUST correctly handle terminal resize events when the iOS keyboard appears or disappears.

**Connection Persistence**

- **FR-007**: Saved connections MUST persist across app launches on iOS.
- **FR-008**: Credentials (passwords and SSH keys) MUST be stored using the iOS secure storage mechanism.
- **FR-009**: The app MUST remember the last active connection and offer to restore it on relaunch.

**File Explorer & Code Peek**

- **FR-010**: Users MUST be able to browse remote directories via SFTP within an active SSH session on iOS.
- **FR-011**: Users MUST be able to view remote file content in a Code Peek modal overlay on iOS.
- **FR-012**: Users MUST be able to download a remote file to local iOS storage.
- **FR-013**: Users MUST be able to share a downloaded file via the iOS native share sheet.
- **FR-014**: The file explorer MUST restore the last-visited directory path within a session.

**Platform Limitations (Out of Scope for iOS)**

- **FR-015**: Magic Deploy (APK auto-install) MUST NOT be present or advertised on iOS, as iOS cannot install Android packages.
- **FR-016**: External Display Support features specific to Android display presentation MUST NOT be required for iOS parity; the iOS app MAY display on external monitors via standard iOS mirroring without custom handling.

### Key Entities

- **SavedConnection**: Represents a stored SSH server profile (host, port, username, auth type). Shared with Android.
- **SSHSession**: An active connection to a remote server carrying a terminal stream and SFTP channel. Platform-specific implementation required for iOS.
- **FileEntry**: A remote filesystem item (name, path, type, size). Shared with Android.
- **StoredCredential**: A securely held password or SSH private key associated with a connection. Must use iOS Keychain on iOS.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An iOS user can establish their first SSH connection within 3 minutes of installing the app.
- **SC-002**: All core features available on Android (SSH terminal, connection management, file explorer, Code Peek, file sharing) work correctly on iOS with no feature-blocking bugs at launch.
- **SC-003**: Features unavailable on iOS (Magic Deploy, Android-specific external display handling) are cleanly absent from the iOS UI with no visible error states or dead UI elements.
- **SC-004**: Stored credentials survive app termination and device reboot on iOS, retrievable without user re-entry.
- **SC-005**: Terminal input latency on iOS is comparable to the Android experience under the same network conditions.

## Assumptions

- The SSH library used on Android (Apache MINA SSHD) is not available for iOS/KMP; an alternative SSH implementation compatible with Kotlin/Native or a platform-native Swift bridge will be required.
- iOS credential storage will use the iOS Keychain, replacing the Android Keystore approach.
- Connection persistence (currently Room/Android) requires a KMP-compatible database solution (e.g., SQLDelight) or a platform-specific stub with equivalent functionality on iOS.
- "External display support" on iOS is interpreted as standard iOS screen mirroring, which requires no custom app code; no special iOS external display feature is in scope.
- Magic Deploy is functionally impossible on iOS (cannot sideload APK packages) and will be omitted entirely from the iOS build.
- Hardware keyboard support on iOS will follow standard UIKit/SwiftUI keyboard event handling through Compose Multiplatform.
