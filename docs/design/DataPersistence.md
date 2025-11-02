# Data Persistence

## Overview

Puzzroomアプリケーションのデータ永続化アーキテクチャ。プロジェクト、間取り図、部屋、家具などの複雑な階層構造を持つデータを**ローカルおよびクラウドに効率的に保存・取得する**設計。

### スコープ

このドキュメントは**データの永続化（保存・読み込み）**に焦点を当てます：

- ✅ ローカルストレージ（ファイル、SQLite）
- ✅ データのシリアライゼーション
- ✅ CRUD操作とRepository Pattern
- ✅ キャッシング戦略
- ✅ データマイグレーション
- ✅ バックアップ・エクスポート

### 関連ドキュメント

永続化以外の以下のトピックは別ドキュメントを参照してください：

- **通信シーケンス図**: [`CommunicationSequenceDiagrams.md`](./CommunicationSequenceDiagrams.md)
  - サーバー・クライアント間の通信フロー
  - 各フェーズでのシーケンス図
  - 認証、同期、リアルタイム編集の詳細
- **リアルタイム共同編集**: [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)
  - Event Sourcing アーキテクチャ
  - WebSocket通信
  - 競合解決とEntity Locking
  - Presence System

### 将来の拡張性

このアーキテクチャは将来的な**複数ユーザーによる共同編集**を想定して設計されています：

- バージョン管理フィールド（version, updatedAt など）の追加
- ユーザー情報（createdBy, ownerId など）の保持
- イベントストアへの移行可能性を考慮

## Data Model Hierarchy

### 基本モデル（現在）

```
Project
├── id: String (UUID)
├── name: String
├── layoutUrl: String?
└── floorPlans: List<FloorPlan>
    ├── id: String
    ├── name: String
    ├── rooms: List<Room>
    ├── furnitures: List<Furniture>
    └── layouts: List<LayoutEntry>
```

### 拡張モデル（将来の共同編集対応）

共同編集を実装する際は、以下のメタデータをデータモデルに追加します：

```kotlin
@Serializable
data class Project(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val name: String,
    val layoutUrl: String? = null,
    val floorPlans: List<FloorPlan> = emptyList(),

    // 将来の共同編集用メタデータ（Optional）
    val version: Long? = null,           // 楽観的ロック用
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val createdBy: String? = null,       // ユーザーID
    val ownerId: String? = null,
)
```

**注:** 共同編集の詳細な設計（Event Sourcing、イベントモデル、Projection など）は [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md) を参照してください。

## Architecture Layers

### 1. Repository Layer (Domain)

```kotlin
interface ProjectRepository {
    // 基本的なCRUD操作
    suspend fun getAllProjects(): Result<List<Project>>
    suspend fun getProjectById(id: String): Result<Project?>
    suspend fun saveProject(project: Project): Result<Unit>
    suspend fun deleteProject(id: String): Result<Unit>
    suspend fun updateProject(project: Project): Result<Unit>

    // リアクティブな監視
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: String): Flow<Project?>
}
```

**責務:**
- ドメインモデルの永続化APIを提供
- データソースの切り替えを抽象化（ローカル / クラウド）
- ビジネスロジックレイヤーから実装詳細を隠蔽

### 2. Data Source Layer

#### Local Data Source

```kotlin
interface LocalProjectDataSource {
    suspend fun getAllProjects(): List<Project>
    suspend fun getProjectById(id: String): Project?
    suspend fun insertProject(project: Project)
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: String)
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: String): Flow<Project?>
}
```

#### Remote Data Source（クラウド同期用）

```kotlin
interface RemoteProjectDataSource {
    // プロジェクトのアップロード・ダウンロード
    suspend fun getAllProjects(): List<Project>
    suspend fun getProject(id: String): Project?
    suspend fun uploadProject(project: Project): Result<Unit>
    suspend fun deleteProject(id: String): Result<Unit>
    suspend fun updateProject(project: Project): Result<Unit>
}
```

**実装例:**
- Firebase Firestore
- Supabase REST API
- カスタムバックエンド（REST API）

**注:** リアルタイム同期（WebSocket、イベントストリーム）の実装は [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md) を参照してください。

### 3. Storage Strategy

#### Phase 1: Local File Storage (初期実装)

**Pros:**
- シンプルな実装
- kotlinx.serializationとの相性が良い
- クロスプラットフォーム対応が容易

**Implementation:**

```kotlin
// commonMain
expect class FileStorage {
    suspend fun writeProject(project: Project, fileName: String)
    suspend fun readProject(fileName: String): Project?
    suspend fun deleteProject(fileName: String)
    suspend fun listProjects(): List<String>
}

// androidMain
actual class FileStorage(private val context: Context) {
    private val projectsDir = File(context.filesDir, "projects")

    init {
        projectsDir.mkdirs()
    }

    actual suspend fun writeProject(project: Project, fileName: String) {
        withContext(Dispatchers.IO) {
            val file = File(projectsDir, "$fileName.json")
            val json = Json.encodeToString(project)
            file.writeText(json)
        }
    }

    actual suspend fun readProject(fileName: String): Project? {
        return withContext(Dispatchers.IO) {
            val file = File(projectsDir, "$fileName.json")
            if (file.exists()) {
                val json = file.readText()
                Json.decodeFromString<Project>(json)
            } else null
        }
    }

    actual suspend fun deleteProject(fileName: String) {
        withContext(Dispatchers.IO) {
            val file = File(projectsDir, "$fileName.json")
            file.delete()
        }
    }

    actual suspend fun listProjects(): List<String> {
        return withContext(Dispatchers.IO) {
            projectsDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        }
    }
}

// iosMain
actual class FileStorage {
    private val documentsPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String

    private val projectsPath = "$documentsPath/projects"

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(
            projectsPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    // Similar implementation for iOS
}
```

**File Structure:**
```
app_data/
└── projects/
    ├── <project-id-1>.json
    ├── <project-id-2>.json
    └── <project-id-3>.json
```

#### Phase 2: SQLite Database (将来の拡張)

複雑なクエリや関連データの効率的な取得が必要になった場合:

**Technology Options:**
- SQLDelight (推奨) - KMP対応、型安全
- Room (Android only)
- Realm Kotlin

**Schema Design:**
```sql
CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    layout_url TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE floor_plans (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    name TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE rooms (
    id TEXT PRIMARY KEY,
    floor_plan_id TEXT NOT NULL,
    name TEXT NOT NULL,
    shape_json TEXT NOT NULL,
    FOREIGN KEY (floor_plan_id) REFERENCES floor_plans(id) ON DELETE CASCADE
);

CREATE TABLE furnitures (
    id TEXT PRIMARY KEY,
    floor_plan_id TEXT NOT NULL,
    name TEXT NOT NULL,
    shape_json TEXT NOT NULL,
    FOREIGN KEY (floor_plan_id) REFERENCES floor_plans(id) ON DELETE CASCADE
);

CREATE TABLE layout_entries (
    id TEXT PRIMARY KEY,
    floor_plan_id TEXT NOT NULL,
    room_id TEXT NOT NULL,
    furniture_id TEXT NOT NULL,
    position_json TEXT NOT NULL,
    rotation REAL NOT NULL,
    FOREIGN KEY (floor_plan_id) REFERENCES floor_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (furniture_id) REFERENCES furnitures(id)
);
```

### 4. Serialization Strategy

**Current Setup:** kotlinx.serialization (既に @Serializable アノテーション付き)

**JSON Configuration:**

```kotlin
val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
```

**Considerations:**
- UUIDの一貫性を保つため、デフォルト値は保存時に生成済みのものを使用
- 将来的なスキーマ変更に備えて `ignoreUnknownKeys = true`

### 5. Caching Strategy

```kotlin
class ProjectRepositoryImpl(
    private val localDataSource: LocalProjectDataSource,
    private val remoteDataSource: RemoteProjectDataSource? = null
) : ProjectRepository {

    // In-memory cache
    private val cache = MutableStateFlow<Map<String, Project>>(emptyMap())

    override suspend fun getAllProjects(): Result<List<Project>> {
        return runCatching {
            val projects = localDataSource.getAllProjects()
            cache.value = projects.associateBy { it.id }
            projects
        }
    }

    override fun observeProjects(): Flow<List<Project>> {
        return localDataSource.observeProjects()
            .onEach { projects ->
                cache.value = projects.associateBy { it.id }
            }
    }
}
```

### 6. Cloud Sync Strategy

クラウド同期は段階的に実装します：

#### Phase 1: Manual Sync
- ユーザーが手動で「同期」ボタンを押す
- シンプルなアップロード・ダウンロード

#### Phase 2: Auto Sync
- バックグラウンドで自動同期
- ネットワーク状態の監視
- オフライン時のキューイング

#### Phase 3: Real-time Sync
- WebSocketベースのリアルタイム同期
- 詳細は [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md) を参照

### 7. Error Handling

```kotlin
sealed class PersistenceError {
    // ローカルストレージエラー
    data class IOError(val cause: Throwable) : PersistenceError()
    data class SerializationError(val cause: Throwable) : PersistenceError()
    data class NotFoundError(val id: String) : PersistenceError()
    data class CorruptedDataError(val fileName: String) : PersistenceError()

    // クラウド同期エラー
    data class NetworkError(val cause: Throwable) : PersistenceError()
    data class SyncError(val cause: Throwable) : PersistenceError()
    data class UnauthorizedError(val message: String) : PersistenceError()
    data class QuotaExceededError(val currentSize: Long, val maxSize: Long) : PersistenceError()
}

typealias PersistenceResult<T> = Result<T>
```

### 8. Migration Strategy

バージョン間のデータ移行:

```kotlin
interface DataMigration {
    val fromVersion: Int
    val toVersion: Int
    suspend fun migrate(data: String): String
}

class MigrationManager(private val migrations: List<DataMigration>) {
    suspend fun migrateIfNeeded(data: String, currentVersion: Int, targetVersion: Int): String {
        var result = data
        var version = currentVersion

        while (version < targetVersion) {
            val migration = migrations.find { it.fromVersion == version }
                ?: throw IllegalStateException("No migration found from version $version")
            result = migration.migrate(result)
            version = migration.toVersion
        }

        return result
    }
}
```

### 9. Backup & Export

```kotlin
interface ProjectExporter {
    suspend fun exportProject(project: Project, format: ExportFormat): ByteArray
    suspend fun importProject(data: ByteArray, format: ExportFormat): Project

    // イベントストリームのエクスポート（完全な履歴を含む）
    suspend fun exportProjectWithHistory(projectId: String): ByteArray
    suspend fun importProjectWithHistory(data: ByteArray): Project
}

enum class ExportFormat {
    JSON,           // シンプルなJSON
    JSON_WITH_EVENTS,  // イベント履歴付き
    ZIP,            // JSON + images
    PDF,            // For sharing（読み取り専用）
}
```

## UI Integration Architecture

### Overview

UIとデータ永続化レイヤーの統合には**MVVM (Model-View-ViewModel) + Repository Pattern**を採用します。

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ProjectList  │  │ RoomScreen   │  │ FurnitureScr │  │
│  │   Screen     │  │              │  │    een       │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │           │
│         └─────────────────┼─────────────────┘           │
│                           │                             │
└───────────────────────────┼─────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────┐
│                    ViewModel Layer                       │
│                    ┌──────────────┐                      │
│                    │ ProjectVM    │                      │
│                    │              │                      │
│                    │ - UiState    │                      │
│                    │ - saveProject│                      │
│                    │ - loadProject│                      │
│                    │ - autoSave() │                      │
│                    └──────┬───────┘                      │
└───────────────────────────┼─────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────┐
│                     Domain Layer                         │
│                    ┌──────────────┐                      │
│                    │ ProjectUseCase│                     │
│                    │              │                      │
│                    │ - SaveProject│                      │
│                    │ - LoadProject│                      │
│                    │ - DeleteProj │                      │
│                    └──────┬───────┘                      │
└───────────────────────────┼─────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────┐
│                      Data Layer                          │
│                ┌────────────────────┐                    │
│                │ ProjectRepository  │                    │
│                │   (Interface)      │                    │
│                └─────────┬──────────┘                    │
│                          │                               │
│                ┌─────────▼──────────┐                    │
│                │ProjectRepositoryImpl│                   │
│                └─────────┬──────────┘                    │
│                          │                               │
│         ┌────────────────┼────────────────┐              │
│         │                │                │              │
│    ┌────▼─────┐    ┌────▼────┐    ┌─────▼──────┐       │
│    │LocalData │    │InMemory │    │RemoteData  │       │
│    │Source    │    │Cache    │    │Source      │       │
│    └────┬─────┘    └─────────┘    └────────────┘       │
│         │                               (Phase 3+)      │
│    ┌────▼─────┐                                         │
│    │FileStorage│                                        │
│    │(expect/  │                                         │
│    │ actual)  │                                         │
│    └──────────┘                                         │
└─────────────────────────────────────────────────────────┘
```

### Ideal User Experience

#### 1. プロジェクト管理フロー

```
[起動] → [プロジェクト一覧] → [編集画面] → [自動保存]
            ↓
         [新規作成]
         [削除]
         [エクスポート]
```

**プロジェクト一覧画面の特徴:**
- カード形式のプロジェクト一覧表示
- サムネイル画像（layoutUrl）
- 最終更新日時の表示
- プロジェクト名のインライン編集
- スワイプで削除（確認ダイアログ付き）
- 検索・フィルタリング機能

**編集画面の特徴:**
- **自動保存**: 変更から500ms後に自動保存（Debouncing）
- 保存状態のビジュアルインジケーター:
  - ✓ 保存済み（グレー）
  - ⟳ 保存中...（アニメーション）
  - ⚠️ 保存失敗（赤、リトライボタン）
- オフライン対応 - ネットワークなしでも動作保証

#### 2. エラーハンドリングUX

**エラー種類別の対応:**

| エラータイプ | ユーザー向けメッセージ | 推奨アクション |
|------------|-------------------|--------------|
| `IOError` | 保存できませんでした。ストレージを確認してください | リトライ |
| `QuotaExceededError` | 容量不足です。古いプロジェクトを削除してください | ストレージ管理 |
| `CorruptedDataError` | ファイルが破損しています。 | バックアップから復元 |
| `NotFoundError` | プロジェクトが見つかりませんでした | プロジェクト一覧に戻る |

**エラー表示例:**
```kotlin
┌────────────────────────────┐
│ ⚠️ プロジェクトを保存できませんでした │
│                            │
│ ストレージの空き容量が不足しています │
│                            │
│ [リトライ] [詳細を見る]      │
└────────────────────────────┘
```

### ViewModel Layer

#### ProjectViewModel

```kotlin
class ProjectViewModel(
    private val saveProjectUseCase: SaveProjectUseCase,
    private val loadProjectUseCase: LoadProjectUseCase,
    private val listProjectsUseCase: ListProjectsUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // 現在のプロジェクト
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // 保存状態
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Saved)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // プロジェクト変更検知
    private val projectChanges = MutableSharedFlow<Project>()

    init {
        // 自動保存の設定（500ms debounce）
        viewModelScope.launch {
            projectChanges
                .debounce(500.milliseconds)
                .collect { project ->
                    autoSave(project)
                }
        }
    }

    // プロジェクト一覧を読み込み
    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectUiState.Loading
            listProjectsUseCase()
                .onSuccess { projects ->
                    _uiState.value = ProjectUiState.ProjectList(projects)
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }

    // プロジェクトを開く
    fun openProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectUiState.Loading
            loadProjectUseCase(projectId)
                .onSuccess { project ->
                    _currentProject.value = project
                    _uiState.value = ProjectUiState.EditingProject(project)
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }

    // プロジェクトを更新（自動保存トリガー）
    fun updateProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            projectChanges.emit(project)
        }
    }

    // 自動保存
    private suspend fun autoSave(project: Project) {
        _saveState.value = SaveState.Saving
        saveProjectUseCase(project)
            .onSuccess {
                _saveState.value = SaveState.Saved
            }
            .onFailure { error ->
                _saveState.value = SaveState.Failed(error.toUiError())
            }
    }

    // 手動保存（即座に保存）
    fun saveNow() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            autoSave(project)
        }
    }

    // プロジェクトを削除
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            deleteProjectUseCase(projectId)
                .onSuccess {
                    loadProjects() // 一覧を再読み込み
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }
}
```

#### UI State定義

```kotlin
// UI State
sealed interface ProjectUiState {
    data object Loading : ProjectUiState
    data class ProjectList(val projects: List<Project>) : ProjectUiState
    data class EditingProject(val project: Project) : ProjectUiState
    data class Error(val error: UiError) : ProjectUiState
}

// 保存状態
sealed interface SaveState {
    data object Saved : SaveState
    data object Saving : SaveState
    data class Failed(val error: UiError) : SaveState
}

// UIエラー
data class UiError(
    val message: String,
    val retryAction: (() -> Unit)? = null,
    val severity: Severity = Severity.Error
) {
    enum class Severity { Info, Warning, Error }
}

// PersistenceError → UiError変換
fun Throwable.toUiError(): UiError {
    return when (this) {
        is PersistenceError.IOError -> UiError(
            message = "保存できませんでした。ストレージを確認してください",
            severity = UiError.Severity.Error
        )
        is PersistenceError.QuotaExceededError -> UiError(
            message = "容量不足です（${currentSize / 1024 / 1024}MB / ${maxSize / 1024 / 1024}MB）",
            severity = UiError.Severity.Warning
        )
        is PersistenceError.CorruptedDataError -> UiError(
            message = "ファイル「$fileName」が破損しています",
            severity = UiError.Severity.Error
        )
        is PersistenceError.NotFoundError -> UiError(
            message = "プロジェクト（ID: $id）が見つかりませんでした",
            severity = UiError.Severity.Warning
        )
        else -> UiError(
            message = "エラーが発生しました: ${this.message}",
            severity = UiError.Severity.Error
        )
    }
}
```

### UseCase Layer

UseCaseは単一責任の原則に従い、各操作を独立したクラスとして実装します。

#### SaveProjectUseCase

```kotlin
class SaveProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(project: Project): Result<Unit> {
        // バリデーション
        if (project.name.isBlank()) {
            return Result.failure(InvalidProjectException("プロジェクト名は必須です"))
        }

        return repository.saveProject(project)
    }
}

class InvalidProjectException(message: String) : Exception(message)
```

#### LoadProjectUseCase

```kotlin
class LoadProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Result<Project> {
        return repository.getProjectById(projectId)
            .mapCatching { project ->
                project ?: throw ProjectNotFoundException(projectId)
            }
    }
}

class ProjectNotFoundException(projectId: String) :
    Exception("Project not found: $projectId")
```

#### ListProjectsUseCase

```kotlin
class ListProjectsUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(): Result<List<Project>> {
        return repository.getAllProjects()
            .map { projects ->
                // 最終更新日でソート（将来的にmetadataを追加）
                projects.sortedByDescending { it.id }
            }
    }
}
```

#### DeleteProjectUseCase

```kotlin
class DeleteProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Result<Unit> {
        return repository.deleteProject(projectId)
    }
}
```

#### UseCasesコンテナ

```kotlin
data class ProjectUseCases(
    val saveProject: SaveProjectUseCase,
    val loadProject: LoadProjectUseCase,
    val listProjects: ListProjectsUseCase,
    val deleteProject: DeleteProjectUseCase,
)
```

### Dependency Injection

#### 共通DIモジュール

```kotlin
object DataModule {
    fun provideProjectRepository(fileStorage: IFileStorage): ProjectRepository {
        val localDataSource = LocalProjectDataSourceImpl(fileStorage)
        return ProjectRepositoryImpl(localDataSource)
    }

    fun provideUseCases(repository: ProjectRepository): ProjectUseCases {
        return ProjectUseCases(
            saveProject = SaveProjectUseCase(repository),
            loadProject = LoadProjectUseCase(repository),
            listProjects = ListProjectsUseCase(repository),
            deleteProject = DeleteProjectUseCase(repository)
        )
    }
}
```

#### プラットフォーム別ViewModel作成

**Android:**

```kotlin
// androidMain
@Composable
fun rememberProjectViewModel(context: Context): ProjectViewModel {
    val fileStorage = remember { FileStorage(context) }
    val repository = remember { DataModule.provideProjectRepository(fileStorage) }
    val useCases = remember { DataModule.provideUseCases(repository) }

    return viewModel {
        ProjectViewModel(
            useCases.saveProject,
            useCases.loadProject,
            useCases.listProjects,
            useCases.deleteProject
        )
    }
}
```

**iOS:**

```kotlin
// iosMain
@Composable
fun rememberProjectViewModel(): ProjectViewModel {
    val fileStorage = remember { FileStorage() }
    val repository = remember { DataModule.provideProjectRepository(fileStorage) }
    val useCases = remember { DataModule.provideUseCases(repository) }

    return viewModel {
        ProjectViewModel(
            useCases.saveProject,
            useCases.loadProject,
            useCases.listProjects,
            useCases.deleteProject
        )
    }
}
```

**共通エントリーポイント:**

```kotlin
// commonMain
@Composable
expect fun rememberProjectViewModel(): ProjectViewModel

// 使用例
@Composable
fun App() {
    val viewModel = rememberProjectViewModel()
    // ...
}
```

### UI Implementation Examples

#### ProjectListScreen

```kotlin
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("マイプロジェクト") },
                actions = {
                    IconButton(onClick = onCreateNew) {
                        Icon(Icons.Default.Add, "新規作成")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProjectUiState.ProjectList -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(state.projects) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }

                    if (state.projects.isEmpty()) {
                        item {
                            EmptyState(
                                message = "プロジェクトがありません",
                                actionText = "新規作成",
                                onAction = onCreateNew
                            )
                        }
                    }
                }
            }
            is ProjectUiState.Error -> {
                ErrorView(
                    error = state.error,
                    onRetry = { viewModel.loadProjects() }
                )
            }
            else -> { /* EditingProjectは別画面 */ }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // サムネイル
            AsyncImage(
                model = project.layoutUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "最終更新: ${project.id}", // TODO: updatedAtに置き換え
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // 削除ボタン
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "削除")
            }
        }
    }
}
```

#### EditProjectScreen

```kotlin
@Composable
fun EditProjectScreen(
    viewModel: ProjectViewModel
) {
    val project by viewModel.currentProject.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "プロジェクト") },
                actions = {
                    // 保存状態インジケーター
                    SaveStateIndicator(
                        saveState = saveState,
                        onRetry = { viewModel.saveNow() }
                    )

                    // 手動保存ボタン
                    IconButton(onClick = { viewModel.saveNow() }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                }
            )
        }
    ) { padding ->
        project?.let { currentProject ->
            Column(modifier = Modifier.padding(padding)) {
                // プロジェクト名編集
                OutlinedTextField(
                    value = currentProject.name,
                    onValueChange = { newName ->
                        viewModel.updateProject(
                            currentProject.copy(name = newName)
                        )
                    },
                    label = { Text("プロジェクト名") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // 既存の編集UI（RoomScreen、FurnitureScreenなど）
                // 変更があれば viewModel.updateProject(updatedProject) を呼ぶ
            }
        }
    }
}

@Composable
fun SaveStateIndicator(
    saveState: SaveState,
    onRetry: () -> Unit
) {
    when (saveState) {
        SaveState.Saved -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "保存済み",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "保存済み",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        SaveState.Saving -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "保存中...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is SaveState.Failed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "保存失敗",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "保存失敗",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    error: UiError,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = when (error.severity) {
                UiError.Severity.Error -> MaterialTheme.colorScheme.error
                UiError.Severity.Warning -> Color(0xFFFF9800)
                UiError.Severity.Info -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("リトライ")
        }
    }
}
```

### Auto-Save Strategy

#### Debouncing実装

```kotlin
// ViewModelで実装済み
init {
    viewModelScope.launch {
        projectChanges
            .debounce(500.milliseconds) // 500ms待機
            .collect { project ->
                autoSave(project)
            }
    }
}
```

**動作:**
1. ユーザーが編集: `viewModel.updateProject(project)` 呼び出し
2. 500ms待機（この間に追加の変更があれば、タイマーリセット）
3. 変更がなくなってから500ms後に自動保存実行

**メリット:**
- ユーザーが編集中は頻繁に保存しない（パフォーマンス向上）
- 編集完了後、自動的に保存（データ損失防止）
- UIのちらつき防止（保存中インジケーターの頻繁な点滅を避ける）

#### 各画面での自動保存統合

各画面では、ユーザーの操作によってプロジェクトが変更された際に `viewModel.updateProject()` を呼び出して自動保存をトリガーします。

##### RoomScreen の自動保存トリガーポイント

**ファイル:** `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/screen/RoomScreen.kt`

| トリガーポイント | ユーザー操作 | 実装箇所 |
|--------------|----------|---------|
| 背景画像選択 | PhotoPickerButtonで画像選択 | `PhotoPickerButton { url -> ... }` (43行目) |
| 部屋の保存 | 「保存」ボタンクリック | `Button(onClick = { ... })` (48行目) |

**実装例:**
```kotlin
// 背景画像選択時
PhotoPickerButton { url ->
    appState.setLayoutUrl(url)
    // プロジェクトを更新して自動保存トリガー
    viewModel.updateProject(appState.project)
}

// 部屋の保存時
Button(onClick = {
    polygons.forEachIndexed { index, polygon ->
        appState.addRoom(Room(name = "Room ${index + 1}", shape = polygon))
    }
    // プロジェクトを更新して自動保存トリガー
    viewModel.updateProject(appState.project)
    savedJson = Json.encodeToString(appState.project)
    showSaveDialog = true
}) {
    Text("保存")
}
```

##### FurnitureScreen の自動保存トリガーポイント

**ファイル:** `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/screen/FurnitureScreen.kt`

| トリガーポイント | ユーザー操作 | 実装箇所 |
|--------------|----------|---------|
| 家具の移動 | キャンバス上で家具をドラッグ | `onFurnitureMoved` コールバック (183-206行目) |
| 家具配置の確定 | ツールバーの「確定」ボタンクリック | `onConfirm` コールバック (222-242行目) |

**実装例:**
```kotlin
// 家具の移動時
onFurnitureMoved = { index, newPosition ->
    placedFurnitures = placedFurnitures.mapIndexed { i, furniture ->
        if (i == index) furniture.copy(position = newPosition) else furniture
    }

    // AppStateも更新
    val updatedLayoutEntry = appState.getCurrentFloorPlan()?.layouts?.getOrNull(index)?.copy(position = newPosition)
    if (updatedLayoutEntry != null) {
        val currentFloorPlan = appState.getCurrentFloorPlan()!!
        val updatedFloorPlan = currentFloorPlan.copy(
            layouts = currentFloorPlan.layouts.mapIndexed { i, layout ->
                if (i == index) updatedLayoutEntry else layout
            }
        )
        val updatedProject = appState.project.copy(
            floorPlans = listOf(updatedFloorPlan)
        )
        appState.updateProject(updatedProject)
        // 自動保存トリガー
        viewModel.updateProject(updatedProject)
    }
}

// 家具配置の確定時
onConfirm = {
    currentPosition?.let { position ->
        val layoutEntry = LayoutEntry(
            room = selectedRoom!!,
            furniture = currentFurniture!!,
            position = position,
            rotation = furnitureRotation.degree()
        )
        placedFurnitures = placedFurnitures + PlacedFurniture(
            furniture = currentFurniture!!,
            position = position,
            rotation = furnitureRotation.degree()
        )
        appState.addLayoutEntry(layoutEntry)
        appState.addFurniture(currentFurniture!!)
        appState.selectFurnitureTemplate(null)
        currentPosition = null

        // 自動保存トリガー
        viewModel.updateProject(appState.project)
    }
}
```

##### 自動保存統合のベストプラクティス

新しい画面を追加する際は、以下のガイドラインに従ってください：

1. **ProjectViewModelの注入**
   ```kotlin
   @Composable
   fun YourScreen(
       appState: AppState,
       viewModel: ProjectViewModel  // ViewModelを受け取る
   )
   ```

2. **SaveStateIndicatorの表示**
   ```kotlin
   SaveStateIndicator(
       saveState = saveState,
       onRetry = { viewModel.saveNow() }
   )
   ```

3. **自動保存のトリガー**
   - プロジェクトデータが変更されるたびに `viewModel.updateProject(appState.project)` を呼び出す
   - Debouncing（500ms）により、頻繁な保存は自動的に間引かれる

4. **トリガーすべき操作**
   - ✅ データモデルの変更（追加、削除、更新）
   - ✅ プロパティの変更（名前、サイズ、位置など）
   - ✅ 設定の変更（背景画像、テンプレート選択など）
   - ❌ 一時的なUI状態（選択状態、ホバーなど）

5. **注意点**
   - `appState.updateProject()` と `viewModel.updateProject()` の両方を呼び出す必要がある
   - `appState`: UI状態の即座の更新（ローカル）
   - `viewModel`: 永続化のトリガー（自動保存）

**悪い例:**
```kotlin
// AppStateのみ更新（永続化されない）
appState.addRoom(room)
```

**良い例:**
```kotlin
// AppStateとViewModelの両方を更新
appState.addRoom(room)
viewModel.updateProject(appState.project)
```

### Migration from AppState to ViewModel

現在の`AppState`から`ProjectViewModel`への移行：

**Before (AppState):**
```kotlin
val appState = remember { AppState() }

// プロジェクト更新
appState.updateProject(newProject)
```

**After (ViewModel):**
```kotlin
val viewModel = rememberProjectViewModel()

// プロジェクト更新（自動保存トリガー）
viewModel.updateProject(newProject)
```

**主な違い:**
- `AppState`: メモリ内のみの状態管理
- `ViewModel`: 永続化統合、自動保存、エラーハンドリング

### Key Design Decisions

#### 1. 自動保存 vs 手動保存

✅ **自動保存（推奨）**:
- Debounce: 500ms
- メリット: ユーザーが保存を意識しない、データ損失リスク最小化
- 手動保存ボタンも提供（即座に保存したい場合）

#### 2. 状態管理: StateFlow

- `StateFlow` でReactive UI更新
- Compose の `collectAsState()` で自動再描画
- ViewModel スコープでライフサイクル管理

#### 3. エラー処理の一元化

- `PersistenceError` → `UiError` への変換レイヤー
- リトライアクションをエラーに含める
- 各エラータイプに適したメッセージとアクション

#### 4. レイヤー分離

| Layer | 責務 | 依存関係 |
|-------|------|---------|
| **UI** | Composable関数のみ、ロジックなし | ViewModel |
| **ViewModel** | UI状態とユーザーアクション | UseCase |
| **UseCase** | 単一責任のビジネスロジック | Repository |
| **Repository** | データソース抽象化 | DataSource |
| **DataSource** | 実際のストレージ操作 | FileStorage |

#### 5. 将来への拡張性

- Phase 3でRemoteDataSourceを追加しやすい設計
- リアルタイム共同編集時も同じViewModelを利用可能
- `observeProject()` でWebSocket経由の変更を受信
- Event Sourcingへの移行が容易

### Summary

この設計により：

✅ **シンプルなUX**: 自動保存で保存を意識させない
✅ **堅牢なエラー処理**: ユーザーフレンドリーなエラーメッセージとリトライ機能
✅ **テスト容易性**: 各レイヤーが独立してテスト可能
✅ **拡張性**: クラウド同期・共同編集への移行が容易
✅ **Kotlin Multiplatform対応**: expect/actualで各プラットフォームに最適化

現在の`AppState`を`ProjectViewModel`に置き換えることで、永続化機能が統合されます。

---

## Implementation Plan

### Phase 1: MVP - Local-Only (現在の優先度)
**目標:** 単一ユーザー、ローカルストレージのみ

1. ✅ データモデルに @Serializable 追加 (完了)
2. FileStorage の expect/actual 実装
   - Android: Context.filesDir
   - iOS: NSDocumentDirectory
3. LocalProjectDataSource 実装
   - JSON ファイルベースの保存・読み込み
4. ProjectRepository 実装（シンプル版）
   - CRUD操作
   - Flow-based observability
5. 基本的なCRUD操作のテスト

**成果物:** ローカルでプロジェクトを作成・編集・保存できるアプリ

---

### Phase 2: Event Sourcing Foundation
**目標:** 共同編集の基盤を準備（まだオフライン）

1. データモデルの拡張
   - メタデータ追加（version, createdBy, updatedBy など）
   - Collaborator, UserPresence モデル
2. ProjectEvent の定義
   - すべての変更をイベントとしてモデリング
3. EventStore の実装
   - ローカルイベントストア（SQLite推奨）
   - Append-only ログ
4. ProjectionEngine の実装
   - イベントストリームから状態を再構築
5. スナップショット機能
   - パフォーマンス最適化
6. Undo/Redo 機能
   - イベントの巻き戻し・再適用

**成果物:** イベントベースの状態管理、完全な変更履歴

---

### Phase 3: Backend & Authentication
**目標:** サーバーサイドインフラの構築

1. バックエンド選定
   - Firebase/Firestore（推奨: リアルタイム対応、認証統合）
   - Supabase（PostgreSQL + Realtime）
   - カスタムバックエンド（Ktor + PostgreSQL + WebSocket）
2. 認証システム
   - Firebase Authentication
   - OAuth2（Google, Apple Sign-in）
   - UserId の実装
3. REST API 実装
   - プロジェクトのCRUD
   - イベントのPush/Pull
   - 共同編集者の管理
4. データベーススキーマ
   - projects テーブル
   - events テーブル（append-only）
   - collaborators テーブル
   - snapshots テーブル

**成果物:** 認証付きバックエンドAPI

---

### Phase 4: Cloud Sync (Single User)
**目標:** 単一ユーザーの複数デバイス間同期

1. RemoteProjectDataSource 実装
   - REST API クライアント
2. SyncEngine 実装
   - Push/Pull 同期
   - 競合検出（まだ単一ユーザーなので競合は稀）
3. オフライン対応
   - ローカル優先（Local-first）
   - バックグラウンド同期
4. ネットワーク状態の監視
   - ConnectionState の実装
5. 同期エラーハンドリング
   - Retry ロジック
   - ユーザーへの通知

**成果物:** 複数デバイスで同じプロジェクトを編集可能（ただし同時編集は不可）

---

### Phase 5: Real-time Collaboration
**目標:** 複数ユーザーのリアルタイム同時編集

**📖 詳細な実装ガイド: [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)**

1. WebSocket インフラ
   - サーバー側: WebSocket エンドポイント
   - クライアント側: RealtimeConnection 実装
   - 自動再接続とハートビート
2. リアルタイムイベント配信
   - サーバーから全クライアントへのブロードキャスト
   - イベントのフィルタリング（プロジェクトごと）
3. Entity Locking System（推奨）
   - 同一エンティティを同時編集できるのは1人のみ
   - 自動タイムアウトとロック解放
   - 視覚的なフィードバック（ロック表示）
4. Optimistic UI Updates
   - 即座にUIに反映
   - サーバー確認待たず
   - 失敗時のロールバック
5. Presence システム
   - 誰がオンラインか
   - カーソル位置の共有
   - 選択中のエンティティ表示
6. Performance Optimization
   - Debouncing（高頻度イベントの間引き）
   - Batching（複数イベントのまとめ送信）

**成果物:** Google Docs/Figma ライクなリアルタイム共同編集

**実装の詳細（WebSocket接続、ロック機構、UI統合など）は [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md) を参照してください。**

---

### Phase 6: Advanced Features
**目標:** より高度な共同編集機能

1. 細かい権限管理
   - Read-only vs Editor
   - エンティティレベルのロック
2. コメント・アノテーション機能
   - 特定の家具や部屋へのコメント
3. バージョン管理
   - ブランチ機能（代替案の検討）
   - マージ機能
4. 変更履歴の可視化
   - タイムライン表示
   - 特定時点への復元
5. Export/Import 機能
   - PDF生成
   - 画像エクスポート
   - プロジェクトテンプレート

**成果物:** プロフェッショナル向け共同編集ツール

---

## Technology Stack Recommendations

### Phase 1-2 (Local)
- **Storage:** File-based JSON (kotlinx.serialization)
- **Database:** なし（シンプルに）

### Phase 3-4 (Backend)
- **Backend:** Firebase（推奨）または Supabase
  - Firebase: 簡単、リアルタイム対応、認証統合
  - Supabase: PostgreSQL、より柔軟、OSS
- **Auth:** Firebase Authentication / Supabase Auth
- **Database:** Firestore / PostgreSQL

### Phase 5-6 (Realtime)
- **WebSocket:**
  - Firebase Realtime Database / Firestore (自動対応)
  - Supabase Realtime
  - カスタム: Ktor WebSocket
- **Local DB:** SQLDelight（イベントストア用）
- **HTTP Client:** Ktor Client（KMP対応）

## Testing Strategy

### Unit Tests - Repository

```kotlin
class ProjectRepositoryTest {
    private lateinit var repository: ProjectRepository
    private lateinit var fakeLocalDataSource: FakeLocalProjectDataSource

    @Test
    fun `save and retrieve project`() = runTest {
        val project = Project(name = "Test Project")
        repository.saveProject(project)

        val retrieved = repository.getProjectById(project.id).getOrNull()
        assertEquals(project, retrieved)
    }

    @Test
    fun `delete project removes it from storage`() = runTest {
        val project = Project(name = "Test Project")
        repository.saveProject(project)
        repository.deleteProject(project.id)

        val retrieved = repository.getProjectById(project.id).getOrNull()
        assertNull(retrieved)
    }

    @Test
    fun `update project modifies existing data`() = runTest {
        val project = Project(name = "Original Name")
        repository.saveProject(project)

        val updated = project.copy(name = "Updated Name")
        repository.updateProject(updated)

        val retrieved = repository.getProjectById(project.id).getOrNull()
        assertEquals("Updated Name", retrieved?.name)
    }

    @Test
    fun `observe projects emits changes`() = runTest {
        val projects = mutableListOf<List<Project>>()
        val job = launch {
            repository.observeProjects().take(2).toList(projects)
        }

        repository.saveProject(Project(name = "Project 1"))
        delay(100)
        repository.saveProject(Project(name = "Project 2"))

        job.join()
        assertEquals(2, projects.size)
        assertEquals(2, projects.last().size)
    }
}
```

### Unit Tests - File Storage

```kotlin
class FileStorageTest {
    private lateinit var storage: FileStorage

    @Test
    fun `write and read project`() = runTest {
        val project = Project(name = "Test Project")
        storage.writeProject(project, "test-project")

        val retrieved = storage.readProject("test-project")
        assertEquals(project, retrieved)
    }

    @Test
    fun `list projects returns all saved projects`() = runTest {
        storage.writeProject(Project(name = "Project 1"), "proj-1")
        storage.writeProject(Project(name = "Project 2"), "proj-2")

        val list = storage.listProjects()
        assertEquals(2, list.size)
        assertTrue(list.contains("proj-1"))
        assertTrue(list.contains("proj-2"))
    }

    @Test
    fun `delete project removes file`() = runTest {
        storage.writeProject(Project(name = "Test"), "test")
        storage.deleteProject("test")

        val retrieved = storage.readProject("test")
        assertNull(retrieved)
    }
}
```

### Unit Tests - Serialization

```kotlin
class SerializationTest {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `serialize and deserialize project`() {
        val project = Project(
            name = "Test Project",
            floorPlans = listOf(
                FloorPlan(name = "Floor 1")
            )
        )

        val serialized = json.encodeToString(project)
        val deserialized = json.decodeFromString<Project>(serialized)

        assertEquals(project, deserialized)
    }

    @Test
    fun `handles missing optional fields`() {
        val jsonString = """{"id":"1","name":"Test","floorPlans":[]}"""

        val project = json.decodeFromString<Project>(jsonString)

        assertEquals("Test", project.name)
        assertNull(project.layoutUrl)
    }
}
```

### Integration Tests - Cloud Sync

```kotlin
class CloudSyncTest {
    private lateinit var repository: ProjectRepository
    private lateinit var remoteDataSource: RemoteProjectDataSource

    @Test
    fun `sync uploads local changes to cloud`() = runTest {
        val project = Project(name = "Local Project")
        repository.saveProject(project)

        repository.syncToCloud()

        val cloudProjects = remoteDataSource.getAllProjects()
        assertTrue(cloudProjects.any { it.id == project.id })
    }

    @Test
    fun `sync downloads cloud changes to local`() = runTest {
        val cloudProject = Project(name = "Cloud Project")
        remoteDataSource.uploadProject(cloudProject)

        repository.syncFromCloud()

        val local = repository.getProjectById(cloudProject.id).getOrNull()
        assertEquals(cloudProject, local)
    }
}
```

## Security Considerations

### 1. Local Storage
- **Android:** Internal storage (app-private)
- **iOS:** Documents directory (app-sandboxed)
- イベントストア（SQLite）もアプリ内ストレージ

### 2. Authentication & Authorization
- **認証:** Firebase Authentication / OAuth2
  - セキュアなトークン管理
  - 自動トークン更新
- **認可:** プロジェクトレベルの権限チェック
  - すべてのAPI呼び出しでユーザーIDを検証
  - サーバー側でCollaboratorロールを強制

```kotlin
// サーバー側の権限チェック例
suspend fun checkPermission(userId: UserId, projectId: String, requiredRole: CollaboratorRole) {
    val project = getProject(projectId)
    val collaborator = project.collaborators.find { it.userId == userId }

    when (requiredRole) {
        CollaboratorRole.VIEWER -> {
            require(collaborator != null) { "User not authorized to view this project" }
        }
        CollaboratorRole.EDITOR -> {
            require(collaborator?.role in listOf(CollaboratorRole.EDITOR, CollaboratorRole.OWNER)) {
                "User not authorized to edit this project"
            }
        }
        CollaboratorRole.OWNER -> {
            require(project.ownerId == userId) { "User is not the owner" }
        }
    }
}
```

### 3. Data Transmission Security
- **HTTPS:** すべてのAPI通信
- **WebSocket Secure (WSS):** リアルタイム通信
- **JWT トークン:** API認証
  - 短い有効期限（1時間）
  - Refresh token の実装

### 4. Data Privacy
- **個人情報:** ユーザー名、メールアドレスの保護
- **プロジェクトデータ:** デフォルトでプライベート
- **アクセスログ:** 誰がいつアクセスしたかの記録

### 5. イベントの改ざん防止
- **Server-side validation:** すべてのイベントをサーバーで検証
- **Sequence numberの管理:** サーバーが割り当て（クライアントは提案のみ）
- **Signature（オプション）:** イベントに署名を追加して改ざん検出

```kotlin
@Serializable
data class SignedEvent(
    val event: ProjectEvent,
    val signature: String,  // HMAC-SHA256
    val timestamp: Instant,
)

fun verifyEventSignature(signedEvent: SignedEvent, secretKey: String): Boolean {
    val payload = Json.encodeToString(signedEvent.event)
    val expectedSignature = hmacSha256(payload, secretKey)
    return expectedSignature == signedEvent.signature
}
```

### 6. Rate Limiting
- **API呼び出し制限:** ユーザーごとに1分あたり100リクエスト
- **イベント送信制限:** プロジェクトごとに1秒あたり10イベント
- **DDoS対策:** Cloudflare / AWS WAF

### 7. Sensitive Data Handling
- **layoutUrl:** 外部リソースのURLのため暗号化不要
- **ユーザー作成データ:** 基本的に非機密
- **環境変数:** API keys, secrets は環境変数で管理

### 8. バックアップ暗号化（オプション）
将来的にエンタープライズ向け機能として:
- End-to-end encryption for cloud backup
- ユーザー管理の暗号化キー
- ゼロ知識アーキテクチャ（サーバーが復号化キーを持たない）

## Performance Considerations

### 1. Large Projects
- **遅延読み込み:** FloorPlanを個別にロード可能にする
- **Pagination:** プロジェクト一覧表示時
- **仮想スクロール:** 大量の家具を表示する際

### 2. Event Store Optimization
- **Snapshotting:** 100イベントごとにスナップショット作成
- **Event compaction:** 古いイベントをまとめる
  - 例: 100回のFurnitureMovedを1つに集約
- **Index:** projectId, sequenceNumber にインデックス

```sql
CREATE INDEX idx_events_project_sequence
ON events(project_id, sequence_number);

CREATE INDEX idx_events_timestamp
ON events(timestamp);
```

### 3. Serialization
- **Protocol Buffers（オプション）:** JSONより高速・コンパクト
- **大きなPolygonデータの最適化:**
  - 座標の精度を制限（小数点2桁まで）
  - 座標圧縮アルゴリズム
- **画像データ:** 別ファイルで管理 (layoutUrl)、CDN経由

### 4. Memory Management
- **LRU cache:** 最近アクセスしたプロジェクト
- **Weak references:** キャッシュデータ
- **イベントストリームのメモリ使用量:**
  - Flow を使った遅延評価
  - 一度に全イベントをメモリに載せない

```kotlin
// 悪い例: 全イベントをメモリに載せる
val events = eventStore.getEvents(projectId).getOrThrow()
val project = projectionEngine.project(events)

// 良い例: ストリーミング処理
suspend fun projectFromEventStream(projectId: String): Project {
    var state: Project? = null

    eventStore.observeEvents(projectId)
        .take(1000)  // 制限
        .collect { event ->
            state = projectionEngine.applyEvent(state, event)
        }

    return state!!
}
```

### 5. Network Optimization
- **Delta sync:** 差分のみ送信
- **Batch requests:** 複数のイベントを1リクエストにまとめる
- **Compression:** gzip/brotli
- **CDN:** 静的リソース（画像、テンプレート）

### 6. Real-time Performance
- **Debouncing:** 連続する変更を間引く
  - 例: ドラッグ中は100msごとに1回のみイベント送信
- **Local-first:** UIはローカル状態で即座に更新
- **WebSocket connection pooling:** 複数プロジェクト間で接続を共有

```kotlin
// Debouncing example
val furnitureMoveEvents = MutableSharedFlow<FurnitureMoved>()

furnitureMoveEvents
    .debounce(100.milliseconds)
    .collect { event ->
        syncEngine.pushEvent(event)
    }
```

### 7. Database Query Optimization
- **N+1 問題の回避:** JOIN を活用
- **Projection の最適化:** 必要なフィールドのみ取得
- **Connection pooling:** データベース接続の再利用

### 8. Monitoring & Profiling
- **メトリクス収集:**
  - イベント処理時間
  - 同期レイテンシ
  - メモリ使用量
- **Slow query detection:** 500ms 以上かかるクエリをログ
- **Client-side APM:** Sentry, Firebase Performance

## References

### Kotlin Multiplatform & Serialization
- [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization)
- [Kotlin Multiplatform file storage](https://kotlinlang.org/docs/multiplatform-mobile-concurrent-mutability.html)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Ktor Client](https://ktor.io/docs/getting-started-ktor-client.html)

### Event Sourcing & CQRS
- [Event Sourcing Pattern - Martin Fowler](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event Store Design](https://www.eventstore.com/event-sourcing)

### Collaborative Editing
- [Operational Transformation](https://en.wikipedia.org/wiki/Operational_transformation)
- [Conflict-free Replicated Data Types (CRDTs)](https://crdt.tech/)
- [Google Docs' Operational Transformation](https://drive.googleblog.com/2010/09/whats-different-about-new-google-docs.html)
- [Figma's Multiplayer Technology](https://www.figma.com/blog/how-figmas-multiplayer-technology-works/)
- [Local-first Software](https://www.inkandswitch.com/local-first/)

### Backend Technologies
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Supabase](https://supabase.com/docs)
- [WebSocket Protocol](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)

### Performance & Optimization
- [Database Indexing Best Practices](https://use-the-index-luke.com/)
- [Protocol Buffers vs JSON](https://auth0.com/blog/beating-json-performance-with-protobuf/)
- [Debouncing and Throttling](https://css-tricks.com/debouncing-throttling-explained-examples/)

---

## Summary

このドキュメントは、Puzzroomアプリケーションの**データ永続化**に焦点を当てた設計を提供します。

### 主要な設計判断

1. **Storage Strategy**
   - Phase 1: ファイルベース（JSON）- シンプル、KMP対応
   - Phase 2: SQLite - クエリ最適化、リレーショナル
   - Phase 3: クラウド同期 - Firebase / Supabase

2. **Repository Pattern**
   - データソースの抽象化
   - ローカル / クラウドの切り替え可能
   - テスト容易性

3. **kotlinx.serialization**
   - 型安全なシリアライゼーション
   - KMP完全対応
   - 将来の拡張性（Protocol Buffers移行可能）

4. **Local-first Architecture**
   - ローカルストレージを主とする
   - オフライン動作保証
   - クラウドは補助的な同期先

### スコープの明確化

**このドキュメントでカバー:**
- ✅ ローカルおよびクラウドへのデータ保存・読み込み
- ✅ シリアライゼーション
- ✅ CRUD操作
- ✅ キャッシング
- ✅ マイグレーション

**このドキュメントでカバーしない（別ドキュメント参照）:**
- ❌ Event Sourcing アーキテクチャ → [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)
- ❌ リアルタイム共同編集 → [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)
- ❌ WebSocket通信 → [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)
- ❌ 競合解決とロック機構 → [`RealtimeCollaborationGuide.md`](./RealtimeCollaborationGuide.md)

### 次のステップ

1. **Phase 1 の実装（MVP）**
   - `FileStorage` の expect/actual 実装
   - `LocalProjectDataSource` 実装
   - `ProjectRepository` 実装
   - 基本的なCRUD操作のテスト

2. **Phase 2 の準備**
   - SQLDelight のセットアップ
   - データベーススキーマ設計
   - マイグレーション戦略の詳細化

3. **Phase 3-4 の計画**
   - バックエンド選定（Firebase / Supabase）
   - 認証システムの設計
   - クラウド同期の仕様策定
