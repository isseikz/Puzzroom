# UI Layer - Atomic Design Architecture

このディレクトリには、Atomic Design原則に基づいて構成されたUIコンポーネントが含まれています。

## Directory Structure

```
ui/
├── atoms/              # 原子 - 最小単位のUI要素
│   ├── AppButton.kt        # ボタンコンポーネント
│   ├── AppCard.kt          # カードコンポーネント
│   ├── AppIcon.kt          # アイコンコンポーネント
│   ├── AppText.kt          # テキストコンポーネント
│   └── AppTheme.kt         # カラーテーマ定義
│
├── molecules/          # 分子 - Atomsの組み合わせ
│   ├── ActionButtonGroup.kt     # ボタングループ
│   ├── EmptyStateMessage.kt     # 空状態表示
│   ├── ErrorMessage.kt          # エラー表示
│   ├── ProjectCardItem.kt       # プロジェクトカード
│   └── SaveStateIndicator.kt    # 保存状態表示
│
├── organisms/          # 有機体 - 大きなUIセクション
│   ├── EditorToolbar.kt         # エディタツールバー
│   └── ProjectList.kt           # プロジェクト一覧
│
├── templates/          # テンプレート - ページレイアウト
│   ├── ProjectListTemplate.kt   # プロジェクト一覧レイアウト
│   └── RoomEditorTemplate.kt    # 部屋編集レイアウト
│
├── pages/              # ページ - ViewModelと接続
│   └── ProjectListPage.kt       # プロジェクト一覧ページ
│
├── component/          # 既存コンポーネント（段階的に移行中）
├── screen/             # 既存画面（段階的に移行中）
├── state/              # UI状態定義
└── viewmodel/          # ViewModel
```

## Atomic Design Hierarchy

### 1. Atoms (原子) - `ui/atoms/`
最小単位で、それ以上分解できないUI要素。

**特徴:**
- 単独で意味を持つ
- 再利用性が最も高い
- アプリ全体で統一されたスタイル

**例:**
- `AppButton` - ボタン
- `AppText` - テキスト
- `AppCard` - カード
- `AppIcon` - アイコン

### 2. Molecules (分子) - `ui/molecules/`
複数のAtomsを組み合わせて、特定の機能を持たせたコンポーネント。

**特徴:**
- 単一の機能を持つ
- Atomsで構成される
- イベントハンドラを受け取る

**例:**
- `SaveStateIndicator` - 保存状態の表示
- `ProjectCardItem` - プロジェクトカード（画像、テキスト、削除ボタン）
- `EmptyStateMessage` - 空状態のメッセージ

### 3. Organisms (有機体) - `ui/organisms/`
MoleculesやAtomsを組み合わせて、画面の大きなセクションを形成。

**特徴:**
- 複雑なUI構造
- データのコレクションを扱う
- ビジネスロジックに近い

**例:**
- `ProjectList` - プロジェクト一覧の表示
- `EditorToolbar` - エディタのツールバー

### 4. Templates (テンプレート) - `ui/templates/`
ページのレイアウト構造。具体的なデータは含まない。

**特徴:**
- レイアウトの骨組み
- Scaffoldなどの構造を含む
- コンテンツをComposable関数として受け取る

**例:**
- `ProjectListTemplate` - プロジェクト一覧のレイアウト
- `RoomEditorTemplate` - 部屋編集のレイアウト

### 5. Pages (ページ) - `ui/pages/`
Templatesに実際のデータとロジックを流し込む。ViewModelとやり取り。

**特徴:**
- ViewModelから状態を取得
- イベントをViewModelへ送る
- State Down, Events Upのパターン

**例:**
- `ProjectListPage` - プロジェクト一覧ページ

## Data Flow

```
┌─────────────────────────────────────────────────┐
│                  ViewModel                       │
│  (State management, Business logic)              │
└─────────────────┬───────────────────────────────┘
                  │ State ↓
┌─────────────────▼───────────────────────────────┐
│                   Page                           │
│  (Connects ViewModel to UI)                      │
└─────────────────┬───────────────────────────────┘
                  │ Props ↓
┌─────────────────▼───────────────────────────────┐
│                Template                          │
│  (Layout structure)                              │
└─────────────────┬───────────────────────────────┘
                  │ Content ↓
┌─────────────────▼───────────────────────────────┐
│                Organism                          │
│  (Complex UI sections)                           │
└─────────────────┬───────────────────────────────┘
                  │ Components ↓
┌─────────────────▼───────────────────────────────┐
│                Molecule                          │
│  (Functional groups)                             │
└─────────────────┬───────────────────────────────┘
                  │ Basic elements ↓
┌─────────────────▼───────────────────────────────┐
│                  Atom                            │
│  (Basic UI elements)                             │
└──────────────────────────────────────────────────┘

Events flow up: Atom → Molecule → Organism → Template → Page → ViewModel
State flows down: ViewModel → Page → Template → Organism → Molecule → Atom
```

## Color Theme

アプリ全体で統一された暖色系のカラーテーマ:

- **Primary**: 暖かいオレンジ系 (#E07A5F)
- **Secondary**: 落ち着いたベージュ系 (#F4D3B5)
- **Tertiary**: アクセント用の暖色 (#D4A574)
- **Background**: 暖色系の背景 (#FFFBF7)

詳細は `docs/design/ColorPalette.md` を参照。

## Usage Guidelines

### 新しいコンポーネントを追加する場合

1. **適切な層を選択**
   - 最小単位？ → `atoms/`
   - 複数のAtomsの組み合わせ？ → `molecules/`
   - 複雑なセクション？ → `organisms/`
   - レイアウト？ → `templates/`
   - ViewModel接続？ → `pages/`

2. **命名規則**
   - Atoms: `App[Element]` (例: AppButton, AppText)
   - Molecules: `[Feature][Component]` (例: SaveStateIndicator, ProjectCardItem)
   - Organisms: `[Feature][Section]` (例: ProjectList, EditorToolbar)
   - Templates: `[Screen]Template` (例: ProjectListTemplate)
   - Pages: `[Screen]Page` (例: ProjectListPage)

3. **Props設計**
   - データは上から下へ
   - イベントハンドラは上へ伝える
   - ModifierをサポートするよりStateオブジェクトを優先

### 既存コンポーネントの移行

既存の `component/` と `screen/` のコンポーネントは段階的に移行中:

1. 新しいAtomic Designコンポーネントを作成
2. 既存コンポーネントを互換性レイヤーでラップ
3. 段階的にリファクタリング

## Documentation

詳細なドキュメントは `docs/design/` にあります:

- `AtomicDesign.md` - Atomic Design構造の説明
- `AtomicDesignImplementation.md` - 実装の詳細
- `ComponentCatalog.md` - コンポーネントカタログと使用例
- `ColorPalette.md` - カラーパレットの詳細

## Examples

### Atom使用例

```kotlin
AppButton(
    text = "保存",
    onClick = { /* action */ }
)
```

### Molecule使用例

```kotlin
SaveStateIndicator(
    saveState = SaveState.Saved,
    onRetry = { /* retry */ }
)
```

### Organism使用例

```kotlin
ProjectList(
    projects = listOf(project1, project2),
    onProjectClick = { id -> /* open */ },
    onProjectDelete = { id -> /* delete */ }
)
```

### Template使用例

```kotlin
ProjectListTemplate(
    onCreateNew = { /* create */ }
) { paddingValues ->
    // Content with paddingValues
}
```

### Page使用例

```kotlin
ProjectListPage(
    viewModel = viewModel,
    onProjectClick = { id -> /* navigate */ },
    onCreateNew = { /* navigate */ }
)
```

## Best Practices

1. **Single Responsibility**: 各コンポーネントは単一の責務を持つ
2. **Composition over Inheritance**: 継承より合成を優先
3. **Immutable Props**: propsは不変にする
4. **State Hoisting**: 状態は適切な層に配置
5. **Testability**: テスト可能な設計
6. **Accessibility**: アクセシビリティを考慮
7. **Performance**: remember、LaunchedEffectを適切に使用

## Contributing

新しいコンポーネントを追加する際は:

1. 適切な階層に配置
2. ドキュメントコメントを追加
3. ComponentCatalogに使用例を追加
4. 既存のスタイルガイドに従う
5. アクセシビリティを確保

## Migration Status

- ✅ Atomic Design構造の作成
- ✅ カラーテーマの定義
- ✅ 基本Atomsの実装
- ✅ ProjectListScreenの移行
- ✅ RoomScreen、FurnitureScreenの部分的な更新
- 🔄 その他のコンポーネントの段階的移行（進行中）

---

For more information, see the documentation in `docs/design/`.
