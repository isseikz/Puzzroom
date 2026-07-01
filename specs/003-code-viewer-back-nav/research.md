# Research: Navigation from Code Viewer to File Explorer

## Decision: Dual-Action Header for Code Viewer

The `CodeViewerSheet` will be updated to include both a "Back" button and a "Close" button in its header. This provides explicit navigation paths for returning to the context (File Explorer) versus dismissing the entire file browsing session.

### Technical Context & Findings
- **Current State**: `CodeViewerSheet` is a `ModalBottomSheet` triggered by setting `selectedFilePath` in `TerminalScreen`. Closing it (setting `selectedFilePath = null`) reveals the `FileExplorerSheet` underneath.
- **Back Action**: Should return the user to the File Explorer. Implementation: `onBack = { selectedFilePath = null }`.
- **Close Action**: Should exit the entire file browsing workflow. Implementation: `onClose = { selectedFilePath = null; showFileExplorer = false }`.
- **Layout**: Header Row will be: `[Back Button] [Title/Filename] [spacer] [Close Button]`.
- **Styling**: Must maintain the "terminal green" aesthetic of the `mobile-vibe-terminal` module while adhering to Atomic Design principles (Atoms/Molecules).

### UI Component Strategy
- **Back Button**: Standard `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack`.
- **Close Button**: Existing `IconButton` with `Icons.Default.Close`.
- **Header Structure**: A Molecule-level component `CodeViewerHeader` (or similar) will be used if refactoring is warranted, otherwise implemented directly in `CodeViewerSheet`.

### Rationale
- **Bi-directional Navigation**: Addresses the user's need for a clear return path to the explorer.
- **Distinction**: Provides a way to "exit" the entire feature (Close) versus "stepping back" (Back), common in deep navigation flows.

### Alternatives Considered
- **Replacing Close with Back**: Rejected because the user explicitly requested a button "next to the existing close button".
- **Implicit Navigation (System Back Only)**: Rejected because on-screen cues are necessary for discoverability, especially in a terminal-like environment.
