# Quickstart: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06

## Setup

```bash
# The feature branch already exists; simply check it out (worktree not needed)
git checkout 005-modifier-keys-handling
```

> **Note**: `git worktree add` cannot create a worktree for a branch already checked out in the main repo. All work was done directly in the main repo on the feature branch. Branch isolation is maintained via normal git checkout.

## Run Tests

```bash
# Run Android unit tests (confirmed working — no emulator needed)
./gradlew :mobile-vibe-terminal:testDebugUnitTest

# To force a clean rebuild before running tests:
./gradlew :mobile-vibe-terminal:testDebugUnitTest --rerun-tasks
```

> **Note**: `cleanJvmTest` / `jvmTest` tasks do not exist in this project. The correct task is `testDebugUnitTest`. The `desktopTest` target has pre-existing compile failures in `MacroInputPanel.kt` unrelated to this feature (dependency `io.github.isseikz.kmpinput` not available on Desktop).

The entire modifier key logic lives in `commonMain` and is testable via `testDebugUnitTest`. No Android emulator or device required for unit tests.

## Key Files

| File | Role |
|------|------|
| `input/KeyboardInputProcessor.kt` | Add modifier-aware CSI sequences here |
| `input/ModifierButtonState.kt` | New enum: INACTIVE / ONE_SHOT / LOCKED |
| `input/ModifierBitmask.kt` | New: combine UI state + KeyEvent flags |
| `viewmodel/TerminalScreenModel.kt` | Replace Bool flags with ModifierButtonState |
| `ui/components/macro/MacroInputPanel.kt` | Add Shift, three-state visuals, double-tap |
| `commonTest/input/KeyboardInputProcessorTest.kt` | Add modifier+cursor/fn key tests |
| `commonTest/input/ModifierBitmaskTest.kt` | New: test combine() and toModifierNumber() |

## Implementation Order

1. `ModifierButtonState.kt` — trivial enum, no dependencies
2. `ModifierBitmask.kt` + `ModifierBitmaskTest.kt` — pure logic, test-driven
3. `KeyboardInputProcessor.kt` (modifier-aware sequences) + extended tests
4. `TerminalScreenModel.kt` — state model update + toggle/consume logic
5. `MacroInputPanel.kt` — UI: Shift button, three-state colors, double-tap gesture

## Modifier Bitmask Quick Reference

| Modifier | Bit | xterm number |
|----------|-----|-------------|
| Shift | 1 | 2 |
| Alt | 2 | 3 |
| Ctrl | 4 | 5 |
| Meta | 8 | 9 |
| Shift+Ctrl | 5 | 6 |
| Shift+Alt+Ctrl | 7 | 8 |

`modifier_number = bitmask + 1`

## Escape Sequence Quick Reference

```
Cursor + mod:     ESC [ 1 ; <modNum> {A|B|C|D|H|F}
Function + mod:   ESC [ <keyNum> ; <modNum> ~
  F1..F4 key numbers (modified only): 11, 12, 13, 14
  F5..F12 key numbers: 15, 17, 18, 19, 20, 21, 23, 24
Ctrl+Backspace:   0x08
Alt+Ctrl+char:    ESC + (char - 0x40)   e.g. Alt+Ctrl+C = ESC 0x03
```

## Visual State Colors (theme tokens only)

```kotlin
val containerColor = when (state) {
    ModifierButtonState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
    ModifierButtonState.ONE_SHOT -> MaterialTheme.colorScheme.primary
    ModifierButtonState.LOCKED   -> MaterialTheme.colorScheme.tertiary
}
```
