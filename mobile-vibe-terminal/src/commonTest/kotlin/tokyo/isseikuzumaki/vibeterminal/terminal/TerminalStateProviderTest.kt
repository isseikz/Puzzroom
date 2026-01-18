package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

/**
 * [TerminalStateProvider]のユニットテスト。
 *
 * ターミナル状態の管理とコールバック機構のテストを行う。
 */
class TerminalStateProviderTest {

    /**
     * requestToggleIme()がonToggleImeRequestコールバックを呼び出すことを検証。
     *
     * ## Android TV向け機能
     * Alt+iショートカットがMainActivityで検出されると、requestToggleIme()が
     * 呼び出される。これによりTerminalScreenModelのtoggleImeMode()が
     * トリガーされ、IMEモードとRAWモードが切り替わる。
     */
    @Test
    fun testRequestToggleImeInvokesCallback() {
        var callbackInvoked = false
        TerminalStateProvider.onToggleImeRequest = {
            callbackInvoked = true
        }

        TerminalStateProvider.requestToggleIme()

        assertTrue(callbackInvoked, "onToggleImeRequest callback should be invoked")

        // クリーンアップ
        TerminalStateProvider.onToggleImeRequest = null
    }

    /**
     * onToggleImeRequestがnullの場合、requestToggleIme()が例外を投げないことを検証。
     */
    @Test
    fun testRequestToggleImeWithNullCallback() {
        TerminalStateProvider.onToggleImeRequest = null

        // 例外が発生しないことを確認
        TerminalStateProvider.requestToggleIme()
    }

    /**
     * setCommandMode()がisCommandModeの状態を更新することを検証。
     */
    @Test
    fun testSetCommandModeUpdatesState() {
        TerminalStateProvider.setCommandMode(true)
        assertTrue(TerminalStateProvider.isCommandMode.value)

        TerminalStateProvider.setCommandMode(false)
        assertFalse(TerminalStateProvider.isCommandMode.value)
    }

    /**
     * setHardwareKeyboardConnected()がisHardwareKeyboardConnectedの状態を更新することを検証。
     */
    @Test
    fun testSetHardwareKeyboardConnectedUpdatesState() {
        TerminalStateProvider.setHardwareKeyboardConnected(true)
        assertTrue(TerminalStateProvider.isHardwareKeyboardConnected.value)

        TerminalStateProvider.setHardwareKeyboardConnected(false)
        assertFalse(TerminalStateProvider.isHardwareKeyboardConnected.value)
    }
}
