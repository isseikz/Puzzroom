# Quickstart: Selectable Code Viewer

## Overview

The `Selectable Code Viewer` feature enables text selection and copying in the `CodeViewerSheet` component. This guide covers how to set up the implementation.

## Prerequisites

- **Kotlin**: 2.2.20
- **Compose Multiplatform**: 1.9.0
- **SSH Connectivity**: The SSH server must be available to fetch file content via the `SshRepository`.

## Implementation Checklist

- [ ] Import `androidx.compose.foundation.text.selection.*` in `CodeViewerSheet.kt`.
- [ ] Wrap the `LazyColumn` content inside a `SelectionContainer`.
- [ ] Wrap the line number `Text` inside a `DisableSelection` block.
- [ ] Add a `ContentCopy` icon button to the `CodeViewerSheet` header.
- [ ] Implement the `onCopyAll` action using `ClipboardManager.copyToClipboard(fileContent)`.

## Example Usage

```kotlin
SelectionContainer {
    LazyColumn(...) {
        itemsIndexed(lines) { index, line ->
            Row(...) {
                DisableSelection {
                    Text(text = "${index + 1}", ...)
                }
                Text(text = SyntaxHighlighter.highlight(line, extension), ...)
            }
        }
    }
}
```

## Running the Application

To test the changes:
1.  Navigate to a project and open a code file in the terminal viewer.
2.  Long-press on a code line to trigger selection handles.
3.  Drag the handles to select multiple lines.
4.  Select "Copy" from the context menu.
5.  Try the "Copy All" icon in the header.
6.  Paste the content in another app to verify formatting.
