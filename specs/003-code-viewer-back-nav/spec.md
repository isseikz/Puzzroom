# Feature Specification: Navigation from Code Viewer to File Explorer

**Feature Branch**: `003-code-viewer-back-nav`
**Created**: 2026-02-21
**Status**: Draft
**Input**: User description: "A navigation from code viewer to file explorer which is open just before code viewer by pressing back button next to the existing close button"

## Clarifications

### Session 2026-02-21
- Q: Navigation Source Context → A: Return to the File Explorer (the origin of navigation)
- Q: Visual Hierarchy and Layout → A: Option A (Standard Left/Start: Back arrow on the left, Close icon on the far right)
- Q: Hardware/System Back Button Behavior → A: Option A (Identical to "Back" button behavior)
- Q: Empty Navigation Stack Behavior → A: Option A (Logical Fallback: Navigate to root File Explorer)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate Back to File Explorer (Priority: P1)

As a user who has just opened a file for viewing, I want to quickly return to the file explorer to continue browsing other files without losing my context in the directory.

**Why this priority**: Essential for a productive browsing experience. Without a back button, users may be forced to use a "Close" action that might reset their entire navigation context or terminal session.

**Independent Test**: Can be tested by navigating to a deep directory in the File Explorer, opening a code file, and then tapping the "Back" button to verify return to the exact same directory in the explorer.

**Acceptance Scenarios**:

1. **Given** the user is in the File Explorer at directory `/src/main`, **When** they open `App.kt`, **Then** the Code Viewer opens.
2. **Given** the user is in the Code Viewer, **When** they tap the "Back" button (next to the "Close" button), **Then** the application returns to the File Explorer at `/src/main`.

---

### User Story 2 - Distinction Between Back and Close (Priority: P2)

As a user, I want to clearly understand the difference between returning to the explorer ("Back") and dismissing the current task or terminal session ("Close").

**Why this priority**: Prevents user confusion and accidental session termination.

**Independent Test**: Verify that "Back" returns to the explorer while "Close" performs its existing distinct action (e.g., closing the terminal tab or overlay).

**Acceptance Scenarios**:

1. **Given** the user is in the Code Viewer, **When** they see the header, **Then** both a "Back" and a "Close" button are visible and distinguishable.
2. **Given** the user is in the Code Viewer, **When** they tap "Close", **Then** the application performs the existing close behavior (which may differ from "Back").

---

### Edge Cases

- **File Explorer state loss**: If the system is under memory pressure, ensure the File Explorer's directory and scroll position are restored when navigating back.
- **Navigation from other sources**: The "Back" button MUST return the user to their immediate previous screen (e.g., Search results, Recent files list) if they did not arrive from the File Explorer.
- **Empty Navigation Stack**: If the Code Viewer is opened with no history (e.g., deep link), the "Back" button MUST navigate to the root directory of the File Explorer.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Code Viewer header MUST include a "Back" button.
- **FR-002**: The "Back" button MUST be positioned at the start (left for LTR) of the header, following standard mobile navigation patterns.
- **FR-003**: The "Close" button MUST remain at the end (far right) of the header to distinguish it from navigation.
- **FR-004**: The "Back" button MUST return the user to the File Explorer.
- **FR-005**: The application MUST re-open the File Explorer if it was dismissed when the file was opened.
- **FR-006**: The application MUST preserve the state of the File Explorer (e.g., scroll position, current path) when returning via the "Back" button.
- **FR-006**: The "Back" button icon MUST use a standard navigation symbol (e.g., a left-pointing arrow).
- **FR-007**: System hardware/gesture back actions MUST trigger the same navigation logic as the on-screen "Back" button.

### Key Entities

- **Code Viewer**: The screen or component displaying the file content.
- **File Explorer**: The screen or component from which the file was selected.
- **Navigation Stack**: The internal state management that tracks the history of screens.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of "Back" actions return the user to the correct previous screen (File Explorer).
- **SC-002**: Navigation transition from Code Viewer to File Explorer completes in under 300ms.
- **SC-003**: Zero reports of "state loss" (e.g., explorer resetting to root) when returning from the Code Viewer via the "Back" button.
