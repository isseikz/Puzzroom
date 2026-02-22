# Research: Selectable Code Viewer

## Overview

The `CodeViewerSheet` component currently displays code content line-by-line using a `LazyColumn`. This approach is efficient for large files but does not natively support multi-line text selection or copying to the system clipboard.

## Decision: `SelectionContainer` with `DisableSelection`

We will use the standard Compose `SelectionContainer` from the `androidx.compose.foundation.text.selection` package to wrap the `LazyColumn` content.

### Rationale
- **Cross-Item Selection**: `SelectionContainer` in modern Compose versions (1.9.0) supports cross-item selection within a `LazyColumn`, which is essential for multi-line code snippets.
- **Platform Native Context Menu**: It automatically integrates with the platform's native selection handles and "Copy" actions, reducing custom code.
- **Selective Copying**: By wrapping the line number `Text` components in `DisableSelection`, we can ensure that only the code content is copied to the clipboard, providing a better user experience.

### Alternatives Considered
- **Custom Selection Manager**: We could have built a custom selection engine similar to `SelectableTerminalContainer`. However, for standard text content in a `LazyColumn`, this would be over-engineering and would require significant effort to map coordinates to line/character positions accurately across syntax highlighting.
- **One Large Text Component**: Putting the entire file content into a single `Text` component would make selection trivial but would severely degrade performance for large source files.

## Decision: Header "Copy All" Action

We will add a "Copy All" icon button to the `CodeViewerSheet` header.

### Rationale
- **Accessibility**: Provides a one-tap way to copy the entire file content, addressing the "Select All" requirement (User Story 3) in a more direct and mobile-friendly manner.
- **Platform Consistency**: This is a common pattern in mobile code viewers where selecting the whole file via handles can be cumbersome.

## Implementation Details

- **Whitespace Preservation**: Since the original `fileContent` (the raw string) is available, the "Copy All" action will use the raw string directly to ensure all whitespace, including leading and trailing spaces, is preserved.
- **Syntax Highlighting**: The visual highlighting remains unchanged, and the selection handles will overlay correctly on the text.
- **Clipboard Management**: Use the existing `ClipboardManager.copyToClipboard(text)` utility.
