# Implementation Plan: Modifier Keys Handling

**Branch**: `005-modifier-keys-handling` | **Date**: 2026-03-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-modifier-keys-handling/spec.md`

## Summary

Extend the existing keyboard input pipeline to support full xterm modifier key handling. The app already has `KeyboardInputProcessor` (pure function, commonMain) that converts key events to escape sequences, and `TerminalScreenModel` that holds `isCtrlActive`/`isAltActive` boolean toggle state. This feature adds: (1) a three-state modifier button model (inactive / one-shot / locked) with double-tap gesture, (2) a Shift toggle button, (3) combined modifier bitmask computation from all input sources, and (4) modifier-aware escape sequences for cursor keys, function keys, and special keys in the existing `KeyboardInputProcessor`.

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
| I. Atomic Design | PASS | Modifier button is an existing `FilterChip` in `MacroInputPanel` (organism). New `ModifierButtonState` only affects its `selected`/color parameters — no new hierarchy violation. |
| II. KMP Integrity | PASS | All new logic in `commonMain`: `ModifierButtonState`, `ModifierBitmask`, `KeyboardInputProcessor` extension. Double-tap gesture handled in Compose (platform-agnostic). |
| III. Theme Consistency | PASS | Three visual states use `MaterialTheme.colorScheme` tints only (primary / tertiary / surfaceVariant). No hardcoded colors. |
| IV. Unidirectional Data Flow | PASS | `TerminalState` extended with new modifier fields → flows down to `MacroInputPanel` → tap/double-tap events propagate up via callbacks to `TerminalScreenModel`. |
| V. Simplicity First | PASS | Extends three existing files (`KeyboardInputProcessor`, `TerminalState`, `MacroInputPanel`). Adds two small new files (`ModifierButtonState`, `ModifierBitmask`). No new layers or abstractions beyond minimum needed. |
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
│   ├── KeyboardInputProcessor.kt          # MODIFY — add modifier-aware sequences
│   ├── ModifierButtonState.kt             # NEW — INACTIVE / ONE_SHOT / LOCKED enum
│   └── ModifierBitmask.kt                 # NEW — combine UI state + KeyEvent modifier flags
├── viewmodel/
│   └── TerminalScreenModel.kt             # MODIFY — replace Bool flags with ModifierButtonState
└── ui/
    └── components/
        └── macro/
            └── MacroInputPanel.kt         # MODIFY — Shift button, three-state visuals, double-tap

commonTest/kotlin/tokyo/isseikuzumaki/vibeterminal/
├── input/
│   ├── KeyboardInputProcessorTest.kt      # MODIFY — add modifier+cursor/fn key test cases
│   └── ModifierBitmaskTest.kt             # NEW — test bitwise OR combination logic
```

**Structure Decision**: Single-project KMP module. All new files in `commonMain` to preserve testability. No new modules or layers needed.

## Complexity Tracking

> No constitution violations requiring justification.
