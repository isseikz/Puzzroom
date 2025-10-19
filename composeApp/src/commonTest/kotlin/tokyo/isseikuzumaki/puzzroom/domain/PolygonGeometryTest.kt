package tokyo.isseikuzumaki.puzzroom.domain

import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PolygonGeometryTest {

    @Test
    fun testApplySimilarityTransformation() {
        // Create a square 100x100
        val square = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        // Set edge 0 to 200cm (scale factor: 2.0)
        val scaledSquare = PolygonGeometry.applySimilarityTransformation(
            polygon = square,
            referenceEdgeIndex = 0,
            newLength = 200
        )

        // Check that all edges are scaled proportionally
        val edgeLengths = PolygonGeometry.calculateEdgeLengths(scaledSquare)
        assertEquals(200, edgeLengths[0], "First edge should be 200cm")
        assertEquals(200, edgeLengths[1], "Second edge should be scaled to 200cm")
        assertEquals(200, edgeLengths[2], "Third edge should be scaled to 200cm")
        assertEquals(200, edgeLengths[3], "Fourth edge should be scaled to 200cm")
    }

    @Test
    fun testFindNearestEdge() {
        val square = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        // Point near the first edge (bottom edge)
        val nearFirstEdge = Point(50.cm(), 5.cm())
        val edgeIndex = PolygonGeometry.findNearestEdge(nearFirstEdge, square, threshold = 20.0)
        
        assertNotNull(edgeIndex, "Should find an edge")
        assertEquals(0, edgeIndex, "Should find the first edge")
    }

    @Test
    fun testCalculateEdgeLengths() {
        val square = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        val lengths = PolygonGeometry.calculateEdgeLengths(square)
        
        assertEquals(4, lengths.size)
        assertEquals(100, lengths[0])
        assertEquals(100, lengths[1])
        assertEquals(100, lengths[2])
        assertEquals(100, lengths[3])
    }

    @Test
    fun testCalculateInteriorAngle() {
        // Square with 90-degree angles
        val square = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        val angle = PolygonGeometry.calculateInteriorAngle(square, 0)
        
        // Should be approximately 90 degrees (allow some floating point error)
        assertTrue(angle > 89.0 && angle < 91.0, "Angle should be approximately 90 degrees, but was $angle")
    }

    @Test
    fun testAdjustEdgeLength() {
        val square = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        // Adjust the first edge to 150cm
        val adjusted = PolygonGeometry.adjustEdgeLength(square, 0, 150)
        
        val lengths = PolygonGeometry.calculateEdgeLengths(adjusted)
        assertEquals(150, lengths[0], "First edge should be 150cm")
        
        // Other edges should remain the same
        assertEquals(100, lengths[2], "Third edge should remain 100cm")
        assertEquals(100, lengths[3], "Fourth edge should remain 100cm")
    }

    @Test
    fun testMoveVertex() {
        val triangle = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(50.cm(), 100.cm())
            )
        )

        val movedTriangle = PolygonGeometry.moveVertex(
            polygon = triangle,
            vertexIndex = 2,
            newPosition = Point(50.cm(), 150.cm())
        )

        assertEquals(Point(50.cm(), 150.cm()), movedTriangle.points[2])
        assertEquals(triangle.points[0], movedTriangle.points[0])
        assertEquals(triangle.points[1], movedTriangle.points[1])
    }

    @Test
    fun testFindNearestVertex() {
        val triangle = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(50.cm(), 100.cm())
            )
        )

        // Point very close to first vertex
        val nearFirst = Point(2.cm(), 2.cm())
        val vertexIndex = PolygonGeometry.findNearestVertex(nearFirst, triangle, threshold = 20.0)
        
        assertNotNull(vertexIndex)
        assertEquals(0, vertexIndex)
    }

    @Test
    fun testIsPolygonClosed() {
        val closedTriangle = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(50.cm(), 100.cm())
            )
        )

        assertTrue(PolygonGeometry.isPolygonClosed(closedTriangle, tolerance = 1.0))
    }

    @Test
    fun testCalculateDistance() {
        val p1 = Point(0.cm(), 0.cm())
        val p2 = Point(3.cm(), 4.cm())
        
        val distance = PolygonGeometry.calculateDistance(p1, p2)
        
        // 3-4-5 triangle
        assertEquals(5.0, distance, 0.01)
    }
}
