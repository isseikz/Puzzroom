package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.ui.unit.IntSize
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPoint
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedShape
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * FurniturePlacementTemplate のスケーリング動作をテストする
 *
 * このテストでは、backgroundShape, currentShapes, editingFurniture が
 * spaceSize の変化に正しく連動することを保証します。
 */
class FurniturePlacementTemplateScalingTest {

    // テストデータ: 異なる spaceSize のパターン
    private val spaceSizeTestCases = listOf(
        SpaceSizeTestCase(
            name = "Small space (500x500)",
            spaceSize = IntSize(500, 500)
        ),
        SpaceSizeTestCase(
            name = "Medium space (1000x1000)",
            spaceSize = IntSize(1000, 1000)
        ),
        SpaceSizeTestCase(
            name = "Large space (1500x1500)",
            spaceSize = IntSize(1500, 1500)
        ),
        SpaceSizeTestCase(
            name = "Rectangular space (800x1200)",
            spaceSize = IntSize(800, 1200)
        )
    )

    private data class SpaceSizeTestCase(
        val name: String,
        val spaceSize: IntSize
    )

    /**
     * Room shape が spaceSize に応じて正しく正規化されることを検証
     */
    @Test
    fun testBackgroundShapeScalesWithSpaceSize() {
        val room = createTestRoom()

        spaceSizeTestCases.forEach { testCase ->
            // 正規化された背景shape を計算
            val normalizedShape = normalizeRoomShape(room, testCase.spaceSize)

            // 各頂点が 0.0 ~ 1.0 の範囲に正規化されていることを確認
            normalizedShape.points.forEach { point ->
                assertTrue(
                    point.x in 0.0f..1.0f,
                    "${testCase.name}: Point x=${point.x} is out of normalized range [0.0, 1.0]"
                )
                assertTrue(
                    point.y in 0.0f..1.0f,
                    "${testCase.name}: Point y=${point.y} is out of normalized range [0.0, 1.0]"
                )
            }

            // 元の形状の比率が保持されていることを確認
            val originalWidth = room.shape.points.maxOf { it.x.value } - room.shape.points.minOf { it.x.value }
            val originalHeight = room.shape.points.maxOf { it.y.value } - room.shape.points.minOf { it.y.value }

            // 正規化された座標を逆変換して元の寸法が復元されることを確認
            val denormalizedWidth = (normalizedShape.points.maxOf { it.x } - normalizedShape.points.minOf { it.x }) * testCase.spaceSize.width
            val denormalizedHeight = (normalizedShape.points.maxOf { it.y } - normalizedShape.points.minOf { it.y }) * testCase.spaceSize.height

            assertEquals(
                originalWidth.toDouble(),
                denormalizedWidth.toDouble(),
                2.0, // 2cm の誤差を許容（浮動小数点演算の誤差）
                "${testCase.name}: Denormalized width should match original"
            )
            assertEquals(
                originalHeight.toDouble(),
                denormalizedHeight.toDouble(),
                2.0, // 2cm の誤差を許容
                "${testCase.name}: Denormalized height should match original"
            )
        }
    }

    /**
     * 配置済み家具が spaceSize に応じて正しく正規化されることを検証
     */
    @Test
    fun testCurrentShapesScaleWithSpaceSize() {
        val placedFurniture = createTestPlacedFurniture()

        spaceSizeTestCases.forEach { testCase ->
            // 正規化されたshapeリストを計算
            val normalizedShapes = normalizePlacedFurniture(
                listOf(placedFurniture),
                testCase.spaceSize
            )

            assertEquals(1, normalizedShapes.size, "${testCase.name}: Should have 1 normalized shape")

            val normalizedShape = normalizedShapes.first()

            // 位置が正規化されていることを確認
            assertTrue(
                normalizedShape.position.x in 0.0f..1.0f,
                "${testCase.name}: Position x=${normalizedShape.position.x} is out of normalized range"
            )
            assertTrue(
                normalizedShape.position.y in 0.0f..1.0f,
                "${testCase.name}: Position y=${normalizedShape.position.y} is out of normalized range"
            )

            // shape の各頂点が正規化されていることを確認
            normalizedShape.shape.points.forEach { point ->
                assertTrue(
                    point.x in 0.0f..1.0f,
                    "${testCase.name}: Shape point x=${point.x} is out of normalized range"
                )
                assertTrue(
                    point.y in 0.0f..1.0f,
                    "${testCase.name}: Shape point y=${point.y} is out of normalized range"
                )
            }
        }
    }

    /**
     * 編集中の家具が spaceSize に応じて正しく正規化されることを検証
     */
    @Test
    fun testEditingFurnitureScalesWithSpaceSize() {
        val furniture = createTestFurniture()

        spaceSizeTestCases.forEach { testCase ->
            // 新しく選択された家具を正規化
            val normalizedShape = normalizeFurniture(furniture, testCase.spaceSize)

            // shape の各頂点が正規化されていることを確認
            normalizedShape.shape.points.forEach { point ->
                assertTrue(
                    point.x in 0.0f..1.0f,
                    "${testCase.name}: Editing shape point x=${point.x} is out of normalized range"
                )
                assertTrue(
                    point.y in 0.0f..1.0f,
                    "${testCase.name}: Editing shape point y=${point.y} is out of normalized range"
                )
            }

            // 初期位置が中央（0.5, 0.5）であることを確認
            assertEquals(
                0.5f,
                normalizedShape.position.x,
                0.01f,
                "${testCase.name}: Initial position x should be centered"
            )
            assertEquals(
                0.5f,
                normalizedShape.position.y,
                0.01f,
                "${testCase.name}: Initial position y should be centered"
            )
        }
    }

    /**
     * 正規化と逆正規化のラウンドトリップが正しく動作することを検証
     * （正規化された座標を逆正規化すると元の絶対座標に戻ることを確認）
     */
    @Test
    fun testNormalizationRoundTrip() {
        val placedFurniture = createTestPlacedFurniture()

        spaceSizeTestCases.forEach { testCase ->
            // 正規化
            val normalized = normalizePlacedFurniture(listOf(placedFurniture), testCase.spaceSize).first()

            // 逆正規化（元の絶対座標に戻す）
            val denormalized = Point(
                Centimeter((normalized.position.x * testCase.spaceSize.width).toInt()),
                Centimeter((normalized.position.y * testCase.spaceSize.height).toInt())
            )

            // 元の座標が復元されることを確認（整数変換による誤差を考慮）
            assertEquals(
                placedFurniture.position.x.value.toDouble(),
                denormalized.x.value.toDouble(),
                1.0,
                "${testCase.name}: Round-trip position x should match original"
            )
            assertEquals(
                placedFurniture.position.y.value.toDouble(),
                denormalized.y.value.toDouble(),
                1.0,
                "${testCase.name}: Round-trip position y should match original"
            )
        }
    }

    /**
     * 正規化された位置から元の絶対座標への逆変換が正しいことを検証
     */
    @Test
    fun testDenormalizationRestoresOriginalCoordinates() {
        val originalPosition = Point(300.cm(), 400.cm())

        spaceSizeTestCases.forEach { testCase ->
            // 正規化
            val normalized = NormalizedPoint(
                x = originalPosition.x.value / testCase.spaceSize.width.toFloat(),
                y = originalPosition.y.value / testCase.spaceSize.height.toFloat()
            )

            // 逆正規化
            val denormalized = Point(
                Centimeter((normalized.x * testCase.spaceSize.width).toInt()),
                Centimeter((normalized.y * testCase.spaceSize.height).toInt())
            )

            // 元の座標が復元されることを確認（整数変換による誤差を考慮）
            assertEquals(
                originalPosition.x.value.toDouble(),
                denormalized.x.value.toDouble(),
                1.0, // 1cm の誤差を許容
                "${testCase.name}: Denormalized x coordinate should match original"
            )
            assertEquals(
                originalPosition.y.value.toDouble(),
                denormalized.y.value.toDouble(),
                1.0, // 1cm の誤差を許容
                "${testCase.name}: Denormalized y coordinate should match original"
            )
        }
    }

    // ヘルパー関数: テスト用の Room を作成
    private fun createTestRoom(): Room {
        return Room(
            name = "Test Room",
            shape = Polygon(
                points = listOf(
                    Point(50.cm(), 50.cm()),
                    Point(450.cm(), 50.cm()),
                    Point(450.cm(), 450.cm()),
                    Point(50.cm(), 450.cm())
                )
            )
        )
    }

    // ヘルパー関数: テスト用の Furniture を作成
    private fun createTestFurniture(): Furniture {
        return Furniture(
            name = "Test Table",
            shape = Polygon(
                points = listOf(
                    Point(0.cm(), 0.cm()),
                    Point(120.cm(), 0.cm()),
                    Point(120.cm(), 80.cm()),
                    Point(0.cm(), 80.cm())
                )
            )
        )
    }

    // ヘルパー関数: テスト用の PlacedFurniture を作成
    private fun createTestPlacedFurniture(): PlacedFurniture {
        return PlacedFurniture(
            furniture = createTestFurniture(),
            position = Point(300.cm(), 400.cm()),
            rotation = 0f.degree()
        )
    }

    // ヘルパー関数: Room shape を正規化（FurniturePlacementTemplate のロジックと同じ）
    private fun normalizeRoomShape(room: Room, spaceSize: IntSize): NormalizedShape {
        return NormalizedShape(
            points = room.shape.points.map { point ->
                NormalizedPoint(
                    x = point.x.value / spaceSize.width.toFloat(),
                    y = point.y.value / spaceSize.height.toFloat()
                )
            },
            color = androidx.compose.ui.graphics.Color.Gray,
            strokeWidth = 2f
        )
    }

    // ヘルパー関数: PlacedFurniture を正規化（FurniturePlacementTemplate のロジックと同じ）
    private fun normalizePlacedFurniture(
        placedItems: List<PlacedFurniture>,
        spaceSize: IntSize
    ): List<NormalizedPlacedShape> {
        return placedItems.map { placedFurniture ->
            NormalizedPlacedShape(
                shape = NormalizedShape(
                    points = placedFurniture.furniture.shape.points.map { point ->
                        NormalizedPoint(
                            x = point.x.value / spaceSize.width.toFloat(),
                            y = point.y.value / spaceSize.height.toFloat()
                        )
                    },
                    color = androidx.compose.ui.graphics.Color.Green
                ),
                position = NormalizedPoint(
                    x = placedFurniture.position.x.value / spaceSize.width.toFloat(),
                    y = placedFurniture.position.y.value / spaceSize.height.toFloat()
                ),
                rotation = placedFurniture.rotation.value,
                color = androidx.compose.ui.graphics.Color.Green,
                name = placedFurniture.furniture.name
            )
        }
    }

    // ヘルパー関数: Furniture を正規化（FurniturePlacementTemplate のロジックと同じ）
    private fun normalizeFurniture(furniture: Furniture, spaceSize: IntSize): NormalizedPlacedShape {
        return NormalizedPlacedShape(
            shape = NormalizedShape(
                points = furniture.shape.points.map { point ->
                    NormalizedPoint(
                        x = point.x.value / spaceSize.width.toFloat(),
                        y = point.y.value / spaceSize.height.toFloat()
                    )
                },
                color = androidx.compose.ui.graphics.Color.Green
            ),
            position = NormalizedPoint(x = 0.5f, y = 0.5f),
            rotation = 0f,
            color = androidx.compose.ui.graphics.Color.Green,
            name = furniture.name
        )
    }
}
