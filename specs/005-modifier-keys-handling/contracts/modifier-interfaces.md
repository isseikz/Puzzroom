# Contracts: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06

This feature has no REST/GraphQL API. Contracts are Kotlin function signatures and their behavioral guarantees.

---

## `ModifierBitmask.combine()`

```kotlin
fun combine(
    keyEventData: KeyEventData,
    shiftState: ModifierButtonState,
    ctrlState: ModifierButtonState,
    altState: ModifierButtonState
): Int
```

**Contract**:
- Returns bitwise OR of all active modifier sources
- `shiftState/ctrlState/altState` in `ONE_SHOT` or `LOCKED` contribute their bit
- `INACTIVE` contributes 0
- Hardware flags from `keyEventData` (`isShiftPressed`, `isCtrlPressed`, `isAltPressed`, `isMetaPressed`) also contribute their bits
- Duplicate sources for the same modifier do not double-count (idempotent OR)
- Result is always in range `[0, 15]`

**Examples**:
| keyEventData | UI state | Result |
|-------------|----------|--------|
| no modifiers | Ctrl ONE_SHOT | 4 |
| Shift pressed | Ctrl ONE_SHOT | 5 (1 OR 4) |
| Shift pressed | Shift ONE_SHOT | 1 (not 2, idempotent) |
| no modifiers | no modifiers | 0 |

---

## `ModifierBitmask.toModifierNumber()`

```kotlin
fun toModifierNumber(bitmask: Int): Int
```

**Contract**:
- Returns `bitmask + 1` for `bitmask > 0` (xterm convention)
- Returns `0` for `bitmask == 0` (sentinel: no modifier parameter in sequence)

---

## `KeyboardInputProcessor.processKeyEvent()`

```kotlin
fun processKeyEvent(
    event: KeyEventData,
    uiModifierBitmask: Int = 0
): KeyResult
```

**Contract**:
- `action == UP` always returns `Ignored` (unchanged)
- Modifier-only key codes always return `Ignored` (unchanged)
- Combined bitmask = `(hardware flags from event)` OR `uiModifierBitmask`
- **All special key + modifier resolution uses the combined bitmask** — not just `event.isShiftPressed` etc.

**Cursor keys** (Up/Down/Left/Right/Home/End):
- combined bitmask > 0 → `Handled("ESC[1;<modNum><dir>")`
- combined bitmask == 0 → `Handled("ESC[<dir>")` (unchanged)

**Function keys** (F1–F12):
- combined bitmask > 0 → `Handled("ESC[<keyNum>;<modNum>~")` (F1–F4 use key numbers 11–14)
- combined bitmask == 0 → unchanged (F1–F4: SS3; F5–F12: CSI tilde)

**Special keys with modifier** (uses combined bitmask for all modifier bits):
- Tab, Shift bit set → `Handled("\u001B[Z")` (backtab)
- Tab, no Shift → `Handled("\t")` (unchanged)
- Backspace (DEL), Ctrl bit set → `Handled("\u0008")`
- Backspace (DEL), no Ctrl → `Handled("\u007F")` (unchanged)
- Insert/Delete/PageUp/PageDown, any modifier → `Handled("ESC[<keyNum>;<modNum>~")`
- Insert/Delete/PageUp/PageDown, no modifier → unchanged (`ESC[2~`, `ESC[3~`, etc.)

**Printable characters** (priority order — first matching rule wins):
- Alt bit + Ctrl bit set (Shift ignored) → `Handled("\u001B" + ctrlByte)`
- Ctrl bit set (Shift ignored for A–Z) → `Handled(ctrlByte)`
  - e.g. Ctrl+Shift+V → `\x16` (same as Ctrl+V; Shift does not alter control characters)
- Alt bit set, no Ctrl → `Handled("\u001B" + char)` where char reflects Shift state
  - e.g. Alt+Shift+A → `\u001BA`; Alt+A → `\u001Ba`
- No Ctrl, no Alt → `PassThrough`

---

## `TerminalScreenModel` — modifier toggle events

```kotlin
fun onModifierTap(modifier: ModifierKey)
// Single tap: INACTIVE → ONE_SHOT; ONE_SHOT → INACTIVE; LOCKED → INACTIVE

fun onModifierDoubleTap(modifier: ModifierKey)
// Double tap: INACTIVE → LOCKED; (other states behave as single tap to INACTIVE)

enum class ModifierKey { SHIFT, CTRL, ALT }
```

**Contract**:
- After **any** key is sent (regardless of whether the modifier affected the output): all `ONE_SHOT` modifiers become `INACTIVE` — no state leaks
- `LOCKED` modifiers are not cleared by key sends
- Double-tap supersedes single-tap in the same gesture sequence

---

## `MacroInputPanel` — new parameters

```kotlin
@Composable
fun MacroInputPanel(
    // ... existing params ...
    shiftState: ModifierButtonState,    // NEW
    ctrlState: ModifierButtonState,     // REPLACES isCtrlActive: Boolean
    altState: ModifierButtonState,      // REPLACES isAltActive: Boolean
    onModifierTap: (ModifierKey) -> Unit,       // REPLACES onToggleCtrl/onToggleAlt
    onModifierDoubleTap: (ModifierKey) -> Unit, // NEW
    // ...
)
```

**Visual contract**:
| State | Container color |
|-------|----------------|
| INACTIVE | `MaterialTheme.colorScheme.surfaceVariant` |
| ONE_SHOT | `MaterialTheme.colorScheme.primary` |
| LOCKED | `MaterialTheme.colorScheme.tertiary` |

**Haptic contract**:
- Every tap on a modifier button (single or double) triggers `LocalHapticFeedback.performHapticFeedback()` synchronously within the `pointerInput` callback
- Feedback fires within 50ms of touch (FR-027)

---

## `KeyboardInputProcessor.getCtrlSequenceForChar()` (NEW)

```kotlin
fun getCtrlSequenceForChar(char: Char): String?
```

**Contract**:
- Maps a printable character to its Ctrl control byte
- Covers letters `a`–`z` / `A`–`Z` → `\u0001`–`\u001A`
- Covers symbols: `[` → `\u001B`, `\` → `\u001C`, `]` → `\u001D`, `6` → `\u001E`, `-` → `\u001F`, ` ` (space) → `\u0000`
- Returns `null` for characters with no defined Ctrl mapping
- Used by `sendInput()` IME path as a char-based alternative to the keyCode-based `getCtrlKeySequence()`
- Must produce identical output to `getCtrlKeySequence()` for overlapping inputs
