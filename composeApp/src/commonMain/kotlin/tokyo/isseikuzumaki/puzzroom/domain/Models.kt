package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Room(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val shapes: List<PlacedShapeData> = emptyList(),
)

@Serializable
data class Furniture(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val shape: Polygon,
)

@Serializable
data class LayoutEntry(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val room: Room,
    val furniture: Furniture,
    val position: Point,
    val rotation: Degree,
) {
    init {
        // TODO("部屋に収まっているかのチェック")
        // TODO("他の家具と重なっていないかのチェック")
    }
}

@Serializable
data class FloorPlan(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String = "Floor Plan",
    val rooms: List<Room> = emptyList(),
    val furnitures: List<Furniture> = emptyList(),
    val layouts: List<LayoutEntry> = emptyList(),
)

@Serializable
data class Project(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val layoutUrl: String? = null,
    val floorPlans: List<FloorPlan> = emptyList(),
)

/**
 * Furniture category
 */
enum class FurnitureCategory {
    LIVING,      // Living
    BEDROOM,     // Bedroom
    KITCHEN,     // Kitchen
    DINING,      // Dining
    BATHROOM,    // Bathroom
    OFFICE,      // Office
    CUSTOM       // Custom
}

/**
 * Furniture template
 */
@Serializable
data class FurnitureTemplate(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val category: FurnitureCategory,
    val width: Centimeter,
    val depth: Centimeter,
) {
    init {
        require(name.isNotBlank()) { "家具テンプレート名は必須です" }
        require(width.value > 0) { "幅は0より大きい値を指定してください" }
        require(depth.value > 0) { "奥行きは0より大きい値を指定してください" }
    }

    /**
     * Create furniture from template (rectangle)
     */
    fun createFurniture(): Furniture {
        val polygon = Polygon(
            points = listOf(
                Point(Centimeter(0), Centimeter(0)),
                Point(width, Centimeter(0)),
                Point(width, depth),
                Point(Centimeter(0), depth)
            )
        )
        return Furniture(name = name, shape = polygon)
    }

    companion object {
        // Preset furniture templates
        val PRESETS = listOf(
            // Living
            FurnitureTemplate(
                name = "Sofa (2-seater)",
                category = FurnitureCategory.LIVING,
                width = Centimeter(150),
                depth = Centimeter(80)
            ),
            FurnitureTemplate(
                name = "Sofa (3-seater)",
                category = FurnitureCategory.LIVING,
                width = Centimeter(200),
                depth = Centimeter(90)
            ),
            FurnitureTemplate(
                name = "Center table",
                category = FurnitureCategory.LIVING,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "TV stand",
                category = FurnitureCategory.LIVING,
                width = Centimeter(180),
                depth = Centimeter(40)
            ),

            // Bedroom
            FurnitureTemplate(
                name = "Single bed",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(100),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "Semi-double bed",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(120),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "Double bed",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(140),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "Wardrobe",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),

            // Dining
            FurnitureTemplate(
                name = "Dining table (4 people)",
                category = FurnitureCategory.DINING,
                width = Centimeter(150),
                depth = Centimeter(85)
            ),
            FurnitureTemplate(
                name = "Dining table (6 people)",
                category = FurnitureCategory.DINING,
                width = Centimeter(180),
                depth = Centimeter(90)
            ),
            FurnitureTemplate(
                name = "Dining chair",
                category = FurnitureCategory.DINING,
                width = Centimeter(45),
                depth = Centimeter(50)
            ),

            // Kitchen
            FurnitureTemplate(
                name = "Refrigerator",
                category = FurnitureCategory.KITCHEN,
                width = Centimeter(60),
                depth = Centimeter(70)
            ),
            FurnitureTemplate(
                name = "Cupboard",
                category = FurnitureCategory.KITCHEN,
                width = Centimeter(90),
                depth = Centimeter(45)
            ),

            // Office
            FurnitureTemplate(
                name = "Desk",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "Office chair",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(60),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "Bookshelf",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(90),
                depth = Centimeter(30)
            )
        )
    }
}
