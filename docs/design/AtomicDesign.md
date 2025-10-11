# Atomic Design 構造

このプロジェクトは Atomic Design の原則に従ってUIを構築しています。

## ディレクトリ構造

```
composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/
├── atoms/           # 原子 - 最小のUI要素
├── molecules/       # 分子 - Atomsの組み合わせ
├── organisms/       # 有機体 - MoleculesとAtomsの組み合わせ
├── templates/       # テンプレート - レイアウト構造
├── pages/           # ページ - ViewModelとの接続
├── component/       # 既存コンポーネント（互換性のため）
└── screen/          # 既存画面（段階的に移行）
```

## 階層の説明

### Atoms (原子)
最小のUI要素。単独で意味を持ち、それ以上分解できません。

**例:**
- `AppButton.kt` - アプリ全体で統一されたボタン
- `AppText.kt` - テキスト表示
- `AppCard.kt` - カード
- `AppIcon.kt` - アイコン
- `AppTheme.kt` - カラーテーマ（落ち着いた暖色系）

**特徴:**
- プロパティは最小限
- スタイルは統一
- 再利用性が最も高い

### Molecules (分子)
複数のAtomsを組み合わせて、特定の機能を持たせます。

**例:**
- `SaveStateIndicator.kt` - 保存状態表示
- `ProjectCardItem.kt` - プロジェクトカード
- `EmptyStateMessage.kt` - 空状態メッセージ
- `ErrorMessage.kt` - エラー表示

**特徴:**
- 単一の機能を持つ
- 複数のAtomsで構成
- イベントハンドラを受け取る

### Organisms (有機体)
MoleculesやAtomsを組み合わせて、画面の大きなセクションを形成します。

**例:**
- `ProjectList.kt` - プロジェクト一覧

**特徴:**
- 複雑なUI構造
- ビジネスロジックに近い
- データのコレクションを扱う

### Templates (テンプレート)
ページのレイアウト構造。コンテンツの具体的なデータは含みません。

**例:**
- `ProjectListTemplate.kt` - プロジェクト一覧のレイアウト

**特徴:**
- レイアウトの骨組み
- コンテンツをComposable関数として受け取る
- ScaffoldやNavigation構造を含む

### Pages (ページ)
Templatesに実際のデータとロジックを流し込みます。ViewModelとやり取りするのはこの層のみです。

**例:**
- `ProjectListPage.kt` - プロジェクト一覧ページ

**特徴:**
- ViewModelから状態を取得
- イベントをViewModelへ送る
- State Down, Events Upのパターン

## デザイン原則

### カラーテーマ
落ち着いた暖色系の色を使用:
- **Primary**: 暖かいオレンジ系 (#E07A5F)
- **Secondary**: 落ち着いたベージュ系 (#F4D3B5)
- **Tertiary**: アクセント用の暖色 (#D4A574)
- **Background**: 暖色系の背景 (#FFFBF7)

### データフロー
```
ViewModel → Page → Template → Organism → Molecule → Atom
    ↑                                                    |
    └─────────────── Events ─────────────────────────────┘
```

- **State Down**: データは上から下へ流れる
- **Events Up**: イベントは下から上へ伝わる

## 実装例

### 新しいコンポーネントを追加する

1. **Atom**を作る場合:
```kotlin
@Composable
fun AppNewAtom(
    text: String,
    modifier: Modifier = Modifier
) {
    // シンプルな実装
}
```

2. **Molecule**を作る場合:
```kotlin
@Composable
fun NewMolecule(
    data: Data,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // AtomsとMoleculesを組み合わせ
    AppCard {
        AppButton(text = data.title, onClick = onClick)
    }
}
```

3. **Page**を作る場合:
```kotlin
@Composable
fun NewPage(
    viewModel: ViewModel
) {
    val state by viewModel.state.collectAsState()
    
    NewTemplate { padding ->
        // Organismsを配置
    }
}
```

## 移行戦略

既存のコンポーネントは段階的に移行します:

1. ✅ 新しいAtomic Design構造を作成
2. ✅ 新しいコンポーネントから使用開始
3. 🔄 既存コンポーネントを互換性レイヤーでラップ
4. 📝 段階的に既存画面をリファクタリング

## 参考資料

- [Atomic Design Methodology](https://atomicdesign.bradfrost.com/)
- [Jetpack Compose Architecture Guide](https://developer.android.com/jetpack/compose/architecture)
