package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * [KeyboardInputProcessor]のユニットテスト。
 *
 * このテストスイートは、キーボード入力からターミナルエスケープシーケンスへの変換を検証する。
 * ANSI X3.64 / ECMA-48標準およびxterm拡張に準拠している。
 *
 * ## 背景: ターミナルエスケープシーケンス
 *
 * ターミナルエミュレータは、特殊キー（矢印、ファンクションキー等）を「エスケープシーケンス」
 * （ESC文字 0x1B で始まる文字列）を使用して通信する。これらのシーケンスは1970〜80年代に
 * VT100/VT220などのハードウェアターミナル向けに標準化された。
 *
 * 主なシーケンス形式:
 * - **CSIシーケンス**: `ESC [` の後にパラメータが続く（例: 上矢印は `ESC[A`）
 * - **SS3シーケンス**: `ESC O` の後に文字が続く（例: F1は `ESC OP`）
 *
 * ## 制御文字
 *
 * Ctrl+文字の組み合わせはASCII制御文字（0x00-0x1F）を生成する。
 * このマッピングは1963年のASCII標準に由来する:
 * - Ctrl+A = 0x01, Ctrl+B = 0x02, ..., Ctrl+Z = 0x1A
 * - 制御文字は次の式で計算: (文字のASCIIコード) - 0x40
 *
 * ## JISキーボードレイアウト
 *
 * この実装はApple Magic Keyboard JIS（日本工業規格）レイアウトに最適化されている。
 * USレイアウトとは記号キーの位置が異なる:
 * - Shift+2 は `"` を生成（US: `@`）
 * - Shift+6 は `&` を生成（US: `^`）
 * - 専用のYEN（¥）キーとRO（ろ）キーが存在
 *
 * @see KeyboardInputProcessor
 * @see <a href="https://invisible-island.net/xterm/ctlseqs/ctlseqs.html">XTerm Control Sequences</a>
 * @see <a href="https://ecma-international.org/publications-and-standards/standards/ecma-48/">ECMA-48 Standard</a>
 */
class KeyboardInputProcessorTest {

    /**
     * テスト用のキーダウンイベントを作成するヘルパー関数。
     *
     * @param keyCode [KeyCodes]からのキーコード
     * @param ctrl Ctrlモディファイアが押されているか
     * @param alt Altモディファイアが押されているか
     * @param shift Shiftモディファイアが押されているか
     * @param meta Meta（Macのコマンドキー）モディファイアが押されているか
     */
    private fun keyDown(
        keyCode: Int,
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        meta: Boolean = false
    ) = KeyEventData(
        keyCode = keyCode,
        action = KeyAction.DOWN,
        isCtrlPressed = ctrl,
        isAltPressed = alt,
        isShiftPressed = shift,
        isMetaPressed = meta
    )

    /**
     * テスト用のキーアップイベントを作成するヘルパー関数。
     */
    private fun keyUp(keyCode: Int) = KeyEventData(
        keyCode = keyCode,
        action = KeyAction.UP
    )

    // =========================================================================
    // キーイベントフィルタリングのテスト
    // =========================================================================

    /**
     * KEY_UPイベントが無視されることを検証。
     *
     * ## 理由
     * ターミナル入力は文字ベースであり、イベントベースではない。KEY_DOWNイベントのみを
     * 処理してターミナルに文字/シーケンスを送信する。KEY_UPを処理すると二重入力になる。
     *
     * これはターミナルエミュレータの標準的な動作に従っている。キーリリースイベントは
     * リモートホストに送信されない。
     */
    @Test
    fun testKeyUpIsIgnored() {
        val result = KeyboardInputProcessor.processKeyEvent(keyUp(KeyCodes.A))
        assertIs<KeyboardInputProcessor.KeyResult.Ignored>(result)
    }

    /**
     * 単独のモディファイアキー押下が無視されることを検証。
     *
     * ## 理由
     * モディファイアキー（Shift、Ctrl、Alt、Meta）単体ではターミナル入力を生成しない。
     * 他のキーと組み合わせて押されたときのみ、その動作を変更する。
     *
     * これらを無視しないと、Shiftキーを単独で押したときにターミナルに何かが送信されてしまい、
     * 不正な動作となる。
     *
     * ## 対象キー
     * - Shift（左/右）
     * - Ctrl（左/右）
     * - Alt（左/右）
     * - Meta/Command（左/右）
     * - Caps Lock、Num Lock、Scroll Lock
     * - Functionキー（Fn）
     */
    @Test
    fun testModifierKeysAreIgnored() {
        val modifierKeys = listOf(
            KeyCodes.SHIFT_LEFT,
            KeyCodes.SHIFT_RIGHT,
            KeyCodes.CTRL_LEFT,
            KeyCodes.CTRL_RIGHT,
            KeyCodes.ALT_LEFT,
            KeyCodes.ALT_RIGHT,
            KeyCodes.META_LEFT,
            KeyCodes.META_RIGHT,
            KeyCodes.CAPS_LOCK,
            KeyCodes.NUM_LOCK,
            KeyCodes.SCROLL_LOCK,
            KeyCodes.FUNCTION
        )

        modifierKeys.forEach { keyCode ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.Ignored>(result, "Modifier key $keyCode should be ignored")
        }
    }

    /**
     * モディファイアなしの通常の文字キーがPassThroughを返すことを検証。
     *
     * ## 理由
     * 通常の文字入力（Ctrl/Altなしの文字、数字）は、このプロセッサではなく
     * システムのInputConnection/IMEで処理されるべき。これはハードウェアキーボードを
     * AndroidのテキストInputシステムと併用する際の二重入力を防ぐために重要。
     *
     * PassThrough結果は、呼び出し元に対してこのキーを通常のテキスト入力パイプラインで
     * システムに処理させるよう指示する。
     */
    @Test
    fun testRegularLetterKeysPassThrough() {
        val letters = listOf(KeyCodes.A, KeyCodes.B, KeyCodes.Z)
        letters.forEach { keyCode ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.PassThrough>(result)
        }
    }

    // =========================================================================
    // 矢印キーのテスト - ANSIカーソルキーシーケンス
    // =========================================================================

    /**
     * 上矢印キーが`ESC[A`を生成することを検証。
     *
     * ## ターミナル標準
     * 矢印キーはANSIカーソル移動シーケンス（CSIシーケンス）を使用:
     * - 上:    `ESC[A` (CSI A)
     * - 下:    `ESC[B` (CSI B)
     * - 右:    `ESC[C` (CSI C)
     * - 左:    `ESC[D` (CSI D)
     *
     * これらのシーケンスはECMA-48で定義されており、ターミナルエミュレータで
     * 広くサポートされている。vim、less、シェルなどのアプリケーションは
     * カーソル移動やコマンド履歴ナビゲーションの実装にこれらを使用する。
     */
    @Test
    fun testArrowKeyUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[A", result.sequence)
    }

    /**
     * 下矢印キーが`ESC[B`を生成することを検証。
     * @see testArrowKeyUp シーケンス形式の説明
     */
    @Test
    fun testArrowKeyDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[B", result.sequence)
    }

    /**
     * 右矢印キーが`ESC[C`を生成することを検証。
     * @see testArrowKeyUp シーケンス形式の説明
     */
    @Test
    fun testArrowKeyRight() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_RIGHT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[C", result.sequence)
    }

    /**
     * 左矢印キーが`ESC[D`を生成することを検証。
     * @see testArrowKeyUp シーケンス形式の説明
     */
    @Test
    fun testArrowKeyLeft() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_LEFT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[D", result.sequence)
    }

    // =========================================================================
    // ナビゲーションキーのテスト - PCスタイルキー
    // =========================================================================

    /**
     * Homeキーが`ESC[H`を生成することを検証。
     *
     * ## ターミナル標準
     * ナビゲーションキーは拡張CSIシーケンスを使用。HomeとEndキーは
     * xtermのデフォルトモードでより短いシーケンス（`ESC[H`と`ESC[F`）を持つ。
     *
     * 注: 一部のターミナルはHomeに`ESC[1~`を使用するが、`ESC[H`はxterm規約に
     * 従う現代のターミナルエミュレータでより一般的。
     */
    @Test
    fun testHomeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_HOME))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[H", result.sequence)
    }

    /**
     * Endキーが`ESC[F`を生成することを検証。
     * @see testHomeKey シーケンス形式の説明
     */
    @Test
    fun testEndKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_END))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[F", result.sequence)
    }

    /**
     * Page Upが`ESC[5~`を生成することを検証。
     *
     * ## ターミナル標準
     * Page Up/Down、Insert、Deleteは「チルダ」形式を使用: `ESC[n~`
     * nはキーを識別する数字:
     * - Insert:    `ESC[2~`
     * - Delete:    `ESC[3~`
     * - Page Up:   `ESC[5~`
     * - Page Down: `ESC[6~`
     *
     * これらはVT220ターミナルに由来する。
     */
    @Test
    fun testPageUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[5~", result.sequence)
    }

    /**
     * Page Downが`ESC[6~`を生成することを検証。
     * @see testPageUp シーケンス形式の説明
     */
    @Test
    fun testPageDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[6~", result.sequence)
    }

    /**
     * Insertキーが`ESC[2~`を生成することを検証。
     * @see testPageUp シーケンス形式の説明
     */
    @Test
    fun testInsertKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.INSERT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[2~", result.sequence)
    }

    /**
     * Deleteキーが`ESC[3~`を生成することを検証。
     *
     * ## 重要な区別
     * Deleteキー（前方削除）はBackspaceとは異なる:
     * - Delete (FORWARD_DEL): `ESC[3~` - カーソル位置/後の文字を削除
     * - Backspace (DEL): `0x7F` - カーソル前の文字を削除
     *
     * @see testBackspaceKey
     */
    @Test
    fun testDeleteKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.FORWARD_DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[3~", result.sequence)
    }

    // =========================================================================
    // 特殊キーのテスト
    // =========================================================================

    /**
     * Enterキーが CR (0x0D) を生成することを検証。
     *
     * ## ターミナル標準
     * Enter/ReturnはCarriage Return（CR、0x0D、'\r'）を送信する。
     * ターミナルまたはシェルは、ターミナル設定に基づいてこれを適切な
     * 改行コード（CR、LF、またはCRLF）に変換する。
     *
     * 注: 一部のシステムはLF (0x0A) を期待するが、CRはキーボードEnterキー
     * 入力の標準。
     */
    @Test
    fun testEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    /**
     * テンキーのEnterがメインのEnterキーと同じ動作をすることを検証。
     *
     * ## 理由
     * 一貫性のため、両方のEnterキーは同じ結果を生成すべき。
     * 一部のアプリケーションはこれらを区別するが、ターミナル入力では
     * 同等に扱う。
     */
    @Test
    fun testNumpadEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUMPAD_ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    /**
     * Tabキーが HT (0x09) を生成することを検証。
     *
     * ## ターミナル標準
     * TabはHorizontal Tab文字（HT、0x09、'\t'）を送信する。
     * これは元のASCII制御文字の一つ。
     */
    @Test
    fun testTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\t", result.sequence)
    }

    /**
     * Shift+Tabがバックタブシーケンス`ESC[Z`を生成することを検証。
     *
     * ## ターミナル標準
     * Shift+Tab（バックタブまたはリバースタブ）は`ESC[Z`（CSI Z）を送信する。
     * これはCBT（Cursor Backward Tabulation）としても知られる。
     *
     * アプリケーションでフォームフィールドやUI要素を逆方向にナビゲート
     * するために使用される。
     */
    @Test
    fun testShiftTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[Z", result.sequence)
    }

    /**
     * Escapeキーが ESC (0x1B) を生成することを検証。
     *
     * ## ターミナル標準
     * EscapeキーはESC文字（0x1B）を送信する。これはすべてのエスケープ
     * シーケンスの開始文字でもある。vimなどのアプリケーションはモード切替に
     * これを使用する。
     */
    @Test
    fun testEscapeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ESCAPE))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence)
    }

    /**
     * Backspaceキーが DEL (0x7F) を生成することを検証。
     *
     * ## ターミナル標準
     * 現代のターミナルはBackspaceにDEL（0x7F）を送信する。これはxtermの
     * デフォルトであり、最も互換性の高い選択。
     *
     * ## 歴史的背景
     * 古いシステムはBackspaceにBS（0x08）を送信することがあった。DELとBSの
     * 選択は設定問題の原因となってきた。DELは現在ターミナルエミュレータの
     * 事実上の標準となっている。
     *
     * @see testDeleteKey 前方削除について
     */
    @Test
    fun testBackspaceKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u007F", result.sequence)
    }

    // =========================================================================
    // ファンクションキーのテスト - VT220/xtermシーケンス
    // =========================================================================

    /**
     * すべてのファンクションキー（F1-F12）が正しいエスケープシーケンスを生成することを検証。
     *
     * ## ターミナル標準
     * ファンクションキーは2つの異なるシーケンス形式を使用:
     *
     * ### F1-F4: SS3シーケンス（VT100スタイル）
     * - F1: `ESC O P`
     * - F2: `ESC O Q`
     * - F3: `ESC O R`
     * - F4: `ESC O S`
     *
     * VT100由来のSS3（Single Shift 3、ESC O）プレフィックスを使用。
     *
     * ### F5-F12: CSIチルダシーケンス（VT220スタイル）
     * - F5:  `ESC[15~`
     * - F6:  `ESC[17~`
     * - F7:  `ESC[18~`
     * - F8:  `ESC[19~`
     * - F9:  `ESC[20~`
     * - F10: `ESC[21~`
     * - F11: `ESC[23~`
     * - F12: `ESC[24~`
     *
     * 番号に欠番がある（16、22がない）のは、VT220でそれらのコードが
     * 他のキーに使用されていた歴史的経緯による。
     */
    @Test
    fun testFunctionKeys() {
        val expectedSequences = mapOf(
            KeyCodes.F1 to "\u001BOP",
            KeyCodes.F2 to "\u001BOQ",
            KeyCodes.F3 to "\u001BOR",
            KeyCodes.F4 to "\u001BOS",
            KeyCodes.F5 to "\u001B[15~",
            KeyCodes.F6 to "\u001B[17~",
            KeyCodes.F7 to "\u001B[18~",
            KeyCodes.F8 to "\u001B[19~",
            KeyCodes.F9 to "\u001B[20~",
            KeyCodes.F10 to "\u001B[21~",
            KeyCodes.F11 to "\u001B[23~",
            KeyCodes.F12 to "\u001B[24~"
        )

        expectedSequences.forEach { (keyCode, expected) ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.Handled>(result, "F key $keyCode should be handled")
            assertEquals(expected, result.sequence, "F key $keyCode sequence mismatch")
        }
    }

    // =========================================================================
    // Ctrl+キー組み合わせのテスト - ASCII制御文字
    // =========================================================================

    /**
     * Ctrl+CがASCII 3（ETX - End of Text）を生成することを検証。
     *
     * ## Unixシグナル
     * Ctrl+CはフォアグラウンドプロセスにSIGINTを送信し、通常は実行中の
     * コマンドを中断/キャンセルするために使用される。これは最も重要な
     * ターミナル制御シーケンスの一つ。
     */
    @Test
    fun testCtrlC() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.C, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0003", result.sequence)
    }

    /**
     * Ctrl+DがASCII 4（EOT - End of Transmission）を生成することを検証。
     *
     * ## Unix動作
     * Ctrl+DはターミナルにEOF（End of File）を通知する。シェルで空行に
     * 入力すると、通常はログアウトまたはシェルを閉じる。
     * stdinを読み取るプログラムでは、入力の終了を通知する。
     */
    @Test
    fun testCtrlD() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.D, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0004", result.sequence)
    }

    /**
     * Ctrl+ZがASCII 26（SUB - Substitute）を生成することを検証。
     *
     * ## Unixシグナル
     * Ctrl+ZはSIGTSTP（Terminal Stop）を送信し、フォアグラウンドプロセスを
     * サスペンドする。プロセスは`fg`（フォアグラウンド）または`bg`
     * （バックグラウンド）コマンドで再開できる。
     */
    @Test
    fun testCtrlZ() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.Z, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001A", result.sequence)
    }

    /**
     * Ctrl+LがASCII 12（FF - Form Feed）を生成することを検証。
     *
     * ## ターミナル動作
     * Ctrl+Lは慣例的にターミナル画面のクリア/リフレッシュに使用される。
     * ほとんどのシェルとターミナルアプリケーションは、これに応答して
     * ディスプレイを再描画する。
     */
    @Test
    fun testCtrlL() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.L, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u000C", result.sequence)
    }

    /**
     * Ctrl+[がASCII 27（ESC）を生成することを検証。
     *
     * ## vimユーザー向け
     * これはvimユーザーにとって重要 - Ctrl+[はEscapeキーを押すのと同等。
     * これは'['がASCII 0x5Bで、0x5B - 0x40 = 0x1B（ESC）となるため。
     *
     * ホームポジションから手を動かさずにEscape機能を使用できる。
     */
    @Test
    fun testCtrlLeftBracket() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.LEFT_BRACKET, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence)
    }

    /**
     * Ctrl+SpaceがASCII 0（NUL）を生成することを検証。
     *
     * ## Emacsユーザー向け
     * Ctrl+SpaceはEmacsでテキスト選択のマークを設定するために一般的に使用される。
     * NUL文字（0x00）がアプリケーションに送信される。
     */
    @Test
    fun testCtrlSpace() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.SPACE, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0000", result.sequence)
    }

    /**
     * すべてのCtrl+文字組み合わせが正しいASCII制御文字を生成することを検証。
     *
     * ## ASCII制御文字マッピング
     * マッピングは元のASCII標準に従う:
     * - Ctrl+A = 0x01 (SOH - Start of Heading)
     * - Ctrl+B = 0x02 (STX - Start of Text)
     * - ...
     * - Ctrl+Z = 0x1A (SUB - Substitute)
     *
     * 計算式: Ctrl+X = ASCII(X) - 0x40
     * 例: Ctrl+A = 0x41 - 0x40 = 0x01
     *
     * ## 一般的な用途
     * - Ctrl+A (1): 全選択、tmuxプレフィックス、readline行頭
     * - Ctrl+C (3): SIGINT - プロセス中断
     * - Ctrl+D (4): EOF - ファイル/入力終端
     * - Ctrl+E (5): readline行末
     * - Ctrl+K (11): readline行末まで削除
     * - Ctrl+U (21): readline行頭まで削除
     * - Ctrl+W (23): readline前の単語を削除
     * - Ctrl+Z (26): SIGTSTP - プロセスサスペンド
     */
    @Test
    fun testAllCtrlLetters() {
        val letters = listOf(
            KeyCodes.A to 1, KeyCodes.B to 2, KeyCodes.C to 3, KeyCodes.D to 4,
            KeyCodes.E to 5, KeyCodes.F to 6, KeyCodes.G to 7, KeyCodes.H to 8,
            KeyCodes.I to 9, KeyCodes.J to 10, KeyCodes.K to 11, KeyCodes.L to 12,
            KeyCodes.M to 13, KeyCodes.N to 14, KeyCodes.O to 15, KeyCodes.P to 16,
            KeyCodes.Q to 17, KeyCodes.R to 18, KeyCodes.S to 19, KeyCodes.T to 20,
            KeyCodes.U to 21, KeyCodes.V to 22, KeyCodes.W to 23, KeyCodes.X to 24,
            KeyCodes.Y to 25, KeyCodes.Z to 26
        )

        letters.forEach { (keyCode, expectedAscii) ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode, ctrl = true))
            assertIs<KeyboardInputProcessor.KeyResult.Handled>(result, "Ctrl+letter should be handled")
            assertEquals(expectedAscii.toChar().toString(), result.sequence, "Ctrl+$keyCode should produce ASCII $expectedAscii")
        }
    }

    // =========================================================================
    // Alt+キー組み合わせのテスト - Meta/Escapeプレフィックス
    // =========================================================================

    /**
     * Alt+Aが`ESC a`（0x1B 0x61）を生成することを検証。
     *
     * ## ターミナル標準
     * Alt（Meta）キー組み合わせは、ESCの後に文字を続けて送信される。
     * これは「Meta sends Escape」モードとして知られ、ターミナル
     * アプリケーションで最も互換性の高いアプローチ。
     *
     * ## 代替エンコーディング
     * 一部のシステムは「8-bit Meta」を使用し、Alt+Aが0xE1（0x61 | 0x80）を
     * 送信するが、これはあまり一般的ではなく、UTF-8エンコーディングと
     * 競合する可能性がある。
     */
    @Test
    fun testAltA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001Ba", result.sequence)
    }

    /**
     * Alt+Shift+Aが`ESC A`（大文字）を生成することを検証。
     *
     * ## Shiftとの相互作用
     * Shiftも押されている場合、ESCの後の文字は大文字になる。
     * これによりアプリケーションはAlt+aとAlt+Aを区別できる。
     */
    @Test
    fun testAltShiftA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001BA", result.sequence)
    }

    /**
     * Alt+数字が`ESC`の後に数字を続けて生成することを検証。
     *
     * ## 用途
     * Alt+数字の組み合わせは様々なアプリケーションで使用される:
     * - ウィンドウマネージャ: デスクトップ切替（Alt+1、Alt+2など）
     * - ターミナル: タブ切替
     * - エディタ: クイックナビゲーション
     */
    @Test
    fun testAltNumber() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUM_1, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B1", result.sequence)
    }

    // =========================================================================
    // isModifierKeyのテスト
    // =========================================================================

    /**
     * モディファイアキーの正しい識別を検証。
     *
     * ## 目的
     * この関数は、ターミナル入力を生成すべきではない単独のモディファイアキー
     * 押下をフィルタリングするために使用される。
     */
    @Test
    fun testIsModifierKeyTrue() {
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.SHIFT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CTRL_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.ALT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.META_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CAPS_LOCK))
    }

    /**
     * 非モディファイアキーが正しく識別されることを検証。
     */
    @Test
    fun testIsModifierKeyFalse() {
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.A))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.ENTER))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.DPAD_UP))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.F1))
    }

    // =========================================================================
    // getSpecialKeySequenceのテスト
    // =========================================================================

    /**
     * 通常のキーが特殊シーケンスを持たないことを検証。
     *
     * 通常の文字キー（文字、数字）はgetSpecialKeySequenceからnullを返すべき。
     * これは、それらが異なる方法（Ctrl/Alt組み合わせまたはシステムへの
     * パススルー）で処理されるべきであることを示す。
     */
    @Test
    fun testGetSpecialKeySequenceReturnsNullForRegularKeys() {
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.A, hasShift = false))
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.NUM_1, hasShift = false))
    }

    /**
     * 矢印キーシーケンス生成を検証。
     * @see testArrowKeyUp 詳細な説明
     */
    @Test
    fun testGetSpecialKeySequenceArrows() {
        assertEquals("\u001B[A", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_UP, hasShift = false))
        assertEquals("\u001B[B", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_DOWN, hasShift = false))
        assertEquals("\u001B[C", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_RIGHT, hasShift = false))
        assertEquals("\u001B[D", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_LEFT, hasShift = false))
    }

    // =========================================================================
    // getCtrlKeySequenceのテスト
    // =========================================================================

    /**
     * 非制御キーがnullを返すことを検証。
     *
     * 定義されたCtrlマッピングを持つキーのみがシーケンスを返すべき。
     * 矢印キーとファンクションキーはCtrl文字の相当物を持たない。
     */
    @Test
    fun testGetCtrlKeySequenceReturnsNullForUnmappedKeys() {
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.DPAD_UP))
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.F1))
    }

    /**
     * 特殊なCtrlキー組み合わせを検証。
     *
     * ## Ctrl+[ = ESC
     * [testCtrlLeftBracket]で説明したように、これは制御文字計算式の結果:
     * '[' (0x5B) - 0x40 = 0x1B (ESC)
     *
     * ## Ctrl+Space = NUL
     * Space (0x20) とCtrlモディファイアは通常NUL (0x00) を送信する。
     * これは計算式から外れるが、確立された慣例。
     */
    @Test
    fun testGetCtrlKeySequenceSpecialCases() {
        assertEquals("\u001B", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.LEFT_BRACKET))
        assertEquals("\u0000", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.SPACE))
    }

    // =========================================================================
    // getCharFromKeyCodeのテスト
    // =========================================================================

    /**
     * Shift状態による文字キーの文字生成を検証。
     *
     * Alt+キー組み合わせに使用される基本的な文字マッピングをテストする。
     */
    @Test
    fun testGetCharFromKeyCodeLetters() {
        assertEquals('a', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = false))
        assertEquals('A', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = true))
        assertEquals('z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = false))
        assertEquals('Z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = true))
    }

    /**
     * 数字キーの文字生成を検証。
     *
     * 注: Shift記号はUSレイアウトではなくJISレイアウトに従う。
     * @see testJisLayoutShiftNumbers JIS固有の記号について
     */
    @Test
    fun testGetCharFromKeyCodeNumbers() {
        assertEquals('1', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = false))
        assertEquals('!', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = true))
        assertEquals('0', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_0, hasShift = false))
    }

    /**
     * 記号キーの文字生成を検証。
     */
    @Test
    fun testGetCharFromKeyCodeSymbols() {
        assertEquals('-', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = false))
        assertEquals('=', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = true))
        assertEquals('[', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = false))
        assertEquals('{', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = true))
    }

    /**
     * 特殊キーが文字にマップされないことを検証。
     *
     * 矢印キー、Enter、ファンクションキーなどは印刷可能な文字を生成せず、
     * この関数からnullを返すべき。
     */
    @Test
    fun testGetCharFromKeyCodeReturnsNullForSpecialKeys() {
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.DPAD_UP, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.ENTER, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.F1, hasShift = false))
    }

    // =========================================================================
    // JISキーボードレイアウトのテスト
    // =========================================================================

    /**
     * JISレイアウトのShift+数字記号マッピングを検証。
     *
     * ## JIS vs USレイアウトの違い
     * JIS（日本工業規格）キーボードレイアウトは、USレイアウトとは
     * Shift+数字の記号が異なる:
     *
     * | キー | JIS (Shift) | US (Shift) |
     * |------|-------------|------------|
     * | 2    | "           | @          |
     * | 6    | &           | ^          |
     * | 7    | '           | &          |
     * | 8    | (           | *          |
     * | 9    | )           | (          |
     *
     * この実装はApple Magic Keyboard JISレイアウトを対象としている。
     * 日本で一般的に使用されている。
     */
    @Test
    fun testJisLayoutShiftNumbers() {
        assertEquals('"', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_2, hasShift = true))
        assertEquals('&', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_6, hasShift = true))
        assertEquals('\'', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_7, hasShift = true))
        assertEquals('(', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_8, hasShift = true))
        assertEquals(')', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_9, hasShift = true))
    }

    /**
     * JIS固有キー（YENとROキー）の処理を検証。
     *
     * ## JIS固有キー
     * JISキーボードにはUSキーボードにない追加のキーがある:
     *
     * ### YENキー（¥）
     * - USキーボードのバックスラッシュの位置にある
     * - Shiftなし: バックスラッシュ '\' を生成（パス区切り用）
     * - Shift: パイプ '|' を生成
     *
     * ### ROキー（ろ）
     * - 右Shiftの近くに位置
     * - 日本語入力に使用
     * - Shiftなし: バックスラッシュ '\' を生成
     * - Shift: アンダースコア '_' を生成
     *
     * 注: AndroidはこれらをKEYCODE_BACKSLASHとは別のキーコード
     * （KEYCODE_YEN、KEYCODE_RO）として報告する。
     */
    @Test
    fun testJisSpecificKeys() {
        // YENキー（¥）
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = false))
        assertEquals('|', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = true))
        // ROキー（ろ）
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = false))
        assertEquals('_', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = true))
    }
}
