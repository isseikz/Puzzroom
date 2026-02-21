# Tasks: Navigation from Code Viewer to File Explorer

**Input**: Design documents from `/specs/003-code-viewer-back-nav/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, quickstart.md

**Tests**: Tests are NOT explicitly requested in the spec, so implementation-focused tasks are prioritized.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify the current state and prepare for changes.

- [x] T001 Verify existing `CodeViewerSheet` and `TerminalScreen` implementation in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core UI component updates required for all navigation features.

- [x] T002 Update `CodeViewerSheet` signature to include `onBack: () -> Unit` and rename `onDismiss` to `onClose: () -> Unit` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T003 Update `TerminalScreen` to provide callbacks for `onBack` and `onClose` to the `CodeViewerSheet` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/screens/TerminalScreen.kt`

---

## Phase 3: User Story 1 - Navigate Back to File Explorer (Priority: P1) 🎯 MVP

**Goal**: Allow users to return to the File Explorer from the Code Viewer while maintaining context.

**Independent Test**: Open a file from the File Explorer, tap the new "Back" button, and verify the File Explorer remains visible at the same directory.

### Implementation for User Story 1

- [x] T004 [US1] Add "Back" `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack` to the start of the header `Row` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T005 [US1] Implement `onBack` logic in `TerminalScreen` to set `selectedFilePath = null` while keeping `showFileExplorer = true` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/screens/TerminalScreen.kt`

**Checkpoint**: User Story 1 is functional: users can navigate between explorer and viewer.

---

## Phase 4: User Story 2 - Distinction Between Back and Close (Priority: P2)

**Goal**: Provide a clear "exit" path for the entire file browsing session.

**Independent Test**: Open a file from the File Explorer, tap the "Close" button, and verify both the Code Viewer and the File Explorer are dismissed.

### Implementation for User Story 2

- [x] T006 [US2] Update the existing "Close" `IconButton` in the header to trigger the new `onClose` callback in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T007 [US2] Implement `onClose` logic in `TerminalScreen` to set both `selectedFilePath = null` and `showFileExplorer = false` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/screens/TerminalScreen.kt`

**Checkpoint**: User Story 2 is functional: users can exit the entire workflow in one tap.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Hardware integration and visual refinements.

- [x] T008 [P] Integrate `BackHandler` in `TerminalScreen` to ensure system back press behaves as "Back" (returns to explorer if viewer is open) in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/screens/TerminalScreen.kt`
- [x] T009 [P] Refine header layout for standard spacing and "terminal green" theme consistency in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T010 [P] Final validation against `quickstart.md` success criteria.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Phase 1.
- **User Stories (Phase 3 & 4)**: Depend on Phase 2. Can be implemented sequentially or in parallel.
- **Polish (Phase 5)**: Depends on all user stories.

### Parallel Opportunities

- T008, T009, and T010 can be worked on in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and 2.
2. Implement User Story 1 (Back button).
3. **STOP and VALIDATE**: Verify back navigation works without closing the explorer.

### Incremental Delivery

1. Foundation ready.
2. User Story 1 added (Navigate Back).
3. User Story 2 added (Close Session).
4. System integration (Hardware back) and Polish.
