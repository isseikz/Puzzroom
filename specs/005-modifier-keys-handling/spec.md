# Feature Specification: Modifier Keys Handling

**Feature Branch**: `005-modifier-keys-handling`
**Created**: 2026-03-05
**Status**: Draft
**Input**: User description: "Modifier Keys Handling for Mobile Vibe Terminal SSH client"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ctrl Key Combos in SSH Session (Priority: P1)

A developer using the terminal app needs to send control characters to the SSH server — for example, pressing Ctrl+C to interrupt a running process, or Ctrl+Z to suspend it. Since the device has no physical Ctrl key, the user taps the on-screen Ctrl toggle button and then taps the desired key. The correct control character is immediately transmitted to the remote server.

**Why this priority**: Ctrl key combinations (especially Ctrl+C, Ctrl+Z, Ctrl+D) are essential for basic terminal operation. Without them, users cannot interrupt processes or perform fundamental shell operations. This is the minimum viable modifier key feature.

**Independent Test**: Can be tested by connecting to an SSH session, tapping the Ctrl button, tapping "C", and verifying the running process is interrupted on the server.

**Acceptance Scenarios**:

1. **Given** a connected SSH session, **When** the user taps the Ctrl toggle button and then taps the "C" key, **Then** a Ctrl+C signal (interrupt) is sent to the server and any running process stops.
2. **Given** the Ctrl button is tapped once (one-shot), **When** the user sends a key combination, **Then** the Ctrl toggle automatically deactivates after the key is sent.
3. **Given** the Ctrl button is double-tapped (locked), **When** the user sends multiple key presses, **Then** Ctrl remains active for each key press until the user taps it again to unlock.

---

### User Story 2 - On-Screen Modifier Toggle Buttons (Priority: P1)

A user working in the SSH terminal needs to use modifier keys (Shift, Alt, Ctrl) that are not available or easily accessible on the software keyboard. The app shows clearly visible toggle buttons in the fixed key row. When tapped, a button visually changes state (highlighted), and provides haptic feedback so the user knows the modifier is active.

**Why this priority**: The toggle buttons are the primary UI mechanism for modifier keys on a touchscreen device. Without them, modifier combinations are inaccessible to most users.

**Independent Test**: Can be tested in isolation — launch the app, observe the modifier buttons in the key row, tap each, verify visual state changes and haptic feedback within 50ms, without needing an active SSH connection.

**Acceptance Scenarios**:

1. **Given** the app is open, **When** the user taps a modifier button once, **Then** the button visually switches to one-shot active state and haptic feedback is provided within 50ms.
2. **Given** a modifier button is in one-shot active state, **When** the user presses a key, **Then** the modifier is applied and the button automatically returns to inactive state.
3. **Given** the app is open, **When** the user double-taps a modifier button, **Then** the button visually switches to locked active state (distinct from one-shot active) and haptic feedback is provided.
4. **Given** a modifier button is in locked state, **When** the user presses multiple keys in sequence, **Then** the modifier is applied to each key and the button remains locked.
5. **Given** a modifier button is in locked state, **When** the user taps it once, **Then** the button returns to inactive state and the modifier is no longer applied.

---

### User Story 3 - Modified Cursor and Function Keys (Priority: P2)

A developer using a text editor (vim, nano) or shell over SSH needs to send modified cursor and function key combinations — such as Ctrl+Right to move word-by-word, Shift+F5 to trigger IDE shortcuts, or Alt+Left for shell word navigation. The app sends the correct xterm escape sequences that the remote application can interpret.

**Why this priority**: Cursor and function key combinations are heavily used in text editors and interactive tools. Without correct escape sequences, remote applications receive unrecognized input, breaking workflows.

**Independent Test**: Can be tested by connecting to an SSH session running a terminal application (e.g., vim), activating a modifier, pressing a cursor key, and verifying the expected navigation behavior occurs on the remote side.

**Acceptance Scenarios**:

1. **Given** an active SSH session, **When** Ctrl is active and the user presses the Right cursor key, **Then** the sequence `ESC[1;5C` is transmitted to the server.
2. **Given** an active SSH session, **When** Shift is active and the user presses the Up cursor key, **Then** the sequence `ESC[1;2A` is transmitted.
3. **Given** no modifier is active and the terminal is in normal cursor mode, **When** the user presses the Up cursor key, **Then** the sequence `ESC[A` is transmitted.
4. **Given** no modifier is active and the terminal is in application cursor mode (DECCKM), **When** the user presses the Up cursor key, **Then** the sequence `ESBOA` is transmitted.

---

### User Story 4 - Combined Modifier Sources (Priority: P2)

A user with a physical keyboard attached to their Android device can use hardware modifier keys (Shift, Ctrl, Alt) simultaneously with the on-screen modifier toggle buttons. The app merges both sources so that, for example, pressing Shift on the hardware keyboard while the Ctrl button is toggled on results in a Shift+Ctrl combination being sent to the server.

**Why this priority**: Users with physical keyboards should have a consistent and correct experience. Incorrect merging would cause wrong escape sequences and break terminal workflows.

**Independent Test**: Can be tested by attaching a physical keyboard, pressing a hardware modifier while an on-screen button is active, and verifying the combined escape sequence reaches the SSH server.

**Acceptance Scenarios**:

1. **Given** the on-screen Ctrl button is active and the user presses Shift+Up on a physical keyboard, **Then** the sequence for Shift+Ctrl+Up (`ESC[1;6A`) is transmitted.
2. **Given** both the IME and the on-screen button report Shift, **When** a key is pressed, **Then** only a single Shift modifier is applied (no duplication).

---

### User Story 5 - Alt Key and Special Key Combinations (Priority: P3)

A developer using shell shortcuts (Alt+F for word-forward in bash, Alt+D for delete-word, Shift+Tab for reverse tab completion) needs these to work correctly through the SSH app. The app sends the proper escape sequences for Alt-prefixed characters and handles special keys like Tab, Backspace, and Enter appropriately.

**Why this priority**: These are productivity shortcuts. The app is usable without them, but they significantly improve the experience for power users.

**Independent Test**: Can be tested in a bash shell over SSH — toggle Alt, press "F", and verify the cursor moves forward one word.

**Acceptance Scenarios**:

1. **Given** an active SSH session, **When** Alt is active and the user presses "f", **Then** the bytes `ESC 0x66` are transmitted.
2. **Given** an active SSH session, **When** Shift is active and the user presses Tab, **Then** the backtab sequence `ESC[Z` is transmitted.
3. **Given** an active SSH session, **When** Ctrl is active and the user presses Backspace, **Then** the byte `0x08` is transmitted.

---

### Edge Cases

- If both the IME and on-screen button report the same modifier simultaneously, only one instance of that modifier is applied — no doubling of the modifier number.
- Dead keys and composing sequences from the software keyboard do not trigger modifier processing until the composition is committed.
- When the terminal is in application cursor mode (DECCKM), unmodified cursor keys use SS3-style sequences (`ESBOA`), but modified cursor keys always use CSI format regardless of mode.
- The Meta key may not be physically available on most Android devices; when detected via hardware events it is mapped to xterm modifier bitmask value 8.
- Modifier toggle buttons pressing does not itself transmit any character — only the subsequent non-modifier key triggers transmission.

## Requirements *(mandatory)*

### Functional Requirements

**Modifier Toggle Buttons**

- **FR-001**: The app MUST provide on-screen toggle buttons for Shift, Alt, and Ctrl in the terminal key row.
- **FR-002**: Each toggle button MUST display three distinct visual states: inactive (default), one-shot active, and locked active.
- **FR-003**: While a toggle button is active (one-shot or locked), the corresponding modifier MUST be applied to every non-modifier key event.
- **FR-004**: A single tap on a toggle button MUST activate one-shot mode: the modifier is applied to the next key press and then automatically deactivated.
- **FR-005**: A double-tap on a toggle button MUST activate lock mode: the modifier stays active across all subsequent key presses until the button is tapped once to deactivate.
- **FR-006**: One-shot and locked states MUST be visually distinct from each other and from the inactive state (e.g., different highlight intensity or color).

**Input Source Handling**

- **FR-008**: The app MUST capture modifier state from on-screen toggle buttons, software keyboard (IME) events, and physical keyboard events.
- **FR-009**: The app MUST read modifier state from keyboard events using the standard modifier detection methods exposed by the platform (`isShiftPressed`, `isCtrlPressed`, `isAltPressed`, `isMetaPressed`).

**Modifier Combination**

- **FR-010**: The app MUST combine modifier states from all active sources using a bitwise OR operation, evaluated at the moment a non-modifier key is pressed.
- **FR-011**: Duplicate modifiers from multiple sources MUST resolve to a single modifier instance (bitwise OR is idempotent).

**Escape Sequence Conversion — Printable Characters**

- **FR-012**: Ctrl + alphabetic letter (A–Z) MUST transmit the corresponding ASCII control character (`0x01`–`0x1A`).
- **FR-013**: Alt + character MUST transmit ESC (`0x1B`) followed by the character byte.
- **FR-014**: Shift + character MUST transmit the shifted variant of the character as determined by the keyboard layout, with no additional escape sequence.
- **FR-015**: Alt + Ctrl + character MUST transmit ESC followed by the Ctrl-modified byte.

**Escape Sequence Conversion — Cursor Keys**

- **FR-016**: Modified cursor keys (Up, Down, Left, Right, Home, End) MUST transmit the CSI parameterized sequence `ESC[1;<modifier_number><direction>`.
- **FR-017**: Unmodified cursor keys MUST transmit standard sequences (`ESC[A` etc.) in normal cursor mode, and SS3-style sequences (`ESBOA` etc.) in application cursor mode (DECCKM).
- **FR-018**: Modified cursor keys MUST always use CSI format regardless of DECCKM state.

**Escape Sequence Conversion — Function Keys**

- **FR-019**: Modified function keys (F1–F12) MUST transmit `ESC[<key_number>;<modifier_number>~`.
- **FR-020**: Unmodified F1–F4 MUST use VT-mode sequences (`ESBOP`–`EBSOS`) when no modifier is active; F5–F12 use CSI tilde sequences.

**Escape Sequence Conversion — Special Keys**

- **FR-021**: Shift+Tab MUST transmit the backtab sequence `ESC[Z`.
- **FR-022**: Ctrl+Backspace MUST transmit `0x08`.
- **FR-023**: Insert, Delete, Page Up, Page Down with modifiers MUST transmit `ESC[<key_number>;<modifier_number>~`.

**SSH Transmission**

- **FR-024**: The converted byte sequence MUST be written to the SSH channel's input stream immediately upon key event processing, with no additional buffering of individual keystrokes.
- **FR-025**: No additional framing or encoding MUST be applied to the byte sequence before transmission.

**Non-Functional**

- **FR-026**: Key-to-sequence conversion MUST complete in under 5ms.
- **FR-027**: Modifier toggle button haptic and/or visual feedback MUST occur within 50ms of touch.
- **FR-028**: The escape sequence conversion logic MUST be implemented as a pure function with no dependency on the Android UI framework, to enable unit testing.
- **FR-029**: The escape sequence mapping MUST be structured to support future terminal emulation modes (e.g., kitty keyboard protocol, modifyOtherKeys mode 2) without requiring a full redesign.

### Key Entities

- **ModifierBitmask**: Represents the combined modifier bitmask at the time of a key event. Attributes: shift (bit 1), alt (bit 2), ctrl (bit 4), meta (bit 8). Computed as bitwise OR of all active sources (UI buttons + hardware/IME KeyEvent flags).
- **ModifierButtonState**: Tracks whether each on-screen modifier button is inactive, one-shot active, or locked active. State transitions: inactive → one-shot (single tap) → inactive (after next key); inactive → locked (double-tap) → inactive (tap to unlock).
- **KeyEvent**: A key press event from any input source, carrying the key code and raw modifier flags from the platform.
- **EscapeSequence**: The resolved byte sequence to be transmitted to the SSH server for a given key code + ModifierBitmask combination.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can interrupt a remote process using Ctrl+C via the on-screen button within 2 taps, with the signal reaching the server in under 5ms of key processing time.
- **SC-002**: All standard xterm modifier escape sequences for cursor keys, function keys, and special keys are transmitted correctly, verified by automated unit tests covering 100% of the mapping table.
- **SC-003**: Modifier toggle buttons provide haptic and/or visual feedback within 50ms of touch on target devices.
- **SC-004**: Modifier state from on-screen buttons and physical/software keyboard inputs is correctly merged — verified by tests covering single-source, dual-source, and duplicate-source scenarios.
- **SC-005**: The escape sequence conversion logic can be executed and validated entirely in unit tests without running on an Android device.
- **SC-006**: Zero reported cases of modifier state leaking across key events (modifier applied to more than the intended key in one-shot mode).

## Assumptions

- The xterm modifier number convention (1 + bitmask) is the target standard; no other terminal protocols (kitty, modifyOtherKeys) are in scope for initial implementation.
- One-shot mode (single tap) is the default interaction; lock mode (double-tap) requires no configuration.
- No on-screen Meta toggle button is provided; Meta is only detected from physical keyboard events.
- Dead-key and IME composition events are already handled upstream; this feature only processes committed key events.
- The SSH channel's input stream is already established before key events are processed.
