# 📱 Mobile Vibe Terminal - Master Design Document

**Project Name:** Mobile Vibe Terminal (Code Name: `mobile-vibe`)
**Version:** 2.6.0 (The "Smart File Explorer" Update)
**Date:** 2025-12-29
**Target Platform:** Android (Primary), Desktop/JVM (Secondary), iOS (Future)
**Language:** Kotlin (Kotlin Multiplatform)

## 1. プロジェクト概要 (Executive Summary)

### 1.1 コンセプト: "Vertical Vibe Coding"

スマートフォン（縦画面）での開発体験を再定義する、AI時代のSSHクライアント。
Claude Code 等の **Agentic AI** をバックエンドで動かすことを前提とし、フロントエンドは「AIへの指示(Chat)」「コード確認(View)」「動作検証(Deploy)」を、アプリを切り替えることなくシームレスに完結させることを目的とする。

### 1.2 解決する課題 (Core Problems & Solutions)

| 課題領域 | 現状の課題 | Mobile Vibe Terminal の解決策 |
|---|---|---|
| **Input** | VIM操作と長文チャット入力の要件が相反し、どちらも使いづらい。 | **Context-Aware Hybrid Input**: 実行中のアプリ（Vim/Shell/AI）に応じて、入力欄の有無とツールバーを最適化するハイブリッド入力システム。 |
| **Context** | AIに「このファイルを見て」と指示する際、パスの手打ちが苦痛。 | **File Picker Integration**: アプリ内のファイルブラウザから、選択したファイルのパスを直接ターミナル入力欄に注入する機能。 |
| **Review** | コード確認のためにVimを開いたりアプリを切り替えると文脈が切れる。 | **Code Peek Overlay**: SSH経由でファイルを裏読みし、モーダルでサッと確認できるビューアを搭載。 |
| **Persistence** | アプリ再起動で接続と作業状態が失われる。 | **Auto Session Restore**: 最後のSSH接続を記憶し、tmux/screen等のスタートアップコマンドを自動実行して作業を復帰。 |

---

## 2. システムアーキテクチャ (Technical Architecture)

Kotlin Multiplatform (KMP) を採用。共通ロジックを最大化しつつ、コンテキスト検知やSSH制御を抽象化して扱う。

### 2.1 技術スタック (Tech Stack)

| レイヤー | 技術/ライブラリ | 用途 |
|---|---|---|
| **Language** | **Kotlin 2.0+** | K2コンパイラによる高速ビルドと安全性。 |
| **UI Framework** | **Compose Multiplatform** | Android/Desktop/iOSでUIコードを共有。 |
| **Navigation** | **Voyager** | ScreenModelによるMVVMアーキテクチャ。 |
| **DI** | **Koin** | DSLベースの依存性注入。 |
| **Database** | **Room (KMP)** | サーバー設定の永続化。 |
| **SSH Core** | **Apache MINA** | 非同期処理に強いSSH実装。 |

### 2.2 モジュール構成図

```mermaid
graph TD
    subgraph "Shared (commonMain)"
        UI[<b>UI Layer</b><br>Compose Screens]
        InputMgr[<b>Context Input Mgr</b><br>Profile Switching Logic]
        Domain[<b>Domain Layer</b><br>Interfaces (SshRepo)<br>UseCases]
        Data[<b>Data Layer</b><br>Room DB<br>DataStore]
    end

    subgraph "Android (androidMain)"
        Mina[<b>Apache MINA SSHD</b>]
        OscParser[<b>OSC Parser</b><br>Window Title Detection]
        KeyStore[<b>Android Keystore</b><br>Credential Encryption]
    end

    UI --> InputMgr
    InputMgr --> OscParser
    InputMgr --> Domain
    Domain -.-> Mina
    Data -.-> KeyStore
```

---

## 3. UI/UX デザイン仕様 (Design Specifications)

**テーマ:** "Neon Focus" - 黒背景 (`#0D1117`) にネオングリーン (`#39D353`) とソフトレッド (`#FF7B72`) のアクセント。

### 3.1 画面遷移フロー

```mermaid
graph LR
    Terminal[<b>Main Terminal</b>] -->|Toolbar: Text Mode| InputBar[<b>Input Area Visible</b>]
    Terminal -->|Toolbar: Cmd Mode| RawKey[<b>Direct Key Send</b>]
    
    Terminal -->|Toolbar: /add| FilePicker[<b>File Explorer (Picker Mode)</b>]
    FilePicker -->|Select File| InputBar
```

### 3.2 詳細要件: Context-Aware Input System

コンテキスト（使用中のアプリ）に応じて、以下の2つのモードとツールバーを動的に切り替える。

#### A. Command Mode (Default / VIM / Shell)
* **UI:** 下部のテキスト入力欄（Input Bar）は非表示。
* **Action:** ソフトウェアキーボードのタップを直接SSHへ送信。
* **Nav Tab:** 矢印キーはターミナルへのカーソル移動 (`\033[A`等) として動作。
* **Toolbar:** Esc, Tab, Ctrl (Sticky/Toggle), |, ->

#### B. Text Mode (AI Agent / Commenting)
* **UI:** 下部にテキスト入力欄と送信ボタンを表示。
* **Action:** Android IME（音声入力、グライド入力）を使用可能。送信ボタンで一括送信。
* **Nav Tab:** 矢印キーは**入力欄内のカーソル移動（推敲用）**として動作。
* **Toolbar:** Text Mode切替, Paste, Markdown記号, /Cmds

#### C. Dynamic Toolbar Profiles
現在のコンテキストに応じてツールバーの内容を変化させる。
* **Vim Profile:** Esc, :, /, u, Ctrl
* **Claude Code Profile:** 📂/add, /cost, 🛑STOP, Markdown, Enter(Approve)
* **Kotlin Profile:** fun, val, ->, ${}

#### D. File Explorer & Picker
* **Viewer Mode:** ファイルを開く（既存機能）。
* **Picker Mode (New):** ファイルを選択し、そのパス文字列を呼び出し元の入力欄に返す。複数選択時はスペース区切りで連結する。

#### E. Smart File Explorer Path Management (v2.6.0) ✅
* **初回オープン:** SSHセッションのホームディレクトリ（`$HOME`）を初期パスとして使用。
* **2回目以降:** 最後に開いたディレクトリパスをデータベースに永続化し、アプリ再起動後も同じパスから開始。
* **実装:**
  * `ServerConnection`エンティティに`lastFileExplorerPath`カラムを追加（DB v5）
  * `ConnectionRepository`に`updateLastFileExplorerPath`/`getLastFileExplorerPath`メソッドを追加
  * `FileExplorerSheet`に`initialPath`パラメータと`onPathChanged`コールバックを追加

#### F. SSH Connection State Sync (v2.6.1) ✅
* **課題:** 画面オフ/バックグラウンド時にSSH接続が切断されても、UIが接続状態のまま表示される問題
* **解決策:**
  * `LifecycleResumeEffect`でアプリのフォアグラウンド復帰を検知
  * 復帰時に`sshRepository.isConnected()`で実際の接続状態を確認
  * 切断されていれば自動再接続を試行
* **実装:**
  * `TerminalState`に`isReconnecting`と端末サイズ（`lastTerminalCols`等）を追加
  * `TerminalScreenModel.onAppResumed()`：ライフサイクル復帰時の接続確認と再接続
  * `TerminalScreenModel.reconnect()`：保存された端末サイズで再接続
  * `TerminalScreen`にReconnecting状態のUIインジケータを追加

---

## 4. 機能詳細 (Logic Specification)

### 4.1 SSH通信 & コンテキスト検知
* **Window Title Parsing:**
  * 受信ストリームから `OSC 0;Title\007` 等のエスケープシーケンスを監視。
  * タイトル文字列（例: "vi MainActivity.kt", "claude"）を解析し、UIプロファイルを自動切り替え。
* **xterm-256color Compliance:** `TERM=xterm-256color` 送信により、Vim等のタイトル設定機能を有効化。

### 4.2 Security (Credential Storage)
* **Encryption Strategy:**
  * 生のパスワードは保存しない。
  * **Android Keystore** で生成した鍵ペアを使用し、認証情報を暗号化して **DataStore** に保存。

### 4.3 Claude Code Integration
* **Shortcut Commands:** `/cost`, `/clear`, `/compact` 等の定型文メニュー実装。
* **Panic Button:** `Ctrl+C` を即座に送信する専用ボタン（AIの暴走停止用）。

---

## 5. データベース設計 (Room)

**Table: `server_connections`** (Schema Version: 3)

| Column | Type | Note |
|---|---|---|
| `id` | Long (PK) | Auto Increment |
| `alias` | String | 表示名 |
| `host` | String | IP / Domain |
| `startupCommand` | String? | シェル起動時に実行するコマンド（例: "tmux attach"） |
| `isAutoReconnect` | Boolean | アプリ再起動時の自動再接続フラグ |
| ... | ... | (その他の接続情報はv2.4.0準拠) |

---

## 6. 実装ロードマップ (Implementation Roadmap)

優先順位の原則: 「基礎的な入力体験の修復」→「独自価値の提供」→「自動化・洗練」

### Phase 4: Foundation & Security (Completed) ✅
*   **Auto Session Restore:** アプリ再起動時の自動接続復帰 (Implemented).
*   **Secure Storage:** Android Keystore + DataStore による認証情報の暗号化保存 (Implemented in v2.4.1).
*   **File Explorer:** 基本的なファイルブラウジングとViewer機能 (Implemented).

### Phase 5: Hybrid Input Foundation (UX Critical) 🔥
**ステータス: 未実装 (最優先着手)**
**目標:** VIMなどの既存ツールと、日本語入力（IME）が競合する現状の「使いづらさ」を解消する。

* [ ] **Manual Mode Switching (UI):**
  * コマンドモード（入力欄なし）とテキストモード（入力欄あり）を切り替えるState管理。
  * ツールバーへのモード切替ボタン配置。
* [ ] **Smart Nav Tab (Logic):**
  * テキストモード時は矢印キーが「入力欄内のカーソル移動」になるようイベントハンドラを分岐。
* [ ] **Sticky Modifier Keys (Input):**
  * Ctrl / Alt キーを「押しっぱなし（Toggle）」状態に保持するロジックの実装。

### Phase 6: AI Agent Integration (USP - Unique Selling Proposition) 🚀
**ステータス: 未実装 (Phase 5完了後)**
**目標:** 「スマホでClaude Codeを使うならこのアプリしかない」というキラー機能を実装する。

* [ ] **File Picker "Path Injection" Mode:**
  * `FileExplorerScreen` に「パス選択モード」を追加実装。
  * ファイル選択時に絶対パス（可能な場合は相対パス）を呼び出し元に返す処理。
* [ ] **Claude Toolbar Profile:**
  * 📂 `/add` ボタン（Picker連携）、🛑 `STOP (Ctrl+C)` ボタンの配置。
  * よく使うコマンドのメニュー化。

### Phase 7: Context Automation (Smart Polish) ✨
**ステータス: 未実装**
**目標:** 手動で行っていたモード切替を自動化し、体験を洗練させる。

* [ ] **OSC Title Parser:** SSHストリーム解析とアプリ名抽出。
* [ ] **Auto Profile Switching:** 解析結果に基づくプロファイル自動適用。
* [ ] **Advanced Rendering:** Canvasベースのレンダラー実装 (xterm Alternate Screen対応)。

### Phase 8: DevOps & Expansion (Future) 🛠️
**ステータス: 未実装**

* [ ] **Magic Deploy:** ログ監視によるAPK自動インストール。
* [ ] **iOS Support:** KMPのiOS対応。

---

## 7. プロダクトオーナー判断 (Product Owner Notes)

**優先順位の根拠:**
* **Phase 5 (Input) が最優先:** 現状の「VimでCtrlキーが押しにくい」「コメント入力時に入力欄が邪魔」という二重苦は、アプリの利用継続を阻害する欠陥レベルの課題であるため。
* **Phase 6 (AI) の戦略的価値:** スマホでのパス手打ちは最大のペインポイントであり、これを解決するFile Picker連携は他社製アプリに対する決定的な差別化要因となる。自動化（Phase 7）よりも先に実装し、早期に価値を提供する。
