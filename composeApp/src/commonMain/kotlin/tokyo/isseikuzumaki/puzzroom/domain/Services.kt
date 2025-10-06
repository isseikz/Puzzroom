package tokyo.isseikuzumaki.puzzroom.domain


/**
 * 3点が凸頂点かチェック（反時計回り）
 */
private fun isConvex(a: Point, b: Point, c: Point): Boolean {
    return crossProduct(a, b, c) > 0
}

/**
 * 多角形を三角形に分割する
 * @param shape 分割する多角形
 * @return 分割された三角形のリスト
 */
fun triangulate(shape: Shape): List<Triangle> {
    val points = shape.points.toMutableList()
    if (points.size < 3) return emptyList()
    if (points.size == 3) {
        return when (shape) {
            is Triangle -> listOf(shape)
            is Polygon -> listOf(Triangle(points[0], points[1], points[2]))
        }
    }

    val triangles = mutableListOf<Triangle>()
    while (points.size > 3) {
        var earFound = false
        for (i in points.indices) {
            val prevIndex = (i - 1 + points.size) % points.size
            val nextIndex = (i + 1) % points.size

            val a = points[prevIndex]
            val b = points[i]
            val c = points[nextIndex]

            if (isConvex(a, b, c)) {
                val ear = Triangle(a, b, c)
                val otherPoints = points.filterIndexed { index, _ ->
                    index != prevIndex && index != i && index != nextIndex
                }
                if (!otherPoints.any { ear.contains(it) }) {
                    triangles.add(ear)
                    points.removeAt(i)
                    earFound = true
                    break
                }
            }
        }
        if (!earFound) break // No ear found, possibly a complex polygon
    }
    if (points.size == 3) {
        triangles.add(Triangle(points[0], points[1], points[2]))
    }
    return triangles
}
