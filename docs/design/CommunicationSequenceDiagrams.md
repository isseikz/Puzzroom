# 通信シーケンス図

このドキュメントは、Puzzroomアプリケーションにおけるサーバー、システム、クライアントデバイス間の通信内容を示すシーケンス図を提供します。

## 目次

1. [概要](#概要)
2. [アーキテクチャの段階的進化](#アーキテクチャの段階的進化)
3. [Phase 1: ローカルのみ（現在の実装）](#phase-1-ローカルのみ現在の実装)
4. [Phase 2-4: クラウド同期](#phase-2-4-クラウド同期)
5. [Phase 5: リアルタイム共同編集](#phase-5-リアルタイム共同編集)
6. [補足図](#補足図)

## 概要

Puzzroomアプリケーションは、段階的にアーキテクチャを進化させる設計となっています。このドキュメントでは、各フェーズにおけるコンポーネント間の通信パターンをシーケンス図で示します。

### システムコンポーネント

- **クライアントデバイス**: Android、iOS、Web、Desktop上で動作するPuzzroomアプリケーション
- **アプリケーション層**: UI、ViewModel、UseCase
- **データ層**: Repository、DataSource、Storage
- **バックエンドサーバー**: Firebase/Supabaseまたはカスタムバックエンド（Phase 3以降）
- **データベース**: Firestore/PostgreSQL（Phase 3以降）

## アーキテクチャの段階的進化

```mermaid
graph LR
    A[Phase 1: Local Only] --> B[Phase 2: Event Sourcing Foundation]
    B --> C[Phase 3: Backend & Auth]
    C --> D[Phase 4: Cloud Sync]
    D --> E[Phase 5: Real-time Collaboration]
    E --> F[Phase 6: Advanced Features]

    style A fill:#D4856A
    style E fill:#E8B4A0
```

## Phase 1: ローカルのみ（現在の実装）

### 1.1 プロジェクト一覧読み込み

ユーザーがアプリを起動し、プロジェクト一覧を表示する際のシーケンス。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as ProjectListPage
    participant VM as ProjectViewModel
    participant UC as ListProjectsUseCase
    participant Repo as ProjectRepository
    participant DS as LocalDataSource
    participant FS as FileStorage<br/>(Android/iOS/Web/Desktop)

    User->>UI: アプリ起動
    UI->>VM: loadProjects()
    VM->>UC: invoke()
    UC->>Repo: getAllProjects()
    Repo->>DS: getAllProjects()
    DS->>FS: listProjects()
    
    Note over FS: ローカルファイルシステム<br/>から.jsonファイル一覧を取得
    
    FS-->>DS: List<String> (ファイル名)
    
    loop 各プロジェクトファイル
        DS->>FS: readProject(fileName)
        FS-->>DS: Project (JSON deserialized)
    end
    
    DS-->>Repo: List<Project>
    Repo-->>UC: Result<List<Project>>
    UC-->>VM: Result<List<Project>>
    VM-->>UI: UiState.ProjectList
    UI-->>User: プロジェクト一覧表示
```

**特徴:**
- すべての処理がローカルで完結
- サーバー通信なし
- 各プラットフォームでFileStorageの実装が異なる（expect/actual）

### 1.2 プロジェクトの作成と自動保存

ユーザーが新しいプロジェクトを作成し、編集する際のシーケンス。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as EditProjectPage
    participant VM as ProjectViewModel
    participant UC as SaveProjectUseCase
    participant Repo as ProjectRepository
    participant DS as LocalDataSource
    participant FS as FileStorage

    User->>UI: 新規プロジェクト作成
    UI->>VM: createProject(name)
    Note over VM: Project オブジェクト生成<br/>(UUID自動割当)
    
    User->>UI: プロジェクトを編集<br/>(部屋追加、家具配置等)
    UI->>VM: updateProject(project)
    
    Note over VM: Debounce 500ms<br/>連続する変更を間引く
    
    VM->>UC: invoke(project)
    UC->>Repo: saveProject(project)
    Repo->>DS: insertProject(project) or<br/>updateProject(project)
    DS->>FS: writeProject(project, fileName)
    
    Note over FS: JSON serialization<br/>ファイルシステムに書き込み<br/>(app_data/projects/<id>.json)
    
    FS-->>DS: Success
    DS-->>Repo: Success
    Repo-->>UC: Result.success()
    UC-->>VM: Result.success()
    VM-->>UI: SaveState.Saved
    UI-->>User: "保存済み" インジケーター表示
```

**特徴:**
- 自動保存（Debounce 500ms）
- kotlinx.serializationによるJSON変換
- プラットフォーム固有のファイルシステムAPI使用

### 1.3 プロジェクトの読み込みと編集

既存プロジェクトを開いて編集する際のシーケンス。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant ListUI as ProjectListPage
    participant EditUI as EditProjectPage
    participant VM as ProjectViewModel
    participant UC as LoadProjectUseCase
    participant Repo as ProjectRepository
    participant DS as LocalDataSource
    participant FS as FileStorage

    User->>ListUI: プロジェクトをクリック
    ListUI->>VM: openProject(projectId)
    VM->>UC: invoke(projectId)
    UC->>Repo: getProjectById(projectId)
    Repo->>DS: getProjectById(projectId)
    DS->>FS: readProject(projectId)
    
    Note over FS: ファイル読み込み<br/>JSONデシリアライゼーション
    
    FS-->>DS: Project
    DS-->>Repo: Project
    Repo-->>UC: Result<Project>
    UC-->>VM: Result<Project>
    VM-->>EditUI: UiState.EditingProject
    EditUI-->>User: プロジェクト編集画面表示
    
    Note over User,EditUI: ユーザーが編集を開始
    
    User->>EditUI: 家具を移動
    EditUI->>VM: updateProject(modifiedProject)
    Note over VM: 自動保存トリガー<br/>(1.2と同様)
```

### 1.4 プロジェクトの削除

プロジェクトを削除する際のシーケンス。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as ProjectListPage
    participant VM as ProjectViewModel
    participant UC as DeleteProjectUseCase
    participant Repo as ProjectRepository
    participant DS as LocalDataSource
    participant FS as FileStorage

    User->>UI: 削除ボタンをクリック
    UI->>UI: 確認ダイアログ表示
    User->>UI: 削除を確認
    UI->>VM: deleteProject(projectId)
    VM->>UC: invoke(projectId)
    UC->>Repo: deleteProject(projectId)
    Repo->>DS: deleteProject(projectId)
    DS->>FS: deleteProject(projectId)
    
    Note over FS: ファイル削除<br/>(app_data/projects/<id>.json)
    
    FS-->>DS: Success
    DS-->>Repo: Success
    Repo-->>UC: Result.success()
    UC-->>VM: Result.success()
    VM->>VM: loadProjects()
    Note over VM: プロジェクト一覧を再読み込み
    VM-->>UI: UiState.ProjectList (更新後)
    UI-->>User: プロジェクト一覧表示<br/>(削除済み項目なし)
```

## Phase 2-4: クラウド同期

Phase 3でバックエンドと認証を導入し、Phase 4で複数デバイス間の同期を実現します。

### 2.1 ユーザー認証

アプリ起動時の認証フロー。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as LoginPage
    participant AuthVM as AuthViewModel
    participant Firebase as Firebase Auth
    
    User->>UI: アプリ起動
    
    alt 既にログイン済み
        UI->>AuthVM: checkAuthState()
        AuthVM->>Firebase: getCurrentUser()
        Firebase-->>AuthVM: User (token valid)
        AuthVM-->>UI: AuthState.Authenticated
        UI-->>User: メイン画面へ遷移
    else 未ログイン
        UI-->>User: ログイン画面表示
        User->>UI: Google Sign-inボタンをクリック
        UI->>AuthVM: signInWithGoogle()
        AuthVM->>Firebase: signInWithCredential()
        Firebase-->>AuthVM: User + Auth Token
        AuthVM-->>UI: AuthState.Authenticated
        UI-->>User: メイン画面へ遷移
    end
```

### 2.2 プロジェクト一覧の同期（クラウドからの読み込み）

認証後、クラウドからプロジェクト一覧を同期する。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as ProjectListPage
    participant VM as ProjectViewModel
    participant UC as ListProjectsUseCase
    participant Repo as ProjectRepository
    participant LocalDS as LocalDataSource
    participant RemoteDS as RemoteDataSource
    participant Backend as Backend API<br/>(Firebase/Supabase)
    participant DB as Database<br/>(Firestore/PostgreSQL)

    User->>UI: プロジェクト一覧を開く
    UI->>VM: loadProjects()
    VM->>UC: invoke()
    UC->>Repo: getAllProjects()
    
    par ローカルデータを即座に表示
        Repo->>LocalDS: getAllProjects()
        LocalDS-->>Repo: List<Project> (cached)
        Repo-->>UC: Result<List<Project>>
        UC-->>VM: Result (ローカル)
        VM-->>UI: UiState.ProjectList (ローカル)
        UI-->>User: ローカルデータ表示<br/>(即座に表示)
    and バックグラウンドでクラウドから同期
        Repo->>RemoteDS: getAllProjects()
        RemoteDS->>Backend: GET /api/projects<br/>Header: Authorization: Bearer <token>
        Backend->>Backend: トークン検証
        Backend->>DB: SELECT * FROM projects<br/>WHERE owner_id = <user_id>
        DB-->>Backend: List<ProjectData>
        Backend-->>RemoteDS: 200 OK<br/>Body: List<Project>
        RemoteDS-->>Repo: List<Project> (cloud)
        
        Note over Repo: ローカルとクラウドの差分を検出<br/>- 新規プロジェクト<br/>- 更新されたプロジェクト<br/>- 削除されたプロジェクト
        
        Repo->>LocalDS: 差分を適用<br/>(insert/update/delete)
        LocalDS-->>Repo: Success
        Repo-->>UC: Result<List<Project>> (merged)
        UC-->>VM: Result (最新)
        VM-->>UI: UiState.ProjectList (最新)
        UI-->>User: 最新のプロジェクト一覧表示
    end
```

**特徴:**
- **Local-first**: まずローカルデータを表示し、UX向上
- **バックグラウンド同期**: クラウドから最新データを取得
- **認証トークン**: すべてのAPI呼び出しにBearerトークンを含める

### 2.3 プロジェクトの作成とクラウドへの同期

新規プロジェクトを作成し、クラウドに同期する。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant UI as EditProjectPage
    participant VM as ProjectViewModel
    participant UC as SaveProjectUseCase
    participant Repo as ProjectRepository
    participant LocalDS as LocalDataSource
    participant RemoteDS as RemoteDataSource
    participant Backend as Backend API
    participant DB as Database

    User->>UI: 新規プロジェクト作成
    UI->>VM: createProject(name)
    User->>UI: プロジェクトを編集
    UI->>VM: updateProject(project)
    
    Note over VM: Debounce 500ms
    
    VM->>UC: invoke(project)
    UC->>Repo: saveProject(project)
    
    Repo->>LocalDS: saveProject(project)
    Note over LocalDS: ローカルに即座に保存<br/>(オフライン対応)
    LocalDS-->>Repo: Success
    Repo-->>UC: Result.success()
    UC-->>VM: Result.success()
    VM-->>UI: SaveState.Saved
    
    Note over Repo: バックグラウンドで<br/>クラウドに同期
    
    Repo->>RemoteDS: uploadProject(project)
    RemoteDS->>Backend: POST /api/projects<br/>Authorization: Bearer <token><br/>Body: Project JSON
    Backend->>Backend: トークン検証<br/>ユーザーID取得
    Backend->>DB: INSERT INTO projects<br/>(id, owner_id, name, data, ...)
    DB-->>Backend: Success
    Backend-->>RemoteDS: 201 Created
    RemoteDS-->>Repo: Success
    
    Note over Repo: 同期完了<br/>ローカルの同期フラグ更新
```

**特徴:**
- **即座にローカル保存**: オフラインでも動作
- **バックグラウンド同期**: ネットワークが利用可能になったら自動的にアップロード
- **Optimistic UI**: UIは保存完了を即座に反映

### 2.4 競合解決（単一ユーザー・複数デバイス）

同じユーザーが複数デバイスで編集した場合の競合解決。

```mermaid
sequenceDiagram
    participant Device1 as デバイス1<br/>(Android)
    participant Device2 as デバイス2<br/>(iOS)
    participant Backend as Backend API
    participant DB as Database

    Note over Device1: ユーザーがDevice1で編集
    Device1->>Device1: updateProject(project)<br/>version: 1 -> 2
    Device1->>Backend: PUT /api/projects/{id}<br/>version: 2<br/>If-Match: "1"
    Backend->>DB: SELECT version FROM projects<br/>WHERE id = {id}
    DB-->>Backend: version = 1
    Backend->>DB: UPDATE projects SET version = 2<br/>WHERE id = {id} AND version = 1
    DB-->>Backend: Success (1 row updated)
    Backend-->>Device1: 200 OK<br/>version: 2
    
    Note over Device2: 同時にDevice2で編集<br/>(古いバージョンベース)
    Device2->>Device2: updateProject(project)<br/>version: 1 -> 2 (conflicting)
    Device2->>Backend: PUT /api/projects/{id}<br/>version: 2<br/>If-Match: "1"
    Backend->>DB: SELECT version FROM projects<br/>WHERE id = {id}
    DB-->>Backend: version = 2 (already updated!)
    Backend-->>Device2: 409 Conflict<br/>message: "Version mismatch"
    
    Device2->>Backend: GET /api/projects/{id}
    Backend->>DB: SELECT * FROM projects<br/>WHERE id = {id}
    DB-->>Backend: Project (version: 2)
    Backend-->>Device2: 200 OK<br/>Project (latest)
    
    Note over Device2: ローカルの変更と<br/>サーバーの最新版をマージ
    Device2->>Device2: mergeChanges(local, remote)
    Device2->>Backend: PUT /api/projects/{id}<br/>version: 3 (merged)<br/>If-Match: "2"
    Backend->>DB: UPDATE projects<br/>WHERE id = {id} AND version = 2
    DB-->>Backend: Success
    Backend-->>Device2: 200 OK
```

**特徴:**
- **楽観的ロック**: versionフィールドでバージョン管理
- **If-Match ヘッダー**: 競合検出
- **マージ戦略**: Last-Write-Wins または ユーザーによる手動マージ

## Phase 5: リアルタイム共同編集

複数ユーザーが同時に同じプロジェクトを編集できるリアルタイム共同編集。

### 3.1 WebSocket接続の確立

プロジェクトを開いた際にWebSocket接続を確立する。

```mermaid
sequenceDiagram
    participant User1 as ユーザー1
    participant Client1 as クライアント1
    participant WS as WebSocket Server
    participant Backend as Backend API
    participant DB as Database
    participant Client2 as クライアント2
    participant User2 as ユーザー2

    User1->>Client1: プロジェクトを開く
    Client1->>Backend: GET /api/projects/{id}
    Backend->>DB: SELECT * FROM projects
    DB-->>Backend: Project data
    Backend-->>Client1: 200 OK, Project
    
    Client1->>WS: WebSocket接続<br/>ws://server/projects/{id}?token=<jwt>
    WS->>WS: JWT検証
    WS->>DB: SELECT collaborators<br/>WHERE project_id = {id}<br/>AND user_id = <user>
    DB-->>WS: Collaborator (role: EDITOR)
    WS-->>Client1: Connection established
    
    WS->>Client1: PresenceUpdate<br/>{users: [User1]}
    Client1-->>User1: "User1が参加しました"
    
    Note over User2: User2が同じプロジェクトを開く
    
    User2->>Client2: プロジェクトを開く
    Client2->>WS: WebSocket接続
    WS-->>Client2: Connection established
    
    WS->>Client1: PresenceUpdate<br/>{users: [User1, User2]}
    WS->>Client2: PresenceUpdate<br/>{users: [User1, User2]}
    
    Client1-->>User1: "User2が参加しました"
    Client2-->>User2: "User1がすでに参加しています"
```

**特徴:**
- **JWT認証**: WebSocket接続時にトークン検証
- **Presence通知**: 誰がオンラインかをリアルタイムで共有
- **プロジェクトごとのチャンネル**: 各プロジェクトで独立したWebSocketチャンネル

### 3.2 リアルタイム編集 - Entity Locking方式

推奨方式：同一エンティティを同時編集できるのは1人のみ。

```mermaid
sequenceDiagram
    participant User1 as ユーザー1
    participant Client1 as クライアント1
    participant WS as WebSocket Server
    participant EventStore as Event Store<br/>(Database)
    participant Client2 as クライアント2
    participant User2 as ユーザー2

    Note over User1: User1が家具Aを選択
    User1->>Client1: 家具Aをクリック
    Client1->>WS: LockRequest<br/>{entityId: "furniture-A"}
    WS->>WS: ロック状態を確認
    
    alt ロック可能
        WS->>WS: ロック取得<br/>(user: User1, entity: furniture-A)<br/>timeout: 30s
        WS-->>Client1: LockAcquired<br/>{entityId: "furniture-A"}
        WS->>Client2: EntityLocked<br/>{entityId: "furniture-A",<br/>lockedBy: "User1"}
        Client1-->>User1: 家具Aの編集可能<br/>(ハイライト表示)
        Client2-->>User2: 家具Aがロック中<br/>(User1が編集中)
    else 既にロック済み
        WS-->>Client1: LockDenied<br/>{entityId: "furniture-A",<br/>lockedBy: "User2"}
        Client1-->>User1: "User2が編集中です"
    end
    
    Note over User1: User1が家具Aを移動
    User1->>Client1: 家具Aをドラッグ
    Client1->>Client1: Optimistic UI update<br/>(即座にローカル反映)
    Client1->>WS: Event: FurnitureMoved<br/>{furnitureId: "furniture-A",<br/>position: {x: 100, y: 200}}
    
    WS->>WS: ロック保有者の確認<br/>(User1 = OK)
    WS->>EventStore: イベントを保存<br/>(sequence number割当)
    EventStore-->>WS: Success<br/>sequence: 42
    
    WS->>Client1: EventAck<br/>{sequence: 42, eventId: "..."}
    WS->>Client2: Event: FurnitureMoved<br/>{sequence: 42,<br/>furnitureId: "furniture-A",<br/>position: {x: 100, y: 200}}
    
    Client2->>Client2: Projection適用
    Client2-->>User2: 家具Aが移動<br/>(リアルタイム反映)
    
    Note over User1: User1が編集完了
    User1->>Client1: 家具Aの選択を解除
    Client1->>WS: ReleaseLock<br/>{entityId: "furniture-A"}
    WS->>WS: ロック解放
    WS->>Client2: EntityUnlocked<br/>{entityId: "furniture-A"}
    Client2-->>User2: 家具Aが編集可能に
```

**特徴:**
- **Entity Locking**: 同一エンティティへの同時編集を防止
- **Optimistic UI**: ローカルで即座に反映、サーバー確認は非同期
- **自動タイムアウト**: 30秒後にロック自動解放（接続断絶時の対策）
- **Event Sourcing**: すべての変更をイベントとして記録

### 3.3 リアルタイム編集 - カーソル位置共有

他のユーザーのカーソル位置をリアルタイムで表示。

```mermaid
sequenceDiagram
    participant User1 as ユーザー1
    participant Client1 as クライアント1
    participant WS as WebSocket Server
    participant Client2 as クライアント2
    participant User2 as ユーザー2

    loop カーソル移動中（Throttle: 100ms）
        User1->>Client1: マウス移動
        Client1->>Client1: Throttle (100ms)
        Client1->>WS: CursorMoved<br/>{userId: "User1",<br/>position: {x: 250, y: 300}}
        WS->>Client2: CursorMoved<br/>{userId: "User1",<br/>position: {x: 250, y: 300}}
        Client2-->>User2: User1のカーソル表示<br/>(ラベル付き)
    end
    
    Note over User2: User2もマウスを動かす
    
    loop カーソル移動中
        User2->>Client2: マウス移動
        Client2->>WS: CursorMoved<br/>{userId: "User2",<br/>position: {x: 400, y: 150}}
        WS->>Client1: CursorMoved<br/>{userId: "User2",<br/>position: {x: 400, y: 150}}
        Client1-->>User1: User2のカーソル表示
    end
```

**特徴:**
- **Throttle**: 100msごとに送信（帯域幅節約）
- **ユーザー識別**: カーソルごとに色・ラベル表示
- **低遅延**: WebSocketによる双方向通信

### 3.4 接続断絶とリカバリー

ネットワーク切断時のリカバリー処理。

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant Client as クライアント
    participant WS as WebSocket Server
    participant EventStore as Event Store

    Note over Client,WS: 接続中、正常に動作
    
    Client->>WS: Event: RoomAdded<br/>{sequence: 10}
    WS->>EventStore: 保存
    EventStore-->>WS: Success
    WS-->>Client: EventAck {sequence: 10}
    
    Note over Client,WS: ネットワーク切断発生
    
    Client->>Client: 接続エラー検出
    Client-->>User: "オフラインモード"<br/>インジケーター表示
    
    Note over Client: ローカルで編集継続<br/>(Event Queue に蓄積)
    
    User->>Client: 家具を追加
    Client->>Client: ローカルに保存<br/>Event Queue: [<br/>  FurnitureAdded {local_seq: 1}<br/>]
    
    Note over Client,WS: ネットワーク復旧
    
    Client->>WS: WebSocket再接続<br/>query: last_seq=10
    WS-->>Client: Connection established
    
    WS->>EventStore: SELECT * FROM events<br/>WHERE project_id = {id}<br/>AND sequence > 10
    EventStore-->>WS: List<Event> (11-15)
    WS->>Client: MissedEvents<br/>{events: [seq 11-15]}
    
    Client->>Client: ローカルイベントと<br/>サーバーイベントをマージ
    
    Note over Client: 競合チェック:<br/>- 同じエンティティ編集なし<br/>- マージ可能
    
    Client->>WS: Event: FurnitureAdded<br/>(キューからリプレイ)
    WS->>EventStore: 保存
    EventStore-->>WS: Success (sequence: 16)
    WS-->>Client: EventAck {sequence: 16}
    
    Client-->>User: "オンラインに戻りました"
```

**特徴:**
- **自動再接続**: 指数バックオフで再接続試行
- **Event Queue**: オフライン中のイベントをローカルに保持
- **Catch-up**: 再接続時、欠落したイベントを取得
- **Conflict Detection**: マージ時に競合を検出

### 3.5 複数ユーザーの同時編集（成功ケース）

異なるエンティティを編集する場合、問題なく同時編集可能。

```mermaid
sequenceDiagram
    participant User1 as ユーザー1
    participant Client1 as クライアント1
    participant WS as WebSocket Server
    participant EventStore as Event Store
    participant Client2 as クライアント2
    participant User2 as ユーザー2

    par User1が家具Aを編集
        User1->>Client1: 家具Aを移動
        Client1->>WS: LockRequest {entityId: "furniture-A"}
        WS-->>Client1: LockAcquired
        Client1->>WS: Event: FurnitureMoved<br/>{furnitureId: "furniture-A", ...}
        WS->>EventStore: 保存 (sequence: 20)
        WS->>Client2: Event broadcast<br/>{sequence: 20, ...}
        Client2-->>User2: 家具Aが移動 (反映)
    and User2が家具Bを編集
        User2->>Client2: 家具Bを回転
        Client2->>WS: LockRequest {entityId: "furniture-B"}
        WS-->>Client2: LockAcquired
        Client2->>WS: Event: FurnitureRotated<br/>{furnitureId: "furniture-B", ...}
        WS->>EventStore: 保存 (sequence: 21)
        WS->>Client1: Event broadcast<br/>{sequence: 21, ...}
        Client1-->>User1: 家具Bが回転 (反映)
    end
    
    Note over Client1,Client2: 両方のクライアントが<br/>最新状態に同期
```

**特徴:**
- **並列編集**: 異なるエンティティなら同時編集可能
- **イベント順序保証**: Sequence numberで順序を保証
- **即座の反映**: Optimistic UI + サーバー確認

## 補足図

### システムアーキテクチャ全体図

各フェーズでのアーキテクチャの進化を示します。

```mermaid
graph TB
    subgraph "Phase 1: Local Only"
        A1[Client Device]
        A2[Local FileStorage]
        A1 <-->|read/write| A2
    end
    
    subgraph "Phase 3-4: Cloud Sync"
        B1[Client Device 1]
        B2[Client Device 2]
        B3[Backend API]
        B4[(Database)]
        B5[Local Storage 1]
        B6[Local Storage 2]
        
        B1 <-->|sync| B3
        B2 <-->|sync| B3
        B3 <--> B4
        B1 <--> B5
        B2 <--> B6
    end
    
    subgraph "Phase 5: Real-time Collaboration"
        C1[Client 1]
        C2[Client 2]
        C3[Client 3]
        C4[WebSocket Server]
        C5[Backend API]
        C6[(Event Store)]
        C7[(Database)]
        
        C1 <-->|WebSocket| C4
        C2 <-->|WebSocket| C4
        C3 <-->|WebSocket| C4
        C4 <--> C6
        C1 <-->|REST API| C5
        C2 <-->|REST API| C5
        C3 <-->|REST API| C5
        C5 <--> C7
    end
    
    style A1 fill:#D4856A
    style B1 fill:#E8B4A0
    style B2 fill:#E8B4A0
    style C1 fill:#E5D4C1
    style C2 fill:#E5D4C1
    style C3 fill:#E5D4C1
```

### データフロー図（Phase 5）

```mermaid
graph LR
    User[ユーザー操作] --> UI[UI Layer]
    UI --> VM[ViewModel]
    VM --> UC[UseCase]
    UC --> Repo[Repository]
    
    Repo --> LocalDS[Local DataSource]
    Repo --> RemoteDS[Remote DataSource]
    Repo --> WSClient[WebSocket Client]
    
    LocalDS --> LS[(Local Storage)]
    RemoteDS --> API[Backend REST API]
    WSClient --> WSS[WebSocket Server]
    
    API --> DB[(Database)]
    WSS --> EventStore[(Event Store)]
    
    WSS -.broadcast.-> WSClient
    EventStore --> Projection[Projection Engine]
    Projection --> Repo
    
    style User fill:#D4856A
    style UI fill:#E8B4A0
    style Repo fill:#E5D4C1
```

### Event Sourcingのデータフロー

```mermaid
sequenceDiagram
    participant User as ユーザー操作
    participant UI as UI
    participant VM as ViewModel
    participant Repo as Repository
    participant ES as Event Store
    participant PE as Projection Engine
    participant State as Current State

    User->>UI: 家具を移動
    UI->>VM: updateFurniture(...)
    VM->>Repo: applyEvent(FurnitureMoved)
    
    par Append to Event Store
        Repo->>ES: append(event)
        ES-->>Repo: Success (sequence: N)
    and Update Current State
        Repo->>PE: project(event)
        PE->>State: apply(event)
        State-->>PE: new state
        PE-->>Repo: updated state
    end
    
    Repo-->>VM: Result.success()
    VM-->>UI: UiState updated
    UI-->>User: 画面更新
    
    Note over ES: イベントは不変<br/>append-onlyログ
    Note over State: 現在の状態<br/>(イベントから再構築可能)
```

## まとめ

### 各フェーズの特徴比較

| フェーズ | 通信方式 | データストレージ | 同期 | 共同編集 |
|---------|---------|---------------|-----|---------|
| **Phase 1** | なし（ローカルのみ） | Local FileStorage | なし | 不可 |
| **Phase 2** | イベントベースの設計のみ | Local + Event Store | なし | 不可 |
| **Phase 3-4** | REST API (HTTPS) | Local + Cloud (Firestore/PostgreSQL) | バックグラウンド同期 | 単一ユーザーのみ |
| **Phase 5** | WebSocket (WSS) + REST API | Local + Cloud + Event Store | リアルタイム | 複数ユーザー可能 |

### 技術スタック

- **Phase 1**: kotlinx.serialization, expect/actual FileStorage
- **Phase 2**: Event Sourcing設計、SQLDelight (Event Store)
- **Phase 3-4**: Firebase Auth, Firestore/Supabase, Ktor Client
- **Phase 5**: WebSocket (Ktor/Firebase Realtime), Entity Locking

### セキュリティ

すべてのフェーズで以下を考慮：
- **Phase 3以降**: JWT認証、HTTPS/WSS暗号化通信
- **Authorization**: ユーザーごとのプロジェクトアクセス制御
- **Event Validation**: サーバー側でイベントの妥当性検証
- **Rate Limiting**: API呼び出し制限

## 参考資料

- [Data Persistence Design](./DataPersistence.md) - 詳細な永続化設計
- [Use Case Analysis](./UseCaseAnalysis.md) - ユースケース分析
- [Atomic Design Architecture](./AtomicDesignArchitecture.md) - UI設計

---

**ドキュメントバージョン**: 1.0  
**最終更新日**: 2025-11-02
