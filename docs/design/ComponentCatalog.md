# コンポーネントカタログ (Component Catalog)

Atomic Design階層ごとのコンポーネント一覧と使用例

## Atoms (原子)

### AppButton
プライマリボタン

```kotlin
AppButton(
    text = "保存",
    onClick = { /* action */ },
    enabled = true
)
```

### AppSecondaryButton
セカンダリボタン（アウトライン）

```kotlin
AppSecondaryButton(
    text = "キャンセル",
    onClick = { /* action */ }
)
```

### AppTextButton
テキストボタン

```kotlin
AppTextButton(
    text = "詳細",
    onClick = { /* action */ }
)
```

### AppText
標準テキスト

```kotlin
AppText(
    text = "これは本文です",
    style = MaterialTheme.typography.bodyMedium
)
```

### AppTitleText
タイトルテキスト

```kotlin
AppTitleText(text = "ページタイトル")
```

### AppSubtitleText
サブタイトルテキスト

```kotlin
AppSubtitleText(text = "セクション名")
```

### AppCaptionText
キャプションテキスト

```kotlin
AppCaptionText(text = "補足情報")
```

### AppCard
カード

```kotlin
// クリック可能なカード
AppCard(
    onClick = { /* action */ }
) {
    // content
}

// 静的なカード
AppCard {
    // content
}
```

### AppIcon
アイコン

```kotlin
AppIcon(
    imageVector = Icons.Default.Check,
    contentDescription = "完了",
    tint = MaterialTheme.colorScheme.primary
)
```

### AppIconButton
アイコンボタン

```kotlin
AppIconButton(
    imageVector = Icons.Default.Delete,
    contentDescription = "削除",
    onClick = { /* action */ },
    tint = MaterialTheme.colorScheme.error
)
```

## Molecules (分子)

### SaveStateIndicator
保存状態インジケーター

```kotlin
SaveStateIndicator(
    saveState = SaveState.Saved, // or SaveState.Saving, SaveState.Failed
    onRetry = { /* retry action */ }
)
```

### ProjectCardItem
プロジェクトカード

```kotlin
ProjectCardItem(
    project = project,
    onClick = { /* open project */ },
    onDelete = { /* delete project */ }
)
```

### EmptyStateMessage
空状態メッセージ（汎用）

```kotlin
EmptyStateMessage(
    title = "データがありません",
    description = "新しいアイテムを作成しましょう",
    actionText = "作成",
    onAction = { /* create action */ }
)
```

### EmptyProjectListMessage
プロジェクト一覧用の空状態メッセージ

```kotlin
EmptyProjectListMessage(
    onCreateNew = { /* create project */ }
)
```

### ErrorMessage
エラーメッセージ

```kotlin
ErrorMessage(
    error = UiError(
        message = "エラーが発生しました",
        severity = UiError.Severity.Error
    ),
    onRetry = { /* retry action */ }
)
```

### ActionButtonGroup
アクションボタングループ

```kotlin
ActionButtonGroup(
    buttons = listOf(
        ActionButton(
            text = "保存",
            onClick = { /* save */ }
        ),
        ActionButton(
            text = "キャンセル",
            onClick = { /* cancel */ },
            isSecondary = true
        )
    )
)
```

## Organisms (有機体)

### ProjectList
プロジェクト一覧

```kotlin
ProjectList(
    projects = listOf(project1, project2),
    onProjectClick = { projectId -> /* open */ },
    onProjectDelete = { projectId -> /* delete */ }
)
```

### EditorToolbar
エディタツールバー

```kotlin
EditorToolbar(
    saveState = SaveState.Saved,
    onSaveRetry = { /* retry */ },
    actions = listOf(
        ActionButton("保存", onClick = { /* save */ }),
        ActionButton("読み込み", onClick = { /* load */ }, isSecondary = true)
    ),
    extraContent = {
        // PhotoPickerButtonなどの追加コンテンツ
    }
)
```

## Templates (テンプレート)

### ProjectListTemplate
プロジェクト一覧テンプレート

```kotlin
ProjectListTemplate(
    onCreateNew = { /* create */ }
) { paddingValues ->
    // コンテンツ
    // paddingValuesを使用してScaffoldのpaddingを適用
}
```

### RoomEditorTemplate
部屋編集テンプレート

```kotlin
RoomEditorTemplate(
    saveState = saveState,
    onSaveRetry = { /* retry */ },
    toolbarActions = listOf(/* actions */),
    toolbarExtraContent = { /* extra content */ },
    mainContent = {
        // メインキャンバス
    },
    sidePanel = {
        // サイドパネル
    }
)
```

## Pages (ページ)

### ProjectListPage
プロジェクト一覧ページ

```kotlin
ProjectListPage(
    viewModel = projectViewModel,
    onProjectClick = { projectId -> /* navigate */ },
    onCreateNew = { /* navigate */ }
)
```

## カラーテーマ (Color Theme)

### WarmLightColorScheme
暖色系ライトテーマ

```kotlin
MaterialTheme(
    colorScheme = WarmLightColorScheme
) {
    // コンテンツ
}
```

### WarmDarkColorScheme
暖色系ダークテーマ

```kotlin
MaterialTheme(
    colorScheme = WarmDarkColorScheme
) {
    // コンテンツ
}
```

### AppColors
個別の色にアクセス

```kotlin
// プライマリカラー
AppColors.WarmOrange
AppColors.WarmOrangeDark
AppColors.WarmOrangeLight

// セカンダリカラー
AppColors.WarmBeige
AppColors.WarmBeigeDark
AppColors.WarmBeigeLight

// ターシャリカラー
AppColors.WarmTerracotta
AppColors.WarmTerracottaDark
AppColors.WarmTerracottaLight

// 背景色
AppColors.WarmBackground
AppColors.WarmSurface
AppColors.WarmSurfaceVariant

// テキストカラー
AppColors.WarmTextPrimary
AppColors.WarmTextSecondary
AppColors.WarmTextTertiary

// ステータスカラー
AppColors.WarmSuccess
AppColors.WarmWarning
AppColors.WarmError
```

## 使用ガイドライン

### 新しいコンポーネントを作成する場合

1. **Atom**: 最小単位で再利用可能な要素（ボタン、テキスト、アイコンなど）
2. **Molecule**: 複数のAtomsを組み合わせた機能単位
3. **Organism**: MoleculesとAtomsを組み合わせた複雑なUIセクション
4. **Template**: ページのレイアウト構造
5. **Page**: ViewModelと接続し、実際のデータを流し込む

### データフローの原則

- **State Down**: ViewModelから下へデータを流す
- **Events Up**: ユーザー操作は下から上へイベントを送る
- ViewModelとの接続はPageレイヤーのみ

### スタイリングの原則

- Atomsで統一されたスタイルを定義
- MaterialThemeのcolorSchemeを使用
- カスタムカラーはAppColorsに定義
- 余白やサイズは.dpユニットを使用
