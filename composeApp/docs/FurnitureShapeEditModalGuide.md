# 図形編集モーダルボトムシート実装ガイド

## 概要

このドキュメントは、家具の図形編集のためのモーダルボトムシート実装について説明します。
Material Design 3のガイドラインとAtomic Designの原則に従って実装されています。

## コンポーネント構成

### Atoms（原子）
- `AppTextField` - テキスト入力フィールド
- `AppButton` / `AppOutlinedButton` - ボタン
- `AppIconButton` - アイコンボタン（閉じるボタンに使用）
- `AppText` - テキスト表示
- `AppCard` - カード

### Molecules（分子）

#### 1. DecimalDimensionInput
幅と高さの入力フィールド（小数第1位まで対応）

```kotlin
DecimalDimensionInput(
    widthValue = "100.0",
    heightValue = "50.0",
    onWidthChange = { /* 処理 */ },
    onHeightChange = { /* 処理 */ }
)
```

#### 2. ShapeSelector
図形テンプレートの選択

```kotlin
ShapeSelector(
    selectedShape = ShapeTemplate.RECTANGLE,
    onShapeSelected = { shape -> /* 処理 */ }
)
```

対応形状：
- `RECTANGLE` - 長方形
- `CIRCLE` - 円形
- `L_SHAPE` - L字型
- `CUSTOM` - カスタム

#### 3. TextureSelector
テクスチャの選択

```kotlin
TextureSelector(
    selectedTexture = TextureOption.WOOD_LIGHT,
    onTextureSelected = { texture -> /* 処理 */ }
)
```

対応テクスチャ：
- `NONE` - なし
- `WOOD_LIGHT` - 明るい木
- `WOOD_DARK` - 暗い木
- `METAL` - 金属
- `FABRIC` - 布
- `GLASS` - ガラス

### Organisms（生物）

#### FurnitureShapeEditForm
図形編集の完全なフォーム

```kotlin
FurnitureShapeEditForm(
    initialData = FurnitureShapeFormData(
        name = "テーブル",
        width = "120.0",
        height = "80.0",
        shape = ShapeTemplate.RECTANGLE,
        texture = TextureOption.WOOD_LIGHT
    ),
    onDismiss = { /* キャンセル処理 */ },
    onSave = { formData -> 
        // 保存処理
        println("保存: ${formData.name}")
    }
)
```

## ModalBottomSheetの使用方法

### 基本実装

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourScreen() {
    // 1. ボトムシートの表示状態を管理
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // 2. シートの状態を管理
    val sheetState = rememberModalBottomSheetState()
    
    // 3. メインコンテンツ
    Box(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { showBottomSheet = true }) {
            Text("編集")
        }
    }
    
    // 4. ModalBottomSheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // スワイプダウンまたはスクリムタップで閉じる
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            FurnitureShapeEditForm(
                initialData = /* データ */,
                onDismiss = { showBottomSheet = false },
                onSave = { formData ->
                    // 保存処理
                    showBottomSheet = false
                }
            )
        }
    }
}
```

## UXガイドラインの遵守

### 必須要件

1. **閉じるボタン** ✓
   - フォームの右上に「×」ボタンを配置
   - `AppIconButton` with `Icons.Default.Close`

2. **戻る操作のサポート** ✓
   - `onDismissRequest` コールバックで実装
   - デバイスの戻るボタンで閉じられる

3. **ネストの禁止** ✓
   - 複数のボトムシートを重ねない
   - 1つのボトムシートのみを表示

### 状態管理パターン

```kotlin
// プログラムから閉じる場合
scope.launch {
    sheetState.hide()  // アニメーション
}.invokeOnCompletion {
    if (!sheetState.isVisible) {
        showBottomSheet = false  // コンポジションから削除
    }
}

// ジェスチャーで閉じる場合
ModalBottomSheet(
    onDismissRequest = {
        showBottomSheet = false  // 直接更新
    }
)
```

## バリデーション

フォームは以下の検証を行います：

1. **名前**: 空白でないこと
2. **幅**: 0より大きい数値
3. **高さ**: 0より大きい数値

すべての条件を満たすまで保存ボタンは無効化されます。

## カラーテーマ

ウォームカラーテーマを使用：
- Primary: Warm terracotta (#D4856A)
- Secondary: Soft peach (#E8B4A0)
- Tertiary: Warm beige (#E5D4C1)
- Background: Warm white (#FAF4F0)

選択された項目は `primaryContainer` 色で表示されます。

## 例

完全な実装例は `FurnitureShapeEditExample.kt` を参照してください。

```kotlin
// シンプルな使用例
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleFurnitureEdit() {
    var showSheet by remember { mutableStateOf(false) }
    
    AppButton(
        text = "図形を編集",
        onClick = { showSheet = true }
    )
    
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            FurnitureShapeEditForm(
                onDismiss = { showSheet = false },
                onSave = { data ->
                    println("保存: $data")
                    showSheet = false
                }
            )
        }
    }
}
```

## トラブルシューティング

### ボトムシートが表示されない
- `showBottomSheet` が `true` になっているか確認
- `@OptIn(ExperimentalMaterial3Api::class)` を追加

### 閉じる動作が機能しない
- `onDismissRequest` で状態を更新しているか確認
- `onDismiss` コールバックで状態を更新しているか確認

### バリデーションエラー
- 数値フィールドに有効な数値が入力されているか確認
- 名前フィールドが空でないか確認
