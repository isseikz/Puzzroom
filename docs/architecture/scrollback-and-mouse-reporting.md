# Scrollback Buffer & Mouse Reporting Architecture

## Overview

This document describes the architecture for implementing scrollback buffer and mouse reporting features in Mobile Vibe Terminal. The implementation is divided into phases, with Phase 1 focusing on byobu/tmux compatibility.

## Current State Analysis

### Existing Implementation

The terminal emulator already supports:
- **Alternate Screen Buffer** (`ESC[?1049h/l`): Switching between primary and alternate screens
- **Scroll Region** (DECSTBM): Partial screen scrolling via `ESC[{top};{bottom}r`
- **Basic VT100/ANSI sequences**: Cursor movement, colors, text styling

### Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    TerminalScreenBuffer                      │
│  - primaryBuffer: Array<Array<TerminalCell>>                │
│  - alternateBuffer: Array<Array<TerminalCell>>?             │
│  - cursorRow, cursorCol                                     │
│  - scrollTop, scrollBottom (scroll region)                  │
│  - isAlternateScreen: Boolean                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    AnsiEscapeParser                          │
│  - Parses ESC sequences                                     │
│  - Updates TerminalScreenBuffer                             │
│  - Handles DECSET/DECRST (h/l commands)                     │
└─────────────────────────────────────────────────────────────┘
```

### Gap Analysis for byobu Support

| Feature | Current State | Required for byobu |
|---------|--------------|-------------------|
| Alternate screen (1049) | Implemented | Required |
| Mouse reporting (1000, 1002, 1003, 1006) | Not implemented | Required |
| Mouse scroll events | Not implemented | Required |
| Scrollback buffer | Not implemented | Nice to have |

## Phase 1: byobu/tmux Mouse Support

### Goals

1. Enable mouse scroll events within byobu/tmux sessions
2. Support SGR Extended Mouse Mode (1006) - the modern standard
3. Maintain clean separation between terminal emulation and input handling

### Architecture Design

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TerminalEmulator                             │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      ScreenManager                            │  │
│  │  ┌─────────────────┐      ┌─────────────────┐                │  │
│  │  │  PrimaryScreen  │      │  AlternateScreen │                │  │
│  │  │  + scrollback   │      │  (no scrollback) │                │  │
│  │  │    (Phase 2)    │      │                  │                │  │
│  │  └─────────────────┘      └─────────────────┘                │  │
│  │           ↑                       ↑                           │  │
│  │           └───── activeScreen ────┘                          │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    MouseInputHandler                          │  │
│  │  - reportingMode: MouseReportingMode                         │  │
│  │  - onScroll(direction, x, y) -> sends to remote or local     │  │
│  │  - onClick(button, x, y) -> sends to remote if enabled       │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                   AnsiEscapeParser                            │  │
│  │  - Existing CSI parsing                                       │  │
│  │  - NEW: Mouse mode handling (1000, 1002, 1003, 1006)         │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Details

#### 1. MouseReportingMode (New)

```kotlin
enum class MouseReportingMode {
    NONE,           // No mouse reporting
    X10,            // 1000: Click only
    BUTTON_EVENT,   // 1002: Click + drag
    ANY_EVENT,      // 1003: All mouse movement
}

enum class MouseEncodingMode {
    NORMAL,         // Legacy encoding (limited to 223 columns)
    SGR,            // 1006: SGR extended mode (recommended)
}
```

#### 2. MouseInputHandler (New)

Responsibilities:
- Track current mouse reporting mode
- Convert UI touch/scroll events to terminal escape sequences
- Decide whether to send events to remote or handle locally

```kotlin
class MouseInputHandler(
    private val screenManager: ScreenManager,
    private val sendToRemote: (String) -> Unit
) {
    var reportingMode: MouseReportingMode = MouseReportingMode.NONE
    var encodingMode: MouseEncodingMode = MouseEncodingMode.NORMAL

    fun onScroll(direction: ScrollDirection, col: Int, row: Int) {
        when {
            // Alternate screen + mouse reporting enabled -> send to remote
            screenManager.isAlternateScreen && reportingMode != MouseReportingMode.NONE -> {
                sendMouseEvent(
                    button = if (direction == UP) 64 else 65,
                    col = col,
                    row = row,
                    pressed = true
                )
            }
            // Primary screen -> local scrollback (Phase 2)
            !screenManager.isAlternateScreen -> {
                // TODO: Phase 2 - scroll local scrollback buffer
            }
        }
    }

    private fun sendMouseEvent(button: Int, col: Int, row: Int, pressed: Boolean) {
        when (encodingMode) {
            MouseEncodingMode.SGR -> {
                // SGR format: ESC [ < Cb ; Cx ; Cy M/m
                val suffix = if (pressed) "M" else "m"
                sendToRemote("\u001b[<$button;$col;${row}$suffix")
            }
            MouseEncodingMode.NORMAL -> {
                // Legacy format: ESC [ M Cb Cx Cy (limited range)
                // Not recommended, but may be needed for compatibility
            }
        }
    }
}
```

#### 3. AnsiEscapeParser Extensions

Add handling for mouse mode escape sequences:

```kotlin
// In executeCsiCommand(), extend 'h' (DECSET) handling:
'h' -> {
    if (isPrivate) {
        when {
            params.contains(1049) -> { /* existing */ }
            params.contains(1000) -> mouseHandler.reportingMode = MouseReportingMode.X10
            params.contains(1002) -> mouseHandler.reportingMode = MouseReportingMode.BUTTON_EVENT
            params.contains(1003) -> mouseHandler.reportingMode = MouseReportingMode.ANY_EVENT
            params.contains(1006) -> mouseHandler.encodingMode = MouseEncodingMode.SGR
        }
    }
}

// In executeCsiCommand(), extend 'l' (DECRST) handling:
'l' -> {
    if (isPrivate) {
        when {
            params.contains(1049) -> { /* existing */ }
            params.contains(1000) -> mouseHandler.reportingMode = MouseReportingMode.NONE
            params.contains(1002) -> mouseHandler.reportingMode = MouseReportingMode.NONE
            params.contains(1003) -> mouseHandler.reportingMode = MouseReportingMode.NONE
            params.contains(1006) -> mouseHandler.encodingMode = MouseEncodingMode.NORMAL
        }
    }
}
```

#### 4. ScreenManager (Refactor)

Extract screen management from `TerminalScreenBuffer` for cleaner separation:

```kotlin
class ScreenManager(rows: Int, cols: Int) {
    private val primaryScreen = TerminalScreen(rows, cols)
    private val alternateScreen = TerminalScreen(rows, cols)

    private var _activeScreen: TerminalScreen = primaryScreen
    val activeScreen: TerminalScreen get() = _activeScreen

    val isAlternateScreen: Boolean
        get() = _activeScreen === alternateScreen

    // Saved cursor for screen switching
    private var savedCursor: CursorState? = null

    fun enterAlternateScreen() {
        savedCursor = primaryScreen.saveCursorState()
        _activeScreen = alternateScreen
        alternateScreen.clear()
    }

    fun exitAlternateScreen() {
        _activeScreen = primaryScreen
        savedCursor?.let { primaryScreen.restoreCursorState(it) }
    }
}
```

### UI Integration

#### Touch Event Handling in TerminalCanvas

```kotlin
@Composable
fun TerminalCanvas(
    screenManager: ScreenManager,
    mouseHandler: MouseInputHandler,
    charWidth: Float,
    charHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (mouseHandler.reportingMode != MouseReportingMode.NONE) {
                        val col = (change.position.x / charWidth).toInt() + 1
                        val row = (change.position.y / charHeight).toInt() + 1
                        val direction = if (dragAmount < 0) ScrollDirection.UP else ScrollDirection.DOWN
                        mouseHandler.onScroll(direction, col, row)
                    }
                }
            }
    ) {
        // ... existing drawing code
    }
}
```

### Sequence Diagram: Mouse Scroll in byobu

```
User        TerminalCanvas    MouseInputHandler    SSH Connection    byobu/tmux
  │              │                   │                   │               │
  │─ swipe up ──→│                   │                   │               │
  │              │── onScroll(UP) ──→│                   │               │
  │              │                   │── check mode ────→│               │
  │              │                   │   (SGR enabled)   │               │
  │              │                   │                   │               │
  │              │                   │── ESC[<64;5;10M ─→│               │
  │              │                   │                   │──────────────→│
  │              │                   │                   │               │
  │              │                   │                   │←─ screen ─────│
  │              │                   │                   │   update      │
  │              │←─────────────────────────────────────────────────────│
  │←─ redraw ────│                   │                   │               │
```

## Phase 2: Scrollback Buffer (Future)

### Overview

Phase 2 will add scrollback buffer support for the primary screen, allowing users to scroll back through command history when not in alternate screen mode.

### Planned Architecture

```kotlin
class ScrollbackBuffer(private val maxLines: Int = 10000) {
    private val lines = ArrayDeque<Array<TerminalCell>>()

    fun addLine(line: Array<TerminalCell>) {
        if (lines.size >= maxLines) {
            lines.removeFirst()
        }
        lines.addLast(line.copyOf())
    }

    fun getLines(offset: Int, count: Int): List<Array<TerminalCell>> {
        // Return lines from scrollback for display
    }
}

class TerminalScreen(
    val rows: Int,
    val cols: Int,
    private val hasScrollback: Boolean = false
) {
    val buffer: Array<Array<TerminalCell>> = ...
    private val scrollback: ScrollbackBuffer? =
        if (hasScrollback) ScrollbackBuffer() else null

    fun scrollUp() {
        scrollback?.addLine(buffer[0])
        // ... shift lines
    }
}
```

### Phase 2 Scope

- Scrollback buffer storage
- Local scroll handling in primary screen
- Scroll position indicator UI
- Memory management for large histories

## File Changes Summary

### Phase 1 Files to Modify

| File | Change Type | Description |
|------|-------------|-------------|
| `terminal/MouseReportingMode.kt` | New | Enum definitions |
| `terminal/MouseInputHandler.kt` | New | Mouse event handling |
| `terminal/AnsiEscapeParser.kt` | Modify | Add mouse mode parsing |
| `terminal/TerminalScreenBuffer.kt` | Modify | Minor refactoring for ScreenManager |
| `ui/components/TerminalCanvas.kt` | Modify | Add scroll gesture detection |
| `viewmodel/TerminalScreenModel.kt` | Modify | Wire up MouseInputHandler |

### Phase 2 Files (Future)

| File | Change Type | Description |
|------|-------------|-------------|
| `terminal/ScrollbackBuffer.kt` | New | Scrollback storage |
| `terminal/ScreenManager.kt` | New | Screen lifecycle management |
| `terminal/TerminalScreen.kt` | New | Per-screen buffer with optional scrollback |

## Testing Strategy

### Phase 1 Tests

1. **Unit Tests**
   - MouseInputHandler generates correct SGR sequences
   - AnsiEscapeParser correctly sets mouse modes
   - Mode transitions (enable/disable) work correctly

2. **Integration Tests**
   - Connect to SSH server with byobu
   - Verify scroll gestures are sent as mouse events
   - Verify byobu responds to scroll events

### Manual Test Cases

1. Launch byobu via SSH
2. Run a command with long output (e.g., `ls -la /`)
3. Use `Ctrl+b [` to enter copy mode
4. Swipe to scroll - verify scrolling works
5. Exit byobu - verify return to normal state

## Migration Path

### From Current to Phase 1

1. Add `MouseInputHandler` without modifying existing code
2. Extend `AnsiEscapeParser` with mouse mode handling
3. Update UI layer to capture scroll gestures
4. Wire components together in ViewModel

### From Phase 1 to Phase 2

1. Extract `ScreenManager` from `TerminalScreenBuffer`
2. Add `ScrollbackBuffer` class
3. Modify `MouseInputHandler` to handle local scroll
4. Add scroll position UI indicator

## Background: Mouse Reporting Technology

### History of Terminal Mouse Support

Terminal mouse support originated with **X10** compatibility mode in xterm, dating back to the early days of X Window System. The protocol has evolved through several iterations to address limitations discovered over time.

### Evolution of Mouse Protocols

```
1980s   X10 Mouse Protocol (Basic)
  │     - Button press only, no release events
  │     - Coordinates encoded as single bytes
  │
1990s   VT200/Normal Mode (1000)
  │     - Added button release events
  │     - Added modifier key support (Shift, Ctrl, Meta)
  │
2000s   Button Event (1002) / Any Event (1003)
  │     - Added motion tracking
  │     - Still limited by legacy encoding
  │
2010    UTF-8 Mode (1005) - Failed attempt
  │     - Tried to use UTF-8 for coordinates
  │     - Caused conflicts with actual UTF-8 text
  │
2010    urxvt Mode (1015) - Problematic
  │     - Used decimal encoding
  │     - Conflicted with CSI M (delete line)
  │
2012    SGR Extended Mode (1006) - Success ✓
        - Clean, unambiguous format
        - No coordinate limits
        - Widely adopted (vim, emacs, tmux, etc.)
```

### Legacy Protocol Limitations

The original X10/VT200 mouse protocol encodes coordinates as:
```
ESC [ M Cb Cx Cy

Where Cx and Cy are: (coordinate + 32) as a single byte
```

**Critical problems:**

1. **Coordinate limit of 223 columns/rows**
   - Maximum value: 255 - 32 = 223
   - Modern terminals often exceed this (e.g., 4K displays)

2. **UTF-8 encoding conflicts**
   - Coordinates above 127 create multi-byte UTF-8 sequences
   - Terminal may misinterpret mouse data as text

3. **Character set layer issues**
   - Coordinates above 95 broken by charset converters (e.g., luit)
   - Values may be transformed unexpectedly

4. **Overflow behavior**
   - Column 227 encodes to byte value 3 (ETX = Ctrl+C)
   - Can trigger unintended terminal interrupts

### Why SGR Mode (1006) Succeeded

SGR Extended Mouse Mode solved all legacy issues:

```
Legacy:  ESC [ M Cb Cx Cy        (binary, limited)
SGR:     ESC [ < Cb ; Cx ; Cy M  (decimal, unlimited)
```

**Key advantages:**

| Feature | Legacy | SGR (1006) |
|---------|--------|------------|
| Coordinate range | 1-223 | Unlimited |
| UTF-8 safe | No | Yes |
| Button release info | Ambiguous | Explicit (M/m) |
| Parsing | Complex | Simple CSI format |
| Adoption | Universal | Modern apps |

**Format details:**
- `<` after CSI clearly distinguishes from other sequences
- Semicolon-separated decimal values (no encoding tricks)
- `M` for press, `m` for release (unambiguous)
- Button codes don't add 32 (cleaner values)

### Mouse Tracking Modes Explained

| Mode | DECSET | Events Reported | Use Case |
|------|--------|-----------------|----------|
| X10 | 9 | Press only | Legacy compatibility |
| Normal | 1000 | Press + Release | Click detection |
| Button Event | 1002 | + Drag motion | Text selection |
| Any Event | 1003 | + All motion | Mouse cursor apps |

**Mode 1000 (Normal/VT200)**
```
Reports: Button press, button release, modifiers
Does NOT report: Mouse movement
Use case: Simple click handling
```

**Mode 1002 (Button Event)**
```
Reports: Everything in 1000 + motion while button held
Does NOT report: Motion without button
Use case: Drag-based text selection, drawing
```

**Mode 1003 (Any Event)**
```
Reports: All mouse events including hover
Warning: Generates heavy traffic
Use case: Mouse cursor rendering, debugging
```

### Encoding Mode Independence

Encoding modes (how data is formatted) are **independent** of tracking modes (what events are reported):

```
Tracking Mode (WHAT)     Encoding Mode (HOW)
┌─────────────────┐     ┌─────────────────┐
│ 1000: Normal    │     │ Default: Binary │
│ 1002: Button    │  ×  │ 1005: UTF-8     │
│ 1003: Any       │     │ 1006: SGR       │
└─────────────────┘     │ 1015: urxvt     │
                        └─────────────────┘
```

Example: tmux typically enables `1002` (button event) + `1006` (SGR encoding):
```
ESC[?1002h   # Enable button event tracking
ESC[?1006h   # Use SGR encoding format
```

### References

- [Xterm Control Sequences](https://www.xfree86.org/current/ctlseqs.html) - Official xterm documentation
- [xterm.js Issue #1962](https://github.com/xtermjs/xterm.js/issues/1962) - X10 protocol limitations
- [mosh PR #461](https://github.com/mobile-shell/mosh/pull/461) - Mouse mode implementation discussion

---

## Appendix: Mouse Reporting Escape Sequences

### Enable/Disable Modes

| Sequence | Description |
|----------|-------------|
| `ESC[?1000h` | Enable X10 mouse reporting |
| `ESC[?1000l` | Disable X10 mouse reporting |
| `ESC[?1002h` | Enable button event tracking |
| `ESC[?1002l` | Disable button event tracking |
| `ESC[?1003h` | Enable any event tracking |
| `ESC[?1003l` | Disable any event tracking |
| `ESC[?1006h` | Enable SGR extended mode |
| `ESC[?1006l` | Disable SGR extended mode |

### SGR Mouse Event Format

```
ESC [ < Cb ; Cx ; Cy M   (button press)
ESC [ < Cb ; Cx ; Cy m   (button release)

Cb = button code
  0   = left button
  1   = middle button
  2   = right button
  64  = scroll up
  65  = scroll down
  +4  = shift held
  +8  = meta/alt held
  +16 = control held

Cx = column (1-based)
Cy = row (1-based)
```

### Example

User scrolls up at column 10, row 5:
```
ESC[<64;10;5M
```
