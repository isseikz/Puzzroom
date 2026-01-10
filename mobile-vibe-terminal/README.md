# Vibe Terminal

**Version:** 2.5.0  
**Platform:** Android, Desktop (JVM), iOS (Future)  
**Language:** Kotlin Multiplatform

## 概要 (Overview)

Vibe Terminal は、AI時代のSSHクライアントです。スマートフォンでの開発体験を最適化し、Claude Code等のAgentic AIをバックエンドで動かすことを前提に設計されています。

## 主な機能 (Key Features)

- **SSH Terminal**: Apache MINAによる高性能なSSH接続
- **Code Peek Overlay**: SSH経由でファイルをモーダル表示
- **Magic Deploy**: ビルド完了を検知、またはサーバーからのトリガーを受信してAPKを自動転送・インストール
- **Connection Management**: サーバー接続設定の永続化と管理
- **Smart File Explorer**: SSHセッションの作業ディレクトリを起点としたファイルブラウザ
- **External Display Support**: 外部モニター接続時にターミナルを最適化表示。大画面でのコーディングやログ監視が可能（デスクトップモード対応）
- **Hardware Stability**: 物理キーボードの挿抜や外部ディスプレイ接続時のリサイズ等によるActivity再生成を防止し、実行中のセッション状態を維持

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

### Smart File Explorer

File Explorerは、使いやすさを重視したパス管理機能を提供します。

**初回オープン時:**
- SSHセッションのホームディレクトリ（`$HOME`）を初期パスとして使用
- ユーザーのホームディレクトリからファイル探索を開始

**2回目以降のオープン:**
- 最後に開いたディレクトリをデータベースに永続化
- アプリを再起動しても、前回開いたディレクトリから再開可能

この機能により、File Explorerの使い勝手が大幅に向上し、毎回ルートディレクトリから探索する必要がなくなります。

## Magic Deploy

Magic Deployは、リモートサーバーでビルドされたアプリ（APK）を、即座に手元のAndroid端末に転送・インストールする機能です。以下の2つのモードをサポートしています。

### 1. リモートトリガーモード (Recommended)

SSHのリモートポートフォワーディングを利用した、高速かつ確実なデプロイ方法です。
Vibe Terminalは接続時にサーバー側のポート `58080` をリッスンします。

**使用方法:**
サーバー側でAPKのパスをポート `58080` に送信するだけでトリガーされます。

```bash
echo "/path/to/app.apk" | nc localhost 58080
```

**特徴:**
- **即時実行:** ログ出力を待つことなく、コマンド実行と同時に処理が開始されます。
- **Gradle統合:** Androidプロジェクトの場合、`assembleDebug` タスクの完了後に自動的にトリガーする設定が可能です（本プロジェクトの `build.gradle.kts` に実装済み）。
- **SFTP高速転送:** パスを受け取ると、アプリはSFTP経由で直接ファイルをダウンロードします。
- **セキュリティ:** ダウンロード開始前に確認ダイアログが表示され、意図しないファイルの転送を防ぎます（設定で自動インストールも可能）。

#### Gradle設定例

Androidプロジェクトの `build.gradle.kts` (app module) に以下のタスクを追加することで、`assembleDebug` 完了時に自動的にデプロイをトリガーできます。

```kotlin
// build.gradle.kts

abstract class NotifyApkPathTask : DefaultTask() {
    @get:javax.inject.Inject
    abstract val execOperations: org.gradle.process.ExecOperations
    
    @get:InputDirectory
    abstract val apkDirectory: DirectoryProperty

    @TaskAction
    fun notifyPath() {
        val dir = apkDirectory.get().asFile
        // デバッグビルドのAPKを検索
        val apkFile = dir.walkTopDown().find { it.name.endsWith(".apk") && !it.name.contains("unaligned") }
        
        if (apkFile != null) {
            val absolutePath = apkFile.absolutePath
            println("Found APK at: $absolutePath")
            try {
                // ローカルポート58080にパスを送信 (ncコマンドが必要)
                execOperations.exec {
                    commandLine("sh", "-c", "echo \"$absolutePath\" | nc -w 1 localhost 58080")
                    isIgnoreExitValue = true
                }
                println("Sent APK path to localhost:58080")
            } catch (e: Exception) {
                println("Failed to send APK path: ${e.message}")
            }
        }
    }
}

// タスクを登録
tasks.register<NotifyApkPathTask>("notifyApkPath") {
    apkDirectory.set(layout.buildDirectory.dir("outputs/apk/debug"))
}

// assembleDebugの後に実行するよう設定
afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("notifyApkPath")
    }
}
```

### 2. ログ出力検知モード (Legacy)

ターミナルの標準出力を監視し、特定のパターンにマッチする行を検出するとデプロイを開始します。
設定不要で手軽に利用できますが、ログの流れが速い場合に見逃す可能性があります。

**使用方法:**
`ServerConnection` の `deployPattern` で設定された正規表現（デフォルト: `>> VIBE_DEPLOY: (.*)`）にマッチするログを出力します。

```bash
echo ">> VIBE_DEPLOY: /home/user/project/build/app.apk"
```

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
- [Xterm-Control-Sequences.html](docs/Xterm-Control-Sequences.html) - Xterm制御シーケンス

## ライセンス (License)

このモジュールはPuzzroomプロジェクトの一部です。
