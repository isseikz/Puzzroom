# Tasks: Selectable and Copy-able Code Viewer

**Input**: Design documents from `/specs/001-copy-code-viewer/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, quickstart.md

**Tests**: Manual validation as per `spec.md` and `quickstart.md`. Automated tests are not explicitly requested but manual verification steps are included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 [P] Verify project dependencies (Compose 1.9.0) in `gradle/libs.versions.toml`
- [x] T002 [P] Review `CodeViewerSheet.kt` and `ClipboardManager.kt` for integration points
- [x] T003 [P] Configure/Verify imports for `androidx.compose.foundation.text.selection.*` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Ensure `ClipboardManager.copyToClipboard` is accessible in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/util/ClipboardManager.kt`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 & 2 - Select and Copy with Formatting (Priority: P1) 🎯 MVP

**Goal**: Enable multi-line text selection and copying while preserving code formatting and excluding line numbers.

**Independent Test**: Long-press code in the viewer, select multiple lines, copy, and paste into another app. Verify formatting matches and no line numbers are included.

### Implementation for User Story 1 & 2

- [x] T005 [US1] Wrap the `LazyColumn` content in `SelectionContainer` within `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T006 [US1] Wrap the line number `Text` component in `DisableSelection` within `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T007 [US1] Verify that long-press triggers selection handles and "Copy" action appears in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T008 [US2] Validate that copied multi-line blocks preserve indentation when pasted from `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`

**Checkpoint**: At this point, basic selection and copying are fully functional.

---

## Phase 4: User Story 3 - Select All / Copy All (Priority: P2)

**Goal**: Provide a one-tap "Copy All" button to copy the entire file content.

**Independent Test**: Tap the "Copy All" icon in the header and verify the entire file content is in the clipboard.

### Implementation for User Story 3

- [x] T009 [US3] Add a `ContentCopy` icon button to the header `Row` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T010 [US3] Implement the click listener for the "Copy All" button to call `ClipboardManager.copyToClipboard(fileContent!!)` in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T011 [US3] Add a "Copy All" tooltip or string resource for the icon button description in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`

**Checkpoint**: All user stories are now implemented and functional.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and documentation

- [x] T012 [P] Verify selection behavior with large files (scroll performance) in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T013 [P] Ensure "Copy All" button is only visible when `fileContent` is not null in `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`
- [x] T014 Run validation of all success criteria as defined in `specs/001-copy-code-viewer/spec.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion
- **User Stories (Phase 3+)**: Depend on Foundational phase completion
- **Polish (Final Phase)**: Depends on all user stories being complete

### Parallel Opportunities

- T001, T002, T003 can run in parallel.
- T012 and T013 can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 & 2 Only)

1. Complete Setup and Foundational tasks.
2. Implement `SelectionContainer` and `DisableSelection`.
3. Validate that users can select and copy code without line numbers.

### Incremental Delivery

1. Foundation ready (T001-T004) [X]
2. Core selection functionality (T005-T008) -> MVP Ready! [X]
3. Enhanced "Copy All" functionality (T009-T011) [X]
4. Final polish and verification (T012-T014) [X]
