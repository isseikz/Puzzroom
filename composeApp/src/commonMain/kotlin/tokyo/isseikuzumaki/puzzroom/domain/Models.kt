package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Room(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val shape: Polygon,
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
    val width: Length,
    val depth: Length,
) {
    init {
        require(name.isNotBlank()) { "家具テンプレート名は必須です" }
        require(width.cm > 0) { "幅は0より大きい値を指定してください" }
        require(depth.cm > 0) { "奥行きは0より大きい値を指定してください" }
    }

    /**
     * Create furniture from template (rectangle)
     */
    fun createFurniture(): Furniture {
        val polygon = Polygon(
            points = listOf(
                Point(Centimeter(0), Centimeter(0)),
                Point(width.cm, Centimeter(0)),
                Point(width.cm, depth.cm),
                Point(Centimeter(0), depth.cm)
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
                width = Length(150.cm),
                depth = Length(80.cm)
            ),
            FurnitureTemplate(
                name = "Sofa (3-seater)",
                category = FurnitureCategory.LIVING,
                width = Length(200.cm),
                depth = Length(90.cm)
            ),
            FurnitureTemplate(
                name = "Center table",
                category = FurnitureCategory.LIVING,
                width = Length(120.cm),
                depth = Length(60.cm)
            ),
            FurnitureTemplate(
                name = "TV stand",
                category = FurnitureCategory.LIVING,
                width = Length(180.cm),
                depth = Length(40.cm)
            ),

            // Bedroom
            FurnitureTemplate(
                name = "Single bed",
                category = FurnitureCategory.BEDROOM,
                width = Length(100.cm),
                depth = Length(200.cm)
            ),
            FurnitureTemplate(
                name = "Semi-double bed",
                category = FurnitureCategory.BEDROOM,
                width = Length(120.cm),
                depth = Length(200.cm)
            ),
            FurnitureTemplate(
                name = "Double bed",
                category = FurnitureCategory.BEDROOM,
                width = Length(140.cm),
                depth = Length(200.cm)
            ),
            FurnitureTemplate(
                name = "Wardrobe",
                category = FurnitureCategory.BEDROOM,
                width = Length(120.cm),
                depth = Length(60.cm)
            ),

            // Dining
            FurnitureTemplate(
                name = "Dining table (4 people)",
                category = FurnitureCategory.DINING,
                width = Length(150.cm),
                depth = Length(85.cm)
            ),
            FurnitureTemplate(
                name = "Dining table (6 people)",
                category = FurnitureCategory.DINING,
                width = Length(180.cm),
                depth = Length(90.cm)
            ),
            FurnitureTemplate(
                name = "Dining chair",
                category = FurnitureCategory.DINING,
                width = Length(45.cm),
                depth = Length(50.cm)
            ),

            // Kitchen
            FurnitureTemplate(
                name = "Refrigerator",
                category = FurnitureCategory.KITCHEN,
                width = Length(60.cm),
                depth = Length(70.cm)
            ),
            FurnitureTemplate(
                name = "Cupboard",
                category = FurnitureCategory.KITCHEN,
                width = Length(90.cm),
                depth = Length(45.cm)
            ),

            // Office
            FurnitureTemplate(
                name = "Desk",
                category = FurnitureCategory.OFFICE,
                width = Length(120.cm),
                depth = Length(60.cm)
            ),
            FurnitureTemplate(
                name = "Office chair",
                category = FurnitureCategory.OFFICE,
                width = Length(60.cm),
                depth = Length(60.cm)
            ),
            FurnitureTemplate(
                name = "Bookshelf",
                category = FurnitureCategory.OFFICE,
                width = Length(90.cm),
                depth = Length(30.cm)
            )
        )
    }
}
