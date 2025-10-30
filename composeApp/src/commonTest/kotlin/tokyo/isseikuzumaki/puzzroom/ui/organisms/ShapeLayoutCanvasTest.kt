package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ShapeLayoutCanvas hit detection functions
 */
class ShapeLayoutCanvasTest {

    @Test
    fun `distanceFromPointToLineSegment - point on horizontal segment`() {
        val point = NormalizedPoint(0.5f, 0.5f)
        val start = NormalizedPoint(0.0f, 0.5f)
        val end = NormalizedPoint(1.0f, 0.5f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        assertEquals(0.0f, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point on vertical segment`() {
        val point = NormalizedPoint(0.5f, 0.5f)
        val start = NormalizedPoint(0.5f, 0.0f)
        val end = NormalizedPoint(0.5f, 1.0f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        assertEquals(0.0f, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point perpendicular to segment`() {
        val point = NormalizedPoint(0.5f, 0.6f)
        val start = NormalizedPoint(0.0f, 0.5f)
        val end = NormalizedPoint(1.0f, 0.5f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        // Distance should be 0.1 (perpendicular distance)
        assertEquals(0.1f, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point closest to segment start`() {
        val point = NormalizedPoint(0.0f, 0.0f)
        val start = NormalizedPoint(0.5f, 0.5f)
        val end = NormalizedPoint(1.0f, 0.5f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        // Distance should be sqrt((0.5)^2 + (0.5)^2) ≈ 0.707
        val expected = kotlin.math.sqrt(0.5f * 0.5f + 0.5f * 0.5f)
        assertEquals(expected, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point closest to segment end`() {
        val point = NormalizedPoint(1.0f, 1.0f)
        val start = NormalizedPoint(0.0f, 0.5f)
        val end = NormalizedPoint(0.5f, 0.5f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        // Distance should be sqrt((0.5)^2 + (0.5)^2) ≈ 0.707
        val expected = kotlin.math.sqrt(0.5f * 0.5f + 0.5f * 0.5f)
        assertEquals(expected, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point on diagonal segment`() {
        val point = NormalizedPoint(0.5f, 0.5f)
        val start = NormalizedPoint(0.0f, 0.0f)
        val end = NormalizedPoint(1.0f, 1.0f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        assertEquals(0.0f, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - degenerate segment (point)`() {
        val point = NormalizedPoint(0.5f, 0.5f)
        val start = NormalizedPoint(0.3f, 0.3f)
        val end = NormalizedPoint(0.3f, 0.3f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        // Distance should be sqrt((0.2)^2 + (0.2)^2) ≈ 0.283
        val expected = kotlin.math.sqrt(0.2f * 0.2f + 0.2f * 0.2f)
        assertEquals(expected, distance, 0.001f)
    }

    @Test
    fun `distanceFromPointToLineSegment - point beyond segment end on extended line`() {
        val point = NormalizedPoint(1.5f, 0.5f)
        val start = NormalizedPoint(0.0f, 0.5f)
        val end = NormalizedPoint(1.0f, 0.5f)
        
        val distance = distanceFromPointToLineSegment(point, start, end)
        
        // Should measure distance to end point, not the extended line
        // Distance should be 0.5
        assertEquals(0.5f, distance, 0.001f)
    }

    @Test
    fun `isPointNearShape - point on shape edge`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point on the bottom edge of the shape (y = 0.4)
        val point = NormalizedPoint(0.5f, 0.4f)
        
        assertTrue(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - point very close to shape edge`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point just outside the shape, but within tolerance (0.02 = 20 pixels on 1000px canvas)
        val point = NormalizedPoint(0.5f, 0.42f)
        
        assertTrue(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - point far from shape`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point far from the shape
        val point = NormalizedPoint(0.8f, 0.8f)
        
        assertFalse(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - point inside shape but not near edges`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point in the center of the shape, far from edges
        // Center would be at (0.5, 0.5), which is 0.1 normalized units from nearest edge
        // 0.1 normalized = 100 pixels on 1000px canvas, which is > 30px tolerance
        val point = NormalizedPoint(0.5f, 0.5f)
        
        assertFalse(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - point near corner`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point near the bottom-left corner (0.4, 0.4)
        val point = NormalizedPoint(0.41f, 0.41f)
        
        assertTrue(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - empty shape returns false`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(points = emptyList()),
            position = NormalizedPoint(0.5f, 0.5f)
        )
        
        val point = NormalizedPoint(0.5f, 0.5f)
        
        assertFalse(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - single point shape`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(NormalizedPoint(0.0f, 0.0f))
            ),
            position = NormalizedPoint(0.5f, 0.5f)
        )
        
        // Point very close to the single point
        val point = NormalizedPoint(0.51f, 0.5f)
        
        assertTrue(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - triangular shape`() {
        val canvasSize = IntSize(1000, 1000)
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.1f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point on the bottom edge of triangle
        val point = NormalizedPoint(0.5f, 0.4f)
        
        assertTrue(isPointNearShape(point, shape, canvasSize))
    }

    @Test
    fun `isPointNearShape - respects different canvas sizes`() {
        // Smaller canvas means larger normalized tolerance
        val smallCanvas = IntSize(300, 300) // 30px tolerance = 0.1 normalized
        val largeCanvas = IntSize(3000, 3000) // 30px tolerance = 0.01 normalized
        
        val shape = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = NormalizedPoint(0.4f, 0.4f)
        )
        
        // Point 0.05 units away (50 pixels on 1000px canvas)
        val point = NormalizedPoint(0.5f, 0.45f)
        
        // Should be detected on small canvas (tolerance ~0.1)
        assertTrue(isPointNearShape(point, shape, smallCanvas))
        
        // Should NOT be detected on large canvas (tolerance ~0.01)
        assertFalse(isPointNearShape(point, shape, largeCanvas))
    }

    @Test
    fun `NormalizedPoint - operator functions work correctly`() {
        val p1 = NormalizedPoint(0.3f, 0.4f)
        val p2 = NormalizedPoint(0.1f, 0.2f)
        
        val sum = p1 + p2
        assertEquals(0.4f, sum.x, 0.001f)
        assertEquals(0.6f, sum.y, 0.001f)
        
        val diff = p1 - p2
        assertEquals(0.2f, diff.x, 0.001f)
        assertEquals(0.2f, diff.y, 0.001f)
        
        val scaled = p1 * 2f
        assertEquals(0.6f, scaled.x, 0.001f)
        assertEquals(0.8f, scaled.y, 0.001f)
    }

    @Test
    fun `NormalizedPoint - dot product calculates correctly`() {
        val p1 = NormalizedPoint(0.3f, 0.4f)
        val p2 = NormalizedPoint(0.5f, 0.6f)
        
        val dot = p1.dot(p2)
        
        // 0.3 * 0.5 + 0.4 * 0.6 = 0.15 + 0.24 = 0.39
        assertEquals(0.39f, dot, 0.001f)
    }

    @Test
    fun `NormalizedPoint - lengthSquared calculates correctly`() {
        val p = NormalizedPoint(0.3f, 0.4f)
        
        val lengthSq = p.lengthSquared()
        
        // 0.3^2 + 0.4^2 = 0.09 + 0.16 = 0.25
        assertEquals(0.25f, lengthSq, 0.001f)
    }
}
