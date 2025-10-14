package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.PreviewTemplate
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import kotlin.math.roundToInt

/**
 * Grid slot identifiers for the 3x3 adaptive grid
 * グリッド内の各スロットを識別するための列挙型
 */
private enum class GridSlot {
    C1, C2, C3,
    C4, C5, C6,
    C7, C8, C9
}

/**
 * Adaptive 3x3 Grid Organism
 *
 * 3x3グリッドレイアウトで、中央のセル(C5)のサイズに基づいて周囲のセルのサイズが決まります。
 *
 * Grid Layout:
 * ```
 * C1  C2  C3
 * C4  C5  C6
 * C7  C8  C9
 * ```
 *
 * Sizing Rules:
 * - Corner cells (C1, C3, C7, C9): Use [commonSize] for both width and height
 * - Center cell (C5): Determines the height of left/right cells and width of top/bottom cells
 * - Left/Right cells (C4, C6): Width = [commonSize], Height = C5's height
 * - Top/Bottom cells (C2, C8): Width = C5's width, Height = [commonSize]
 *
 * Aspect Ratio Behavior:
 * When C5 content is wrap_content, it uses the available width (parent width - 2 * commonSize)
 * and maintains its intrinsic aspect ratio to determine the height.
 *
 * Usage Example:
 * ```kotlin
 * AdaptiveNineGrid(
 *     commonSize = 48.dp,
 *     c1Content = { Icon(...) },
 *     c2Content = { Text("Top") },
 *     c3Content = { Icon(...) },
 *     c4Content = { Text("Left") },
 *     c5Content = { Image(...) }, // Center content determines sizing
 *     c6Content = { Text("Right") },
 *     c7Content = { Icon(...) },
 *     c8Content = { Text("Bottom") },
 *     c9Content = { Icon(...) }
 * )
 * ```
 *
 * @param modifier Modifier for the grid container
 * @param commonSize Common size for corner cells and side cell dimensions
 * @param c1Content Content for top-left corner (C1)
 * @param c2Content Content for top center (C2)
 * @param c3Content Content for top-right corner (C3)
 * @param c4Content Content for middle-left (C4)
 * @param c5Content Content for center cell (C5) - the reference cell
 * @param c6Content Content for middle-right (C6)
 * @param c7Content Content for bottom-left corner (C7)
 * @param c8Content Content for bottom center (C8)
 * @param c9Content Content for bottom-right corner (C9)
 */
@Composable
fun AdaptiveNineGrid(
    modifier: Modifier = Modifier,
    commonSize: Dp,
    c1Content: @Composable (BoxScope) -> Unit,
    c2Content: @Composable (BoxScope) -> Unit,
    c3Content: @Composable (BoxScope) -> Unit,
    c4Content: @Composable (BoxScope) -> Unit,
    c5Content: @Composable (BoxScope) -> Unit, // 中央の基準となるスロット
    c6Content: @Composable (BoxScope) -> Unit,
    c7Content: @Composable (BoxScope) -> Unit,
    c8Content: @Composable (BoxScope) -> Unit,
    c9Content: @Composable (BoxScope) -> Unit,
) {
    // Slot API を利用してコンテンツを定義
    val contentList = listOf(
        c1Content, c2Content, c3Content,
        c4Content, c5Content, c6Content,
        c7Content, c8Content, c9Content
    )
    val slotIds = GridSlot.entries

    Layout(
        content = {
            // 各コンテンツに一意のIDを割り当てる
            contentList.forEachIndexed { index, content ->
                Box(modifier = Modifier.layoutId(slotIds[index]), content = content)
            }
        },
        modifier = modifier
    ) { measurables, constraints ->

        // Dpをピクセル値(px)に変換
        val s = commonSize.roundToPx()
        val fixedConstraints = Constraints.fixed(s, s)

        // ----------------------------------------------------
        // 1. C5の測定準備 (中央の基準要素)
        // ----------------------------------------------------

        val c5Measurable = measurables.first { it.layoutId == GridSlot.C5 }

        // C5が利用できる最大空間を計算 (親の最大空間から固定セル分を引く)
        val centerMaxW = maxOf(0, constraints.maxWidth - 2 * s)
        val centerMaxH = maxOf(0, constraints.maxHeight - 2 * s)

        // ----------------------------------------------------
        // 2. C5のサイズ決定ロジック (Wrap Content + アスペクト比維持)
        // ----------------------------------------------------

        // Step A: C5を制約なし(Loose Constraints)で測定し、コンテンツの自然な幅と高さを取得する。
        // これにより、コンテンツが持つべきアスペクト比が判明する。
        val c5Natural = c5Measurable.measure(
            Constraints(maxWidth = centerMaxW, maxHeight = centerMaxH)
        )
        val naturalWidth = c5Natural.width.toFloat()
        val naturalHeight = c5Natural.height.toFloat()

        // デフォルトのアスペクト比を1.0f (1:1)として、0除算を避ける
        val ratio = if (naturalHeight > 0 && naturalWidth > 0 && 
                        naturalWidth.isFinite() && naturalHeight.isFinite()) {
            naturalWidth / naturalHeight
        } else {
            1.0f
        }

        // Step B: 最終的なC5の幅と高さを決定する

        // 要件: C5の幅が wrap_content の場合でも、親の最大利用可能幅 (centerMaxW) を使用する。
        val c5FinalWidth = centerMaxW

        val c5FinalHeight = if (ratio > 0 && c5FinalWidth > 0) {
            // 計算された幅に基づいて、アスペクト比を維持した高さを算出
            val calculatedHeight = (c5FinalWidth / ratio).roundToInt()

            // 算出した高さを、利用可能な最大高さ (centerMaxH) で制限する
            calculatedHeight.coerceIn(0, centerMaxH)
        } else {
            // アスペクト比を計算できないか、幅がゼロの場合、利用可能な最大高さをそのまま使用
            centerMaxH
        }

        // Step C: 最終決定されたサイズでC5を再測定する
        val c5Placeable = c5Measurable.measure(Constraints.fixed(c5FinalWidth, c5FinalHeight))

        // C5の確定したサイズを基準値として設定 [要件]
        val centerWidth = c5Placeable.width
        val centerHeight = c5Placeable.height

        // ----------------------------------------------------
        // 3. 他のセル（C1, C3, C7, C9, C2, C4, C6, C8）の測定
        // ----------------------------------------------------

        val placeables = mutableMapOf<GridSlot, Placeable>()
        placeables[GridSlot.C5] = c5Placeable

        // コーナーセル (C1, C3, C7, C9) は共通サイズ (s x s)
        val cornerSlots = listOf(GridSlot.C1, GridSlot.C3, GridSlot.C7, GridSlot.C9)
        cornerSlots.forEach { id ->
            val m = measurables.first { it.layoutId == id }
            placeables[id] = m.measure(fixedConstraints)
        }

        // 左右のセル (C4, C6): 幅=共通サイズ (s), 高さ=C5の高さ (centerHeight) [要件]
        val c4c6Constraints = Constraints.fixed(width = s, height = centerHeight)
        placeables[GridSlot.C4] = measurables.first { it.layoutId == GridSlot.C4 }.measure(c4c6Constraints)
        placeables[GridSlot.C6] = measurables.first { it.layoutId == GridSlot.C6 }.measure(c4c6Constraints)

        // 上下のセル (C2, C8): 幅=C5の幅 (centerWidth), 高さ=共通サイズ (s) [要件]
        val c2c8Constraints = Constraints.fixed(width = centerWidth, height = s)
        placeables[GridSlot.C2] = measurables.first { it.layoutId == GridSlot.C2 }.measure(c2c8Constraints)
        placeables[GridSlot.C8] = measurables.first { it.layoutId == GridSlot.C8 }.measure(c2c8Constraints)

        // ----------------------------------------------------
        // 4. グリッド全体のサイズ決定と配置
        // ----------------------------------------------------

        val totalWidth = s + centerWidth + s
        val totalHeight = s + centerHeight + s

        // レイアウトサイズを親の制約内に収める (通常はぴったり合うはず)
        val layoutWidth = totalWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        // 配置を実行 (Placeable.PlacementScope)
        layout(layoutWidth, layoutHeight) {
            val x1 = 0
            val x2 = s
            val x3 = s + centerWidth

            val y1 = 0
            val y2 = s
            val y3 = s + centerHeight

            // Row 1 (y1)
            placeables[GridSlot.C1]?.placeRelative(x1, y1) 
                ?: error("Missing placeable for C1")
            placeables[GridSlot.C2]?.placeRelative(x2, y1) 
                ?: error("Missing placeable for C2")
            placeables[GridSlot.C3]?.placeRelative(x3, y1) 
                ?: error("Missing placeable for C3")

            // Row 2 (y2)
            placeables[GridSlot.C4]?.placeRelative(x1, y2) 
                ?: error("Missing placeable for C4")
            placeables[GridSlot.C5]?.placeRelative(x2, y2) 
                ?: error("Missing placeable for C5")
            placeables[GridSlot.C6]?.placeRelative(x3, y2) 
                ?: error("Missing placeable for C6")

            // Row 3 (y3)
            placeables[GridSlot.C7]?.placeRelative(x1, y3) 
                ?: error("Missing placeable for C7")
            placeables[GridSlot.C8]?.placeRelative(x2, y3) 
                ?: error("Missing placeable for C8")
            placeables[GridSlot.C9]?.placeRelative(x3, y3) 
                ?: error("Missing placeable for C9")
        }
    }
}

// Preview Helper Composable
@Composable
private fun ColoredCell(
    color: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Preview: Basic grid with icons in corners and text in sides
 * コーナーにアイコン、サイドにテキストを配置した基本的なグリッド
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_Basic() {
    PreviewTemplate {
        Box(modifier = Modifier.size(400.dp).padding(16.dp)) {
            AdaptiveNineGrid(
                commonSize = 48.dp,
                c1Content = { 
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary), 
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Menu, "Menu", tint = Color.White)
                    }
                },
                c2Content = { ColoredCell(MaterialTheme.colorScheme.secondary, "Top") },
                c3Content = { 
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Search, "Search", tint = Color.White)
                    }
                },
                c4Content = { ColoredCell(MaterialTheme.colorScheme.secondary, "Left") },
                c5Content = { ColoredCell(MaterialTheme.colorScheme.tertiary, "Center\nMain Content") },
                c6Content = { ColoredCell(MaterialTheme.colorScheme.secondary, "Right") },
                c7Content = { 
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                },
                c8Content = { ColoredCell(MaterialTheme.colorScheme.secondary, "Bottom") },
                c9Content = { 
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.MoreVert, "More", tint = Color.White)
                    }
                }
            )
        }
    }
}

/**
 * Preview: Small corner size - center content dominates
 * コーナーサイズが小さく、中央のコンテンツが支配的なケース
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_SmallCorners() {
    PreviewTemplate {
        Box(modifier = Modifier.size(400.dp).padding(16.dp)) {
            AdaptiveNineGrid(
                commonSize = 32.dp,
                c1Content = { ColoredCell(Color(0xFF8B4513), "C1") },
                c2Content = { ColoredCell(Color(0xFFDEB887), "C2") },
                c3Content = { ColoredCell(Color(0xFF8B4513), "C3") },
                c4Content = { ColoredCell(Color(0xFFDEB887), "C4") },
                c5Content = { 
                    ColoredCell(Color(0xFFD4856A), "Large\nCenter\nContent\nArea") 
                },
                c6Content = { ColoredCell(Color(0xFFDEB887), "C6") },
                c7Content = { ColoredCell(Color(0xFF8B4513), "C7") },
                c8Content = { ColoredCell(Color(0xFFDEB887), "C8") },
                c9Content = { ColoredCell(Color(0xFF8B4513), "C9") }
            )
        }
    }
}

/**
 * Preview: Large corner size - narrow center
 * コーナーサイズが大きく、中央が狭いケース
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_LargeCorners() {
    PreviewTemplate {
        Box(modifier = Modifier.size(400.dp).padding(16.dp)) {
            AdaptiveNineGrid(
                commonSize = 80.dp,
                c1Content = { ColoredCell(Color(0xFF6B5B95), "1") },
                c2Content = { ColoredCell(Color(0xFF9B8AC4), "2") },
                c3Content = { ColoredCell(Color(0xFF6B5B95), "3") },
                c4Content = { ColoredCell(Color(0xFF9B8AC4), "4") },
                c5Content = { ColoredCell(Color(0xFFE8B4A0), "Center") },
                c6Content = { ColoredCell(Color(0xFF9B8AC4), "6") },
                c7Content = { ColoredCell(Color(0xFF6B5B95), "7") },
                c8Content = { ColoredCell(Color(0xFF9B8AC4), "8") },
                c9Content = { ColoredCell(Color(0xFF6B5B95), "9") }
            )
        }
    }
}

/**
 * Preview: Image viewer layout with controls
 * 画像ビューアレイアウト（コントロール付き）
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_ImageViewer() {
    PreviewTemplate {
        Box(modifier = Modifier.size(450.dp).padding(16.dp)) {
            AdaptiveNineGrid(
                commonSize = 56.dp,
                c1Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF2C3E50)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                },
                c2Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF34495E)),
                        contentAlignment = Alignment.Center) {
                        AppText("Image Viewer", color = Color.White)
                    }
                },
                c3Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF2C3E50)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Share, "Share", tint = Color.White)
                    }
                },
                c4Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF34495E)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.KeyboardArrowLeft, "Previous", tint = Color.White)
                    }
                },
                c5Content = { 
                    Box(Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center) {
                        AppText("📷\nImage\nContent", color = Color.White, 
                            style = MaterialTheme.typography.headlineMedium)
                    }
                },
                c6Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF34495E)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.KeyboardArrowRight, "Next", tint = Color.White)
                    }
                },
                c7Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF2C3E50)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Delete, "Delete", tint = Color.White)
                    }
                },
                c8Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF34495E)),
                        contentAlignment = Alignment.Center) {
                        AppText("1 / 10", color = Color.White)
                    }
                },
                c9Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF2C3E50)),
                        contentAlignment = Alignment.Center) {
                        AppIcon(Icons.Default.Edit, "Edit", tint = Color.White)
                    }
                }
            )
        }
    }
}

/**
 * Preview: Dashboard layout with center content focus
 * ダッシュボードレイアウト（中央コンテンツにフォーカス）
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_Dashboard() {
    PreviewTemplate {
        Box(modifier = Modifier.size(500.dp).padding(16.dp)) {
            AdaptiveNineGrid(
                commonSize = 64.dp,
                c1Content = { 
                    ColoredCell(Color(0xFF4A90E2), "Stats") 
                },
                c2Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF5BA3F5)),
                        contentAlignment = Alignment.Center) {
                        AppText("Navigation Bar", color = Color.White)
                    }
                },
                c3Content = { 
                    ColoredCell(Color(0xFF4A90E2), "Profile") 
                },
                c4Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF5BA3F5)),
                        contentAlignment = Alignment.Center) {
                        AppText("Menu", color = Color.White)
                    }
                },
                c5Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center) {
                        AppText(
                            "Main Dashboard\nContent Area\n\n📊 Charts\n📈 Data\n📋 Reports",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                c6Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF5BA3F5)),
                        contentAlignment = Alignment.Center) {
                        AppText("Tools", color = Color.White)
                    }
                },
                c7Content = { 
                    ColoredCell(Color(0xFF4A90E2), "Help") 
                },
                c8Content = { 
                    Box(Modifier.fillMaxSize().background(Color(0xFF5BA3F5)),
                        contentAlignment = Alignment.Center) {
                        AppText("Status Bar", color = Color.White)
                    }
                },
                c9Content = { 
                    ColoredCell(Color(0xFF4A90E2), "Settings") 
                }
            )
        }
    }
}

