# Quickstart: Navigation from Code Viewer to File Explorer

## Overview
Implement a dual-action header in the `CodeViewerSheet` to differentiate between "going back" (to File Explorer) and "closing" (the entire browsing session).

## Implementation Steps

### 1. Update `CodeViewerSheet`
Modify the component to accept separate `onBack` and `onClose` callbacks.

**File**: `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/components/CodeViewerSheet.kt`

- Add `onBack: () -> Unit` parameter.
- Add an `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack` at the start of the header `Row`.
- Keep the existing `IconButton` (Close) and map it to the `onClose` callback (formerly `onDismiss`).
- Ensure the header layout is: `[Back Button] [Title Column] [Close Button]`.

### 2. Update `TerminalScreen`
Modify the state management for both actions.

**File**: `mobile-vibe-terminal/src/commonMain/kotlin/tokyo/isseikuzumaki/vibeterminal/ui/screens/TerminalScreen.kt`

- In the `CodeViewerSheet` call:
  - `onBack = { selectedFilePath = null }`
  - `onClose = { selectedFilePath = null; showFileExplorer = false }`

### 3. Verify Layout
Ensure the buttons are visually distinct and correctly spaced:
- **Back Arrow**: Standard navigation icon for returning.
- **Close (X)**: Standard "exit" icon.

## Success Criteria
- Tapping Back returns to the File Explorer at the exact same path and scroll position.
- Tapping Close dismisses both the Code Viewer and the File Explorer.
- System hardware/gesture back press behaves as "Back" (returns to explorer).

## Testing
- **Unit Test**: Verify the callbacks are triggered by the respective buttons in `CodeViewerSheet`.
- **UI Test**: Open File Explorer, open Code Viewer, tap Back, verify File Explorer is still visible. Tap Close, verify both are dismissed.
