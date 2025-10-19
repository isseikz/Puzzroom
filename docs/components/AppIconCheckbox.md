# AppIconCheckbox

## 概要 (Overview)

`AppIconCheckbox` は、アイコンのスタイルで checked / unchecked 状態を表現する Atom コンポーネントです。既存の `AppIconButton` を拡張して作成されています。

## 特徴 (Features)

- **状態トグル**: クリック時に checked / unchecked を自動的にトグルします
- **視覚的区別**: 
  - **Unchecked 状態**: アイコンが薄い灰色 (`Color.LightGray`) で表示
  - **Checked 状態**: アイコンが強調色 (デフォルト: `MaterialTheme.colorScheme.primary` - 温かいテラコッタ色 #D4856A) で表示

## 使用方法 (Usage)

### 基本的な使用例

```kotlin
var isChecked by remember { mutableStateOf(false) }

AppIconCheckbox(
    imageVector = Icons.Default.Add,
    contentDescription = "Add item",
    checked = isChecked,
    onCheckedChange = { isChecked = it }
)
```

### カスタマイズ例

```kotlin
var isChecked by remember { mutableStateOf(true) }

AppIconCheckbox(
    imageVector = Icons.Default.Delete,
    contentDescription = "Delete",
    checked = isChecked,
    onCheckedChange = { isChecked = it },
    enabled = true,
    checkedTint = MaterialTheme.colorScheme.error,  // カスタム色
    uncheckedTint = Color.Gray                      // カスタム色
)
```

## API

### Parameters

| パラメータ | 型 | 必須 | デフォルト値 | 説明 |
|-----------|-----|------|------------|------|
| `imageVector` | `ImageVector` | ✓ | - | 表示するアイコン |
| `contentDescription` | `String?` | ✓ | - | アクセシビリティの説明 |
| `checked` | `Boolean` | ✓ | - | チェック状態 |
| `onCheckedChange` | `(Boolean) -> Unit` | ✓ | - | チェック状態が変更されたときのコールバック |
| `modifier` | `Modifier` | ✗ | `Modifier` | コンポーネントの Modifier |
| `enabled` | `Boolean` | ✗ | `true` | 有効/無効の状態 |
| `checkedTint` | `Color` | ✗ | `MaterialTheme.colorScheme.primary` | チェック時の色 |
| `uncheckedTint` | `Color` | ✗ | `Color.LightGray` | 未チェック時の色 |

## デザインシステムでの位置づけ

- **階層**: Atom (最小単位のコンポーネント)
- **カテゴリ**: Interactive Icon
- **ベース**: `AppIconButton` を拡張
- **テーマ**: Puzzroom の温かい色パレットに準拠

## 実装の詳細

`AppIconCheckbox` は、以下の方法で実装されています：

1. `AppIconButton` をベースとして使用
2. `onClick` ハンドラーで `onCheckedChange(!checked)` を呼び出し、状態を自動トグル
3. `checked` 状態に基づいて `tint` 色を動的に変更

```kotlin
AppIconButton(
    imageVector = imageVector,
    contentDescription = contentDescription,
    onClick = { onCheckedChange(!checked) },
    modifier = modifier,
    enabled = enabled,
    tint = if (checked) checkedTint else uncheckedTint
)
```

## DesignSystemShowcase での確認

`DesignSystemShowcase` ページで実際の動作を確認できます。3つのサンプル（Add, Edit, Delete アイコン）が表示され、クリックすることで状態をトグルできます。

## 関連コンポーネント

- `AppIcon`: 基本的なアイコン表示
- `AppIconButton`: クリック可能なアイコンボタン
- `AppButton`: 標準ボタン
