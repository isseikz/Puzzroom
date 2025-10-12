# Component Migration Examples

このドキュメントでは、既存のコンポーネントを Atomic Design パターンに移行する具体的な例を示します。

## Example 1: Button の移行

### Before (既存のコード)
```kotlin
Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {
    Text("保存")
}
```

### After (Atomic Design)
```kotlin
AppButton(
    text = "保存",
    onClick = { /* action */ }
)
```

**メリット:**
- スタイルが統一される
- コード量が減る
- 変更が一箇所で済む

---

## Example 2: ProjectCard の移行

### Before (モノリシックなコンポーネント)
```kotlin
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (project.layoutUrl != null) {
                AsyncImage(
                    model = project.layoutUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = project.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.floorPlans.size} floor plans",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Project") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

### After (Atomic Design - 分解して再構成)

#### Step 1: Atoms を使用
```kotlin
// Atoms を活用
AppCard()
AppIconButton()
HorizontalSpacer()
```

#### Step 2: Molecules を作成/使用
```kotlin
// ImageWithFallback Molecule
ImageWithFallback(
    imageUrl = project.layoutUrl,
    fallbackText = project.name
)

// TitleWithSubtitle Molecule
TitleWithSubtitle(
    title = project.name,
    subtitle = "${project.floorPlans.size} floor plans"
)

// ConfirmationDialog Molecule
ConfirmationDialog(
    title = "Delete Project",
    message = "Are you sure?",
    onConfirm = onDelete,
    onDismiss = { showDeleteDialog = false },
    isDestructive = true
)
```

#### Step 3: Organism として統合
```kotlin
@Composable
fun ProjectCardItem(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageWithFallback(
                imageUrl = project.layoutUrl,
                fallbackText = project.name
            )
            
            HorizontalSpacer(width = 16.dp)
            
            TitleWithSubtitle(
                title = project.name,
                subtitle = "${project.floorPlans.size} floor plans",
                modifier = Modifier.weight(1f)
            )
            
            AppIconButton(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                onClick = { showDeleteDialog = true },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Project",
            message = "Are you sure you want to delete \"${project.name}\"?",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true
        )
    }
}
```

**メリット:**
- 150行 → 50行に削減
- 各パーツが再利用可能
- テストしやすい
- 変更の影響範囲が明確

---

## Example 3: Screen の移行

### Before (従来の画面)
```kotlin
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Projects") },
                actions = {
                    IconButton(onClick = onCreateNew) {
                        Icon(Icons.Default.Add, "Create New")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProjectUiState.ProjectList -> {
                if (state.projects.isEmpty()) {
                    // Empty state...
                } else {
                    LazyColumn(...) {
                        items(state.projects) { project ->
                            ProjectCard(...)
                        }
                    }
                }
            }
            is ProjectUiState.Error -> {
                // Error view...
            }
        }
    }
}
```

### After (Atomic Design - Template + Organisms)

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
        actions = {
            AppIconButton(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New",
                onClick = onCreateNew
            )
        }
    ) { modifier ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> {
                LoadingIndicator(modifier = modifier)
            }
            is ProjectUiState.ProjectList -> {
                if (state.projects.isEmpty()) {
                    EmptyState(
                        title = "No projects",
                        message = "Let's create a new project",
                        actionText = "Create New",
                        onAction = onCreateNew,
                        modifier = modifier
                    )
                } else {
                    ProjectList(
                        projects = state.projects,
                        onProjectClick = onProjectClick,
                        onProjectDelete = { id -> viewModel.deleteProject(id) },
                        modifier = modifier
                    )
                }
            }
            is ProjectUiState.Error -> {
                ErrorDisplay(
                    error = state.error,
                    onRetry = { viewModel.loadProjects() },
                    modifier = modifier
                )
            }
        }
    }
}
```

**メリット:**
- レイアウト構造（Template）とロジック（Page）が分離
- 各状態表示が再利用可能なOrganism
- 他の画面でも同じTemplateとOrganismsを使用可能
- テストしやすい構造

---

## Example 4: RoomScreen の移行計画

### 現在の構造分析
```
RoomScreen (大きなモノリシック)
├── Header (SaveStateIndicator, PhotoPickerButton, Buttons)
├── Main Content
│   ├── Canvas (EditablePolygonCanvas)
│   └── Sidebar
│       ├── PolygonListPanel
│       └── Input Panels (Dimension/Angle)
└── Dialogs (Save/Load)
```

### Atomic Design への分解

#### Organisms を作成
```kotlin
// 1. ヘッダー部分
@Composable
fun RoomEditorHeader(
    saveState: SaveState,
    onRetry: () -> Unit,
    onPhotoSelect: (String) -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit
) {
    Row(...) {
        SaveStateIndicator(saveState, onRetry)
        PhotoPickerButton(onPhotoSelect)
        AppButton("Save", onClick = onSave)
        AppButton("Load", onClick = onLoad)
    }
}

// 2. サイドバー
@Composable
fun RoomEditorSidebar(
    polygons: List<Polygon>,
    selectedIndex: Int?,
    editMode: EditMode,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    // ... 他のプロップス
) {
    Column(...) {
        PolygonListPanel(...)
        
        if (editMode is EditMode.Editing) {
            DimensionAngleEditor(...)
        }
    }
}
```

#### Template を使用
```kotlin
@Composable
fun RoomPage(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    val project by viewModel.currentProject.collectAsState()
    
    EditorScreenTemplate(
        header = {
            RoomEditorHeader(...)
        },
        canvas = { modifier ->
            EditablePolygonCanvas(
                polygons = polygons,
                modifier = modifier,
                ...
            )
        },
        sidebar = { modifier ->
            RoomEditorSidebar(
                polygons = polygons,
                modifier = modifier,
                ...
            )
        }
    )
}
```

**メリット:**
- 責任が明確に分離される
- 各部分を個別にテスト可能
- 他のエディタ画面でも同じTemplateを使用可能
- 段階的に移行可能（既存の動作を保ちながら）

---

## 移行のステップ

### Phase 1: 準備
1. 既存のコンポーネントを分析
2. どのAtomsとMoleculesが使えるか確認
3. 新しいMoleculesやOrganismsが必要か検討

### Phase 2: 段階的移行
1. まず小さいコンポーネントから（ボタン、テキストなど）
2. Moleculesを作成
3. Organismsに統合
4. 最後にPageとして完成

### Phase 3: クリーンアップ
1. 古いコードを削除
2. ドキュメントを更新
3. テストを追加

---

## Tips

### 1. 一度にすべて移行しない
既存の画面は動作を保ちつつ、新しい機能から Atomic Design を適用していく。

### 2. 共通パターンを見つける
複数の画面で使われているパターンは Molecule や Organism として抽出する。

### 3. プロップスは最小限に
必要なデータのみを渡し、デフォルト値を活用する。

### 4. 責任を明確に
- Atoms: 見た目のみ
- Molecules: 小さな機能
- Organisms: 大きなセクション
- Templates: レイアウト
- Pages: データとロジック

### 5. テストを書く
各階層でテストを書くことで、変更の影響を最小限に抑える。

---

このガイドに従うことで、既存のコードを段階的に、かつ安全に Atomic Design パターンに移行できます。
