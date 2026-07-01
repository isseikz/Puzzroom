# プロジェクト設計書: Unison (Shadowing Sync Player)

1. 要件定義 (Requirements)
1.1. プロダクト概要
シャドーイング学習において不可避な「発話の遅れ（ラグ）」を技術的に解消し、お手本と自分の声を完全に同期（ユニゾン）させて再生することで、リズムやピッチの正確な自己評価を可能にするツール。
1.2. コア機能 (MVP)
 * ファイルインポート: デバイス内の音声ファイルを読み込む。
 * 同時録音 (Full Duplex): 原音を再生しながら、マイク入力を録音する。
 * ラグ補正 (Latency Compensation): 録音音声の再生開始タイミングをミリ秒単位で調整（早める/遅らせる）する。
 * ミックス再生: 原音と補正済み録音音声を、指定した音量バランスで同時再生する。
1.3. ターゲットプラットフォーム
 * Android (minSdk 24+): Compose Multiplatform を使用し、オーディオエンジンはネイティブ実装。
 * (Future): iOS, Webへコードベースを拡張可能にする設計。
2. 情報構造設計 (Information Architecture)
アプリは「ライブラリ（ファイル選択）」と「セッション（録音・編集）」の2つの大きなコンテキストに分かれます。
画面遷移図 (Navigation Flow)
graph LR
    A[LibraryScreen] -- URI渡し --> B((Session Graph))
    
    subgraph "Session Graph (Scoped ViewModel)"
        B[RecorderScreen] -->|録音完了| C[SyncEditorScreen]
        C -->|Retry| B
    end
    
    C -->|Finish| A

2.1. LibraryScreen (ライブラリ画面)
アプリの入り口。軽量な処理のみ行う。
 * 目的: 練習対象のファイルを選択し、セッションを開始する。
 * 表示情報:
   * アプリタイトル。
   * 「ファイルを開く」アクションボタン。
   * エラー/警告メッセージ（ファイル読み込み失敗時など）。
 * ユーザー操作:
   * ファイル選択: システムのファイルピッカーを起動。
 * バリデーション:
   * 選択されたファイルが「音声ファイル（MIME type）」であるか簡易チェックを行う。
   * OKならファイルURIを引数にして Session Graph へ遷移。NGならスナックバー表示。
2.2. RecorderScreen (レコーディング画面)
セッション開始。ここから「重い」ViewModelが生成される。
 * 目的: 原音を聞きながらシャドーイング（録音）を行う。
 * 表示情報:
   * 読み込みステータス（ロード中 / 準備完了 / エラー）。
   * ファイル名。
   * 再生プログレスバー（現在位置 / 総時間）。
   * 録音インジケータ。
 * ユーザー操作:
   * REC / STOP: 録音の開始と停止（トグル）。
 * 遷移:
   * STOP押下後、自動的（または確認ボタン）に SyncEditorScreen へ遷移。
   * 「戻る」操作で LibraryScreen へ（セッション破棄）。
2.3. SyncEditorScreen (同期・調整画面)
このアプリのコア体験を提供する画面。
 * 目的: ズレを補正し、ユニゾン再生で評価する。
 * 表示情報:
   * 簡易波形または再生バー。
   * 現在の設定値（オフセットms, ミックス比率）。
 * ユーザー操作:
   * 同期調整: スライダー操作（-2000ms 〜 +500ms）。
   * 音量バランス: スライダー操作（原音 vs 自分）。
   * 確認再生: 設定を反映してプレビュー再生。
   * リトライ: 現在の設定を維持したまま RecorderScreen へ戻る。
   * 完了: セッションを終了し LibraryScreen へ戻る。
3. 実装設計 (Technical Architecture)
Compose Multiplatform + Koin を用いた、メモリ安全性の高い設計を採用する。
3.1. レイヤー構成
| Layer | Component | Description |
|---|---|---|
| UI | Compose Multiplatform | commonMain に配置。画面描画のみ担当。 |
| ViewModel | Jetpack ViewModel | LibraryViewModel (Global/Light) と SessionViewModel (Scoped/Heavy) に分離。 |
| Repository | AudioRepository | インターフェース定義 (commonMain) と実装 (androidMain) の分離。 |
| Driver | AudioEngine | AudioTrack, AudioRecord 等の低レイヤー操作を隠蔽するラッパー。 |
3.2. ViewModel 設計とスコープ管理
巨大なPCMデータを扱うため、ViewModelの生存期間（Scope）管理を徹底する。
A. LibraryViewModel
 * Scope: koinViewModel() (Activity/Global scope)
 * 責務: ファイルピッカーの結果受け取り、URIの簡易検証、Navigationイベントの発火。
 * データ: 軽量（URI文字列、UI状態のみ）。
B. SessionViewModel
 * Scope: Navigation Graph Scope (session_graph 内でのみ生存)
 * 責務:
   * Deep Loading: URIから実際にPCMデータをデコードしてメモリ展開。
   * Audio Control: 録音、再生、オフセット計算、ミキシング。
 * データ: 重量（PCM ByteArray, 録音バッファ）。
 * ライフサイクル: ユーザーが LibraryScreen に戻った瞬間、Graphが破棄されるのに伴い onCleared() が呼ばれ、メモリが解放される。
3.3. データフロー詳細
 * LibraryScreen:
   * User selects file -> Uri returns.
   * LibraryViewModel validates mimetype.
   * Navigate to "session_graph/{encodedUri}".
 * Session Graph Entry:
   * Koin creates SessionViewModel(uri).
   * init { loadAudio(uri) } triggers deep loading (decoding to PCM).
   * UI shows Loading Indicator until Ready state.
 * SyncEditor:
   * User moves Slider -> Update offsetMs in SessionViewModel.
   * User taps Play -> AudioEngine.playDual(originalPcm, recordedPcm, offsetMs) called.
   * Engine skips offsetMs of recorded buffer (or delays) and mixes streams.
3.4. 共通インターフェース定義 (commonMain)
// リポジトリ層：データの読み込み
interface AudioRepository {
    // URIからPCMデータ(16bit/44.1kHz/Mono)をロードする
    suspend fun loadPcmData(uri: String): ByteArray
}

// エンジン層：再生・録音制御
interface AudioEngine {
    suspend fun startRecording(): Flow<ByteArray> // 録音ストリーム
    fun stopRecording()
    
    // オフセット付き再生
    // offsetMs < 0 : 録音データを早める（頭出しスキップ）
    fun playDual(original: ByteArray, recorded: ByteArray, offsetMs: Int, balance: Float)
    fun stopPlayback()
}

3.5. Android実装詳細 (androidMain)
 * Decoder: MediaExtractor + MediaCodec を使用して、あらゆる形式（mp3, aac）を統一的なPCMバイト配列に変換する。
 * Latency Handling:
   * AudioTrack の write() はブロッキングが発生しやすいため、Coroutines (Dispatchers.IO) 上で非同期に書き込む。
   * 再生開始時の track.play() のタイミングを基準に同期をとる。
4. 開発ステップ (MVP Roadmap)
 * Skeleton: プロジェクト作成、Navigation Graph構築、画面遷移の実装（中身は空）。
 * Audio Load: LibraryViewModel → SessionViewModel へのURI渡しと、Android側でのPCMデコード実装。
 * Simple Playback: デコードしたPCMを AudioTrack で再生できることを確認。
 * Recording: AudioRecord で音を拾い、メモリに保存する実装。
 * Sync Logic: スライダーの値を受け取り、再生開始位置（配列のインデックス）をずらして2音を同時再生するロジックの実装。

