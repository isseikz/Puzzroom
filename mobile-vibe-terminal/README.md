# Mobile Vibe Terminal

**Version:** 2.4.0  
**Platform:** Android, Desktop (JVM), iOS (Future)  
**Language:** Kotlin Multiplatform

## 概要 (Overview)

Mobile Vibe Terminal は、AI時代のSSHクライアントです。スマートフォンでの開発体験を最適化し、Claude Code等のAgentic AIをバックエンドで動かすことを前提に設計されています。

## 主な機能 (Key Features)

- **SSH Terminal**: Apache MINAによる高性能なSSH接続
- **Code Peek Overlay**: SSH経由でファイルをモーダル表示
- **Magic Deploy**: ビルド完了を検知してAPKを自動転送・インストール
- **Connection Management**: サーバー接続設定の永続化と管理

## アーキテクチャ (Architecture)

- **UI Framework**: Compose Multiplatform
- **Navigation**: Voyager
- **DI**: Koin
- **Database**: Room (KMP)
- **SSH Core**: Apache MINA SSHD

## データベースエンティティ (Database Entities)

### ServerConnection

サーバー接続設定を保存するエンティティです。

```kotlin
@Entity(tableName = "server_connections")
data class ServerConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String, // "password" or "key"
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val deployPattern: String? = ">> VIBE_DEPLOY: (.*)"
)
```

#### `deployPattern` の使い方

`deployPattern` は、SSH接続中のターミナル出力から「デプロイ可能なファイル」を検知するための正規表現パターンです。

**用途:**
- ビルドスクリプトやCI/CDツールがAPKやアプリをビルドした際、その出力パスを自動検知
- 検知したパスからファイルをSFTP経由で自動ダウンロード・インストール

**デフォルト値:**
```
">> VIBE_DEPLOY: (.*)"
```

**使用例:**

ビルドスクリプト側で以下のように出力すると:
```bash
echo ">> VIBE_DEPLOY: /home/user/project/build/app.apk"
```

Mobile Vibe Terminal が自動的に:
1. 出力をパースして `/home/user/project/build/app.apk` を抽出
2. SFTP経由でファイルをダウンロード
3. Androidの場合、ダウンロードしたAPKをインストール

**カスタマイズ:**

プロジェクトに応じてパターンをカスタマイズできます:
```kotlin
// Gradle の場合
deployPattern = "BUILD SUCCESSFUL in .* at (.*\\.apk)"

// Maven の場合
deployPattern = "\\[INFO\\] Building jar: (.*\\.jar)"

// カスタムスクリプトの場合
deployPattern = "DEPLOY_HERE: (.*)"
```

正規表現の第1キャプチャグループ `(.*)` がファイルパスとして扱われます。

## ビルド方法 (Build Instructions)

### Android
```bash
./gradlew :mobile-vibe-terminal:assembleDebug
```

### Desktop (JVM)
```bash
./gradlew :mobile-vibe-terminal:packageDistributionForCurrentOS
```

## テスト (Testing)

```bash
# すべてのテストを実行
./gradlew :mobile-vibe-terminal:test

# Androidのみ
./gradlew :mobile-vibe-terminal:testDebugUnitTest
```

## ドキュメント (Documentation)

詳細なドキュメントは `docs/` ディレクトリを参照してください:

- [DesignDocument.md](docs/DesignDocument.md) - 設計書
- [TestCoverage.md](docs/TestCoverage.md) - テストカバレッジ
- [Xterm-Control-Sequenses.html](docs/Xterm-Control-Sequenses.html) - Xterm制御シーケンス

## ライセンス (License)

このモジュールはPuzzroomプロジェクトの一部です。
