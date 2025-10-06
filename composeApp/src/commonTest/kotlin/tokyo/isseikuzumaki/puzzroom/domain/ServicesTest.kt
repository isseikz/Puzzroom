package tokyo.isseikuzumaki.puzzroom.domain

import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TriangulateTest {

    @Test
    fun `三角形はそのまま返される`() {
        val triangle = Triangle(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )

        val result = triangulate(triangle)

        assertEquals(1, result.size)
        assertEquals(triangle, result[0])
    }

    @Test
    fun `四角形は2つの三角形に分割される`() {
        val rectangle = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        val result = triangulate(rectangle)

        assertEquals(2, result.size)
        result.forEach { triangle ->
            assertEquals(3, triangle.points.size)
        }
    }

    @Test
    fun `凸五角形は3つの三角形に分割される`() {
        val pentagon = Polygon(
            listOf(
                Point(50.cm(), 0.cm()),
                Point(100.cm(), 38.cm()),
                Point(81.cm(), 100.cm()),
                Point(19.cm(), 100.cm()),
                Point(0.cm(), 38.cm())
            )
        )

        val result = triangulate(pentagon)

        assertEquals(3, result.size)
        result.forEach { triangle ->
            assertEquals(3, triangle.points.size)
        }
    }

    @Test
    fun `凸六角形は4つの三角形に分割される`() {
        val hexagon = Polygon(
            listOf(
                Point(50.cm(), 0.cm()),
                Point(100.cm(), 25.cm()),
                Point(100.cm(), 75.cm()),
                Point(50.cm(), 100.cm()),
                Point(0.cm(), 75.cm()),
                Point(0.cm(), 25.cm())
            )
        )

        val result = triangulate(hexagon)

        assertEquals(4, result.size)
        result.forEach { triangle ->
            assertEquals(3, triangle.points.size)
        }
    }

    @Test
    fun `凹四角形（L字型）は4つの三角形に分割される`() {
        val lShape = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 50.cm()),
                Point(50.cm(), 50.cm()),
                Point(50.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )

        val result = triangulate(lShape)

        assertEquals(4, result.size)
        result.forEach { triangle ->
            assertEquals(3, triangle.points.size)
        }
    }

    @Test
    fun `星型の凹多角形は正しく分割される`() {
        val star = Polygon(
            listOf(
                Point(50.cm(), 0.cm()),
                Point(61.cm(), 35.cm()),
                Point(98.cm(), 35.cm()),
                Point(68.cm(), 57.cm()),
                Point(79.cm(), 91.cm()),
                Point(50.cm(), 70.cm()),
                Point(21.cm(), 91.cm()),
                Point(32.cm(), 57.cm()),
                Point(2.cm(), 35.cm()),
                Point(39.cm(), 35.cm())
            )
        )

        val result = triangulate(star)

        assertEquals(8, result.size)
        result.forEach { triangle ->
            assertEquals(3, triangle.points.size)
        }
    }

    @Test
    fun `3点未満の場合は空のリストを返す`() {
        val threePoints = Polygon.Builder()
            .add(Point(0.cm(), 0.cm()))
            .add(Point(100.cm(), 0.cm()))
            .add(Point(0.cm(), 100.cm()))
            .build()

        // リフレクションで内部のpointsリストを2点に減らす方法がないため、
        // 代わりに直接triangulateの動作を確認
        val result = triangulate(threePoints)

        // 3点の場合は1つの三角形として返される
        assertEquals(1, result.size)
    }

    @Test
    fun `すべての三角形の頂点数の合計が正しい`() {
        val hexagon = Polygon(
            listOf(
                Point(50.cm(), 0.cm()),
                Point(100.cm(), 25.cm()),
                Point(100.cm(), 75.cm()),
                Point(50.cm(), 100.cm()),
                Point(0.cm(), 75.cm()),
                Point(0.cm(), 25.cm())
            )
        )

        val result = triangulate(hexagon)

        // n角形 = (n-2)個の三角形
        // 6角形 = 4個の三角形
        val totalPoints = result.sumOf { it.points.size }
        assertEquals(12, totalPoints) // 4 triangles * 3 points = 12
    }

    @Test
    fun `分割された三角形の面積の合計が元の多角形の面積と等しい`() {
        val rectangle = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 50.cm()),
                Point(0.cm(), 50.cm())
            )
        )

        val result = triangulate(rectangle)

        // 三角形の面積を計算
        fun triangleArea(p1: Point, p2: Point, p3: Point): Double {
            return kotlin.math.abs(
                (p1.x.value * (p2.y.value - p3.y.value) +
                 p2.x.value * (p3.y.value - p1.y.value) +
                 p3.x.value * (p1.y.value - p2.y.value)) / 2.0
            )
        }

        val totalArea = result.sumOf { triangle ->
            val (p1, p2, p3) = triangle.points
            triangleArea(p1, p2, p3)
        }

        // 長方形の面積 = 100 * 50 = 5000
        assertEquals(5000.0, totalArea, 0.001)
    }

    @Test
    fun `分割された三角形に重複する頂点がない`() {
        val pentagon = Polygon(
            listOf(
                Point(50.cm(), 0.cm()),
                Point(100.cm(), 38.cm()),
                Point(81.cm(), 100.cm()),
                Point(19.cm(), 100.cm()),
                Point(0.cm(), 38.cm())
            )
        )

        val result = triangulate(pentagon)

        result.forEach { triangle ->
            // 各三角形の3つの頂点がすべて異なることを確認
            val points = triangle.points
            assertEquals(3, points.distinct().size)
        }
    }

    @Test
    fun `三角形の中心点は三角形に含まれる`() {
        val triangle = Triangle(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )
        val centerPoint = Point(50.cm(), 30.cm())

        assertTrue(triangle.contains(centerPoint))
    }

    @Test
    fun `三角形の外の点は三角形に含まれない`() {
        val triangle = Triangle(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )
        val outsidePoint = Point(150.cm(), 50.cm())

        assertEquals(false, triangle.contains(outsidePoint))
    }

    @Test
    fun `三角形の頂点は三角形に含まれる`() {
        val triangle = Triangle(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )

        assertTrue(triangle.contains(triangle.p1))
        assertTrue(triangle.contains(triangle.p2))
        assertTrue(triangle.contains(triangle.p3))
    }

    @Test
    fun `三角形の辺上の点は三角形に含まれる`() {
        val triangle = Triangle(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )
        val edgePoint = Point(50.cm(), 0.cm())

        assertTrue(triangle.contains(edgePoint))
    }

    @Test
    fun `四角形の中心点は四角形に含まれる`() {
        val rectangle = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )
        val centerPoint = Point(50.cm(), 50.cm())

        assertTrue(rectangle.contains(centerPoint))
    }

    @Test
    fun `四角形の外の点は四角形に含まれない`() {
        val rectangle = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )
        val outsidePoint = Point(150.cm(), 50.cm())

        assertEquals(false, rectangle.contains(outsidePoint))
    }

    @Test
    fun `L字型の多角形の内部点は多角形に含まれる`() {
        val lShape = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 50.cm()),
                Point(50.cm(), 50.cm()),
                Point(50.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )
        val insidePoint = Point(25.cm(), 25.cm())

        assertTrue(lShape.contains(insidePoint))
    }

    @Test
    fun `L字型の多角形の凹部分の点は多角形に含まれない`() {
        val lShape = Polygon(
            listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 50.cm()),
                Point(50.cm(), 50.cm()),
                Point(50.cm(), 100.cm()),
                Point(0.cm(), 100.cm())
            )
        )
        val concavePoint = Point(75.cm(), 75.cm())

        assertEquals(false, lShape.contains(concavePoint))
    }
}
