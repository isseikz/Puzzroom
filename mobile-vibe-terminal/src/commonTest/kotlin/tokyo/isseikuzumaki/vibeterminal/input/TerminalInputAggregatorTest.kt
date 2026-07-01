package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalInputAggregatorTest {

    private fun aggregator(bitmask: Int = 0): TerminalInputAggregator {
        var consumed = false
        return TerminalInputAggregator(
            getModifierBitmask = { bitmask },
            consumeOneShotModifiers = { consumed = true },
            onDispatch = {}
        )
    }

    // ── applyModifierToChar ───────────────────────────────────────────────────

    @Test
    fun `no modifier - char passes through unchanged`() {
        val a = aggregator(bitmask = 0)
        assertEquals("a", a.applyModifierToChar('a', 0))
    }

    @Test
    fun `Ctrl + a returns control character 0x01`() {
        val a = aggregator()
        assertEquals("\u0001", a.applyModifierToChar('a', bitmask = 4))
    }

    @Test
    fun `Ctrl + c returns control character 0x03`() {
        val a = aggregator()
        assertEquals("\u0003", a.applyModifierToChar('c', bitmask = 4))
    }

    @Test
    fun `Alt + f returns ESC prefix`() {
        val a = aggregator()
        assertEquals("\u001Bf", a.applyModifierToChar('f', bitmask = 2))
    }

    @Test
    fun `Alt + Ctrl + c returns ESC + control byte`() {
        val a = aggregator()
        assertEquals("\u001B\u0003", a.applyModifierToChar('c', bitmask = 6)) // Alt=2 + Ctrl=4
    }

    @Test
    fun `Ctrl + unmapped char returns char unchanged`() {
        // '!' has no ctrl mapping → pass through as-is
        val a = aggregator()
        assertEquals("!", a.applyModifierToChar('!', bitmask = 4))
    }

    // ── applyModifierToSequence ───────────────────────────────────────────────

    @Test
    fun `Shift + Tab returns backtab sequence via applyModifierToSequence`() {
        val a = aggregator()
        assertEquals("\u001B[Z", a.applyModifierToSequence("\t", bitmask = 1))
    }

    @Test
    fun `Shift + Tab send routes tab through applyModifierToSequence not applyModifierToChar`() {
        val dispatched = mutableListOf<String>()
        val a = TerminalInputAggregator(
            getModifierBitmask = { 1 },  // Shift active
            consumeOneShotModifiers = {},
            onDispatch = { dispatched.add(it) }
        )
        kotlinx.coroutines.runBlocking { a.send("\t") }
        assertEquals("\u001B[Z", dispatched.single())
    }

    @Test
    fun `no Shift + Tab returns plain tab`() {
        val a = aggregator()
        assertEquals("\t", a.applyModifierToSequence("\t", bitmask = 4)) // Ctrl only
    }

    @Test
    fun `Ctrl + Up arrow returns CSI parameterized sequence`() {
        val a = aggregator()
        assertEquals("\u001B[1;5A", a.applyModifierToSequence("\u001B[A", bitmask = 4))
    }

    @Test
    fun `Shift + Up arrow returns CSI with modifier number 2`() {
        val a = aggregator()
        assertEquals("\u001B[1;2A", a.applyModifierToSequence("\u001B[A", bitmask = 1))
    }

    @Test
    fun `Shift + Ctrl + Up returns CSI modifier number 6`() {
        val a = aggregator()
        assertEquals("\u001B[1;6A", a.applyModifierToSequence("\u001B[A", bitmask = 5))
    }

    @Test
    fun `Ctrl + Right returns CSI 1 semicolon 5C`() {
        val a = aggregator()
        assertEquals("\u001B[1;5C", a.applyModifierToSequence("\u001B[C", bitmask = 4))
    }

    @Test
    fun `Alt + Left returns CSI 1 semicolon 3D`() {
        val a = aggregator()
        assertEquals("\u001B[1;3D", a.applyModifierToSequence("\u001B[D", bitmask = 2))
    }

    @Test
    fun `Ctrl + Home returns parameterized Home sequence`() {
        val a = aggregator()
        assertEquals("\u001B[1;5H", a.applyModifierToSequence("\u001B[H", bitmask = 4))
    }

    @Test
    fun `Ctrl + End returns parameterized End sequence`() {
        val a = aggregator()
        assertEquals("\u001B[1;5F", a.applyModifierToSequence("\u001B[F", bitmask = 4))
    }

    @Test
    fun `unknown multi-char sequence passes through unchanged`() {
        val a = aggregator()
        assertEquals("\u001B[2~", a.applyModifierToSequence("\u001B[2~", bitmask = 4))
    }

    // ── send() - side effect: consumeOneShotModifiers ─────────────────────────

    @Test
    fun `send with active modifier calls consumeOneShotModifiers`() {
        var consumed = false
        val dispatched = mutableListOf<String>()
        val a = TerminalInputAggregator(
            getModifierBitmask = { 4 },  // Ctrl active
            consumeOneShotModifiers = { consumed = true },
            onDispatch = { dispatched.add(it) }
        )
        kotlinx.coroutines.runBlocking { a.send("c") }
        assertEquals(true, consumed)
        assertEquals("\u0003", dispatched.single())
    }

    @Test
    fun `send with no modifier does not call consumeOneShotModifiers`() {
        var consumed = false
        val a = TerminalInputAggregator(
            getModifierBitmask = { 0 },
            consumeOneShotModifiers = { consumed = true },
            onDispatch = {}
        )
        kotlinx.coroutines.runBlocking { a.send("a") }
        assertEquals(false, consumed)
    }

    // ── dispatch() - skips modifier application ───────────────────────────────

    @Test
    fun `dispatch sends sequence unchanged even when modifier active`() {
        val dispatched = mutableListOf<String>()
        val a = TerminalInputAggregator(
            getModifierBitmask = { 4 },  // Ctrl active
            consumeOneShotModifiers = {},
            onDispatch = { dispatched.add(it) }
        )
        kotlinx.coroutines.runBlocking { a.dispatch("\u0003") }
        assertEquals("\u0003", dispatched.single())  // not double-applied
    }

    @Test
    fun `dispatch with active modifier still calls consumeOneShotModifiers`() {
        var consumed = false
        val a = TerminalInputAggregator(
            getModifierBitmask = { 4 },
            consumeOneShotModifiers = { consumed = true },
            onDispatch = {}
        )
        kotlinx.coroutines.runBlocking { a.dispatch("\u0003") }
        assertEquals(true, consumed)
    }
}
