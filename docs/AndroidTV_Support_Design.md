# Android TV Support Design Document

## 1. Overview
Enable `mobile-vibe-terminal` to run fully functionally on Android TV devices using a physical keyboard (Bluetooth/USB). The implementation assumes no touchscreen interaction and no software keyboard usage for the terminal session.

## 2. Requirements & Specifications

### 2.1 Platform Support
- **Target:** Android TV.
- **Hardware:**
    - Display: Standard TV resolutions (1080p/4K).
    - Input: Physical Keyboard (USB/Bluetooth) is the primary input method. Remote Control (D-Pad) is used for initial navigation.
    - Touchscreen: Not available.

### 2.2 Functional Requirements
1.  **Installation & Launch:**
    - App must be installable via ADB or Play Store on Android TV.
    - App must appear in the Android TV launcher.
2.  **Navigation (Connection List):**
    - Users must be able to select a saved connection using the D-Pad or Keyboard Arrow keys.
    - Users must be able to trigger the connection (Enter key).
3.  **Terminal Session (Input Strategy):**
    - **Default Mode:** **Direct Input (Raw Mode)**.
        - Upon connection with a hardware keyboard, the app defaults to Raw Mode.
        - Rationale: Most terminal operations are command-based and do not require IME.
    - **IME Mode (Optional):**
        - Users can toggle IME on for multi-language input (e.g., Japanese comments).
    - **Switching:**
        - **Shortcut:** `Alt + i` toggles between Direct Mode and IME Mode.
        - **UI:** The keyboard toggle button in the macro bar must be focusable via D-Pad.
    - **Hybrid Handling (in IME Mode):**
        - Regular text -> System IME.
        - Control/Special keys -> Intercepted by App.

## 3. Technical Design & Changes

### 3.1 Android Manifest Configuration
To ensure compatibility and visibility on Android TV:
- **Feature Flags:**
    - `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
    - `<uses-feature android:name="android.software.leanback" android:required="false" />`
- **Intent Filter:**
    - Add `android.intent.category.LEANBACK_LAUNCHER` to `MainActivity`.

### 3.2 Input Handling Logic

**1. TerminalScreenModel (Initial State):**
- When a hardware keyboard is detected, force `isImeEnabled = false` (Direct Mode).
- Note: This reverts the behavior for mobile devices with keyboards too, which is desirable for power users.

**2. MainActivity (Event Dispatching):**
- Implement an "Always-Try" interception chain in `dispatchKeyEvent`.

```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (!TerminalStateProvider.state.value.isConnected) return super.dispatchKeyEvent(event)

    // 1. Check for Mode Toggle Shortcut (Alt + i)
    if (event.isAltPressed && event.keyCode == KeyEvent.KEYCODE_I && event.action == KeyEvent.ACTION_DOWN) {
        TerminalStateProvider.requestToggleIme() // New method to request toggle
        return true
    }

    // 2. Try HardwareKeyboardHandler (Process Ctrl, Alt, Esc, Arrows, etc.)
    val isCommandMode = TerminalStateProvider.isCommandMode.value
    val result = HardwareKeyboardHandler.processKeyEvent(event, isCommandMode)
    
    if (result is HardwareKeyboardHandler.KeyResult.Handled) {
        // Intercepted (e.g., Ctrl+C, or everything if isCommandMode is true)
        TerminalStateProvider.sendHardwareKeyboardInput(result.sequence)
        return true 
    }
    
    // 3. Fallback to System (IME)
    // Only happens if isCommandMode is false (IME Mode) AND key was regular char
    return super.dispatchKeyEvent(event)
}
```

**3. State Provider & ViewModel:**
- Add `requestToggleIme()` callback mechanism to `TerminalStateProvider` so `MainActivity` can trigger a ViewModel action managed in `TerminalScreenModel`.

### 3.3 UX/UI Considerations
1.  **Visual Focus Indicators:**
    - Ensure `ConnectionCard` and Macro Bar buttons have distinct visual changes when focused via D-Pad.
2.  **Overscan & Safe Areas:**
    - Verify padding for TV displays.
3.  **Readability:**
    - Consider larger default font sizes for TV.

## 4. Implementation Steps
1.  **Manifest Update:** (Completed)
2.  **TerminalStateProvider:** Add `onToggleImeRequest` callback support.
3.  **TerminalScreenModel:** 
    - Change default hardware keyboard mode to `Raw` (Direct).
    - Listen to `onToggleImeRequest`.
4.  **MainActivity:** 
    - Implement the interception logic (`Alt+i` check + `HardwareKeyboardHandler` check).
5.  **Verification:**
    - Default is Raw mode?
    - `Alt+i` toggles IME?
    - IME mode allows Japanese?
    - IME mode still sends Ctrl+C correctly?
