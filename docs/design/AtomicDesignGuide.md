# Atomic Design Implementation Guide

このドキュメントは、Puzzroomプロジェクトにおける Atomic Design の実装について説明します。

## 概要

Atomic Design は、UI コンポーネントを5つの階層で構造化する設計手法です。小さなコンポーネント（Atoms）から大きなコンポーネント（Pages）へと段階的に組み立てていきます。

## Directory Structure

```
ui/
├── atoms/          # 最小のUIコンポーネント (5 components)
├── molecules/      # Atomsを組み合わせた小さな機能単位 (5 components)
├── organisms/      # MoleculesとAtomsを組み合わせた大きなセクション (9 components)
├── templates/      # ページのレイアウト構造 (2 templates)
├── pages/          # 実際のデータを含む完成したページ (2 pages)
├── theme/          # テーマ設定（色、タイポグラフィ）
└── component/      # レガシーコンポーネント（特殊機能向け）
```

## 各階層の説明

### 1. Atoms（原子）

それ以上分解できない最小のUI要素。アプリ全体で一貫したスタイルを持ちます。

**実装例:**
- `AppButton.kt` - ボタン（Primary、Outlined、Text）
- `AppText.kt` - テキスト
- `AppIcon.kt` - アイコン、アイコンボタン
- `AppSpacer.kt` - スペーサー（水平・垂直）
- `AppCard.kt` - カード

**使用方法:**
```kotlin
AppButton(
    text = "保存",
    onClick = { /* アクション */ }
)

AppText(
    text = "プロジェクト名",
    style = MaterialTheme.typography.titleMedium
)
```

### 2. Molecules（分子）

複数のAtomsを組み合わせて、特定の機能を持つ最小単位のコンポーネント。

**実装例:**
- `IconWithLabel.kt` - アイコンとラベルの組み合わせ
- `TitleWithSubtitle.kt` - タイトルとサブタイトル
- `ImageWithFallback.kt` - 画像とフォールバック表示
- `ConfirmationDialog.kt` - 確認ダイアログ
- `SaveStateIndicator.kt` - 保存状態インジケーター

**使用方法:**
```kotlin
TitleWithSubtitle(
    title = "プロジェクト名",
    subtitle = "3 floor plans"
)

ImageWithFallback(
    imageUrl = project.layoutUrl,
    fallbackText = project.name
)
```

### 3. Organisms（有機体）

MoleculesとAtomsを組み合わせて、画面の大きなセクションを形成します。

**実装例:**
- `ProjectCardItem.kt` - プロジェクトカード
- `EmptyState.kt` - 空状態の表示
- `ErrorDisplay.kt` - エラー表示
- `LoadingIndicator.kt` - ローディングインジケータ
- `ProjectList.kt` - プロジェクトリスト
- `SaveLoadDialogs.kt` - 保存/読込ダイアログ
- `PolygonListPanel.kt` - ポリゴンリストパネル
- `FurnitureLibraryPanel.kt` - 家具ライブラリパネル
- `FurniturePlacementToolbar.kt` - 家具配置ツールバー

**使用方法:**
```kotlin
ProjectCardItem(
    project = project,
    onClick = { onProjectClick(project.id) },
    onDelete = { onDelete(project.id) }
)

EmptyState(
    title = "プロジェクトがありません",
    message = "新しいプロジェクトを作成しましょう",
    actionText = "新規作成",
    onAction = onCreateNew
)
```

### 4. Templates（テンプレート）

ページのレイアウト構造を定義します。具体的なデータは含みません。

**実装例:**
- `ListScreenTemplate.kt` - リスト画面のテンプレート（AppBar付き）
- `EditorScreenTemplate.kt` - エディタ画面のテンプレート（Canvas + Sidebar）

**使用方法:**
```kotlin
ListScreenTemplate(
    title = "マイプロジェクト",
    actions = {
        AppIconButton(
            imageVector = Icons.Default.Add,
            contentDescription = "新規作成",
            onClick = onCreateNew
        )
    }
) { modifier ->
    // コンテンツをここに配置
}
```

### 5. Pages（ページ）

Templatesに実際のデータとロジックを組み込んだ、ユーザーに表示される完成形。ViewModelとやり取りします。

**実装例:**
- `ProjectListPage.kt` - プロジェクト一覧ページ

**使用方法:**
```kotlin
@Composable
fun ProjectListPage(
    viewModel: ProjectViewModel,
    onProjectClick: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ListScreenTemplate(
        title = "My Projects",
        actions = { /* アクションボタン */ }
    ) { modifier ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> LoadingIndicator(modifier)
            is ProjectUiState.ProjectList -> {
                if (state.projects.isEmpty()) {
                    EmptyState(...)
                } else {
                    ProjectList(...)
                }
            }
            is ProjectUiState.Error -> ErrorDisplay(...)
        }
    }
}
```

## テーマ

### カラースキーム

落ち着いた暖色系の配色を採用しています：

- **Primary**: テラコッタ/錆色（`#D4856A`）
- **Secondary**: ソフトピーチ/アプリコット色（`#E8B4A0`）
- **Tertiary**: ウォームベージュ/サンド色（`#E5D4C1`）
- **Background**: ウォームホワイト（`#FAF4F0`）
- **Surface**: ホワイト（`#FFFFFF`）
- **Error**: ミュートされたウォームレッド（`#D67568`）

### 使用方法

```kotlin
PuzzroomTheme {
    // アプリ全体のコンテンツ
}
```

## データフローとイベント処理

### State Down, Events Up

Atomic Designでは、以下のデータフローパターンを推奨します：

1. **State Down**: ViewModelがStateをPagesに提供し、PagesからTemplates → Organisms → Molecules → Atomsへと伝達
2. **Events Up**: ユーザー操作（クリックなど）はAtomsから上へ伝達され、最終的にViewModelで処理

```
ViewModel (State)
    ↓
  Pages (State + Event Handlers)
    ↓
Templates (Layout)
    ↓
Organisms (Composed Components + Events)
    ↓
Molecules (Small Components + Events)
    ↓
  Atoms (UI Elements + onClick)
```

### 例

```kotlin
// Page: イベントハンドラを定義
ProjectListPage(
    viewModel = viewModel,
    onProjectClick = { id -> /* navigation */ },
    onCreateNew = { /* create */ }
)

// Organism: イベントを受け取り、内部で処理
ProjectCardItem(
    project = project,
    onClick = { onProjectClick(project.id) },  // Pageからのハンドラ
    onDelete = { onDelete(project.id) }        // Pageからのハンドラ
)

// Atom: 基本的なonClickを受け取る
AppButton(
    text = "削除",
    onClick = onClick  // Organismからのハンドラ
)
```

## MVVMアーキテクチャとの統合

Atomic DesignはMVVMアーキテクチャと組み合わせて使用します：

| コンポーネント | 役割 | Atomic Designとの関係 |
|---|---|---|
| Model | データとビジネスロジック | UIの構造とは直接関係しない |
| ViewModel | UIの状態保持とロジック実行 | PagesにStateを提供、イベントを受け取る |
| View (Composable) | UI表示 | Atoms〜Pagesの全階層 |

## ベストプラクティス

### 1. 再利用性を意識する

Atoms、Moleculesは特定の画面に依存しない、汎用的なコンポーネントとして設計します。

### 2. 一貫性を保つ

すべてのAtomsは統一されたスタイル（色、サイズ、間隔など）を持つようにします。

### 3. 責任を明確にする

- **Atoms**: スタイルのみ、ロジックなし
- **Molecules**: 小さな機能単位
- **Organisms**: 画面の大きなセクション
- **Templates**: レイアウト構造のみ
- **Pages**: データとロジック

### 4. コンポーネント名を明確にする

階層ごとにファイルを分け、わかりやすい名前を付けます。

### 5. プロップスを最小限に

必要なプロップスのみを渡し、デフォルト値を活用します。

## 既存画面の移行方法

既存の画面をAtomic Designに移行する手順：

1. **画面を分析**: 画面を構成要素に分解
2. **Atomsを特定**: ボタン、テキストなど基本要素
3. **Moleculesを作成**: 小さな機能単位を抽出
4. **Organismsを作成**: 大きなセクションを抽出
5. **Templateを作成**: レイアウト構造を定義
6. **Pageを作成**: データとロジックを統合

### 例: RoomScreenの移行

```kotlin
// Before: 単一の大きなコンポーネント
@Composable
fun RoomScreen() {
    Column {
        // ヘッダー
        Row { /* ... */ }
        
        // メインコンテンツ
        Row {
            // キャンバス
            EditablePolygonCanvas(...)
            
            // サイドバー
            Column { /* ... */ }
        }
    }
}

// After: Atomic Design
@Composable
fun RoomPage() {
    EditorScreenTemplate(
        header = { RoomHeader(...) },  // Organism
        canvas = { RoomCanvas(...) },  // Organism
        sidebar = { RoomSidebar(...) }  // Organism
    )
}
```

## 参考資料

- [Atomic Design by Brad Frost](https://atomicdesign.bradfrost.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

---

このガイドラインに従うことで、保守性が高く、拡張しやすいUIアーキテクチャを実現できます。
