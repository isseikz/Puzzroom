package tokyo.isseikuzumaki.puzzroom.domain

import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class EdgesIntersectTest {

    @Test
    fun `線分が内部で交差する場合はtrueを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 100.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 100.cm()),
            Point(100.cm(), 0.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `端点同士が接触する場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(50.cm(), 50.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 50.cm()),
            Point(100.cm(), 100.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `片方の端点が他の線分の内部にある場合はtrueを返す（T字型）`() {
        val e1 = Edge(
            Point(0.cm(), 50.cm()),
            Point(100.cm(), 50.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 0.cm()),
            Point(50.cm(), 50.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `同一直線上で重なる場合はtrueを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 0.cm()),
            Point(150.cm(), 0.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `同一直線上で端点のみ接触する場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(50.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `線分が交差しない場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(50.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 50.cm()),
            Point(50.cm(), 50.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `線分が平行な場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 50.cm()),
            Point(100.cm(), 50.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `延長線上で交差するが線分としては交差しない場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(50.cm(), 50.cm())
        )
        val e2 = Edge(
            Point(60.cm(), 0.cm()),
            Point(100.cm(), 50.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `垂直な線分が交差する場合はtrueを返す`() {
        val e1 = Edge(
            Point(50.cm(), 0.cm()),
            Point(50.cm(), 100.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 50.cm()),
            Point(100.cm(), 50.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `同一直線上で完全に重なる場合はtrueを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `同一直線上で一方が他方を完全に含む場合はtrueを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(25.cm(), 0.cm()),
            Point(75.cm(), 0.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `斜めの線分が同一直線上で重なる場合はtrueを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(100.cm(), 100.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 50.cm()),
            Point(150.cm(), 150.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }

    @Test
    fun `斜めの線分が同一直線上で端点のみ接触する場合はfalseを返す`() {
        val e1 = Edge(
            Point(0.cm(), 0.cm()),
            Point(50.cm(), 50.cm())
        )
        val e2 = Edge(
            Point(50.cm(), 50.cm()),
            Point(100.cm(), 100.cm())
        )

        assertFalse(edgesIntersect(e1, e2))
    }

    @Test
    fun `逆方向の線分が内部で交差する場合はtrueを返す`() {
        val e1 = Edge(
            Point(100.cm(), 100.cm()),
            Point(0.cm(), 0.cm())
        )
        val e2 = Edge(
            Point(0.cm(), 100.cm()),
            Point(100.cm(), 0.cm())
        )

        assertTrue(edgesIntersect(e1, e2))
    }
}
