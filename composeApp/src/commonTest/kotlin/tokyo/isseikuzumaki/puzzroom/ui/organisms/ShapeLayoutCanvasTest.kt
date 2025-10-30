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

    // Test data for distanceFromPointToLineSegment
    private data class DistanceTestCase(
        val name: String,
        val point: NormalizedPoint,
        val segmentStart: NormalizedPoint,
        val segmentEnd: NormalizedPoint,
        val expectedDistance: Float
    )

    private val distanceTestCases = listOf(
        DistanceTestCase(
            name = "point on horizontal segment",
            point = NormalizedPoint(0.5f, 0.5f),
            segmentStart = NormalizedPoint(0.0f, 0.5f),
            segmentEnd = NormalizedPoint(1.0f, 0.5f),
            expectedDistance = 0.0f
        ),
        DistanceTestCase(
            name = "point on vertical segment",
            point = NormalizedPoint(0.5f, 0.5f),
            segmentStart = NormalizedPoint(0.5f, 0.0f),
            segmentEnd = NormalizedPoint(0.5f, 1.0f),
            expectedDistance = 0.0f
        ),
        DistanceTestCase(
            name = "point perpendicular to segment",
            point = NormalizedPoint(0.5f, 0.6f),
            segmentStart = NormalizedPoint(0.0f, 0.5f),
            segmentEnd = NormalizedPoint(1.0f, 0.5f),
            expectedDistance = 0.1f
        ),
        DistanceTestCase(
            name = "point closest to segment start",
            point = NormalizedPoint(0.0f, 0.0f),
            segmentStart = NormalizedPoint(0.5f, 0.5f),
            segmentEnd = NormalizedPoint(1.0f, 0.5f),
            expectedDistance = kotlin.math.sqrt(0.5f * 0.5f + 0.5f * 0.5f)
        ),
        DistanceTestCase(
            name = "point closest to segment end",
            point = NormalizedPoint(1.0f, 1.0f),
            segmentStart = NormalizedPoint(0.0f, 0.5f),
            segmentEnd = NormalizedPoint(0.5f, 0.5f),
            expectedDistance = kotlin.math.sqrt(0.5f * 0.5f + 0.5f * 0.5f)
        ),
        DistanceTestCase(
            name = "point on diagonal segment",
            point = NormalizedPoint(0.5f, 0.5f),
            segmentStart = NormalizedPoint(0.0f, 0.0f),
            segmentEnd = NormalizedPoint(1.0f, 1.0f),
            expectedDistance = 0.0f
        ),
        DistanceTestCase(
            name = "degenerate segment (point)",
            point = NormalizedPoint(0.5f, 0.5f),
            segmentStart = NormalizedPoint(0.3f, 0.3f),
            segmentEnd = NormalizedPoint(0.3f, 0.3f),
            expectedDistance = kotlin.math.sqrt(0.2f * 0.2f + 0.2f * 0.2f)
        ),
        DistanceTestCase(
            name = "point beyond segment end on extended line",
            point = NormalizedPoint(1.5f, 0.5f),
            segmentStart = NormalizedPoint(0.0f, 0.5f),
            segmentEnd = NormalizedPoint(1.0f, 0.5f),
            expectedDistance = 0.5f
        )
    )

    @Test
    fun `distanceFromPointToLineSegment - all test cases`() {
        distanceTestCases.forEach { testCase ->
            val distance = distanceFromPointToLineSegment(
                testCase.point,
                testCase.segmentStart,
                testCase.segmentEnd
            )
            assertEquals(
                testCase.expectedDistance,
                distance,
                0.001f,
                "Failed for case: ${testCase.name}"
            )
        }
    }

    // Test data for isPointNearShape
    private data class ShapeHitTestCase(
        val name: String,
        val point: NormalizedPoint,
        val shape: NormalizedPlacedShape,
        val canvasSize: IntSize,
        val expectedHit: Boolean
    )

    private fun createSquareShape(
        position: NormalizedPoint = NormalizedPoint(0.4f, 0.4f),
        rotation: Float = 0f
    ) = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.0f, 0.0f),
                    NormalizedPoint(0.2f, 0.0f),
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.0f, 0.2f)
                )
            ),
            position = position,
            rotation = rotation
        )

    private val shapeHitTestCases = listOf(
        ShapeHitTestCase(
            name = "point on shape edge",
            point = NormalizedPoint(0.5f, 0.4f),
            shape = createSquareShape(),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "point very close to shape edge",
            point = NormalizedPoint(0.5f, 0.42f),
            shape = createSquareShape(),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "point far from shape",
            point = NormalizedPoint(0.8f, 0.8f),
            shape = createSquareShape(),
            canvasSize = IntSize(1000, 1000),
            expectedHit = false
        ),
        ShapeHitTestCase(
            name = "point inside shape but not near edges",
            point = NormalizedPoint(0.5f, 0.5f),
            shape = createSquareShape(),
            canvasSize = IntSize(1000, 1000),
            expectedHit = false
        ),
        ShapeHitTestCase(
            name = "point near corner",
            point = NormalizedPoint(0.41f, 0.41f),
            shape = createSquareShape(),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "empty shape returns false",
            point = NormalizedPoint(0.5f, 0.5f),
            shape = NormalizedPlacedShape(
                shape = NormalizedShape(points = emptyList()),
                position = NormalizedPoint(0.5f, 0.5f)
            ),
            canvasSize = IntSize(1000, 1000),
            expectedHit = false
        ),
        ShapeHitTestCase(
            name = "single point shape",
            point = NormalizedPoint(0.51f, 0.5f),
            shape = NormalizedPlacedShape(
                shape = NormalizedShape(
                    points = listOf(NormalizedPoint(0.0f, 0.0f))
                ),
                position = NormalizedPoint(0.5f, 0.5f)
            ),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "triangular shape",
            point = NormalizedPoint(0.5f, 0.4f),
            shape = NormalizedPlacedShape(
                shape = NormalizedShape(
                    points = listOf(
                        NormalizedPoint(0.0f, 0.0f),
                        NormalizedPoint(0.2f, 0.0f),
                        NormalizedPoint(0.1f, 0.2f)
                    )
                ),
                position = NormalizedPoint(0.4f, 0.4f)
            ),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        // Rotation test cases
        ShapeHitTestCase(
            name = "rotated shape - point on bottom edge after 45° rotation",
            point = NormalizedPoint(0.471f, 0.471f), // Bottom-center after 45° rotation
            shape = createSquareShape(rotation = 45f),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "rotated shape - point on original right edge after 90° rotation (now top)",
            point = NormalizedPoint(0.3f, 0.6f), // Right edge center becomes top after 90° rotation
            shape = createSquareShape(rotation = 90f),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "rotated shape - point where unrotated edge was, now inside",
            point = NormalizedPoint(0.5f, 0.4f), // Was on edge, now inside after 45° rotation
            shape = createSquareShape(rotation = 45f),
            canvasSize = IntSize(1000, 1000),
            expectedHit = false
        ),
        ShapeHitTestCase(
            name = "rotated shape - 180° rotation, point on original top edge (now bottom)",
            point = NormalizedPoint(0.3f, 0.2f), // Top edge center becomes bottom after 180° rotation
            shape = createSquareShape(rotation = 180f),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        ),
        ShapeHitTestCase(
            name = "rotated shape - 270° rotation, point on edge",
            point = NormalizedPoint(0.5f, 0.2f), // Right edge center after 270° rotation
            shape = createSquareShape(rotation = 270f),
            canvasSize = IntSize(1000, 1000),
            expectedHit = true
        )
    )

    @Test
    fun `isPointNearShape - all test cases`() {
        shapeHitTestCases.forEach { testCase ->
            val result = isPointNearShape(testCase.point, testCase.shape, testCase.canvasSize)
            assertEquals(
                testCase.expectedHit,
                result,
                "Failed for case: ${testCase.name}"
            )
        }
    }

    @Test
    fun `isPointNearShape - respects different canvas sizes`() {
        // Smaller canvas means larger normalized tolerance
        val smallCanvas = IntSize(300, 300) // 30px tolerance = 0.1 normalized
        val largeCanvas = IntSize(3000, 3000) // 30px tolerance = 0.01 normalized
        
        val shape = createSquareShape()
        
        // Point 0.05 units away (50 pixels on 1000px canvas)
        val point = NormalizedPoint(0.5f, 0.45f)
        
        // Should be detected on small canvas (tolerance ~0.1)
        assertTrue(isPointNearShape(point, shape, smallCanvas))
        
        // Should NOT be detected on large canvas (tolerance ~0.01)
        assertFalse(isPointNearShape(point, shape, largeCanvas))
    }

    // Test data for NormalizedPoint operators
    @Test
    fun `NormalizedPoint - operator functions work correctly`() {
        val p1 = NormalizedPoint(0.3f, 0.4f)
        val p2 = NormalizedPoint(0.1f, 0.2f)
        
        // Test addition
        val sum = p1 + p2
        assertEquals(0.4f, sum.x, 0.001f, "Addition failed for x coordinate")
        assertEquals(0.6f, sum.y, 0.001f, "Addition failed for y coordinate")
        
        // Test subtraction
        val diff = p1 - p2
        assertEquals(0.2f, diff.x, 0.001f, "Subtraction failed for x coordinate")
        assertEquals(0.2f, diff.y, 0.001f, "Subtraction failed for y coordinate")
        
        // Test scalar multiplication
        val scaled = p1 * 2f
        assertEquals(0.6f, scaled.x, 0.001f, "Scalar multiplication failed for x coordinate")
        assertEquals(0.8f, scaled.y, 0.001f, "Scalar multiplication failed for y coordinate")
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
