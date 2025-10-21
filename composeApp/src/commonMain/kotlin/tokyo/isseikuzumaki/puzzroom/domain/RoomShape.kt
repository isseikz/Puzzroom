package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable

/**
 * Room shape types for room creation
 */
enum class RoomShapeType {
    WALL,       // 壁 - 線分
    DOOR,       // 扉 - 扇形
    WINDOW;     // 窓 - 線分（将来的な拡張用）

    fun displayName(): String = when (this) {
        WALL -> "Wall"
        DOOR -> "Door"
        WINDOW -> "Window"
    }
}

/**
 * Room shape element representing walls, doors, etc.
 */
@Serializable
data class RoomShapeElement(
    val type: RoomShapeType,
    val shape: Shape,
    val width: Centimeter = Centimeter(10), // デフォルト幅10cm
) {
    companion object {
        /**
         * Create a wall (line segment)
         */
        fun createWall(start: Point, end: Point, width: Centimeter = Centimeter(10)): RoomShapeElement {
            return RoomShapeElement(
                type = RoomShapeType.WALL,
                shape = Polygon(listOf(start, end)),
                width = width
            )
        }

        /**
         * Create a door (sector/arc)
         */
        fun createDoor(position: Point, width: Centimeter = Centimeter(80), angle: Degree = Degree(90f)): RoomShapeElement {
            // 扇形として簡略化（将来的には円弧を実装）
            val points = mutableListOf<Point>()
            points.add(position)
            
            // 扇形の頂点を作成
            val steps = 10
            for (i in 0..steps) {
                val currentAngle = (angle.value * i / steps)
                val radians = Math.toRadians(currentAngle.toDouble())
                val x = position.x.value + (width.value * kotlin.math.cos(radians)).toInt()
                val y = position.y.value + (width.value * kotlin.math.sin(radians)).toInt()
                points.add(Point(Centimeter(x), Centimeter(y)))
            }
            
            return RoomShapeElement(
                type = RoomShapeType.DOOR,
                shape = Polygon(points),
                width = width
            )
        }
    }
}
