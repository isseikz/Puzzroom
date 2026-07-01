# Research: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06

## 1. Existing Codebase State

### What already exists

| Component | File | State |
|-----------|------|-------|
| Platform-independent key model | `input/KeyEventData.kt` | Complete — already has all 4 modifier flags |
| Escape sequence converter | `input/KeyboardInputProcessor.kt` | Partial — handles Ctrl+letter, Alt+char, special keys; **missing modifier+cursor/fn keys** |
| Android keyboard adapter | `input/HardwareKeyboardHandler.kt` | Complete — converts `android.view.KeyEvent` to `KeyEventData`, delegates to `KeyboardInputProcessor` |
| Modifier toggle UI | `ui/components/macro/MacroInputPanel.kt` | Partial — Ctrl and Alt `FilterChip` buttons exist; **Shift missing; only binary state (no lock mode)** |
| ViewModel modifier state | `viewmodel/TerminalScreenModel.kt` | Partial — `isCtrlActive: Boolean` + `isAltActive: Boolean`; **Shift missing; no lock mode; `sendInput()` only handles single-char modifier escaping** |
| Unit tests | `commonTest/input/KeyboardInputProcessorTest.kt` | Comprehensive for existing behavior; **no modifier+cursor/fn key tests** |

### Gaps to close (complete list from thorough analysis)

**Category A — Completely missing:**

1. **Shift toggle button** — absent from `MacroInputPanel`, `TerminalState`, `TerminalScreenModel` (FR-001)
2. **Three-state visual** — `FilterChip` is binary `selected: Boolean` only (FR-002, FR-006)
3. **Double-tap / lock mode** — no gesture detector, no `LOCKED` state (FR-005)
4. **Source merging** — UI button state and `KeyEvent` flags are separate pipelines, never OR-ed (FR-008, FR-010, FR-011)
5. **Modified cursor key sequences** — `getSpecialKeySequence` returns fixed sequences, modifiers ignored (FR-016)
6. **Modified function key sequences** — same; F1–F12 return fixed sequences, modifiers ignored (FR-019)
7. **Ctrl+Backspace → `0x08`** — `getSpecialKeySequence` intercepts `DEL` key and returns before Ctrl check is reached (FR-022)
8. **Modified Insert/Delete/PgUp/PgDn** — fixed sequences only (FR-023)
9. **Meta bitmask** — `isMetaPressed` carried in `KeyEventData` but never contributes to any sequence (EC-6.2)

**Category B — Partially broken:**

10. **UI modifier state ignored by hardware path** — `HardwareKeyboardHandler` passes only `KeyEvent` flags to `KeyboardInputProcessor`; `isCtrlActive`/`isAltActive` from `TerminalState` never read (FR-003, FR-008)
11. **One-shot state leak** — `sendInput()` only clears modifier when `modified = true`; if Ctrl is active but char is not a letter, `modified` stays `false`, modifier not consumed and bleeds into next key press (FR-004)
12. **`sendInput()` Ctrl only handles letters** — `char in 'a'..'z'` misses Ctrl+symbols (`[`, `\`, `-`, space, `6`) that `getCtrlKeySequence` handles on the hardware path (FR-012)
13. **Alt+Ctrl+char fails on hardware path** — `processKeyEvent` returns early from Ctrl branch before reaching Alt check; ESC prefix never prepended (FR-015)
14. **Shift+Tab uses hardware-only Shift** — `getSpecialKeySequence(TAB, hasShift = event.isShiftPressed)` ignores UI Shift button (FR-021)
15. **Haptic feedback not triggered** — `FilterChip` gives visual state change but no explicit `HapticFeedback` call (FR-027)

**Category C — Intentionally deferred:**

16. **DECCKM application cursor mode** — not tracked; normal-mode sequences already correct; deferred (FR-017)
17. **Escape sequence mapping extensibility** — hard-coded `when` blocks; deferred (FR-029)

---

## 2. Modifier Combination Strategy

**Decision**: Compute combined modifier bitmask in `TerminalScreenModel.sendInput()` and pass it into `KeyboardInputProcessor` via an updated signature.

**Rationale**: `KeyboardInputProcessor` is a pure object — it should remain stateless. The ViewModel already owns modifier toggle state. The combination (bitwise OR of `KeyEventData` flags + UI button bits) happens at the call site before delegating to the processor.

**Alternative considered**: Inject modifier state into `KeyboardInputProcessor` directly. Rejected — it would make the processor stateful and harder to test in isolation.

**Bitmask convention** (xterm standard):
```
Shift  = 1  → modifier number = 2
Alt    = 2  → modifier number = 3
Ctrl   = 4  → modifier number = 5
Meta   = 8  → modifier number = 9
Combined = (bitmask sum) + 1
```

---

## 3. Modifier-Aware Escape Sequence Patterns

**Decision**: Add an overload / new parameter to `KeyboardInputProcessor.processKeyEvent()` accepting a combined `modifierBitmask: Int` (0 = no modifiers from UI). The hardware-derived modifier flags in `KeyEventData` are OR-ed with the UI bitmask before processing.

**Critical fix — dispatch ordering**: The current `processKeyEvent` calls `getSpecialKeySequence` first and returns immediately on match. This means cursor keys, Backspace, Insert, Delete, PgUp, PgDn all exit before Ctrl/Alt checks are reached — even with a physical keyboard modifier held. This is the root cause of gaps 5, 7, and 8.

Fix: `getSpecialKeySequence` must receive the combined bitmask and use it to decide which sequence to return — modifier-aware or plain.

### Cursor keys with modifiers
```
Format: ESC [ 1 ; <modifier_number> <direction>
ESC [ A     = Up (no modifier)
ESC [ 1;2A  = Shift+Up
ESC [ 1;5C  = Ctrl+Right
ESC [ 1;6A  = Shift+Ctrl+Up (bitmask 1+4=5, number=6)
```

### Function keys with modifiers
```
Format: ESC [ <key_num> ; <modifier_number> ~
ESC [ 15;2~  = Shift+F5
ESC [ 17;5~  = Ctrl+F6
F1-F4 unmodified: ESC O P/Q/R/S (SS3)
F1-F4 modified: ESC [ 11;2~ etc. (CSI, not SS3)
```

### Ctrl+Backspace
```
0x08  (BS character — distinct from plain Backspace which sends 0x7F)
```

### Alt+Ctrl+character
```
ESC + control_byte
e.g. Alt+Ctrl+C = ESC 0x03
```

---

## 4. Three-State Modifier Button Design

**Decision**: Enum `ModifierButtonState { INACTIVE, ONE_SHOT, LOCKED }` in commonMain.

**State transitions**:
```
INACTIVE     --single tap-->  ONE_SHOT
INACTIVE     --double tap-->  LOCKED
ONE_SHOT     --key press-->   INACTIVE  (auto-deactivate after use)
ONE_SHOT     --single tap-->  INACTIVE  (cancel)
LOCKED       --single tap-->  INACTIVE  (unlock)
```

**TerminalState changes**:
- Remove: `isCtrlActive: Boolean`, `isAltActive: Boolean`
- Add: `ctrlState: ModifierButtonState`, `altState: ModifierButtonState`, `shiftState: ModifierButtonState`

All defaulting to `INACTIVE`.

**Visual mapping** (using theme colors, no hardcoded values):
| State | Visual |
|-------|--------|
| INACTIVE | `surfaceVariant` background |
| ONE_SHOT | `primary` background (same as current active) |
| LOCKED | `tertiary` background (distinct from one-shot) |

---

## 5. Double-Tap Gesture in Compose

**Decision**: Use Compose `detectTapGestures` with `onDoubleTap` callback via `Modifier.pointerInput`. Wrap the `FilterChip` in a `Box` with the gesture detector overlaid, or replace `FilterChip.onClick` with custom gesture handling.

**Rationale**: `FilterChip` only exposes a single `onClick`. To distinguish single-tap from double-tap without a settings flag, we need `pointerInput`. This is platform-agnostic in Compose Multiplatform.

**Alternative considered**: Long-press for lock mode. Rejected — long-press has a ~500ms delay which feels sluggish; double-tap is faster and more discoverable for this use case.

---

## 6. `sendInput()` Extension for Modifier-Aware Processing

**Current behavior**: `sendInput(text: String)` applies Ctrl/Alt to single printable characters only. Cursor key sequences (e.g., `\x1b[A`) are sent directly from `getSpecialKeySequence()` without modifier participation from the UI toggle buttons.

**Required behavior**: When the UI modifier buttons are active, `KeyboardInputProcessor` must see the combined bitmask. This means the hardware keyboard path (`HardwareKeyboardHandler`) must also pass the UI modifier state.

**Decision**: Pass `uiModifierBitmask: Int` into `KeyboardInputProcessor.processKeyEvent()`. `TerminalScreenModel` computes this from current `TerminalState`. `HardwareKeyboardHandler` receives it as a parameter from `MainActivity`/`TerminalScreen`.

**Fix 1 — Ctrl symbols in IME path**: `sendInput()` currently checks `char in 'a'..'z'` which misses Ctrl+symbols. Solution: add `KeyboardInputProcessor.getCtrlSequenceForChar(char: Char): String?` that maps printable characters (not key codes) to control bytes, covering both letters and symbols. `sendInput()` delegates to this function instead of inline letter-only logic.

**Fix 2 — One-shot state leak in IME path**: `sendInput()` only clears modifier state when `modified = true`. ONE_SHOT modifiers must be consumed after every key press regardless of whether the modifier had an effect. Solution: call `applyAndConsumeOneShotModifiers()` unconditionally when any modifier is in ONE_SHOT state at the time of the key press.

**Fix 3 — Alt+Ctrl+char on hardware path**: Current `processKeyEvent` returns from Ctrl branch before checking Alt. Fix: when combined bitmask has both Ctrl and Alt bits set, prepend ESC before the control byte.

---

## 7. Resolved Clarifications

| Item | Resolution |
|------|-----------|
| DECCKM tracking | Deferred — modifier keys always use CSI format (FR-018), unmodified cursor keys already correct for normal mode. Not a blocker. |
| Shift toggle button position | Placed alongside Ctrl and Alt in the existing Controls Row of `MacroInputPanel` |
| `sendInput()` backward compat | Existing callers sending pre-formed sequences (macros, SSH output) are unaffected — modifier escaping only applies to `KeyboardInputProcessor`-routed paths |
