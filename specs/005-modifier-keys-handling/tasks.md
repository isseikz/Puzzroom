# Tasks: Modifier Keys Handling

**Input**: Design documents from `/specs/005-modifier-keys-handling/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no open dependencies)
- **[Story]**: User story this task belongs to (US1–US5)
- Exact file paths included in all task descriptions

## Path Prefix

All Kotlin files live under:
```
mobile-vibe-terminal/src/
  commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
  androidMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
  commonTest/kotlin/tokyo/isseikuzumaki/vibeterminal/
```
Abbreviated below as `[common]`, `[android]`, `[test]`.

---

## Phase 1: Setup

**Purpose**: Isolate feature work in a dedicated worktree per Constitution VI.

- [x] T001 Create git worktree `worktrees/005-modifier-keys-handling/` and symlink `local.properties`: `git worktree add worktrees/005-modifier-keys-handling 005-modifier-keys-handling && ln -s ../../local.properties worktrees/005-modifier-keys-handling/local.properties` — NOTE: Main repo is already checked out on `005-modifier-keys-handling`; separate worktree cannot be created for an already-checked-out branch. All work proceeds in the main repo directory on the feature branch.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data model and signature changes that ALL user stories depend on. No US work can begin until this phase is complete.

**⚠️ CRITICAL**: These tasks establish the shared foundation. Complete all before Phase 3.

- [x] T002 Create `ModifierButtonState` enum (INACTIVE, ONE_SHOT, LOCKED) with `isActive: Boolean` computed property in `[common]input/ModifierButtonState.kt`
- [x] T003 [P] Create `ModifierBitmask` object with `combine(keyEventData, shiftState, ctrlState, altState): Int` and `toModifierNumber(bitmask): Int` in `[common]input/ModifierBitmask.kt`
- [x] T004 [P] Create `ModifierBitmaskTest` with tests for `combine()` (single-source, dual-source, duplicate-source, Meta bit) and `toModifierNumber()` in `[test]input/ModifierBitmaskTest.kt`
- [x] T005 Update `TerminalState` in `[common]viewmodel/TerminalScreenModel.kt`: remove `isCtrlActive: Boolean` and `isAltActive: Boolean`; add `shiftState`, `ctrlState`, `altState: ModifierButtonState = INACTIVE`; update `equals()` and `hashCode()` accordingly
- [x] T006 Add `enum class ModifierKey { SHIFT, CTRL, ALT }` to `[common]input/ModifierButtonState.kt` (alongside `ModifierButtonState`, keeps it in the domain-adjacent input layer so UI does not import from ViewModel); replace `toggleCtrl()` and `toggleAlt()` in `TerminalScreenModel` with `onModifierTap(modifier: ModifierKey)` and `onModifierDoubleTap(modifier: ModifierKey)` in `[common]viewmodel/TerminalScreenModel.kt`
- [x] T007 Add private `applyAndConsumeOneShotModifiers()` to `TerminalScreenModel` that unconditionally sets all ONE_SHOT modifiers to INACTIVE (LOCKED untouched); add private `computeUiModifierBitmask(): Int` that OR-s active UI modifier bits in `[common]viewmodel/TerminalScreenModel.kt`
- [x] T008 Update `processKeyEvent` signature in `KeyboardInputProcessor` to `processKeyEvent(event: KeyEventData, uiModifierBitmask: Int = 0)` and change `getSpecialKeySequence` signature from `(keyCode: Int, hasShift: Boolean)` to `(keyCode: Int, combinedBitmask: Int)` — stubs returning existing behavior to keep tests green in `[common]input/KeyboardInputProcessor.kt`

**Checkpoint**: Run `./gradlew :mobile-vibe-terminal:jvmTest` — all existing tests must pass before proceeding.

---

## Phase 3: User Story 2 — On-Screen Modifier Toggle Buttons (Priority: P1) 🎯 MVP

**Goal**: Ctrl, Alt, and Shift toggle buttons with three visual states (inactive / one-shot / locked), double-tap lock gesture, and haptic feedback.

**Independent Test**: Launch the app without an SSH connection. Tap the Ctrl button once → primary-colored chip. Tap Alt twice quickly → tertiary-colored chip (locked). Tap locked Alt once → returns to inactive. Haptic fires on each interaction.

- [x] T009 [US2] Update `MacroInputPanel` signature: replace `isCtrlActive: Boolean`, `isAltActive: Boolean`, `onToggleCtrl`, `onToggleAlt` with `ctrlState: ModifierButtonState`, `altState: ModifierButtonState`, `shiftState: ModifierButtonState`, `onModifierTap: (ModifierKey) -> Unit`, `onModifierDoubleTap: (ModifierKey) -> Unit` in `[common]ui/components/macro/MacroInputPanel.kt`
- [x] T010 [US2] Add Shift `FilterChip` button to the Controls Row in `MacroInputPanel`; update Ctrl and Alt chips to derive container color from `ModifierButtonState` (INACTIVE → `surfaceVariant`, ONE_SHOT → `primary`, LOCKED → `tertiary`) in `[common]ui/components/macro/MacroInputPanel.kt`
- [x] T011 [US2] Replace `FilterChip.onClick` on each modifier button with `Modifier.pointerInput { detectTapGestures(onTap = { onModifierTap(...) }, onDoubleTap = { onModifierDoubleTap(...) }) }` in `[common]ui/components/macro/MacroInputPanel.kt`
- [x] T012 [US2] Add `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` inside both `onTap` and `onDoubleTap` handlers for each modifier button in `[common]ui/components/macro/MacroInputPanel.kt`
- [x] T013 [US2] Update call site in `TerminalScreen.kt`: wire `shiftState`, `ctrlState`, `altState` from `TerminalState`; replace `onToggleCtrl`/`onToggleAlt` with `onModifierTap`/`onModifierDoubleTap` calling `screenModel.onModifierTap()`/`screenModel.onModifierDoubleTap()` in `[common]ui/screens/TerminalScreen.kt` (and `[android]ui/screens/TerminalScreen.kt` if platform-specific copy exists)

**Checkpoint**: Launch app — three modifier buttons visible, three-state colors correct, double-tap locks, haptic fires.

---

## Phase 4: User Story 1 — Ctrl Key Combos in SSH Session (Priority: P1)

**Goal**: Tapping Ctrl (one-shot) then a letter sends the correct control character via IME input; one-shot auto-clears after the key press; double-tap locks Ctrl for sustained use.

**Independent Test**: Connect to SSH. Tap Ctrl once → one-shot. Type "c" via software keyboard → `\x03` transmitted, Ctrl deactivates. Double-tap Ctrl → locked. Type "c" three times → three `\x03` bytes sent, Ctrl stays locked.

- [x] T014 [US1] Add `getCtrlSequenceForChar(char: Char): String?` to `KeyboardInputProcessor`: maps letters `a`–`z`/`A`–`Z` → `\u0001`–`\u001A` and symbols `[` → `\u001B`, `\` → `\u001C`, `]` → `\u001D`, `6` → `\u001E`, `-` → `\u001F`, space → `\u0000`; returns null for unmapped chars in `[common]input/KeyboardInputProcessor.kt`
- [x] T015 [P] [US1] Add tests for `getCtrlSequenceForChar` covering letters (a, z), symbols (`[`, space), and unmapped chars in `[test]input/KeyboardInputProcessorTest.kt`
- [x] T016 [US1] Rewrite `sendInput()` single-char modifier path in `TerminalScreenModel`: use `getCtrlSequenceForChar` for Ctrl; handle Alt+Ctrl combo (ESC + ctrlByte); call `applyAndConsumeOneShotModifiers()` unconditionally whenever any modifier `isActive` at key-press time in `[common]viewmodel/TerminalScreenModel.kt`

**Checkpoint**: Run `./gradlew :mobile-vibe-terminal:jvmTest` — new `getCtrlSequenceForChar` tests pass. End-to-end: Ctrl+C via UI button sends `\x03`.

---

## Phase 5: User Story 3 — Modified Cursor and Function Keys (Priority: P2)

**Goal**: With any modifier active, cursor and function keys emit CSI parameterized sequences (`\x1b[1;5C` etc.) instead of unmodified sequences.

**Independent Test**: Connect SSH with vim open. Activate Ctrl button. Press Right arrow → word-forward (cursor moves by word). Activate Shift. Press Up → `\x1b[1;2A` observed in a hex-dump terminal or verified by vim behavior.

- [x] T017 [US3] Implement modifier-aware cursor keys in `getSpecialKeySequence(keyCode, combinedBitmask)`: when `toModifierNumber(combinedBitmask) > 0`, return `\x1b[1;<modNum><dir>` for Up/Down/Left/Right/Home/End; else return existing unmodified sequences in `[common]input/KeyboardInputProcessor.kt`
- [x] T018 [US3] Implement modifier-aware function keys in `getSpecialKeySequence`: when modified, F1–F4 use key numbers 11–14 and return `\x1b[<keyNum>;<modNum>~`; F5–F12 return `\x1b[<keyNum>;<modNum>~`; unmodified behavior unchanged in `[common]input/KeyboardInputProcessor.kt`
- [x] T019 [P] [US3] Add unit tests for modified cursor keys: Shift+Up → `\x1b[1;2A`, Ctrl+Right → `\x1b[1;5C`, Shift+Ctrl+Up → `\x1b[1;6A`, unmodified Up → `\x1b[A` in `[test]input/KeyboardInputProcessorTest.kt`
- [x] T020 [P] [US3] Add unit tests for modified function keys: Shift+F5 → `\x1b[15;2~`, Ctrl+F6 → `\x1b[17;5~`, unmodified F1 → `\x1bOP` (SS3 unchanged), modified F1 → `\x1b[11;2~` in `[test]input/KeyboardInputProcessorTest.kt`

**Checkpoint**: Run `./gradlew :mobile-vibe-terminal:jvmTest` — all modified cursor/fn key tests pass alongside existing tests.

---

## Phase 6: User Story 4 — Combined Modifier Sources (Priority: P2)

**Goal**: UI modifier buttons and hardware/software keyboard modifier flags are merged via bitwise OR so simultaneous modifiers from both sources produce the correct combined sequence.

**Independent Test**: Attach physical keyboard. Toggle Ctrl UI button (one-shot). Press Shift+Up on physical keyboard → `\x1b[1;6A` (Shift bit 1 from hardware + Ctrl bit 4 from UI = bitmask 5, modifier number 6). Toggle Shift UI button + press Shift on hardware → only one Shift applied (idempotent OR).

- [x] T021 [US4] Inside `processKeyEvent`, compute `val combinedBitmask = ModifierBitmask.combine(event, shiftState=..., ctrlState=..., altState=...)` — but since processor is stateless, the OR of hardware flags in `event` with `uiModifierBitmask` parameter: `val combined = uiModifierBitmask or (if event.isShiftPressed 1 else 0) or ...`; pass `combined` to all internal dispatch in `[common]input/KeyboardInputProcessor.kt`
- [x] T022 [US4] In `TerminalScreenModel`, expose `computeUiModifierBitmask()` result via `TerminalStateProvider` or pass directly; update the `onHardwareKeyboardInput` path so `HardwareKeyboardHandler` receives and forwards the UI bitmask in `[common]viewmodel/TerminalScreenModel.kt`
- [x] T023 [US4] Update `HardwareKeyboardHandler.processKeyEvent(event, isCommandMode)` to `processKeyEvent(event, isCommandMode, uiModifierBitmask: Int = 0)` and forward `uiModifierBitmask` to `KeyboardInputProcessor.processKeyEvent` in `[android]input/HardwareKeyboardHandler.kt`
- [x] T024 [US4] Update the call site that invokes `HardwareKeyboardHandler.processKeyEvent` (in `MainActivity.kt` or wherever `onKeyDown` is handled) to pass the current `uiModifierBitmask` from `TerminalStateProvider` or `TerminalScreenModel.state.value` in `[android]MainActivity.kt`
- [x] T025 [P] [US4] Add unit tests for `ModifierBitmask.combine()` with hardware+UI dual-source: `(event.isCtrlPressed=true, ctrlState=ONE_SHOT)` → bitmask 4 (not 8); `(event.isShiftPressed=true, ctrlState=ONE_SHOT)` → bitmask 5 in `[test]input/ModifierBitmaskTest.kt`

**Checkpoint**: Run `./gradlew :mobile-vibe-terminal:jvmTest`. Manual test: UI Ctrl + hardware Shift + cursor key → combined CSI sequence.

---

## Phase 7: User Story 5 — Alt Key and Special Key Combinations (Priority: P3)

**Goal**: Alt+letter sends ESC-prefixed char; Alt+Ctrl+char sends ESC+ctrlByte; Shift+Tab (from UI Shift button) sends backtab; Ctrl+Backspace sends `\x08`; Insert/Delete/PgUp/PgDn with modifiers send parameterized sequences.

**Independent Test**: Connect bash over SSH. Toggle Alt, type "f" → word-forward. Toggle Shift, press Tab → reverse-completion. Toggle Ctrl, press Backspace → word deleted (0x08). Toggle Ctrl+Alt, type "c" → ESC+0x03.

- [x] T026 [US5] Fix Alt+Ctrl dispatch order in `processKeyEvent`: when combined bitmask has both Alt bit (2) and Ctrl bit (4) set, return `ESC + ctrlByte` instead of bare `ctrlByte`; add this check before the Ctrl-only branch in `[common]input/KeyboardInputProcessor.kt`
- [x] T027 [US5] Implement Ctrl+Backspace in `getSpecialKeySequence`: when `keyCode == DEL` and Ctrl bit set in `combinedBitmask`, return `\u0008`; else return `\u007F` (unchanged) in `[common]input/KeyboardInputProcessor.kt`
- [x] T027b [US5] Implement Shift+Tab (backtab) in `getSpecialKeySequence`: when `keyCode == TAB` and `(combinedBitmask and 1) != 0` (Shift bit set), return `"\u001B[Z"`; when no Shift, return `"\t"` (unchanged) in `[common]input/KeyboardInputProcessor.kt`
- [x] T028 [US5] Implement modifier-aware Insert/Delete/PageUp/PageDown in `getSpecialKeySequence`: when `combinedBitmask > 0`, return `\x1b[<keyNum>;<modNum>~`; else existing `\x1b[2~` etc. sequences unchanged in `[common]input/KeyboardInputProcessor.kt`
- [x] T029 [P] [US5] Add unit tests for: Alt+f → `\x1bf`, Alt+Ctrl+C → `\x1b\x03`, Ctrl+Backspace → `\x08`, plain Backspace → `\x7F` (unchanged), Shift+Tab via combinedBitmask Shift bit → `\x1b[Z`, Ctrl+Delete → `\x1b[3;5~` in `[test]input/KeyboardInputProcessorTest.kt`

**Checkpoint**: Run `./gradlew :mobile-vibe-terminal:jvmTest` — all US5 tests pass. All previous tests still green.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T030 [P] Verify Meta bit flows from `event.isMetaPressed` through `ModifierBitmask.combine()` into the combined bitmask; add one test asserting `isMetaPressed=true` contributes bit 8 in `[test]input/ModifierBitmaskTest.kt`
- [x] T031 [P] Run the full commonTest suite and confirm zero regressions: `./gradlew :mobile-vibe-terminal:testDebugUnitTest` — BUILD SUCCESSFUL
- [x] T032 Update `specs/005-modifier-keys-handling/quickstart.md` with confirmed test command output and any worktree setup corrections discovered during implementation

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
  └─► Phase 2 (Foundational) — T002–T008 must all complete
        └─► Phase 3 (US2 UI)        ─┐
        └─► Phase 4 (US1 Ctrl IME)   ├─ can start in parallel after Phase 2
        └─► Phase 5 (US3 Cursor/Fn)  ┘
              └─► Phase 6 (US4 Combined) — depends on Phase 5 T017–T018
                    └─► Phase 7 (US5 Alt/Special)
                          └─► Phase 8 (Polish)
```

### Within-Phase Dependencies

**Phase 2**:
- T002 → T003 (ModifierBitmask uses ModifierButtonState)
- T002 → T005 (TerminalState uses ModifierButtonState)
- T003 and T004 are parallel
- T005 → T006 → T007 (sequential in TerminalScreenModel)
- T008 depends on T002 (uses ModifierButtonState)

**Phase 3 (US2)**:
- T009 → T010 → T011 → T012 (sequential edits to MacroInputPanel)
- T013 depends on T009 (needs new MacroInputPanel signature)

**Phase 4 (US1)**:
- T014 → T016 (sendInput uses getCtrlSequenceForChar)
- T015 is parallel with T014 (test file, no dependency on T016)

**Phase 5 (US3)**:
- T008 (Phase 2) → T017, T018 (use new getSpecialKeySequence signature)
- T017 and T018 are parallel (different key groups in same function)
- T019 and T020 are parallel (different test cases)

**Phase 6 (US4)**:
- T017/T018 complete → T021 (adds uiModifierBitmask to processKeyEvent)
- T021 → T022 → T023 → T024 (wiring chain)
- T025 parallel with T021

---

## Parallel Execution Examples

### Phase 2 Parallel Group
```
Parallel: T003 (ModifierBitmask.kt) + T004 (ModifierBitmaskTest.kt)
Then: T005, T006, T007, T008 — sequential in TerminalScreenModel.kt
```

### Phase 5 Parallel Group
```
Parallel: T017 (cursor keys) + T018 (function keys)  — same file, compatible edits
Parallel: T019 (cursor tests) + T020 (fn key tests)
```

### Phase 7 Parallel Group
```
T026, T027, T028 — sequential (same file, different cases in getSpecialKeySequence)
T029 — parallel with T026-T028 (test file)
```

---

## Implementation Strategy

### MVP Scope (Stories US2 + US1 only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (T002–T008) — **CRITICAL blocker**
3. Complete Phase 3: US2 toggle buttons
4. Complete Phase 4: US1 Ctrl combos
5. **STOP and VALIDATE**: Ctrl+C via UI button interrupts remote process; one-shot clears; double-tap locks

### Full Delivery Order

1. Setup + Foundational → foundation ready
2. US2 (toggle buttons) + US1 (Ctrl combos via IME) → P1 complete, shippable
3. US3 (modified cursor/fn keys) → P2 part 1
4. US4 (combined sources) → P2 complete, hardware keyboard users covered
5. US5 (Alt, special keys) → P3 complete
6. Polish → all regressions verified, quickstart updated

---

## Notes

- [P] tasks = different files or non-overlapping edits; safe to run concurrently
- `[test]` tasks should be written alongside implementation (same phase) not after
- After T008, run `./gradlew :mobile-vibe-terminal:jvmTest` to confirm existing tests still pass — the signature change to `getSpecialKeySequence` will require updating its internal callers to pass `0` as the stub bitmask
- `TerminalScreen.kt` may have both a `commonMain` and `androidMain` version — check both and update whichever references `MacroInputPanel`
- Constitution VI: all code changes must be committed in `worktrees/005-modifier-keys-handling/`, not the main repo directory
