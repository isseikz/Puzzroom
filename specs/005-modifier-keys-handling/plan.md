# Implementation Plan: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-modifier-keys-handling/spec.md`
**Status**: Implemented (merged to `main` 2026-03-07)

## Summary

Extend the existing keyboard input pipeline to support full xterm modifier key handling. The app already has `KeyboardInputProcessor` (pure function, commonMain) that converts key events to escape sequences, and `TerminalScreenModel` that holds `isCtrlActive`/`isAltActive` boolean toggle state. This feature adds: (1) a three-state modifier button model (inactive / one-shot / locked) with double-tap gesture, (2) a Shift toggle button, (3) combined modifier bitmask computation from all input sources, (4) modifier-aware escape sequences for cursor keys, function keys, and special keys in the existing `KeyboardInputProcessor`, and (5) a `TerminalInputAggregator` that serves as the single unified dispatch point for all UI input sources.

### Architectural Deviation from Original Plan

During implementation a `TerminalInputAggregator` class was added (not in the original plan) to centralise input dispatch and eliminate duplicated modifier-application logic that would otherwise be scattered across `sendInput()`, `sendFixedKey()`, and the secondary-display / hardware-keyboard callbacks. The aggregator exposes two entry points:

- **`send(rawSequence)`** — for UI sources (IME, FixedKeyRow, macro buttons): applies the current modifier state before dispatching.
- **`dispatch(resolvedSequence)`** — for pre-resolved sources (hardware keyboard via `KeyboardInputProcessor`): skips modifier application to prevent double-encoding, but still consumes ONE_SHOT modifier state so on-screen buttons reset correctly.

This addition is consistent with Constitution principles V (Simplicity First — removes scattered duplication) and VII (Clean Architecture — modifier resolution stays in the `input/` layer).

## Technical Context

**Language/Version**: Kotlin 2.x (Multiplatform)
**Primary Dependencies**: Compose Multiplatform, Voyager ScreenModel, Koin DI
**Storage**: N/A (no persistence for modifier state — ephemeral UI state)
**Testing**: kotlin.test (commonTest, runs on JVM without Android)
**Target Platform**: Android primary; iOS/Desktop/JVM targets exist via KMP
**Project Type**: Single KMP module (`mobile-vibe-terminal`)
**Performance Goals**: Key-to-sequence conversion < 5ms; haptic/visual feedback < 50ms
**Constraints**: All escape sequence logic MUST remain in `commonMain` with no Android framework imports (NFR-5.4 / Constitution VII)
**Scale/Scope**: Modifier state is per-session (in-memory); no persistence required

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Atomic Design | PASS | Modifier button replaced `FilterChip` with a custom `Box+combinedClickable` surface in `MacroInputPanel`. Required to resolve a Compose gesture conflict where `FilterChip`'s internal `Clickable` consumed double-tap events before the outer handler. No hierarchy violation. |
| II. KMP Integrity | PASS | All new logic in `commonMain`: `ModifierButtonState`, `ModifierBitmask`, `KeyboardInputProcessor` extension. Double-tap gesture handled in Compose (platform-agnostic). |
| III. Theme Consistency | PASS | Three visual states use `MaterialTheme.colorScheme` tints only (primary / tertiary / surfaceVariant). No hardcoded colors. |
| IV. Unidirectional Data Flow | PASS | `TerminalState` extended with new modifier fields → flows down to `MacroInputPanel` → tap/double-tap events propagate up via callbacks to `TerminalScreenModel`. |
| V. Simplicity First | PASS | Extends three existing files (`KeyboardInputProcessor`, `TerminalState`, `MacroInputPanel`). Adds three small new files (`ModifierButtonState`, `ModifierBitmask`, `TerminalInputAggregator`). The aggregator eliminates scattered duplication across four separate input callbacks — net reduction in complexity. |
| VI. Worktree-First | PENDING | Worktree `worktrees/005-modifier-keys-handling/` must be created before code work begins. |
| VII. Clean Architecture | PASS | Escape sequence conversion stays in `input/` (domain-adjacent pure logic). `TerminalScreenModel` (ViewModel) owns modifier UI state. `MacroInputPanel` (UI) renders state and emits events. |

## Project Structure

### Documentation (this feature)

```text
specs/005-modifier-keys-handling/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code

```text
mobile-vibe-terminal/src/

commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
├── input/
│   ├── KeyEventData.kt                    # EXISTING — no change
│   ├── KeyCodes.kt                        # EXISTING — no change
│   ├── KeyboardInputProcessor.kt          # MODIFY — modifier-aware sequences; getCtrlSequenceForChar()
│   ├── ModifierButtonState.kt             # NEW — INACTIVE/ONE_SHOT/LOCKED enum + nextOnTap()/nextOnDoubleTap()
│   ├── ModifierBitmask.kt                 # NEW — combine UI state + KeyEvent modifier flags
│   └── TerminalInputAggregator.kt         # NEW — unified dispatch: send() for UI, dispatch() for HW keyboard
├── terminal/
│   └── TerminalStateProvider.kt           # MODIFY — add onSecondaryDisplayInput / onHardwareKeyboardInput callbacks
├── viewmodel/
│   └── TerminalScreenModel.kt             # MODIFY — ModifierButtonState fields; wires inputAggregator
└── ui/
    └── components/
        └── macro/
            └── MacroInputPanel.kt         # MODIFY — Shift button; ModifierChip replaced FilterChip
                                           #          with Box+combinedClickable for double-tap support

commonTest/kotlin/tokyo/isseikuzumaki/vibeterminal/
├── input/
│   ├── KeyboardInputProcessorTest.kt      # MODIFY — modifier+cursor/fn key test cases
│   ├── ModifierBitmaskTest.kt             # NEW — bitwise OR combination logic
│   ├── ModifierButtonStateTest.kt         # NEW — three-state transitions, double-tap platform quirk
│   └── TerminalInputAggregatorTest.kt     # NEW — applyModifierToChar/Sequence, send()/dispatch() side-effects
```

**Structure Decision**: Single-project KMP module. All new files in `commonMain` to preserve testability. No new modules or layers needed.

### Input Dispatch Architecture (as implemented)

```
UI Input Sources
  ├── IME (software keyboard text)
  ├── FixedKeyRow (Tab, ESC, arrow keys, etc.)
  ├── MacroButtonGrid (macro sequences)
  └── Secondary Display
        │
        ▼  inputAggregator.send(rawSequence)
  ┌─────────────────────────────────────────┐
  │         TerminalInputAggregator         │
  │                                         │
  │  send()   ─ applyModifiers()            │
  │             │                           │
  │             ├─ length==1 && char!='\t'  │
  │             │   → applyModifierToChar() │
  │             └─ else                     │
  │                 → applyModifierToSequence()
  │                                         │
  │  dispatch() ─ skip modifier application │
  │  (hardware keyboard, pre-resolved)      │
  │                                         │
  │  Both paths: consumeOneShotModifiers()  │
  │              onDispatch(sequence)       │
  └─────────────────────────────────────────┘
        │
        ▼  SshRepository.sendInput(sequence)

Hardware Keyboard
  └── KeyboardInputProcessor.processKeyEvent()
        │  (already applies combined bitmask)
        ▼  inputAggregator.dispatch(resolvedSequence)
```

## Complexity Tracking

> No constitution violations requiring justification.
