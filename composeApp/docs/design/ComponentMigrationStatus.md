# Component Migration Status

このドキュメントでは、既存の`component/`ディレクトリから Atomic Design 構造への移行状況を記録します。

## ✅ 移行完了 (5 components)

以下のコンポーネントは Atomic Design 構造に移行されました：

### Molecules（分子）
1. **SaveStateIndicator.kt**
   - 移行先: `ui/molecules/SaveStateIndicator.kt`
   - 理由: アイコンとテキストを組み合わせた小さな機能単位
   - 使用箇所: RoomScreen, FurnitureScreen

### Organisms（有機体）
2. **SaveLoadDialogs.kt** (SaveDialog, LoadDialog)
   - 移行先: `ui/organisms/SaveLoadDialogs.kt`
   - 理由: 完全な機能を持つダイアログコンポーネント
   - 使用箇所: RoomScreen

3. **PolygonListPanel.kt**
   - 移行先: `ui/organisms/PolygonListPanel.kt`
   - 理由: リスト表示、選択、編集、削除機能を持つ複合パネル
   - 使用箇所: RoomScreen

4. **FurnitureLibraryPanel.kt**
   - 移行先: `ui/organisms/FurnitureLibraryPanel.kt`
   - 理由: カテゴリフィルター、リスト表示、選択機能を持つ複合パネル
   - 使用箇所: FurnitureScreen

5. **FurniturePlacementToolbar.kt**
   - 移行先: `ui/organisms/FurniturePlacementToolbar.kt`
   - 理由: 複数の入力フィールドとコントロールを持つツールバー
   - 使用箇所: FurnitureScreen

## 📦 既存維持 (8 components)

以下のコンポーネントは特殊性が高いため、`component/`ディレクトリに維持します：

### Canvas関連（描画ロジック）
1. **EditablePolygonCanvas.kt**
   - 理由: Canvas描画とインタラクション処理を含む特殊コンポーネント
   - 特徴: 複雑な描画ロジック、ジェスチャー処理

2. **FurnitureLayoutCanvas.kt**
   - 理由: Canvas描画とドラッグ&ドロップ処理を含む特殊コンポーネント
   - 特徴: 複雑な描画ロジック、配置計算

### 専用入力パネル
3. **AngleInputPanel.kt**
   - 理由: 角度入力に特化した専用UI
   - 特徴: ドメイン固有のロジックを含む

4. **DimensionInputPanel.kt**
   - 理由: 寸法入力に特化した専用UI
   - 特徴: ドメイン固有のロジックを含む

### 入力補助
5. **InputPolygon.kt**
   - 理由: ポリゴン入力の補助機能
   - 特徴: 特殊な入力処理

### プラットフォーム固有実装
6. **PhotoPickerButton.kt**
   - 理由: プラットフォーム固有の実装が必要（Android/iOS別ファイル）
   - 特徴: expect/actual パターンを使用

### その他
7. **TracingTable.kt**
   - 理由: トレース機能に特化したコンポーネント
   - 特徴: 特殊な機能実装

8. **PreviewTemplate.kt**
   - 理由: プレビュー専用のシンプルなラッパー
   - 特徴: 開発用途のユーティリティ

## 移行の判断基準

### Atomic Design に移行するべき
- ✅ 汎用的なUIコンポーネント
- ✅ 複数の画面で再利用される
- ✅ ビジネスロジックを含まない
- ✅ 標準的なCompose APIのみを使用

### 既存維持するべき
- ❌ Canvas描画など特殊な実装が必要
- ❌ プラットフォーム固有の実装が必要
- ❌ ドメイン固有のロジックを強く含む
- ❌ 単一の画面専用コンポーネント

## 更新されたファイル

### Screens
- `ui/screen/RoomScreen.kt` - importを更新
- `ui/screen/FurnitureScreen.kt` - importを更新

### 新規作成
- `ui/molecules/SaveStateIndicator.kt`
- `ui/organisms/SaveLoadDialogs.kt`
- `ui/organisms/PolygonListPanel.kt`
- `ui/organisms/FurnitureLibraryPanel.kt`
- `ui/organisms/FurniturePlacementToolbar.kt`

## 互換性

既存の`component/`ディレクトリのコンポーネントは削除していません。
新しい Atomic Design コンポーネントと既存コンポーネントは共存可能です。

段階的な移行を継続する場合は、以下の方針を推奨します：
1. 新機能は Atomic Design パターンで実装
2. 既存機能の改修時に移行を検討
3. Canvas関連など特殊なコンポーネントは無理に移行しない

## まとめ

- **移行完了**: 5 components
- **既存維持**: 8 components
- **移行率**: 38% (汎用コンポーネントはほぼ完了)

主要な汎用UIコンポーネントの移行は完了しました。
残りの特殊コンポーネントは、その特性を維持するため既存の場所に保持します。
