# 家具テンプレート永続化実装

## 概要

カスタム家具テンプレートを永続化し、プロジェクト間で共有できるようにしました。

## アーキテクチャ

実装は既存のプロジェクト永続化と同じパターンに従っています：

```
UI Layer (ViewModel)
    ↓
Domain Layer (Use Cases)
    ↓
Data Layer (Repository)
    ↓
Data Source Layer
    ↓
Storage Layer (FileStorage)
```

### 主要コンポーネント

#### 1. Storage Layer

**ファイル**: `FileStorage.kt` および各プラットフォーム実装

新しいメソッド:
- `writeFurnitureTemplate(template, fileName)` - 家具テンプレートをJSON形式で保存
- `readFurnitureTemplate(fileName)` - 家具テンプレートを読み込み
- `deleteFurnitureTemplate(fileName)` - 家具テンプレートを削除
- `listFurnitureTemplates()` - すべての家具テンプレートファイル名を取得

**保存場所**:
- Android: `/data/data/[package]/files/furniture_templates/`
- iOS: `Documents/furniture_templates/`
- Web: LocalStorage (キー: `puzzroom_furniture_template_*`)

#### 2. Data Source Layer

**ファイル**: 
- `LocalFurnitureTemplateDataSource.kt` - インターフェース
- `LocalFurnitureTemplateDataSourceImpl.kt` - 実装

役割:
- FileStorageとの連携
- データの読み書き
- Flow による変更通知

#### 3. Repository Layer

**ファイル**:
- `FurnitureTemplateRepository.kt` - インターフェース
- `FurnitureTemplateRepositoryImpl.kt` - 実装

機能:
- データソースの抽象化
- エラーハンドリング
- メモリ内キャッシュ
- Result型によるエラー伝播

#### 4. Use Case Layer

**ファイル**:
- `SaveFurnitureTemplateUseCase.kt` - 保存
- `ListFurnitureTemplatesUseCase.kt` - 一覧取得
- `LoadFurnitureTemplateUseCase.kt` - 個別取得
- `DeleteFurnitureTemplateUseCase.kt` - 削除
- `FurnitureTemplateUseCases.kt` - Use Caseのコンテナ

バリデーション:
- 名前が空でないこと
- 幅と奥行きが0より大きいこと

#### 5. ViewModel Layer

**ファイル**:
- `FurnitureTemplateViewModel.kt` - ViewModel
- `RememberFurnitureTemplateViewModel.kt` - Compose用ファクトリ

機能:
- カスタム家具テンプレートの管理
- プリセットとカスタムテンプレートの統合
- UI状態の管理
- 自動読み込み

#### 6. DI Layer

**ファイル**: `DataModule.kt`

新しいメソッド:
- `provideFurnitureTemplateRepository()` - リポジトリを提供
- `provideFurnitureTemplateUseCases()` - Use Caseを提供

## 主要な変更点

### AppState

**変更前**:
```kotlin
var furnitureTemplates by mutableStateOf<List<FurnitureTemplate>>(FurnitureTemplate.PRESETS)
    private set

fun addCustomFurnitureTemplate(template: FurnitureTemplate) {
    furnitureTemplates = furnitureTemplates + template
}
```

**変更後**:
- `furnitureTemplates` プロパティを削除
- `addCustomFurnitureTemplate()` メソッドを削除
- ViewModelで管理するように変更

### FurnitureCreationPage

**変更**:
- `FurnitureTemplateViewModel` パラメータを追加
- `appState.addCustomFurnitureTemplate()` を `furnitureTemplateViewModel.saveCustomTemplate()` に変更

### FurnitureManagementPage

**変更**:
- `FurnitureTemplateViewModel` パラメータを追加
- `appState.furnitureTemplates` を `furnitureTemplateViewModel.allTemplates.collectAsState()` に変更

### FurnitureScreen

**変更**:
- `FurnitureTemplateViewModel` パラメータを追加
- テンプレート一覧を ViewModel から取得

## データの永続化

### 保存タイミング

カスタム家具テンプレートは以下のタイミングで保存されます：

1. FurnitureCreationPage で「Add to Library」ボタンをクリック
2. プリセットから選択して追加
3. 簡易エディタで作成
4. 詳細エディタで作成

### データ形式

家具テンプレートはJSON形式で保存されます：

```json
{
  "id": "uuid-string",
  "name": "カスタムソファ",
  "category": "CUSTOM",
  "width": 200,
  "depth": 90
}
```

### データの読み込み

- アプリ起動時に `FurnitureTemplateViewModel` が自動的に読み込み
- プリセット家具とカスタム家具を統合して表示
- エラーが発生してもプリセット家具は利用可能

## テスト

### テストファイル

1. `FileStorageTest.kt` - ストレージ層のテスト
   - 家具テンプレートの読み書き
   - プロジェクトとの分離確認

2. `LocalFurnitureTemplateDataSourceTest.kt` - データソース層のテスト
   - CRUD操作
   - Flow による変更通知

3. `FurnitureTemplateRepositoryTest.kt` - リポジトリ層のテスト
   - エラーハンドリング
   - キャッシュ動作

### テスト実行

```bash
./gradlew :composeApp:testDebugUnitTest
```

## 今後の拡張

### Phase 2: SQLite Database

より複雑なクエリや検索機能が必要になった場合：

```sql
CREATE TABLE furniture_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    width INTEGER NOT NULL,
    depth INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_category ON furniture_templates(category);
CREATE INDEX idx_name ON furniture_templates(name);
```

### Phase 3: クラウド同期

複数デバイス間でのデータ同期が必要になった場合：

- `RemoteFurnitureTemplateDataSource` の追加
- 同期ロジックの実装
- 競合解決メカニズム

## 既存機能との互換性

- プリセット家具テンプレートは引き続き利用可能
- 既存のプロジェクトデータには影響なし
- 家具配置機能は変更なし

## まとめ

この実装により：

✅ カスタム家具テンプレートが永続化される
✅ アプリを再起動しても保存した家具が利用できる
✅ すべてのプロジェクトで共通の家具ライブラリを使用できる
✅ プロジェクトデータとは独立して管理される
✅ 既存のアーキテクチャパターンに準拠
✅ 包括的なテストによる品質保証
