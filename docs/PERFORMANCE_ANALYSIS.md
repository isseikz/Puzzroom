# Vibe Terminal Performance Analysis Report

## Executive Summary

ターミナル画面でのボタン応答遅延の主な原因を特定しました。**最も重大な問題は、`sendInput`関数がメインスレッドでブロッキングI/O操作を実行していることです。**

---

## Critical Issues

### Issue 1: `sendInput` がメインスレッドでブロッキングI/Oを実行 (CRITICAL)

**Location**: `TerminalScreenModel.kt:316-346`

```kotlin
fun sendInput(text: String, appendNewline: Boolean = false) {
    if (text.isEmpty()) return

    screenModelScope.launch {  // <-- No dispatcher specified = Dispatchers.Main
        // ... modifier logic ...
        sshRepository.sendInput(toSend)  // <-- Blocking I/O on Main Thread!
    }
}
```

**Root Cause**:
- `screenModelScope.launch` はデフォルトで `Dispatchers.Main` で実行
- `MinaSshdRepository.sendInput()` は以下のブロッキング操作を実行:
  ```kotlin
  outputWriter?.print(input)
  outputWriter?.flush()  // <-- I/O flush blocks until data is sent
  ```

**Impact**:
- マクロボタン押下時に約100-500msのUI遅延
- ソフトウェアキーボードからの入力も同様に遅延

**Fix**:
```kotlin
fun sendInput(text: String, appendNewline: Boolean = false) {
    if (text.isEmpty()) return

    screenModelScope.launch {
        // ... modifier logic ...
        withContext(Dispatchers.IO) {  // <-- Add this
            sshRepository.sendInput(toSend)
        }
    }
}
```

---

### Issue 2: 他のSSH操作もメインスレッドで実行

**Locations**:

| Line | Function | Operation |
|------|----------|-----------|
| 443 | `startFileMonitoring` | `sshRepository.sendInput()` |
| 501 | `resize()` | `sshRepository.resizeTerminal()` |
| 555 | `disconnect()` | `sshRepository.disconnect()` |
| 795 | `disconnectAndClearSession` | `sshRepository.disconnect()` |
| 898 | `onDispose` | `sshRepository.disconnect()` |

**Fix**: 各操作を `withContext(Dispatchers.IO)` でラップ

---

## Moderate Issues

### Issue 3: 過剰なRecomposition

**Location**: `TerminalScreen.kt:71`, `TerminalScreen.kt:452-476`

```kotlin
val state by screenModel.state.collectAsState()  // 全state変更で再コンポジション

MacroInputPanel(
    state = state,  // <-- 全stateを渡している
    // ...
)
```

**Problem**:
- ターミナル出力の各文字で `bufferUpdateCounter` がインクリメント
- `state` 全体が変更されるため、`MacroInputPanel` も毎回再コンポジション
- SSH接続中は秒間数十回の再コンポジションが発生する可能性

**Recommendation**:
1. MacroInputPanelに必要なプロパティのみを渡す
2. `derivedStateOf` を使用して必要な値のみを監視

```kotlin
// Before
MacroInputPanel(state = state, ...)

// After
MacroInputPanel(
    selectedMacroTab = state.selectedMacroTab,
    isImeEnabled = state.isImeEnabled,
    isSoftKeyboardVisible = state.isSoftKeyboardVisible,
    isCtrlActive = state.isCtrlActive,
    isAltActive = state.isAltActive,
    isAlternateScreen = state.isAlternateScreen,
    isConnected = state.isConnected,
    // ...
)
```

---

### Issue 4: TerminalStateの`equals`実装

**Location**: `TerminalScreenModel.kt:50-68`

```kotlin
override fun equals(other: Any?): Boolean {
    // screenBuffer (Array) は比較に含まれていない
    // bufferUpdateCounter で変更を検知
}
```

**Observation**:
- `screenBuffer` を `equals` から除外しているのは正しい（パフォーマンス的に）
- しかし、`bufferUpdateCounter` が毎回インクリメントされるため、常に不等と判定される
- これにより毎回新しい状態として扱われ、再コンポジションが発生

---

## Recommended Fixes (Priority Order)

### Priority 1: sendInput を IO スレッドで実行

```kotlin
fun sendInput(text: String, appendNewline: Boolean = false) {
    if (text.isEmpty()) return

    screenModelScope.launch(Dispatchers.IO) {  // Change here
        var toSend = if (appendNewline) text + "\n" else text

        val currentState = _state.value
        var modified = false

        if (toSend.length == 1) {
            var char = toSend[0]

            if (currentState.isCtrlActive) {
                if (char in 'a'..'z' || char in 'A'..'Z') {
                    char = (char.uppercaseChar().code - 'A'.code + 1).toChar()
                    toSend = char.toString()
                    modified = true
                }
            }

            if (currentState.isAltActive) {
                toSend = "\u001B" + toSend
                modified = true
            }
        }

        if (modified) {
            _state.update { it.copy(isCtrlActive = false, isAltActive = false) }
        }

        Logger.d("SSH_TX: $toSend")
        sshRepository.sendInput(toSend)
    }
}
```

### Priority 2: 他のSSH操作を修正

```kotlin
// resize() - Line 501
screenModelScope.launch {
    withContext(Dispatchers.IO) {
        sshRepository.resizeTerminal(cols, rows, widthPx, heightPx)
    }
    // ...
}

// disconnect() - Line 555
screenModelScope.launch {
    outputListenerJob?.cancel()
    outputListenerJob = null
    withContext(Dispatchers.IO) {
        sshRepository.disconnect()
    }
    _state.update { it.copy(isConnected = false) }
    // ...
}
```

### Priority 3: MacroInputPanelの最適化 (Optional)

状態を分離して不要な再コンポジションを防ぐ

---

## Verification

修正後、以下を確認:
1. マクロボタン押下の即座の反応
2. ソフトウェアキーボードの切り替えの即座の反応
3. CTRL/ALTトグルの即座の反応
4. Android Studio Profiler でメインスレッドのブロッキングがないことを確認

---

## Summary

| Issue | Severity | Estimated Impact | Fix Complexity |
|-------|----------|------------------|----------------|
| sendInput on Main Thread | Critical | 100-500ms delay | Low |
| Other SSH ops on Main Thread | High | Variable delay | Low |
| Excessive Recomposition | Medium | UI jank | Medium |
