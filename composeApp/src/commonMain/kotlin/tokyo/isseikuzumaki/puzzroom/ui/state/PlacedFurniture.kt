package tokyo.isseikuzumaki.puzzroom.ui.state

import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon

/**
 * 配置された家具の状態（UI用）
 */
data class PlacedFurniture(
    val furniture: Furniture,
    val position: Point,
    val rotation: Degree
) {
    /**
     * 指定された点が家具の内部にあるかチェック
     */
    fun contains(point: Point): Boolean {
        val rotatedPoints = furniture.shape.rotateAroundCenterToPoints(position, rotation)
        return pointInPolygon(point, rotatedPoints)
    }
}

// --- Geometry helpers used by the canvas/organisms ---

/**
 * Polygon を指定位置に配置し、幾何中心を軸に回転させた結果を Point のリストで返す
 */
internal fun Polygon.rotateAroundCenterToPoints(position: Point, rotation: Degree): List<Point> =
    this.rotateAroundCenterOffsets(position, rotation).map { it.toPoint() }
