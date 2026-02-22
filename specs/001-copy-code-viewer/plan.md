# Implementation Plan: Selectable and Copy-able Code Viewer

**Branch**: `001-copy-code-viewer` | **Date**: Sunday, February 22, 2026 | **Spec**: `/specs/001-copy-code-viewer/spec.md`
**Input**: Feature specification from `/specs/001-copy-code-viewer/spec.md`

## Summary

The goal is to enable text selection and copying within the `CodeViewerSheet` component. The technical approach involves wrapping the code display logic in a Compose `SelectionContainer`. To ensure a clean copying experience, line numbers will be wrapped in `DisableSelection`. Additionally, a "Copy All" feature will be added to the header for improved accessibility to the entire file content.

## Technical Context

**Language/Version**: Kotlin 2.2.20
**Primary Dependencies**: Compose Multiplatform 1.9.0
**Storage**: N/A (Temporary in-memory storage of SSH file content)
**Testing**: Manual validation of selection gestures, context menu appearance, and clipboard content integrity.
**Target Platform**: Android, iOS, Web, Desktop
**Project Type**: Mobile (Kotlin Multiplatform)
**Performance Goals**: Selection handles should respond with less than 16ms lag; "Copy All" should be near-instant.
**Constraints**: Indentation and formatting must be preserved during copy operations.
**Scale/Scope**: Targeted modification of `CodeViewerSheet.kt` in the `mobile-vibe-terminal` module.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

1. **Atomic Design Compliance**: `CodeViewerSheet` remains a high-level UI component (Page/Template level).
2. **Theme Usage**: Selection highlighting will use default platform behavior or `MaterialTheme` colors if customized.
3. **Platform Independence**: Uses standard `androidx.compose.foundation.text.selection` APIs available in commonMain.
4. **Data Flow**: Selection state is managed internally by Compose; "Copy All" will trigger a one-way event to `ClipboardManager`.
5. **Simplicity First**: Leverages built-in `SelectionContainer` rather than a custom selection engine.
6. **Worktree Isolation**: Development is occurring in the `001-copy-code-viewer` branch.
7. **Clean Architecture Compliance**: Logic remains within the UI layer; clipboard access uses the existing `ClipboardManager` utility.

## Project Structure

### Documentation (this feature)

```text
specs/001-copy-code-viewer/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (N/A for this feature)
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (N/A for this feature)
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/
├── ui/
│   ├── components/
│   │   └── CodeViewerSheet.kt    # Primary modification target
│   └── utils/
│       └── SyntaxHighlighter.kt   # Reference for highlighting logic
└── util/
    └── ClipboardManager.kt        # Utility for copying to clipboard
```

**Structure Decision**: The feature is contained within the `mobile-vibe-terminal` module, specifically targeting the `CodeViewerSheet.kt` component and utilizing existing utilities.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
