package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable
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
    val name: String = "Untitled",
    val layoutUrl: String? = null,
    val floorPlans: List<FloorPlan> = emptyList(),
)

/**
 * 家具のカテゴリ
 */
enum class FurnitureCategory {
    LIVING,      // リビング
    BEDROOM,     // 寝室
    KITCHEN,     // キッチン
    DINING,      // ダイニング
    BATHROOM,    // バスルーム
    OFFICE,      // オフィス
    CUSTOM       // カスタム
}

/**
 * 家具テンプレート
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
    /**
     * テンプレートから家具を作成（長方形）
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
        // プリセット家具テンプレート
        val PRESETS = listOf(
            // リビング
            FurnitureTemplate(
                name = "ソファ（2人掛け）",
                category = FurnitureCategory.LIVING,
                width = Centimeter(150),
                depth = Centimeter(80)
            ),
            FurnitureTemplate(
                name = "ソファ（3人掛け）",
                category = FurnitureCategory.LIVING,
                width = Centimeter(200),
                depth = Centimeter(90)
            ),
            FurnitureTemplate(
                name = "センターテーブル",
                category = FurnitureCategory.LIVING,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "テレビボード",
                category = FurnitureCategory.LIVING,
                width = Centimeter(180),
                depth = Centimeter(40)
            ),

            // 寝室
            FurnitureTemplate(
                name = "シングルベッド",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(100),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "セミダブルベッド",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(120),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "ダブルベッド",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(140),
                depth = Centimeter(200)
            ),
            FurnitureTemplate(
                name = "ワードローブ",
                category = FurnitureCategory.BEDROOM,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),

            // ダイニング
            FurnitureTemplate(
                name = "ダイニングテーブル（4人）",
                category = FurnitureCategory.DINING,
                width = Centimeter(150),
                depth = Centimeter(85)
            ),
            FurnitureTemplate(
                name = "ダイニングテーブル（6人）",
                category = FurnitureCategory.DINING,
                width = Centimeter(180),
                depth = Centimeter(90)
            ),
            FurnitureTemplate(
                name = "ダイニングチェア",
                category = FurnitureCategory.DINING,
                width = Centimeter(45),
                depth = Centimeter(50)
            ),

            // キッチン
            FurnitureTemplate(
                name = "冷蔵庫",
                category = FurnitureCategory.KITCHEN,
                width = Centimeter(60),
                depth = Centimeter(70)
            ),
            FurnitureTemplate(
                name = "食器棚",
                category = FurnitureCategory.KITCHEN,
                width = Centimeter(90),
                depth = Centimeter(45)
            ),

            // オフィス
            FurnitureTemplate(
                name = "デスク",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(120),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "オフィスチェア",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(60),
                depth = Centimeter(60)
            ),
            FurnitureTemplate(
                name = "本棚",
                category = FurnitureCategory.OFFICE,
                width = Centimeter(90),
                depth = Centimeter(30)
            )
        )
    }
}
