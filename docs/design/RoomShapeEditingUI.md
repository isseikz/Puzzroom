# 部屋の形状編集UI - 実装ドキュメント

## 概要

このドキュメントは、部屋の形状を編集するための包括的なUIシステムの実装について説明します。ユーザーは間取り図の画像をインポートし、多角形をトレースして部屋の形状を定義し、辺の長さや頂点の角度を精密に調整できます。

## アーキテクチャ

### 主要コンポーネント

#### 1. ドメインモデル

**PolygonEditState** (`ui/state/PolygonEditState.kt`)
- ポリゴン編集の状態を管理
- ロックされた辺と角度を追跡
- 相似変換の適用状態を管理
- 完全拘束条件の判定

```kotlin
data class PolygonEditState(
    val lockedEdges: Set<Int> = emptySet(),
    val lockedAngles: Set<Int> = emptySet(),
    val hasAppliedSimilarity: Boolean = false
)
```

#### 2. 編集モード

**EditMode** (`ui/component/EditablePolygonCanvas.kt`)
- `Creation`: 新しいポリゴンを作成（頂点をタップで追加）
- `Editing`: 頂点をドラッグで移動
- `DimensionEditing`: 辺を選択して長さを編集
- `AngleEditing`: 頂点を選択して角度を編集

#### 3. UIコンポーネント

**EditablePolygonCanvas** (Organism)
- Canvas APIを使用してポリゴンを描画
- ユーザー入力を検出（タップ、ドラッグ）
- ロック状態を視覚的に表示（金色で強調）
- 編集モードに応じて異なる相互作用を提供

**EdgeDimensionSheet** (Organism)
- ModalBottomSheetを使用した辺の長さ編集
- ロック/アンロック機能
- 相似変換モードと個別調整モードの切り替え
- リアルタイムプレビュー

**AngleEditSheet** (Organism)
- ModalBottomSheetを使用した角度編集
- 90度拘束（ロック）機能
- ポリゴンが開く可能性についての警告
- 完全拘束状態の表示

## 機能実装

### FR-T1: 画像インポート

既存の`PhotoPickerButton`を使用して、間取り図の画像をインポートします。

```kotlin
PhotoPickerButton { url ->
    project?.let {
        val updatedProject = it.copy(layoutUrl = url)
        viewModel.updateProject(updatedProject)
    }
}
```

### FR-T2: 多角形の作成

`EditMode.Creation`で、ユーザーがタップした位置に頂点を追加します。最初の頂点近くでタップすると、ポリゴンが完成します。

```kotlin
if (creationVertices.size >= 2 &&
    (position - creationVertices.first()).getDistance() < 40f
) {
    // ポリゴン完成
    onCompletePolygon(Polygon(points = creationVertices.map { it.toPoint() }))
}
```

### FR-T3: 頂点編集（移動）

`EditMode.Editing`で、頂点をドラッグして移動できます。

```kotlin
onVertexMove: (polygonIndex: Int, vertexIndex: Int, newPosition: Point) -> Unit
```

### FR-D1 & FR-D2: 寸法編集UI表示と相似更新

`EdgeDimensionSheet`が辺を選択した際に表示されます。初回の辺の長さ指定時は、相似変換を適用します。

```kotlin
// Determine if this is the first dimension edit for this polygon
// First edit: apply similarity transformation (scale entire shape proportionally)
// Subsequent edits: adjust only the selected edge (individual adjustment)
val useSimilarity = !editState.hasAppliedSimilarity

val updatedPolygon = if (useSimilarity) {
    // First edit: scale entire polygon proportionally
    PolygonGeometry.applySimilarityTransformation(
        polygon, edgeIndex, newLength
    )
} else {
    // Subsequent edits: adjust only the selected edge
    PolygonGeometry.adjustEdgeLength(
        polygon, edgeIndex, newLength
    )
}
```

### FR-D3: 個別更新（2回目以降）

相似変換適用後は、個別の辺のみを調整し、他の辺の長さは維持します。

### FR-D4: 寸法ロック表示

ロックされた辺は金色（`Color(0xFFFFD700)`）で表示されます。

```kotlin
val isLocked = editState?.isEdgeLocked(edgeIndex) ?: false
val edgeColor = if (isLocked) Color(0xFFFFD700) else color
```

### FR-A1 & FR-A2: 角度編集UIと90度拘束

`AngleEditSheet`で角度を編集でき、90度拘束ボタンで直角にロックできます。

```kotlin
Button(onClick = { 
    onLockTo90(vertexIndex)
    if (!isLocked) {
        onAngleChange(vertexIndex, 90.0)
    }
})
```

### FR-A3: 角度更新ロジック

`PolygonGeometry.adjustAngleLocal`を使用して、指定した頂点の内角を変更します。

### FR-A4: 完全拘束判定

すべての辺がロックされているか、十分な数の辺と角度がロックされている場合、完全拘束と判定します。

```kotlin
fun isFullyConstrained(vertexCount: Int): Boolean {
    val edgeCount = vertexCount
    // A polygon is fully constrained when:
    // 1. All edges are locked, OR
    // 2. n-1 edges and n-3 angles are locked
    //    (because the sum of interior angles in an n-sided polygon is (n-2)×180°,
    //     and the last edge is determined by the first and last vertices meeting)
    return lockedEdges.size == edgeCount || 
           (lockedEdges.size >= edgeCount - 1 && 
            lockedAngles.size >= vertexCount - 3)
}
```

### FR-P1: Polygonオブジェクト通知

すべての編集操作は、更新された`Polygon`オブジェクトをViewModelに通知します。

```kotlin
viewModel.updateProject(updatedProject)
```

## 幾何学的計算

### 相似変換

`PolygonGeometry.applySimilarityTransformation`は、1つの辺の長さを基準に、ポリゴン全体をスケーリングします。

```kotlin
fun applySimilarityTransformation(
    polygon: Polygon,
    referenceEdgeIndex: Int,
    newLength: Int
): Polygon
```

### 辺の長さ調整

`PolygonGeometry.adjustEdgeLength`は、指定した辺の長さのみを変更します。

### 角度調整

`PolygonGeometry.adjustAngleLocal`は、指定した頂点の内角を変更し、前後の辺の長さを維持します。

### 辺と頂点の検出

- `findNearestEdge`: タップ位置に最も近い辺を検出
- `findNearestVertex`: タップ位置に最も近い頂点を検出

## 状態管理

### ViewModel統合

`RoomScreen`は、`ProjectViewModel`と連携して状態を管理します：

```kotlin
var polygonEditStates by remember { mutableStateOf<Map<Int, PolygonEditState>>(emptyMap()) }
```

各ポリゴンには独自の`PolygonEditState`があり、ロック状態を個別に管理します。

### 状態の永続化

ポリゴンの形状は`Project`オブジェクトに保存され、`ProjectViewModel`によって自動的に永続化されます。

## UI/UX設計

### カラーテーマ

- **通常の辺**: 青色（選択時）、赤色（非選択時）
- **ロック済み辺**: 金色（`#FFD700`）
- **頂点**: 青色（通常）、緑色（ドラッグ中）、金色（ロック済み）

### フィードバック

- **相似変換モード**: プライマリコンテナ色の情報カード
- **個別調整モード**: セカンダリコンテナ色の情報カード
- **警告**: ターシャリコンテナ色の警告カード
- **完全拘束**: プライマリコンテナ色の成功表示

### モーダルボトムシート

すべての詳細編集は`ModalBottomSheet`で実行され、メインキャンバスを妨げません：

- 辺を選択 → `EdgeDimensionSheet`が表示
- 頂点を選択 → `AngleEditSheet`が表示

## 使用フロー

1. **背景画像のインポート**: PhotoPickerButtonで間取り図をインポート
2. **ポリゴンの作成**: Creation modeで頂点をタップして追加
3. **頂点の調整**: Editing modeで頂点をドラッグして位置を調整
4. **寸法の設定**: Dimension Editing modeで辺を選択し、長さを入力
   - 初回は相似変換が適用される
   - 2回目以降は個別調整
5. **角度の調整**: Angle Editing modeで頂点を選択し、角度を入力
   - 90度拘束ボタンで直角にロック可能
6. **ロックの設定**: 重要な辺や角度をロックして固定
7. **完全拘束の確認**: すべての寸法が決定されたことを確認

## テスト

### 単体テスト

- `PolygonEditStateTest.kt`: 状態管理のロジックテスト
- `PolygonGeometryTest.kt`: 幾何学的計算のテスト

### テストカバレッジ

- ロック/アンロックの切り替え
- 相似変換の正確性
- 辺と頂点の検出精度
- 完全拘束条件の判定

## 制限事項と注意点

1. **ポリゴンの開き**: 角度調整により、ポリゴンが開く可能性があります。`autoClosePolygon`で自動的に閉じることができます。

2. **浮動小数点誤差**: 幾何学的計算には浮動小数点誤差が含まれる可能性があります。

3. **自己交差**: 現在の実装では、自己交差するポリゴンの検出と防止は行っていません。

4. **ロック済み辺の調整**: ロックされた辺や角度は変更できません。先にロックを解除する必要があります。

## 将来の拡張

- 複数辺の一括調整
- 辺の角度表示と調整
- スナップ機能（グリッド、角度）
- Undo/Redo機能
- ポリゴンのバリデーション（自己交差チェック）
- 複数のポリゴンの同時編集

## 参考資料

- [Atomic Design Guide](./AtomicDesignGuide.md)
- [State Management Guide](./StateManagementGuide.md)
- [Jetpack Compose Canvas Documentation](https://developer.android.com/jetpack/compose/graphics/draw/overview)
- [Material 3 ModalBottomSheet](https://m3.material.io/components/bottom-sheets/overview)
