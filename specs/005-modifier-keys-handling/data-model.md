# Data Model: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06

## Entities

### `ModifierButtonState` (NEW — commonMain)

```
Package: tokyo.isseikuzumaki.vibeterminal.input

Enum values:
  INACTIVE  — button is off; modifier not applied
  ONE_SHOT  — button active for next key only; auto-deactivates after use
  LOCKED    — button active until user taps again to deactivate

State machine:
  INACTIVE  --single tap-->  ONE_SHOT
  INACTIVE  --double tap-->  LOCKED
  ONE_SHOT  --key press-->   INACTIVE
  ONE_SHOT  --single tap-->  INACTIVE   (cancel before key)
  LOCKED    --single tap-->  INACTIVE
```

### `ModifierBitmask` (NEW — commonMain)

```
Package: tokyo.isseikuzumaki.vibeterminal.input

Responsibilities:
  - Combine UI button states + KeyEvent hardware modifier flags
  - Compute xterm modifier number from bitmask

Bit assignments (xterm convention):
  Shift = 1
  Alt   = 2
  Ctrl  = 4
  Meta  = 8

Key function:
  fun combine(
    keyEventData: KeyEventData,
    shiftState: ModifierButtonState,
    ctrlState: ModifierButtonState,
    altState: ModifierButtonState
  ): Int
  // Returns bitwise OR of hardware flags and active UI button bits
  // 0 = no modifiers

  fun toModifierNumber(bitmask: Int): Int
  // Returns bitmask + 1 (xterm convention)
  // 0 bitmask → returns 0 (used as sentinel: "no modifier parameter")
```

### `TerminalState` (MODIFY — existing in `viewmodel/TerminalScreenModel.kt`)

```
Fields removed:
  isCtrlActive: Boolean
  isAltActive: Boolean

Fields added:
  shiftState: ModifierButtonState = ModifierButtonState.INACTIVE
  ctrlState:  ModifierButtonState = ModifierButtonState.INACTIVE
  altState:   ModifierButtonState = ModifierButtonState.INACTIVE
```

### `KeyEventData` (EXISTING — no change)

```
Package: tokyo.isseikuzumaki.vibeterminal.input

Fields (unchanged):
  keyCode: Int
  action: KeyAction
  isCtrlPressed: Boolean
  isAltPressed: Boolean
  isShiftPressed: Boolean
  isMetaPressed: Boolean
```

### `KeyboardInputProcessor` (MODIFY — existing)

```
Package: tokyo.isseikuzumaki.vibeterminal.input

Signature changes:

  // PRIMARY ENTRY POINT — unchanged name, new optional param
  fun processKeyEvent(
    event: KeyEventData,
    uiModifierBitmask: Int = 0   // NEW — from UI toggle buttons
  ): KeyResult
  // Combined bitmask = (hardware flags from event) OR uiModifierBitmask
  // All dispatch uses combined bitmask, not event flags directly

  // INTERNAL — signature change
  OLD: getSpecialKeySequence(keyCode: Int, hasShift: Boolean): String?
  NEW: getSpecialKeySequence(keyCode: Int, combinedBitmask: Int): String?
  // hasShift = (combinedBitmask and 1) != 0
  // hasCtrl  = (combinedBitmask and 4) != 0
  // Dispatch ordering fix: modifier-aware sequences returned here,
  // no early return that bypasses modifier checks

  // NEW helper — for IME/sendInput() path (char-based, not keyCode-based)
  fun getCtrlSequenceForChar(char: Char): String?
  // Maps printable char → control byte
  // Covers letters (a-z → 0x01–0x1A) AND symbols ([ → 0x1B, \ → 0x1C, etc.)
  // Returns null for chars with no defined ctrl mapping

Full dispatch table for getSpecialKeySequence(keyCode, combinedBitmask):
  Tab + Shift bit          → \x1b[Z  (backtab)
  Tab, no Shift            → \t
  Backspace + Ctrl bit     → \x08
  Backspace, no Ctrl       → \x7F
  Cursor + modifier        → ESC [ 1 ; <modNum> <dir>
  Cursor, no modifier      → ESC [ <dir>  (unchanged)
  Insert/Delete/PgUp/PgDn + modifier → ESC [ <keyNum> ; <modNum> ~
  Insert/Delete/PgUp/PgDn, no modifier → ESC [ <keyNum> ~  (unchanged)
  F1–F4 + modifier         → ESC [ 11–14 ; <modNum> ~  (CSI, not SS3)
  F5–F12 + modifier        → ESC [ <keyNum> ; <modNum> ~
  F1–F4, no modifier       → ESC O P/Q/R/S  (SS3, unchanged)
  F5–F12, no modifier      → ESC [ <keyNum> ~  (unchanged)

Printable character dispatch (after special key check):
  Alt bit + Ctrl bit set   → ESC + ctrlByte  (Alt+Ctrl+char fix)
  Ctrl bit set             → ctrlByte  (unchanged, now uses combined bitmask)
  Alt bit set              → ESC + char  (unchanged, now uses combined bitmask)
  No modifier              → PassThrough
```

### `TerminalScreenModel.sendInput()` (MODIFY — existing)

```
Current problems fixed:
  1. Ctrl only handles 'a'..'z' — extended via getCtrlSequenceForChar(char)
  2. ONE_SHOT state leak — applyAndConsumeOneShotModifiers() called
     unconditionally whenever any modifier is in ONE_SHOT state at time of key press

New logic (IME single-char path):
  val combinedHasCtrl = ctrlState.isActive   // ONE_SHOT or LOCKED
  val combinedHasAlt  = altState.isActive
  val combinedHasShift = shiftState.isActive

  if (combinedHasCtrl && combinedHasAlt) {
    val ctrlByte = getCtrlSequenceForChar(char) ?: toSend
    toSend = "\u001B" + ctrlByte
  } else if (combinedHasCtrl) {
    toSend = getCtrlSequenceForChar(char) ?: toSend
  } else if (combinedHasAlt) {
    toSend = "\u001B" + toSend
  }

  // Always consume ONE_SHOT modifiers after any key press
  applyAndConsumeOneShotModifiers()
```

## State Transitions (TerminalScreenModel)

```
onModifierTap(modifier):
  INACTIVE + SINGLE_TAP → ONE_SHOT
  ONE_SHOT + SINGLE_TAP → INACTIVE  (cancel)
  LOCKED   + SINGLE_TAP → INACTIVE  (unlock)

onModifierDoubleTap(modifier):
  INACTIVE + DOUBLE_TAP → LOCKED
  (other states → INACTIVE, same as single tap)

applyAndConsumeOneShotModifiers():
  // Called UNCONDITIONALLY after every key press (regardless of whether modifier affected output)
  // This prevents ONE_SHOT state from leaking to subsequent key presses
  for each modifier in [shift, ctrl, alt]:
    if state == ONE_SHOT → set to INACTIVE
    if state == LOCKED   → keep LOCKED

ModifierButtonState.isActive: Boolean
  // true for ONE_SHOT and LOCKED; false for INACTIVE
  // Used by sendInput() and uiModifierBitmask computation
```

## Haptic Feedback

```
Location: MacroInputPanel — modifier button touch handler
Trigger: every tap on a modifier button (single-tap and double-tap)
API: LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)
     or equivalent light tick available in Compose Multiplatform
Timing: within 50ms of touch (FR-027) — synchronous with pointerInput callback
```

## Validation Rules

- `uiModifierBitmask` is always in range `[0, 15]` (4 modifier bits)
- Combined bitmask is computed at key-down time only (FR-3.3.3)
- `ModifierButtonState.LOCKED` is not auto-cleared by key events
- `ModifierButtonState.ONE_SHOT` is always consumed after any key press, even if the modifier had no effect on the output
- A `KeyEventData` with `action = UP` is always ignored before bitmask combination
- `getCtrlSequenceForChar` must cover the same symbol mappings as `getCtrlKeySequence` (letters + `[`, `\`, `]`, `6`, `-`, space)
