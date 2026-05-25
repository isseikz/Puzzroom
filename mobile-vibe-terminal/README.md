# Vibe Terminal

**Version:** 1.0.9  
**Platform:** Android, Desktop (JVM), iOS (experimental)  
**Language:** Kotlin Multiplatform

## 概要 (Overview)

Vibe Terminal は、AI時代のSSHクライアントです。スマートフォンでの開発体験を最適化し、Claude Code等のAgentic AIをバックエンドで動かすことを前提に設計されています。

## 主な機能 (Key Features)

- **SSH Terminal**: Apache MINAによる高性能なSSH接続
- **Code Peek Overlay**: SSH経由でファイルをモーダル表示。双方向ナビゲーション、テキスト選択・コピー、シンタックスハイライト対応
- **Magic Deploy**: ビルド完了を検知、またはサーバーからのトリガーを受信してAPKを自動転送・インストール
- **Connection Management**: サーバー接続設定の永続化と管理。自動再接続、起動コマンド設定に対応
- **Smart File Explorer**: SSHセッションの作業ディレクトリを起点としたファイルブラウザ。ファイル共有機能付き
- **External Display Support**: 外部モニター接続時にターミナルを最適化表示。大画面でのコーディングやログ監視が可能（デスクトップモード対応）
- **Hardware Stability**: 物理キーボードの挿抜や外部ディスプレイ接続時のリサイズ等によるActivity再生成を防止し、実行中のセッション状態を維持
- **Mouse Reporting**: tmux/byobuなどのターミナルマルチプレクサでのスクロールやマウス操作をサポート
- **Modifier Keys**: Shift/Ctrl/Altキーのxterm escape sequence対応。固定キー行（Fixed Key Row）による素早いキー入力

## アーキテクチャ (Architecture)

- **UI Framework**: Compose Multiplatform
- **Navigation**: Voyager
- **DI**: Koin
- **Database**: Room (KMP)
- **SSH Core**: Apache MINA SSHD
- **Terminal Input**: [kmp-terminal-input](https://github.com/isseikz/kmp-terminal-input)
- **Preferences**: DataStore (KMP)

## データベースエンティティ (Database Entities)

### ServerConnection

サーバー接続設定を保存するエンティティです（スキーマバージョン: 6）。

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
    val keyAlias: String? = null, // Key alias in Android KeyStore (for public key auth)
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val deployPattern: String? = ">> VIBE_DEPLOY: (.*)",
    val startupCommand: String? = null, // Command to execute on shell startup (e.g., "tmux attach || tmux new")
    val isAutoReconnect: Boolean = false, // Enable automatic reconnection on app restart
    val monitorFilePath: String? = null,
    val lastFileExplorerPath: String? = null // Last opened path in File Explorer
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
        val apkFile = dir.walkTopDown().find { it.name.endsWith(".apk") && !it.name.contains("unaligned") }
        
        if (apkFile != null) {
            val absolutePath = apkFile.absolutePath
            println("Found APK at: $absolutePath")
            try {
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

tasks.register<NotifyApkPathTask>("notifyApkPath") {
    apkDirectory.set(layout.buildDirectory.dir("outputs/apk/debug"))
}

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

### iOS (experimental)
iOSターゲット（`iosArm64`, `iosSimulatorArm64`）は設定済みですが、SSH接続等の一部機能はスタブ実装です。

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
- [Troubleshooting.md](docs/Troubleshooting.md) - トラブルシューティング
- [XtermControlSequenceCompliance.md](docs/XtermControlSequenceCompliance.md) - Xterm制御シーケンス準拠状況
- [scrollback-and-mouse-reporting.md](docs/architecture/scrollback-and-mouse-reporting.md) - スクロールバック＆マウスレポーティング設計
- [Maestro テストガイド](docs/maestro/README.md) - UIテスト自動化

## ライセンス (License)

このモジュールはPuzzroomプロジェクトの一部です。
