# 角度調整戦略 - Angle Adjustment Strategy

## Problem Statement

ポリゴンの頂点における角度を調整する際、以下の要件を満たす必要がある：

1. **辺の長さ固定 + 角度調整** → 新しい形状に更新
2. **角度固定 + 辺の長さ調整** → 新しい形状に更新
3. **幾何学的に解なしの場合** → UXとして破綻しない処理

### Geometric Constraints

ポリゴンは以下の制約を満たす必要がある：
- ✅ 閉じている（最初と最後の頂点が一致）
- ✅ すべての辺と角度が定義されている
- ✅ 自己交差しない（オプション）

**問題:** 一部の辺の長さや角度を変更すると、ポリゴンが閉じなくなる可能性がある。

---

## Approach Options

### Option 1: Local Adjustment（ローカル調整）⭐ 推奨

**概要:**
選択した頂点とその隣接する2辺のみを調整し、他の部分は固定。

**メリット:**
- ✅ 予測可能な動作
- ✅ シンプルな実装
- ✅ ユーザーが調整箇所を理解しやすい

**デメリット:**
- ❌ ポリゴンが開く可能性がある
- ❌ 手動で他の頂点も調整して閉じる必要がある

**実装:**
```kotlin
fun adjustAngleLocal(
    polygon: Polygon,
    vertexIndex: Int,
    newAngle: Double  // degrees
): Polygon {
    // 選択した頂点の前後の辺を取得
    val prevVertex = polygon.points[vertexIndex - 1]
    val currentVertex = polygon.points[vertexIndex]
    val nextVertex = polygon.points[vertexIndex + 1]

    // 前の辺の長さと方向を保持
    val prevEdgeLength = calculateDistance(prevVertex, currentVertex)
    val prevEdgeAngle = calculateAngle(prevVertex, currentVertex)

    // 新しい角度で次の頂点を計算
    val newNextAngle = prevEdgeAngle + newAngle
    val nextEdgeLength = calculateDistance(currentVertex, nextVertex)

    val newNextVertex = Point(
        Centimeter((currentVertex.x.value + nextEdgeLength * cos(newNextAngle)).roundToInt()),
        Centimeter((currentVertex.y.value + nextEdgeLength * sin(newNextAngle)).roundToInt())
    )

    // ポリゴンを更新
    return polygon.copy(
        points = polygon.points.mapIndexed { index, point ->
            if (index == vertexIndex + 1) newNextVertex else point
        }
    )
}
```

**UX:**
- 調整後、ポリゴンが開いている部分を視覚的にハイライト
- ユーザーに「他の頂点も調整してポリゴンを閉じてください」と促す

---

### Option 2: Chain Adjustment（連鎖調整）

**概要:**
選択した頂点から順に、連鎖的に調整していく。

**ステップ:**
1. 選択した頂点の角度を変更
2. 次の頂点の位置を再計算
3. さらに次の頂点も再計算...
4. 最後の頂点まで到達したら、最初の頂点に戻れるかチェック

**メリット:**
- ✅ ポリゴンが閉じる可能性が高い（特定の条件下）

**デメリット:**
- ❌ 複雑な計算
- ❌ 予測不可能な動作（ユーザーが意図しない形状になる）
- ❌ 解なしの場合が多い

**実装は複雑なため、推奨しない。**

---

### Option 3: Constrained Adjustment（制約付き調整）⭐⭐ 最も推奨

**概要:**
幾何学的に可能な範囲でのみ調整を許可し、不可能な値は拒否。

**戦略:**

#### 3-1. 固定点アプローチ

**ルール:**
- 選択した頂点の**前の頂点を固定点**とする
- 選択した頂点と次の頂点の位置を再計算
- **残りの頂点は固定**

**制約チェック:**
- 新しい形状が最後の頂点に到達できるか？
- 到達不可能な場合はエラー

```kotlin
/**
 * 角度調整（制約付き）
 *
 * @param polygon 元のポリゴン
 * @param vertexIndex 調整する頂点のインデックス
 * @param newAngle 新しい内角（度数法）
 * @return 調整結果（成功 or 失敗）
 */
fun adjustAngleConstrained(
    polygon: Polygon,
    vertexIndex: Int,
    newAngle: Double
): Result<Polygon> {
    // 範囲チェック
    if (newAngle < 0 || newAngle > 360) {
        return Result.failure(InvalidAngleException("Angle must be between 0 and 360 degrees"))
    }

    // 固定点: vertexIndex-1
    val fixedVertex = polygon.points[vertexIndex - 1]
    val currentVertex = polygon.points[vertexIndex]
    val nextVertex = polygon.points[vertexIndex + 1]

    // 前の辺の方向
    val prevEdgeAngle = atan2(
        (currentVertex.y.value - fixedVertex.y.value).toDouble(),
        (currentVertex.x.value - fixedVertex.x.value).toDouble()
    )

    // 新しい次の辺の方向
    val newNextEdgeAngle = prevEdgeAngle + (180 - newAngle).toRadians()

    // 次の頂点の新しい位置
    val nextEdgeLength = calculateDistance(currentVertex, nextVertex)
    val newNextVertex = Point(
        Centimeter((currentVertex.x.value + nextEdgeLength * cos(newNextEdgeAngle)).roundToInt()),
        Centimeter((currentVertex.y.value + nextEdgeLength * sin(newNextEdgeAngle)).roundToInt())
    )

    // 新しいポリゴンを作成
    val newPoints = polygon.points.mapIndexed { index, point ->
        when (index) {
            vertexIndex + 1 -> newNextVertex
            else -> point
        }
    }

    // 閉じているかチェック（オプション: 許容範囲内か）
    val closureGap = calculateDistance(newPoints.last(), newPoints.first())
    if (closureGap > 1.0) { // 1cm以上の隙間があればエラー
        return Result.failure(PolygonNotClosedException("Polygon cannot be closed with this angle"))
    }

    return Result.success(Polygon(points = newPoints))
}
```

#### 3-2. 両端固定アプローチ（より高度）

**ルール:**
- 選択した頂点の前後の頂点を固定
- 選択した頂点のみ移動
- 2つの辺の長さを保持しながら角度を変更

```kotlin
fun adjustAngleBothEndsFixed(
    polygon: Polygon,
    vertexIndex: Int,
    newAngle: Double
): Result<Polygon> {
    val prevVertex = polygon.points[vertexIndex - 1]
    val currentVertex = polygon.points[vertexIndex]
    val nextVertex = polygon.points[vertexIndex + 1]

    // 両辺の長さを保持
    val prevEdgeLength = calculateDistance(prevVertex, currentVertex)
    val nextEdgeLength = calculateDistance(currentVertex, nextVertex)

    // 前の辺の方向
    val prevEdgeAngle = atan2(
        (currentVertex.y.value - prevVertex.y.value).toDouble(),
        (currentVertex.x.value - prevVertex.x.value).toDouble()
    )

    // 新しい次の辺の方向
    val newNextEdgeAngle = prevEdgeAngle + (180 - newAngle).toRadians()

    // 新しい currentVertex の位置を計算
    // (2つの円の交点を求める問題)

    // 解が2つある場合: ユーザーに選ばせる or 現在の位置に近い方を選ぶ

    // このアプローチは複雑なので、Phase 2以降で実装
}
```

---

### Option 4: Flexible Mode（フレキシブルモード）⭐⭐⭐ 最終的な推奨

**概要:**
複数のモードを提供し、ユーザーが状況に応じて選択できるようにする。

#### Mode A: Open Polygon Mode（開いたポリゴンモード）

**特徴:**
- ポリゴンが開いてもOK
- ユーザーが手動で閉じる
- 自由度が高い

**UI:**
- 開いている部分を赤い点線で表示
- 「ポリゴンが開いています」と警告表示
- 「自動で閉じる」ボタンを提供（最後の辺を自動調整）

#### Mode B: Auto-Close Mode（自動クローズモード）

**特徴:**
- 常にポリゴンを閉じた状態に保つ
- 最後の辺を自動調整

**実装:**
```kotlin
fun adjustAngleWithAutoClose(
    polygon: Polygon,
    vertexIndex: Int,
    newAngle: Double
): Polygon {
    // ローカル調整を実行
    var adjustedPolygon = adjustAngleLocal(polygon, vertexIndex, newAngle)

    // 最後の辺を自動調整してポリゴンを閉じる
    val lastVertex = adjustedPolygon.points[adjustedPolygon.points.size - 2]
    val firstVertex = adjustedPolygon.points[0]

    adjustedPolygon = adjustedPolygon.copy(
        points = adjustedPolygon.points.dropLast(1) + firstVertex
    )

    return adjustedPolygon
}
```

#### Mode C: Constraint Solver Mode（制約ソルバーモード）

**特徴:**
- すべての制約（辺の長さ、角度）を満たすように最適化
- 数値最適化アルゴリズムを使用

**実装は高度なため、Phase 3以降で検討。**

---

## Recommended Implementation Plan

### Phase 1: Basic Angle Input（基本的な角度入力）

**実装:**
- Option 3-1: 固定点アプローチ
- ローカル調整のみ
- エラー処理あり

**UI:**
```kotlin
@Composable
fun AngleInputPanel(
    polygon: Polygon,
    selectedVertexIndex: Int?,
    onAngleChange: (vertexIndex: Int, newAngle: Double) -> Result<Unit>
) {
    selectedVertexIndex?.let { index ->
        val currentAngle = calculateInteriorAngle(polygon, index)

        OutlinedTextField(
            value = currentAngle.toString(),
            onValueChange = { newValue ->
                newValue.toDoubleOrNull()?.let { angle ->
                    val result = onAngleChange(index, angle)
                    if (result.isFailure) {
                        // エラー表示
                        showError(result.exceptionOrNull()?.message)
                    }
                }
            },
            label = { Text("内角 (度)") }
        )
    }
}
```

### Phase 2: Open Polygon Support（開いたポリゴンのサポート）

**追加機能:**
- 開いたポリゴンの視覚化（点線で隙間を表示）
- 「自動で閉じる」ボタン
- 警告メッセージ

### Phase 3: Advanced Constraint Solving（高度な制約解決）

**追加機能:**
- 複数の辺と角度を同時に調整
- 最適化アルゴリズムで解を探索
- リアルタイムプレビュー

---

## UX Design

### 1. リアルタイムフィードバック

```
┌──────────────────────────────────────┐
│ 頂点 2 の内角                         │
│ ┌────────────┐                       │
│ │ 90.0  °    │ [±]                  │
│ └────────────┘                       │
│                                      │
│ 現在: 90.0°                          │
│ 範囲: 0° - 360°                      │
│                                      │
│ ⚠️ 警告: この角度では              │
│    ポリゴンが開きます               │
│    許容角度: 85° - 95°              │
└──────────────────────────────────────┘
```

### 2. 視覚的なプレビュー

- **入力中**: 薄い色で新しい形状をプレビュー表示
- **確定後**: 実際の形状に更新
- **エラー時**: 赤い点線で警告表示

### 3. エラーメッセージ

| エラー | メッセージ | 推奨アクション |
|--------|-----------|---------------|
| 角度範囲外 | "角度は0°〜360°の範囲で入力してください" | 値を修正 |
| ポリゴンが開く | "この角度ではポリゴンが閉じません" | 角度を調整 or 他の頂点も調整 |
| 自己交差 | "ポリゴンが自己交差しています" | 角度を調整 |

---

## Implementation Example

### PolygonGeometry.kt に追加

```kotlin
/**
 * 頂点の内角を計算
 */
fun calculateInteriorAngle(polygon: Polygon, vertexIndex: Int): Double {
    val n = polygon.points.size
    val prevVertex = polygon.points[(vertexIndex - 1 + n) % n]
    val currentVertex = polygon.points[vertexIndex]
    val nextVertex = polygon.points[(vertexIndex + 1) % n]

    // 前の辺のベクトル
    val v1x = currentVertex.x.value - prevVertex.x.value
    val v1y = currentVertex.y.value - prevVertex.y.value

    // 次の辺のベクトル
    val v2x = nextVertex.x.value - currentVertex.x.value
    val v2y = nextVertex.y.value - currentVertex.y.value

    // 内積とクロス積から角度を計算
    val dot = v1x * v2x + v1y * v2y
    val cross = v1x * v2y - v1y * v2x

    val angle = atan2(cross.toDouble(), dot.toDouble())
    return Math.toDegrees(angle)
}

/**
 * 角度を調整（ローカル調整）
 */
fun adjustAngleLocal(
    polygon: Polygon,
    vertexIndex: Int,
    newAngleDegrees: Double
): Result<Polygon> {
    // 範囲チェック
    if (newAngleDegrees < 0 || newAngleDegrees > 360) {
        return Result.failure(IllegalArgumentException("角度は0°〜360°の範囲で入力してください"))
    }

    val n = polygon.points.size
    val prevVertex = polygon.points[(vertexIndex - 1 + n) % n]
    val currentVertex = polygon.points[vertexIndex]
    val nextVertex = polygon.points[(vertexIndex + 1) % n]

    // 前の辺の方向（ラジアン）
    val prevAngle = atan2(
        (currentVertex.y.value - prevVertex.y.value).toDouble(),
        (currentVertex.x.value - prevVertex.x.value).toDouble()
    )

    // 新しい次の辺の方向
    val newAngleRadians = Math.toRadians(newAngleDegrees)
    val newNextAngle = prevAngle + (PI - newAngleRadians)

    // 次の頂点の新しい位置
    val nextEdgeLength = calculateDistance(currentVertex, nextVertex)
    val newNextX = currentVertex.x.value + (nextEdgeLength * cos(newNextAngle)).roundToInt()
    val newNextY = currentVertex.y.value + (nextEdgeLength * sin(newNextAngle)).roundToInt()

    // ポリゴンを更新
    val newPoints = polygon.points.mapIndexed { index, point ->
        if (index == (vertexIndex + 1) % n) {
            Point(Centimeter(newNextX), Centimeter(newNextY))
        } else {
            point
        }
    }

    return Result.success(Polygon(points = newPoints))
}

/**
 * ポリゴンが閉じているかチェック
 */
fun isPolygonClosed(polygon: Polygon, tolerance: Double = 1.0): Boolean {
    if (polygon.points.size < 3) return false

    val firstPoint = polygon.points.first()
    val lastPoint = polygon.points.last()

    return calculateDistance(firstPoint, lastPoint) <= tolerance
}

/**
 * ポリゴンを自動で閉じる（最後の頂点を最初の頂点に合わせる）
 */
fun autoClosePolygon(polygon: Polygon): Polygon {
    if (polygon.points.size < 3) return polygon

    return polygon.copy(
        points = polygon.points.dropLast(1) + polygon.points.first()
    )
}
```

---

## Summary & Recommendation

### 推奨アプローチ: **Option 4 - Flexible Mode**

**Phase 1:**
1. **ローカル調整** - 選択した頂点とその隣接辺のみ調整
2. **開いたポリゴンを許可** - ユーザーが手動で閉じる
3. **視覚的フィードバック** - 開いている部分を赤い点線で表示

**Phase 2:**
1. **自動クローズ機能** - ボタン1つでポリゴンを閉じる
2. **制約チェック** - 不可能な角度を入力時に警告
3. **プレビュー表示** - 調整前に結果を確認

**Phase 3:**
1. **制約ソルバー** - 複数の制約を同時に満たす最適解を探索
2. **両端固定調整** - より高度な幾何計算

### メリット
- ✅ **段階的実装** - Phase 1から順に実装可能
- ✅ **柔軟性** - ユーザーが状況に応じてモードを選択
- ✅ **予測可能** - ローカル調整で動作が明確
- ✅ **拡張性** - Phase 2, 3で機能追加可能

### ユーザー体験
1. **初心者ユーザー** - ローカル調整で直感的に操作
2. **上級ユーザー** - 制約ソルバーで複雑な形状を作成
3. **エラー時** - 明確なフィードバックで修正方法を提示

この設計により、幾何学的な制約を尊重しながら、ユーザーフレンドリーな操作を実現できます！
