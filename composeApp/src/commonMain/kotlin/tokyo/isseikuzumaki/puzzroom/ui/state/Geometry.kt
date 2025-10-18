package tokyo.isseikuzumaki.puzzroom.ui.state

import androidx.compose.ui.geometry.Offset
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 点がポリゴン内にあるかチェック（Ray Casting Algorithm） */
internal fun pointInPolygon(point: Point, polygon: List<Point>): Boolean {
    var inside = false
    var j = polygon.size - 1

    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]

        if ((pi.y.value > point.y.value) != (pj.y.value > point.y.value)) {
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

internal fun Point.toCanvasOffset(): Offset = Offset(x.value.toFloat(), y.value.toFloat())

internal fun Offset.toPoint(): Point = Point(Centimeter(x.roundToInt()), Centimeter(y.roundToInt()))

/** 点を回転させる */
internal fun Offset.rotate(center: Offset, degrees: Float): Offset {
    val radians = degrees / 180f * PI
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val dx = x - center.x
    val dy = y - center.y
    return Offset(
        center.x + dx * c - dy * s,
        center.y + dx * s + dy * c
    )
}

/** Polygon の幾何中心を計算 */
private fun Polygon.centerOffset(): Offset {
    val sumX = points.sumOf { it.x.value.toDouble() }
    val sumY = points.sumOf { it.y.value.toDouble() }
    return Offset(
        (sumX / points.size).toFloat(),
        (sumY / points.size).toFloat()
    )
}

/**
 * Polygon を指定位置に配置し、幾何中心を軸に回転させた座標（Offset）配列を返す
 */
internal fun Polygon.rotateAroundCenterOffsets(position: Point, rotation: Degree): List<Offset> {
    val positionOffset = position.toCanvasOffset()
    val shapeCenter = centerOffset()

    return points.map { point ->
        val pointOffset = point.toCanvasOffset()
        val relativeOffset = Offset(
            pointOffset.x - shapeCenter.x,
            pointOffset.y - shapeCenter.y
        )
        val rotated = relativeOffset.rotate(Offset.Zero, rotation.value)
        Offset(
            positionOffset.x + rotated.x,
            positionOffset.y + rotated.y
        )
    }
}

