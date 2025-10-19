package tokyo.isseikuzumaki.puzzroom.domain

import kotlin.math.*

/**
 * ポリゴンの幾何計算ユーティリティ
 */
object PolygonGeometry {

    /**
     * 2点間の距離を計算
     */
    fun calculateDistance(p1: Point, p2: Point): Double {
        val dx = (p2.x.value - p1.x.value).toDouble()
        val dy = (p2.y.value - p1.y.value).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * ポリゴンの各辺の長さを計算
     */
    fun calculateEdgeLengths(polygon: Polygon): List<Int> {
        return polygon.points.indices.map { index ->
            val p1 = polygon.points[index]
            val p2 = polygon.points[(index + 1) % polygon.points.size]
            calculateDistance(p1, p2).roundToInt()
        }
    }

    /**
     * 指定した辺の長さを変更してポリゴンを調整
     *
     * @param polygon 元のポリゴン
     * @param edgeIndex 変更する辺のインデックス
     * @param newLength 新しい長さ（Centimeter単位）
     * @return 調整されたポリゴン
     */
    fun adjustEdgeLength(
        polygon: Polygon,
        edgeIndex: Int,
        newLength: Int
    ): Polygon {
        require(edgeIndex in polygon.points.indices) {
            "Edge index out of bounds"
        }
        require(newLength > 0) {
            "Edge length must be positive"
        }

        val p1 = polygon.points[edgeIndex]
        val p2 = polygon.points[(edgeIndex + 1) % polygon.points.size]

        // 辺の方向ベクトル
        val dx = p2.x.value - p1.x.value
        val dy = p2.y.value - p1.y.value
        val currentLength = sqrt((dx * dx + dy * dy).toDouble())

        if (currentLength == 0.0) {
            // 長さ0の辺は調整できない
            return polygon
        }

        // 正規化された方向ベクトル
        val normDx = dx / currentLength
        val normDy = dy / currentLength

        // 新しい第2頂点の位置
        val newP2 = Point(
            Centimeter((p1.x.value + newLength * normDx).roundToInt()),
            Centimeter((p1.y.value + newLength * normDy).roundToInt())
        )

        // ポリゴンを更新
        val newPoints = polygon.points.mapIndexed { index, point ->
            if (index == (edgeIndex + 1) % polygon.points.size) {
                newP2
            } else {
                point
            }
        }

        return Polygon(points = newPoints)
    }

    /**
     * 頂点を移動してポリゴンを更新
     *
     * @param polygon 元のポリゴン
     * @param vertexIndex 移動する頂点のインデックス
     * @param newPosition 新しい位置
     * @return 更新されたポリゴン
     */
    fun moveVertex(
        polygon: Polygon,
        vertexIndex: Int,
        newPosition: Point
    ): Polygon {
        require(vertexIndex in polygon.points.indices) {
            "Vertex index out of bounds"
        }

        val newPoints = polygon.points.mapIndexed { index, point ->
            if (index == vertexIndex) newPosition else point
        }

        return Polygon(points = newPoints)
    }

    /**
     * 指定した位置に最も近い頂点を見つける
     *
     * @param position 検索位置
     * @param polygon ポリゴン
     * @param threshold 閾値（この距離以内の頂点のみ検出）
     * @return 頂点のインデックス（見つからない場合はnull）
     */
    fun findNearestVertex(
        position: Point,
        polygon: Polygon,
        threshold: Double = 20.0
    ): Int? {
        var nearestIndex: Int? = null
        var minDistance = threshold

        polygon.points.forEachIndexed { index, point ->
            val distance = calculateDistance(position, point)
            if (distance < minDistance) {
                minDistance = distance
                nearestIndex = index
            }
        }

        return nearestIndex
    }

    /**
     * 頂点の内角を計算（度数法）
     *
     * @param polygon ポリゴン
     * @param vertexIndex 頂点のインデックス
     * @return 内角（度）0〜360
     */
    fun calculateInteriorAngle(polygon: Polygon, vertexIndex: Int): Double {
        require(vertexIndex in polygon.points.indices) {
            "Vertex index out of bounds"
        }
        require(polygon.points.size >= 3) {
            "Polygon must have at least 3 vertices"
        }

        val n = polygon.points.size
        val prev = polygon.points[(vertexIndex - 1 + n) % n]
        val current = polygon.points[vertexIndex]
        val next = polygon.points[(vertexIndex + 1) % n]

        // 2つのベクトルを計算
        val v1x = (prev.x.value - current.x.value).toDouble()
        val v1y = (prev.y.value - current.y.value).toDouble()
        val v2x = (next.x.value - current.x.value).toDouble()
        val v2y = (next.y.value - current.y.value).toDouble()

        // atan2を使って各ベクトルの角度を計算
        val angle1 = atan2(v1y, v1x)
        val angle2 = atan2(v2y, v2x)

        // 内角を計算（反時計回り）
        var angle = angle2 - angle1
        if (angle < 0) angle += 2 * PI

        // 度数法に変換
        return angle * 180.0 / PI
    }

    /**
     * ポリゴンの全頂点の内角を計算
     *
     * @param polygon ポリゴン
     * @return 各頂点の内角リスト（度）
     */
    fun calculateAllInteriorAngles(polygon: Polygon): List<Double> {
        return polygon.points.indices.map { index ->
            calculateInteriorAngle(polygon, index)
        }
    }

    /**
     * 頂点の角度を調整（Local Adjustment方式）
     *
     * 指定した頂点の内角を変更し、前後の辺の長さを維持したまま形状を調整します。
     * この方式では、ポリゴンが開く可能性があります。
     *
     * @param polygon 元のポリゴン
     * @param vertexIndex 調整する頂点のインデックス
     * @param newAngleDegrees 新しい内角（度）
     * @return 調整されたポリゴン（成功時）またはエラーメッセージ
     */
    fun adjustAngleLocal(
        polygon: Polygon,
        vertexIndex: Int,
        newAngleDegrees: Double
    ): Result<Polygon> {
        require(vertexIndex in polygon.points.indices) {
            "Vertex index out of bounds"
        }
        require(polygon.points.size >= 3) {
            "Polygon must have at least 3 vertices"
        }
        require(newAngleDegrees > 0 && newAngleDegrees < 360) {
            "Angle must be between 0 and 360 degrees"
        }

        val n = polygon.points.size
        val prevIndex = (vertexIndex - 1 + n) % n
        val nextIndex = (vertexIndex + 1) % n

        val prev = polygon.points[prevIndex]
        val current = polygon.points[vertexIndex]
        val next = polygon.points[nextIndex]

        // 前の辺の長さと方向を計算
        val prevEdgeLength = calculateDistance(prev, current)
        val prevDx = current.x.value - prev.x.value
        val prevDy = current.y.value - prev.y.value
        val prevAngle = atan2(prevDy.toDouble(), prevDx.toDouble())

        // 新しい角度をラジアンに変換
        val newAngleRad = newAngleDegrees * PI / 180.0

        // 次の辺の新しい方向を計算
        val nextEdgeAngle = prevAngle + PI - newAngleRad

        // 次の辺の長さを維持
        val nextEdgeLength = calculateDistance(current, next)

        // 新しい次の頂点の位置を計算
        val newNextX = current.x.value + (nextEdgeLength * cos(nextEdgeAngle)).roundToInt()
        val newNextY = current.y.value + (nextEdgeLength * sin(nextEdgeAngle)).roundToInt()
        val newNext = Point(Centimeter(newNextX), Centimeter(newNextY))

        // ポリゴンを更新
        val newPoints = polygon.points.mapIndexed { index, point ->
            when (index) {
                nextIndex -> newNext
                else -> point
            }
        }

        return Result.success(Polygon(points = newPoints))
    }

    /**
     * ポリゴンが閉じているかチェック
     *
     * @param polygon ポリゴン
     * @param tolerance 許容誤差（センチメートル）
     * @return 閉じている場合true
     */
    fun isPolygonClosed(polygon: Polygon, tolerance: Double = 1.0): Boolean {
        if (polygon.points.size < 3) return false

        val first = polygon.points.first()
        val last = polygon.points.last()
        val distance = calculateDistance(first, last)

        return distance <= tolerance
    }

    /**
     * 開いたポリゴンを自動的に閉じる
     *
     * 最後の頂点を最初の頂点に近づけて、ポリゴンを閉じます。
     * 中間の頂点は均等に調整されます。
     *
     * @param polygon 開いたポリゴン
     * @return 閉じたポリゴン
     */
    fun autoClosePolygon(polygon: Polygon): Polygon {
        if (polygon.points.size < 3) return polygon
        if (isPolygonClosed(polygon)) return polygon

        val first = polygon.points.first()
        val last = polygon.points.last()

        // 最後の頂点を最初の頂点に移動
        val newPoints = polygon.points.dropLast(1) // 最後の頂点を削除して閉じる

        return Polygon(points = newPoints)
    }

    /**
     * ポリゴンの開き具合（最初と最後の頂点間の距離）を計算
     *
     * @param polygon ポリゴン
     * @return 開き具合（センチメートル）
     */
    fun calculateGapDistance(polygon: Polygon): Double {
        if (polygon.points.size < 3) return 0.0

        val first = polygon.points.first()
        val last = polygon.points.last()

        return calculateDistance(first, last)
    }

    /**
     * 相似変換: 1つの辺の長さを基準にポリゴン全体をスケーリング
     *
     * 初回の辺の長さ指定時に、他の辺も比例して調整します。
     * 画面上の位置関係（形状）は維持されます。
     *
     * @param polygon 元のポリゴン
     * @param referenceEdgeIndex 基準とする辺のインデックス
     * @param newLength 新しい長さ（Centimeter単位）
     * @return スケーリングされたポリゴン
     */
    fun applySimilarityTransformation(
        polygon: Polygon,
        referenceEdgeIndex: Int,
        newLength: Int
    ): Polygon {
        require(referenceEdgeIndex in polygon.points.indices) {
            "Reference edge index out of bounds"
        }
        require(newLength > 0) {
            "Edge length must be positive"
        }

        val p1 = polygon.points[referenceEdgeIndex]
        val p2 = polygon.points[(referenceEdgeIndex + 1) % polygon.points.size]

        // 基準辺の現在の長さ
        val currentLength = calculateDistance(p1, p2)
        
        if (currentLength == 0.0) {
            return polygon
        }

        // スケール係数
        val scale = newLength / currentLength

        // 最初の頂点を原点として、すべての頂点をスケーリング
        val origin = polygon.points.first()
        val scaledPoints = polygon.points.map { point ->
            val relativeX = point.x.value - origin.x.value
            val relativeY = point.y.value - origin.y.value
            Point(
                Centimeter((origin.x.value + relativeX * scale).roundToInt()),
                Centimeter((origin.y.value + relativeY * scale).roundToInt())
            )
        }

        return Polygon(points = scaledPoints)
    }

    /**
     * 指定された辺近くの位置を見つける
     *
     * @param position 検索位置
     * @param polygon ポリゴン
     * @param threshold 閾値（この距離以内の辺のみ検出）
     * @return 辺のインデックス（見つからない場合はnull）
     */
    fun findNearestEdge(
        position: Point,
        polygon: Polygon,
        threshold: Double = 20.0
    ): Int? {
        var nearestIndex: Int? = null
        var minDistance = threshold

        polygon.points.indices.forEach { index ->
            val p1 = polygon.points[index]
            val p2 = polygon.points[(index + 1) % polygon.points.size]
            
            // 点から辺への最短距離を計算
            val distance = distanceFromPointToSegment(position, p1, p2)
            if (distance < minDistance) {
                minDistance = distance
                nearestIndex = index
            }
        }

        return nearestIndex
    }

    /**
     * 点から線分への最短距離を計算
     */
    private fun distanceFromPointToSegment(point: Point, segStart: Point, segEnd: Point): Double {
        val px = point.x.value.toDouble()
        val py = point.y.value.toDouble()
        val x1 = segStart.x.value.toDouble()
        val y1 = segStart.y.value.toDouble()
        val x2 = segEnd.x.value.toDouble()
        val y2 = segEnd.y.value.toDouble()

        val dx = x2 - x1
        val dy = y2 - y1
        
        if (dx == 0.0 && dy == 0.0) {
            // 線分が点の場合
            return calculateDistance(point, segStart)
        }

        // パラメータ t を計算（0 <= t <= 1 の範囲に制限）
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0.0, 1.0)

        // 線分上の最近点
        val nearestX = x1 + clampedT * dx
        val nearestY = y1 + clampedT * dy

        // 点から最近点への距離
        val distX = px - nearestX
        val distY = py - nearestY
        return sqrt(distX * distX + distY * distY)
    }
}
