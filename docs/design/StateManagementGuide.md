# State Management Guide

Puzzroomアプリケーションにおける状態管理の設計ガイド。

## Overview

アプリケーションの状態は大きく2つに分類されます：

1. **永続化データ（ProjectViewModel）** - ディスクに保存され、アプリ終了後も残るデータ
2. **一時的UI状態（AppState）** - セッション中のみ有効で、保存不要なUI状態

この明確な責務分離により、Single Source of Truthを確立し、メンテナンス性と拡張性を向上させます。

---

## Responsibility Separation

### 🗂️ ProjectViewModel - 永続化データとビジネスロジック

**判断基準:**
- ✅ ディスクに保存される
- ✅ アプリを閉じても残る必要がある
- ✅ 複数デバイス間で同期される（将来）
- ✅ Undo/Redoの対象になる

**管理するデータ:**
```kotlin
class ProjectViewModel(...) : ViewModel() {
    // プロジェクトデータ（永続化）
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // 保存状態
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Saved)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // UI状態（プロジェクト一覧、編集中など）
    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // ビジネスロジック
    fun updateProject(project: Project)
    fun saveNow()
    fun deleteProject(projectId: String)
    fun openProject(projectId: String)
    fun createNewProject(name: String)
}
```

**具体的なデータ例:**
- `Project` 全体
  - `name: String` - プロジェクト名
  - `layoutUrl: String?` - 背景画像URL
  - `floorPlans: List<FloorPlan>` - フロアプラン
- `FloorPlan`
  - `rooms: List<Room>` - 部屋のリスト
  - `furnitures: List<Furniture>` - 家具のリスト
  - `layouts: List<LayoutEntry>` - 家具配置情報

**データフロー:**
```
User Action → UI Screen → ProjectViewModel.updateProject()
                              ↓
                         ProjectChanges (SharedFlow)
                              ↓
                         Debounce 500ms
                              ↓
                         Auto Save (FileStorage)
                              ↓
                         SaveState.Saved
```

---

### 🎨 AppState - 一時的UI状態

**判断基準:**
- ✅ セッション中のみ有効（アプリを閉じたら消える）
- ✅ 保存不要
- ✅ 画面遷移時にリセットされる可能性がある
- ✅ UI操作に関連する状態

**管理するデータ:**
```kotlin
class AppState {
    // 選択状態（一時的）
    var selectedRoom by mutableStateOf<Room?>(null)
        private set

    var selectedFurnitureTemplate by mutableStateOf<FurnitureTemplate?>(null)
        private set

    // ドラッグ中の家具（一時的）
    var draggingFurniture by mutableStateOf<DraggingState?>(null)
        private set

    // 現在表示中のダイアログ
    var activeDialog by mutableStateOf<DialogType?>(null)
        private set

    // ズーム・パンの状態（キャンバス）
    var canvasTransform by mutableStateOf(CanvasTransform())
        private set

    // 家具テンプレート（定数）
    val furnitureTemplates: List<FurnitureTemplate> = listOf(
        FurnitureTemplate("ベッド", Centimeter(200), Centimeter(100)),
        FurnitureTemplate("机", Centimeter(120), Centimeter(60)),
        FurnitureTemplate("ソファ", Centimeter(180), Centimeter(90)),
        FurnitureTemplate("椅子", Centimeter(50), Centimeter(50))
    )

    // 公開メソッド
    fun selectRoom(room: Room?)
    fun selectFurnitureTemplate(template: FurnitureTemplate?)
    fun showDialog(dialog: DialogType)
    fun dismissDialog()
}

// 補助データクラス
data class CanvasTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class DraggingState(
    val furniture: Furniture,
    val startPosition: Point
)

sealed interface DialogType {
    object SaveDialog : DialogType
    object LoadDialog : DialogType
    data class ConfirmDelete(val itemId: String) : DialogType
}
```

**具体的なデータ例:**
- **選択状態**
  - `selectedRoom: Room?` - どの部屋を選択中か
  - `selectedFurnitureTemplate: FurnitureTemplate?` - どのテンプレートを選択中か
- **一時的操作状態**
  - `draggingFurniture: DraggingState?` - ドラッグ中の家具
- **表示状態**
  - `activeDialog: DialogType?` - 表示中のダイアログ
  - `canvasTransform: CanvasTransform` - キャンバスのズーム・パン
- **定数データ**
  - `furnitureTemplates: List<FurnitureTemplate>` - ハードコードされたテンプレート

**データフロー:**
```
User Interaction → UI Component → AppState.selectRoom()
                                      ↓
                                  State Update
                                      ↓
                                  UI Recompose
```

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────┐
│                   UI Layer                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
│  │ProjectList │  │ RoomScreen │  │FurnitureSc │ │
│  │  Screen    │  │            │  │    reen    │ │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘ │
│         │               │               │        │
└─────────┼───────────────┼───────────────┼────────┘
          │               │               │
          │               └───────┬───────┘
          │                       │
     ┌────▼────┐          ┌──────▼──────┐
     │ Project │          │   AppState   │
     │ViewModel│          │  (一時的UI)   │
     │(永続化) │          └──────────────┘
     └────┬────┘
          │
          ├─ StateFlow<Project?>
          ├─ StateFlow<SaveState>
          └─ updateProject()
             saveNow()
```

---

## Decision Flow Chart

データをどちらで管理すべきか判断する際のフローチャート：

```
データを管理する必要がある
         ↓
  このデータは保存される？
         ↓
    Yes  │  No
         │
    ┌────▼────┐       ┌─────────────┐
    │ViewModel│       │ 画面遷移後も │
    │         │       │ 必要？      │
    │         │       └──┬──────┬───┘
    │         │      Yes │      │ No
    │         │          │      │
    │         │     ┌────▼──┐   │
    │         │     │ViewModel│ │
    │         │     │(UI State)│ │
    │         │     └────────┘  │
    │         │                 │
    │         │            ┌────▼────┐
    │         │            │AppState │
    │         │            │         │
    └─────────┘            └─────────┘
```

### 判断チェックリスト

| 質問 | Yes → ViewModel | No → AppState |
|------|----------------|---------------|
| このデータはディスクに保存される？ | ✅ | |
| アプリを閉じても残る必要がある？ | ✅ | |
| Undo/Redoの対象？ | ✅ | |
| 複数デバイス間で同期される？（将来） | ✅ | |
| ビジネスロジックと密接に関連？ | ✅ | |
| 画面遷移後もリセットされない？ | ✅ | |

すべてNoなら → **AppState**

---

## Implementation Examples

### Example 1: FurnitureScreen

#### Before（問題あり）

```kotlin
@Composable
fun FurnitureScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    // ❌ AppStateからProjectを取得
    val currentFloorPlan = appState.getCurrentFloorPlan()
    val rooms = currentFloorPlan?.rooms ?: emptyList()

    // ❌ 両方を更新する必要がある
    Button(onClick = {
        appState.updateProject(updatedProject)
        viewModel.updateProject(updatedProject)
    })
}
```

**問題点:**
- 2つの状態管理が存在（AppState, ViewModel）
- 同期の手間がかかる
- Single Source of Truthではない

#### After（改善後）

```kotlin
@Composable
fun FurnitureScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    // ✅ ViewModelからProjectを取得（Single Source of Truth）
    val project by viewModel.currentProject.collectAsState()
    val currentFloorPlan = project?.floorPlans?.firstOrNull()
    val rooms = currentFloorPlan?.rooms ?: emptyList()

    // ✅ ViewModelのみ更新
    Button(onClick = {
        val updatedProject = project?.copy(/* ... */)
        viewModel.updateProject(updatedProject!!)
    })

    // ✅ 一時的UI状態はAppStateから取得
    FurnitureLibraryPanel(
        templates = appState.furnitureTemplates,
        selectedTemplate = appState.selectedFurnitureTemplate,
        onTemplateSelected = { template ->
            appState.selectFurnitureTemplate(template)
        }
    )
}
```

---

### Example 2: RoomScreen

#### Before（問題あり）

```kotlin
@Composable
fun RoomScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    PhotoPickerButton { url ->
        // ❌ AppStateを更新してからViewModelを更新
        appState.setLayoutUrl(url)
        viewModel.updateProject(appState.project)
    }
}
```

#### After（改善後）

```kotlin
@Composable
fun RoomScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    // ✅ ViewModelからProjectを取得
    val project by viewModel.currentProject.collectAsState()

    PhotoPickerButton { url ->
        // ✅ Projectを更新してViewModelのみに通知
        val updatedProject = project?.copy(layoutUrl = url)
        viewModel.updateProject(updatedProject!!)
    }
}
```

---

### Example 3: Handling Selection State

#### AppState（一時的選択状態）

```kotlin
class AppState {
    var selectedRoom by mutableStateOf<Room?>(null)
        private set

    var selectedFurnitureTemplate by mutableStateOf<FurnitureTemplate?>(null)
        private set

    fun selectRoom(room: Room?) {
        selectedRoom = room
    }

    fun selectFurnitureTemplate(template: FurnitureTemplate?) {
        selectedFurnitureTemplate = template
    }
}

// 使用例
@Composable
fun FurnitureScreen(appState: AppState, viewModel: ProjectViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val rooms = project?.floorPlans?.firstOrNull()?.rooms ?: emptyList()

    // ✅ ViewModelからデータ、AppStateから選択状態
    LazyColumn {
        items(rooms) { room ->
            RoomCard(
                room = room,
                isSelected = appState.selectedRoom == room,
                onClick = { appState.selectRoom(room) }
            )
        }
    }
}
```

---

## Data Examples by Category

### ProjectViewModel - Persistent Data

| データ | 型 | 理由 |
|--------|---|------|
| Project全体 | `Project` | プロジェクトのすべてのデータ |
| プロジェクト名 | `String` | 永続化が必要 |
| 背景画像URL | `String?` | 永続化が必要 |
| 部屋のリスト | `List<Room>` | プロジェクトデータ |
| 家具のリスト | `List<Furniture>` | プロジェクトデータ |
| 家具配置 | `List<LayoutEntry>` | 永続化が必要、Undo/Redo対象 |

### AppState - Transient UI State

| データ | 型 | 理由 |
|--------|---|------|
| 選択中の部屋 | `Room?` | 一時的な選択状態 |
| 選択中のテンプレート | `FurnitureTemplate?` | 一時的な選択状態 |
| ドラッグ中の家具 | `DraggingState?` | 操作中の一時的状態 |
| 表示中のダイアログ | `DialogType?` | UI表示状態 |
| キャンバス変形 | `CanvasTransform` | ビューポート状態 |
| 家具テンプレート | `List<FurnitureTemplate>` | ハードコードされた定数 |

---

## Migration Guide

既存コードを新しい責務分担に移行する手順：

### Step 1: AppStateのクリーンアップ

```kotlin
// Before
class AppState {
    var project: Project = Project()  // ❌ 削除

    fun updateProject(project: Project) {  // ❌ 削除
        this.project = project
    }

    fun getCurrentFloorPlan(): FloorPlan? {  // ❌ 削除
        return project.floorPlans.firstOrNull()
    }
}

// After
class AppState {
    var selectedRoom by mutableStateOf<Room?>(null)
        private set

    var selectedFurnitureTemplate by mutableStateOf<FurnitureTemplate?>(null)
        private set

    val furnitureTemplates: List<FurnitureTemplate> = listOf(/* ... */)

    fun selectRoom(room: Room?) {
        selectedRoom = room
    }

    fun selectFurnitureTemplate(template: FurnitureTemplate?) {
        selectedFurnitureTemplate = template
    }
}
```

### Step 2: 画面コンポーネントの更新

```kotlin
// Before
@Composable
fun SomeScreen(appState: AppState, viewModel: ProjectViewModel) {
    val rooms = appState.getCurrentFloorPlan()?.rooms ?: emptyList()

    Button(onClick = {
        appState.updateProject(newProject)
        viewModel.updateProject(newProject)
    })
}

// After
@Composable
fun SomeScreen(appState: AppState, viewModel: ProjectViewModel) {
    val project by viewModel.currentProject.collectAsState()
    val rooms = project?.floorPlans?.firstOrNull()?.rooms ?: emptyList()

    Button(onClick = {
        viewModel.updateProject(newProject)  // ViewModelのみ更新
    })
}
```

### Step 3: App.ktの同期ロジック削除

```kotlin
// Before
@Composable
fun App() {
    val appState = remember { AppState() }
    val projectViewModel = rememberProjectViewModel()

    // ❌ 同期が必要
    LaunchedEffect(projectViewModel.currentProject) {
        projectViewModel.currentProject.collect { project ->
            if (project != null) {
                appState.updateProject(project)
            }
        }
    }
}

// After
@Composable
fun App() {
    val appState = remember { AppState() }
    val projectViewModel = rememberProjectViewModel()

    // ✅ 同期不要！
}
```

---

## Testing Strategy

### ProjectViewModel のテスト

```kotlin
class ProjectViewModelTest {
    @Test
    fun `updateProject triggers auto-save after 500ms`() = runTest {
        val viewModel = ProjectViewModel(/* ... */)
        val project = Project(name = "Test Project")

        viewModel.updateProject(project)

        // 500ms以内は保存されない
        delay(400)
        assertEquals(SaveState.Saved, viewModel.saveState.value)

        // 500ms後に保存される
        delay(200)
        verify(saveProjectUseCase).invoke(project)
    }
}
```

### AppState のテスト

```kotlin
class AppStateTest {
    @Test
    fun `selectRoom updates selectedRoom`() {
        val appState = AppState()
        val room = Room(name = "Living Room", shape = Polygon(/* ... */))

        appState.selectRoom(room)

        assertEquals(room, appState.selectedRoom)
    }

    @Test
    fun `selectRoom can clear selection`() {
        val appState = AppState()
        appState.selectRoom(Room(/* ... */))

        appState.selectRoom(null)

        assertNull(appState.selectedRoom)
    }
}
```

---

## Best Practices

### ✅ DO

1. **ViewModelからProjectを取得**
   ```kotlin
   val project by viewModel.currentProject.collectAsState()
   ```

2. **ViewModelのみ更新**
   ```kotlin
   viewModel.updateProject(updatedProject)
   ```

3. **一時的UI状態はAppState**
   ```kotlin
   appState.selectRoom(room)
   ```

4. **StateFlowを使用**
   ```kotlin
   val currentProject: StateFlow<Project?>
   ```

### ❌ DON'T

1. **AppStateにProjectを保存しない**
   ```kotlin
   // ❌
   class AppState {
       var project: Project
   }
   ```

2. **両方を同期しない**
   ```kotlin
   // ❌
   appState.updateProject(project)
   viewModel.updateProject(project)
   ```

3. **永続化データをAppStateで管理しない**
   ```kotlin
   // ❌
   class AppState {
       var layouts: List<LayoutEntry>
   }
   ```

---

## Future Considerations

### Phase 2: リアルタイム共同編集

ProjectViewModelがWebSocketから変更を受信する場合：

```kotlin
class ProjectViewModel(...) : ViewModel() {
    init {
        viewModelScope.launch {
            realtimeConnection.observeProjectChanges(projectId)
                .collect { remoteProject ->
                    _currentProject.value = remoteProject
                    // AppStateは自動的に最新のProjectを参照
                }
        }
    }
}
```

### Phase 3: 複雑なUI状態管理

AppStateが肥大化する場合は、画面ごとのStateクラスを導入：

```kotlin
class FurnitureScreenState {
    var selectedFurniture by mutableStateOf<Furniture?>(null)
    var draggingState by mutableStateOf<DraggingState?>(null)
    var canvasTransform by mutableStateOf(CanvasTransform())
}

@Composable
fun FurnitureScreen(viewModel: ProjectViewModel) {
    val screenState = remember { FurnitureScreenState() }
    // ...
}
```

---

## Summary

| 側面 | ProjectViewModel | AppState |
|------|-----------------|----------|
| **データの性質** | 永続化データ | 一時的UI状態 |
| **ライフサイクル** | ViewModel（長期） | Composable（短期） |
| **保存** | ✅ | ❌ |
| **同期（将来）** | ✅ | ❌ |
| **Undo/Redo** | ✅ | ❌ |
| **例** | Project, Room, Furniture | selectedRoom, dialog |

**黄金律:** 「保存する必要があるか？」がYesなら**ProjectViewModel**、Noなら**AppState**

---

## References

- [Android Architecture Guide - UI Layer](https://developer.android.com/topic/architecture/ui-layer)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)
- [ViewModel Overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Kotlin StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
