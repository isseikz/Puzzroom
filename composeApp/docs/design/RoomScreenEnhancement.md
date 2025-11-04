# RoomScreen Enhancement - 図形調整と寸法入力機能

## Overview

RoomScreenに以下の機能を追加し、より正確な部屋の形状定義を可能にします：

1. **図形調整機能** - ポリゴンの頂点をドラッグで移動
2. **寸法入力機能** - 各辺の長さをCentimeter単位で整数入力

## Current State

### 現在の機能

```kotlin
@Composable
fun RoomScreen(appState: AppState, viewModel: ProjectViewModel) {
    // InputPolygon で間取り図をもとにポリゴンを作成
    InputPolygon(
        polygons = polygons,
        backgroundImageUrl = project?.layoutUrl
    ) { newPolygon ->
        // クリックで頂点を追加
        // 最初の頂点近くでクリックするとポリゴン完成
        // 即座に永続化
    }
}
```

**特徴:**
- ✅ 間取り図を背景に表示
- ✅ クリックで頂点を追加
- ✅ ポリゴンの自動完成
- ✅ 即座に永続化
- ❌ 頂点の調整不可
- ❌ 寸法の正確な入力不可

## Requirements

### 1. 図形調整機能

**ユーザーストーリー:**
> 間取り図からポリゴンを作成した後、頂点の位置を微調整したい

**機能要件:**
- ポリゴンの選択
- 頂点のドラッグ移動
- 頂点の追加・削除（オプション）
- 変更の即座反映と自動保存

### 2. 寸法入力機能

**ユーザーストーリー:**
> 各辺の正確な長さをCentimeter単位で入力したい

**機能要件:**
- 選択したポリゴンの各辺の長さを表示
- 各辺の長さを整数値で入力
- 入力値に基づいてポリゴンを自動調整
- 変更の即座反映と自動保存

## Design

### UI Layout

```
┌────────────────────────────────────────────────────────┐
│ RoomScreen                                              │
│ ┌────────────────┬───────────────────────────────────┐ │
│ │                │ Polygon List Panel                │ │
│ │                │ ┌───────────────────────────────┐ │ │
│ │                │ │ Room 1            [Edit] [×]  │ │ │
│ │                │ │ 頂点数: 4                      │ │ │
│ │                │ └───────────────────────────────┘ │ │
│ │                │ ┌───────────────────────────────┐ │ │
│ │  Canvas        │ │ Room 2 (Selected) [Edit] [×]  │ │ │
│ │                │ │ 頂点数: 6                      │ │ │
│ │  (Background   │ └───────────────────────────────┘ │ │
│ │   Image +      │                                   │ │
│ │   Polygons)    │ Dimension Input Panel             │ │
│ │                │ (選択時のみ表示)                   │ │
│ │                │ ┌───────────────────────────────┐ │ │
│ │                │ │ 辺 1: [  200  ] cm            │ │ │
│ │                │ │ 辺 2: [  150  ] cm            │ │ │
│ │                │ │ 辺 3: [  200  ] cm            │ │ │
│ │                │ │ 辺 4: [  150  ] cm            │ │ │
│ │                │ │                               │ │ │
│ │                │ │ [適用] [キャンセル]           │ │ │
│ │                │ └───────────────────────────────┘ │ │
│ └────────────────┴───────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

### Interaction Modes

#### Mode 1: Creation Mode（作成モード）

**現在の機能を維持:**
- クリックで頂点を追加
- 最初の頂点近くでクリックするとポリゴン完成
- 即座に永続化

**トリガー:**
- デフォルトモード
- ポリゴンが選択されていない状態

#### Mode 2: Edit Mode（編集モード）

**新機能:**
- 頂点をドラッグで移動
- サイドパネルで寸法を入力

**トリガー:**
- ポリゴンリストから「Edit」ボタンをクリック
- キャンバス上でポリゴンをクリック

### Data Flow

#### 1. 頂点の調整

```
User drags vertex
    ↓
Canvas.onDrag { newPosition }
    ↓
Update polygon.points[index] = newPosition
    ↓
Update Project
    ↓
viewModel.updateProject(updatedProject)
    ↓
Auto-save (500ms debounce)
```

#### 2. 寸法入力

```
User inputs dimension (e.g., 200 cm)
    ↓
Calculate new vertex positions
    ↓
Update polygon.points
    ↓
Update Project
    ↓
viewModel.updateProject(updatedProject)
    ↓
Auto-save (500ms debounce)
```

### State Management

```kotlin
@Composable
fun RoomScreen(appState: AppState, viewModel: ProjectViewModel) {
    val project by viewModel.currentProject.collectAsState()
    var polygons by remember { mutableStateOf(emptyList<Polygon>()) }

    // 編集状態
    var editMode by remember { mutableStateOf<EditMode>(EditMode.Creation) }
    var selectedPolygonIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVertexIndex by remember { mutableStateOf<Int?>(null) }

    // 寸法入力
    var dimensionInputs by remember { mutableStateOf<List<Int>>(emptyList()) }
}

sealed interface EditMode {
    object Creation : EditMode
    data class Editing(val polygonIndex: Int) : EditMode
}
```

## Implementation Plan

### Phase 1: Polygon Selection & List UI

**タスク:**
1. ポリゴンリストUIの作成
2. ポリゴン選択機能
3. 選択状態の可視化（ハイライト）

**ファイル:**
- `ui/component/PolygonListPanel.kt` (新規)
- `ui/screen/RoomScreen.kt` (修正)

### Phase 2: Vertex Editing

**タスク:**
1. 頂点のドラッグ検出
2. 頂点位置の更新
3. ポリゴンの再描画
4. 自動保存トリガー

**ファイル:**
- `ui/component/EditablePolygonCanvas.kt` (新規)
- `ui/screen/RoomScreen.kt` (修正)

### Phase 3: Dimension Input

**タスク:**
1. 寸法入力パネルUI
2. 各辺の長さ計算
3. 寸法変更時の頂点位置調整ロジック
4. 自動保存トリガー

**ファイル:**
- `ui/component/DimensionInputPanel.kt` (新規)
- `domain/PolygonGeometry.kt` (新規 - 幾何計算ロジック)
- `ui/screen/RoomScreen.kt` (修正)

## UI Components

### 1. PolygonListPanel

```kotlin
@Composable
fun PolygonListPanel(
    polygons: List<Polygon>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(polygons) { index, polygon ->
            PolygonCard(
                polygon = polygon,
                isSelected = index == selectedIndex,
                onSelect = { onSelect(index) },
                onEdit = { onEdit(index) },
                onDelete = { onDelete(index) }
            )
        }
    }
}

@Composable
fun PolygonCard(
    polygon: Polygon,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Room ${polygon.hashCode() % 100}", style = MaterialTheme.typography.titleMedium)
                Text("頂点数: ${polygon.points.size}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "編集")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "削除")
                }
            }
        }
    }
}
```

### 2. EditablePolygonCanvas

```kotlin
@Composable
fun EditablePolygonCanvas(
    polygons: List<Polygon>,
    selectedPolygonIndex: Int?,
    selectedVertexIndex: Int?,
    backgroundImageUrl: String?,
    editMode: EditMode,
    onVertexDrag: (polygonIndex: Int, vertexIndex: Int, newPosition: Point) -> Unit,
    onVertexSelect: (polygonIndex: Int, vertexIndex: Int) -> Unit,
    onNewVertex: (position: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragState by remember { mutableStateOf<DragState?>(null) }

    Box(modifier = modifier) {
        // Background image
        backgroundImageUrl?.let {
            AsyncImage(model = it, contentDescription = "Background", modifier = Modifier.fillMaxSize())
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(editMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            when (editMode) {
                                is EditMode.Editing -> {
                                    // 頂点を選択
                                    val (polygonIdx, vertexIdx) = findNearestVertex(offset, polygons, selectedPolygonIndex)
                                    if (polygonIdx != null && vertexIdx != null) {
                                        onVertexSelect(polygonIdx, vertexIdx)
                                        dragState = DragState(polygonIdx, vertexIdx, offset)
                                    }
                                }
                                EditMode.Creation -> {
                                    // 作成モード（既存の動作）
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            dragState?.let { state ->
                                val newOffset = state.currentOffset + dragAmount
                                onVertexDrag(state.polygonIndex, state.vertexIndex, newOffset.toPoint())
                                dragState = state.copy(currentOffset = newOffset)
                            }
                        },
                        onDragEnd = {
                            dragState = null
                        }
                    )
                }
        ) {
            // Draw polygons
            polygons.forEachIndexed { index, polygon ->
                val isSelected = index == selectedPolygonIndex
                val color = if (isSelected) Color.Blue else Color.Red
                val strokeWidth = if (isSelected) 7f else 5f

                // Draw edges
                drawPoints(
                    points = polygon.points.map { it.toCanvasOffset() } + polygon.points.first().toCanvasOffset(),
                    pointMode = PointMode.Polygon,
                    color = color,
                    strokeWidth = strokeWidth
                )

                // Draw vertices (if selected)
                if (isSelected) {
                    polygon.points.forEachIndexed { vertexIndex, point ->
                        val vertexColor = if (vertexIndex == selectedVertexIndex) Color.Green else Color.Blue
                        drawCircle(
                            color = vertexColor,
                            radius = 10f,
                            center = point.toCanvasOffset()
                        )
                    }
                }
            }
        }
    }
}

data class DragState(
    val polygonIndex: Int,
    val vertexIndex: Int,
    val currentOffset: Offset
)
```

### 3. DimensionInputPanel

```kotlin
@Composable
fun DimensionInputPanel(
    polygon: Polygon,
    onDimensionsChange: (List<Int>) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = remember(polygon) {
        polygon.points.indices.map { index ->
            val p1 = polygon.points[index]
            val p2 = polygon.points[(index + 1) % polygon.points.size]
            calculateDistance(p1, p2).roundToInt()
        }
    }

    var inputValues by remember(polygon) { mutableStateOf(dimensions) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text("寸法入力", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 各辺の入力フィールド
        dimensions.indices.forEach { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("辺 ${index + 1}:")
                OutlinedTextField(
                    value = inputValues[index].toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { value ->
                            inputValues = inputValues.toMutableList().apply {
                                this[index] = value
                            }
                        }
                    },
                    label = { Text("cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // アクションボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("キャンセル")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onDimensionsChange(inputValues)
                onApply()
            }) {
                Text("適用")
            }
        }
    }
}
```

## Geometry Calculations

### Distance Calculation

```kotlin
// domain/PolygonGeometry.kt
fun calculateDistance(p1: Point, p2: Point): Double {
    val dx = (p2.x.value - p1.x.value).toDouble()
    val dy = (p2.y.value - p1.y.value).toDouble()
    return sqrt(dx * dx + dy * dy)
}
```

### Polygon Adjustment by Dimensions

**戦略:**
1. **固定点を決める** - 最初の頂点を固定
2. **順番に頂点を配置** - 各辺の長さと角度から次の頂点を計算

```kotlin
fun adjustPolygonByDimensions(
    polygon: Polygon,
    edgeLengths: List<Int>
): Polygon {
    require(edgeLengths.size == polygon.points.size) {
        "Edge lengths must match number of vertices"
    }

    val newPoints = mutableListOf<Point>()

    // 最初の頂点を固定
    newPoints.add(polygon.points[0])

    // 最初の辺の方向を保持
    val firstEdge = polygon.points[1] - polygon.points[0]
    val firstAngle = atan2(
        (firstEdge.y.value).toDouble(),
        (firstEdge.x.value).toDouble()
    )

    var currentAngle = firstAngle
    var currentPoint = polygon.points[0]

    // 各辺の長さから頂点を計算
    edgeLengths.dropLast(1).forEachIndexed { index, length ->
        // 元のポリゴンの角度を計算
        val originalEdge1 = polygon.points[(index + 1) % polygon.points.size] -
                           polygon.points[index]
        val originalEdge2 = polygon.points[(index + 2) % polygon.points.size] -
                           polygon.points[(index + 1) % polygon.points.size]

        val angle1 = atan2(originalEdge1.y.value.toDouble(), originalEdge1.x.value.toDouble())
        val angle2 = atan2(originalEdge2.y.value.toDouble(), originalEdge2.x.value.toDouble())

        // 内角を保持
        val interiorAngle = angle2 - angle1

        // 新しい頂点を計算
        val newX = currentPoint.x.value + (length * cos(currentAngle)).roundToInt()
        val newY = currentPoint.y.value + (length * sin(currentAngle)).roundToInt()
        val newPoint = Point(Centimeter(newX), Centimeter(newY))

        newPoints.add(newPoint)
        currentPoint = newPoint
        currentAngle += interiorAngle
    }

    return Polygon(points = newPoints)
}
```

**注意:**
- この実装は複雑で、辺の長さを変更すると形状が崩れる可能性がある
- 簡易版として、**1つの辺のみ変更可能**にする方が実装が容易

### Simplified Approach（推奨）

**制約:**
- 1つの辺の長さのみ変更可能
- 選択した辺の両端の頂点のうち、片方を固定
- もう片方を移動して辺の長さを調整

```kotlin
fun adjustEdgeLength(
    polygon: Polygon,
    edgeIndex: Int,
    newLength: Int
): Polygon {
    val p1 = polygon.points[edgeIndex]
    val p2 = polygon.points[(edgeIndex + 1) % polygon.points.size]

    // 辺の方向ベクトル
    val dx = p2.x.value - p1.x.value
    val dy = p2.y.value - p1.y.value
    val currentLength = sqrt((dx * dx + dy * dy).toDouble())

    // 正規化された方向ベクトル
    val normDx = dx / currentLength
    val normDy = dy / currentLength

    // 新しい第2頂点の位置
    val newP2 = Point(
        Centimeter((p1.x.value + newLength * normDx).roundToInt()),
        Centimeter((p1.y.value + newLength * normDy).roundToInt())
    )

    // ポリゴンを更新
    return polygon.copy(
        points = polygon.points.mapIndexed { index, point ->
            if (index == (edgeIndex + 1) % polygon.points.size) newP2 else point
        }
    )
}
```

## Testing Strategy

### Unit Tests

```kotlin
class PolygonGeometryTest {
    @Test
    fun `calculateDistance returns correct distance`() {
        val p1 = Point(Centimeter(0), Centimeter(0))
        val p2 = Point(Centimeter(3), Centimeter(4))

        val distance = calculateDistance(p1, p2)

        assertEquals(5.0, distance, 0.01)
    }

    @Test
    fun `adjustEdgeLength updates vertex correctly`() {
        val polygon = Polygon(
            points = listOf(
                Point(Centimeter(0), Centimeter(0)),
                Point(Centimeter(100), Centimeter(0)),
                Point(Centimeter(100), Centimeter(100)),
                Point(Centimeter(0), Centimeter(100))
            )
        )

        val adjusted = adjustEdgeLength(polygon, 0, 200)

        assertEquals(Centimeter(200), adjusted.points[1].x)
        assertEquals(Centimeter(0), adjusted.points[1].y)
    }
}
```

### Integration Tests

1. **ポリゴン選択テスト**
   - ポリゴンをクリックして選択
   - 選択状態が可視化される

2. **頂点ドラッグテスト**
   - 頂点をドラッグして移動
   - ポリゴンが更新される
   - 自動保存される

3. **寸法入力テスト**
   - 寸法を入力
   - ポリゴンが調整される
   - 自動保存される

## Best Practices

### 1. UX Considerations

- **編集モードの明示** - 現在のモードをUIで明確に表示
- **元に戻す機能** - 誤操作に備えてUndo機能を提供（Phase 2以降）
- **スナップ機能** - 頂点を近接する点にスナップ（Phase 2以降）

### 2. Performance

- **Debouncing** - 頂点ドラッグ中は500msごとに自動保存
- **Local State** - ドラッグ中はローカル状態で描画、ドラッグ終了時にProjectを更新

### 3. Data Integrity

- **バリデーション** - 無効なポリゴン（辺が交差する、面積が0など）をチェック
- **制約** - 最小/最大の辺の長さを設定

## Future Enhancements (Phase 2+)

1. **Undo/Redo** - 変更履歴の管理
2. **Snap to Grid** - グリッドへのスナップ機能
3. **Angle Input** - 角度の入力機能
4. **Vertex Addition/Deletion** - 頂点の追加・削除
5. **Multi-select** - 複数頂点の同時移動
6. **Constraints** - 辺の長さや角度の制約
7. **Templates** - 一般的な部屋形状（正方形、長方形など）のテンプレート

## Summary

この設計により：

✅ **直感的な操作** - ドラッグで視覚的に調整 + 寸法で正確に入力
✅ **即座の反映** - 変更が即座に可視化され、自動保存される
✅ **拡張性** - Phase 2以降でUndo/Redo、Snap to Gridなどを追加可能
✅ **データ整合性** - ProjectViewModelで一元管理、自動保存で損失防止

ユーザーは間取り図から大まかな形状を作成し、その後、頂点のドラッグや寸法入力で正確な部屋の形状を定義できます。
