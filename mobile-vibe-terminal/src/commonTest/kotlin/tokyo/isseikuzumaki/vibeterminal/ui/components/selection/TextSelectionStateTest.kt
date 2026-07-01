package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextSelectionStateTest {

    @Test
    fun `空の選択状態では hasSelection は false`() {
        val state = TextSelectionState.Empty
        assertFalse(state.hasSelection)
        assertFalse(state.isSelecting)
        assertNull(state.anchorPosition)
        assertNull(state.currentPosition)
    }

    @Test
    fun `選択中でアンカーとカレントが設定されている場合 hasSelection は true`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(0, 5)
        )
        assertTrue(state.hasSelection)
    }

    @Test
    fun `isSelecting が false の場合 hasSelection は false`() {
        val state = TextSelectionState(
            isSelecting = false,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(0, 5)
        )
        assertFalse(state.hasSelection)
    }

    @Test
    fun `startPosition と endPosition は常に正しい順序で返される（左から右へ選択）`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 2),
            currentPosition = TerminalPosition(0, 8)
        )
        assertEquals(TerminalPosition(0, 2), state.startPosition)
        assertEquals(TerminalPosition(0, 8), state.endPosition)
    }

    @Test
    fun `startPosition と endPosition は常に正しい順序で返される（右から左へ選択）`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 8),
            currentPosition = TerminalPosition(0, 2)
        )
        assertEquals(TerminalPosition(0, 2), state.startPosition)
        assertEquals(TerminalPosition(0, 8), state.endPosition)
    }

    @Test
    fun `startPosition と endPosition は常に正しい順序で返される（複数行、上から下へ選択）`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(1, 5),
            currentPosition = TerminalPosition(3, 10)
        )
        assertEquals(TerminalPosition(1, 5), state.startPosition)
        assertEquals(TerminalPosition(3, 10), state.endPosition)
    }

    @Test
    fun `startPosition と endPosition は常に正しい順序で返される（複数行、下から上へ選択）`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(3, 10),
            currentPosition = TerminalPosition(1, 5)
        )
        assertEquals(TerminalPosition(1, 5), state.startPosition)
        assertEquals(TerminalPosition(3, 10), state.endPosition)
    }

    @Test
    fun `単一行選択でセルが選択範囲内かどうかを正しく判定`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 3),
            currentPosition = TerminalPosition(0, 7)
        )

        // 選択範囲外（左）
        assertFalse(state.isCellSelected(0, 0))
        assertFalse(state.isCellSelected(0, 2))

        // 選択範囲内
        assertTrue(state.isCellSelected(0, 3))
        assertTrue(state.isCellSelected(0, 5))
        assertTrue(state.isCellSelected(0, 7))

        // 選択範囲外（右）
        assertFalse(state.isCellSelected(0, 8))
        assertFalse(state.isCellSelected(0, 10))

        // 別の行
        assertFalse(state.isCellSelected(1, 5))
    }

    @Test
    fun `複数行選択でセルが選択範囲内かどうかを正しく判定`() {
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(1, 5),
            currentPosition = TerminalPosition(3, 10)
        )

        // 開始行より前
        assertFalse(state.isCellSelected(0, 5))

        // 開始行（5列目以降が選択範囲）
        assertFalse(state.isCellSelected(1, 4))
        assertTrue(state.isCellSelected(1, 5))
        assertTrue(state.isCellSelected(1, 10))
        assertTrue(state.isCellSelected(1, 20))

        // 中間行（全列が選択範囲）
        assertTrue(state.isCellSelected(2, 0))
        assertTrue(state.isCellSelected(2, 50))

        // 終了行（10列目まで選択範囲）
        assertTrue(state.isCellSelected(3, 0))
        assertTrue(state.isCellSelected(3, 10))
        assertFalse(state.isCellSelected(3, 11))

        // 終了行より後
        assertFalse(state.isCellSelected(4, 5))
    }

    @Test
    fun `TerminalPosition の比較が正しく動作する`() {
        val pos1 = TerminalPosition(0, 5)
        val pos2 = TerminalPosition(0, 10)
        val pos3 = TerminalPosition(1, 0)
        val pos4 = TerminalPosition(1, 5)

        assertTrue(pos1 < pos2)
        assertTrue(pos2 < pos3)
        assertTrue(pos3 < pos4)
        assertTrue(pos1 < pos3)
        assertTrue(pos1 < pos4)

        assertEquals(0, pos1.compareTo(pos1))
        assertEquals(0, TerminalPosition(2, 3).compareTo(TerminalPosition(2, 3)))
    }
}
