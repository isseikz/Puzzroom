package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable


@Serializable
data class Point(val x: Centimeter, val y: Centimeter) {
    override fun toString(): String {
        return "($x, $y)"
    }
}

data class Edge(val start: Point, val end: Point) {
    init {
        require(length() > 0) { "Edge length must be greater than 0" }
    }

    private fun length(): Double {
        val dx = (end.x.value - start.x.value).toDouble()
        val dy = (end.y.value - start.y.value).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

sealed interface Shape {
    val points: List<Point>
}

data class Triangle(
    val p1: Point,
    val p2: Point,
    val p3: Point,
) : Shape {
    override val points: List<Point> get() = listOf(p1, p2, p3)

    /**
     * 指定された点が三角形内に含まれているかチェック
     * @param point チェックする点
     * @return 三角形内に含まれている場合true
     */
    fun contains(point: Point): Boolean {
        val sign1 = crossProduct(point, p1, p2)
        val sign2 = crossProduct(point, p2, p3)
        val sign3 = crossProduct(point, p3, p1)

        val hasNeg = (sign1 < 0) || (sign2 < 0) || (sign3 < 0)
        val hasPos = (sign1 > 0) || (sign2 > 0) || (sign3 > 0)

        return !(hasNeg && hasPos)
    }
}

@Serializable
data class Polygon(
    override val points: List<Point>,
) : Shape {
    init {
        require(points.size >= 3) { "Polygon must have at least 3 points" }
    }

    /**
     * 指定された点が多角形内に含まれているかチェック
     * Ray Casting Algorithmを使用
     * @param point チェックする点
     * @return 多角形内に含まれている場合true
     */
    fun contains(point: Point): Boolean {
        var inside = false
        var j = points.size - 1

        for (i in points.indices) {
            val pi = points[i]
            val pj = points[j]

            // 点のy座標が辺の範囲内にあるかチェック
            if ((pi.y.value > point.y.value) != (pj.y.value > point.y.value)) {
                // 点から右方向に伸ばした半直線が辺と交差するかチェック
                val intersectX = (pj.x.value - pi.x.value).toLong() *
                                 (point.y.value - pi.y.value).toLong() /
                                 (pj.y.value - pi.y.value).toLong() +
                                 pi.x.value.toLong()

                if (point.x.value < intersectX) {
                    inside = !inside
                }
            }
            j = i
        }

        return inside
    }

    override fun toString(): String {
        return "Polygon(points=$points)"
    }

    class Builder(
        private val points: MutableList<Point> = mutableListOf(),
    ) {
        fun insert(index: Int, point: Point) = apply { this.points.add(index, point) }
        fun add(point: Point) = apply { this.points.add(point) }
        fun build() = Polygon(points)

        /**
         * 交差をチェックする
         * @param point 追加する点
         * @return 交差していなければtrue
         */
        fun intersects(point: Point): Boolean {
            if (points.size < 2) return true
            val newEdgeStart = points.last()
            val newEdgeEnd = point

            for (i in points.indices) {
                val edgeStart = points[i]
                val edgeEnd = points[(i + 1) % points.size]

                // 新しい辺と既存の辺が共有点を持つ場合はスキップ
                if (newEdgeStart == edgeStart || newEdgeStart == edgeEnd ||
                    newEdgeEnd == edgeStart || newEdgeEnd == edgeEnd) {
                    continue
                }

                if (edgesIntersect(Edge(newEdgeStart, newEdgeEnd), Edge(edgeStart, edgeEnd))) {
                    return false // 交差している
                }
            }

            return true // 交差していない
        }

        companion object Companion {
            fun Polygon.edit() = Builder(points.toMutableList())
        }
    }
}
