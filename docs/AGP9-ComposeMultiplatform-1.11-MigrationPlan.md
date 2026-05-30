# AGP 9.0 / Compose Multiplatform 1.11.0 移行案

> Issue: #172
> 対象リポジトリ: Puzzroom monorepo（Kotlin Multiplatform）
> 作成日: 2026-05-30

## 0. このドキュメントの位置づけ

本ドキュメントは **Android Gradle Plugin 9.0** および **Compose Multiplatform 1.11.0** への
移行案である。実際のコード変更は含まず、移行の方針・影響範囲・作業手順（TODO）を整理する。

参考リンク（移行方針の根拠。実装着手前に必ず最新版を確認すること）:

- 新しい KMP のデフォルト構造 (JetBrains Blog, 2026-05):
  https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
- Reddit r/Kotlin スレッド: https://www.reddit.com/r/Kotlin/s/d2uoUaXLze

> ⚠️ 注記: 本ドキュメントの「バージョン番号」「プラグイン ID」など版に依存する記述は、
> 上記リンクおよび各公式リリースノートと突き合わせて確定すること。
> 不確実な箇所には 🔎(要確認) を付している。

---

## 1. 現状（As-Is）

### 1.1 ルート構成

| 項目 | 現行バージョン | 定義場所 |
| --- | --- | --- |
| Gradle | 8.14.3 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin (AGP) | 8.11.2 | `gradle/libs.versions.toml` (`agp`) |
| Kotlin | 2.2.20 | `libs.versions.toml` (`kotlin`) |
| Compose Multiplatform | 1.9.0 | `libs.versions.toml` (`composeMultiplatform`) |
| Compose Hot Reload | 1.0.0-beta07 | `libs.versions.toml` (`composeHotReload`) |
| KSP | 2.2.20-2.0.4 | `libs.versions.toml` (`ksp`) |
| compileSdk / targetSdk | 36 / 36 | `libs.versions.toml` |
| minSdk | 24 | `libs.versions.toml` |
| JVM ターゲット | 11 | 各モジュール `compileOptions` / `jvmTarget` |

### 1.2 モジュール一覧

| モジュール | 種別 | Android プラグイン | KMP ターゲット | 特記事項 |
| --- | --- | --- | --- | --- |
| `shared-ui` | ライブラリ | `com.android.library` | android, iosArm64, iosSimulatorArm64, jvm, js, wasmJs | 全アプリ共通 UI（Atomic Design） |
| `composeApp` | アプリ | `com.android.application` | android, iosArm64, iosSimulatorArm64, jvm, js, wasmJs | Hot Reload, Kover, Serialization |
| `nlt-app` | アプリ | `com.android.application` | android, iosArm64, iosSimulatorArm64 | Firebase, google-services |
| `quick-deploy-app` | アプリ | `com.android.application` | android, iosArm64, iosSimulatorArm64 | Firebase, Ktor, WorkManager, `buildConfig=true` |
| `unison-app` | アプリ | `com.android.application` | android, iosArm64, iosSimulatorArm64 | 標準構成 |
| `mobile-vibe-terminal` | アプリ | `com.android.application` | android, iosArm64, iosSimulatorArm64, desktop(jvm) | Room/KSP, Koin, Voyager, MINA SSHD, Robolectric, Hyperion, カスタム Gradle タスク |

共通点:
- バージョンは Gradle Version Catalog (`gradle/libs.versions.toml`) で一元管理。
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` を有効化。
- `org.gradle.configuration-cache=true` / `org.gradle.caching=true` を有効化。
- 全モジュールが `namespace` を DSL で明示済み（AGP 8 以降の必須要件を満たす）。

---

## 2. 目標（To-Be）

| 項目 | 目標バージョン | 備考 |
| --- | --- | --- |
| AGP | 9.0.x 🔎 | Gradle 9.x が前提 |
| Gradle | 9.x 🔎 | AGP 9.0 が要求する最小バージョンに合わせる |
| Kotlin | 2.2.20 → AGP 9 / CMP 1.11 と互換のある版 🔎 | KSP・Compose Compiler も連動して更新 |
| Compose Multiplatform | 1.11.0 | Compose 関連の `org.jetbrains.*` ライブラリも連動 |
| JDK（ビルド実行） | 17 以上 🔎 | AGP 9.0 / Gradle 9.x の実行要件 |

---

## 3. プロジェクト構造上の変化点

### 3.1 KMP ライブラリモジュールの Android プラグイン刷新（最重要）

AGP 9.0 / 新しい KMP デフォルト構造では、**KMP ライブラリモジュール**における Android 設定が
従来の `com.android.library` + `androidTarget()` + トップレベル `android {}` ブロックから、
新プラグイン **`com.android.kotlin.multiplatform.library`** 🔎 へ移行する。

新プラグインでは Android 設定を `kotlin {}` ブロック内の `androidLibrary {}` DSL に集約する。

**対象: `shared-ui`（唯一の `com.android.library` 利用モジュール）**

Before（現行 `shared-ui/build.gradle.kts` 抜粋）:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)        // com.android.library
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    // iosArm64(), iosSimulatorArm64(), jvm(), js(), wasmJs() ...
}

android {                                     // トップレベル android ブロック
    namespace = "tokyo.isseikuzumaki.shared.ui"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions { /* Java 11 */ }
}
```

After（新プラグイン適用イメージ）🔎:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)     // com.android.kotlin.multiplatform.library
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {                          // androidTarget() の代替（kotlin ブロック内に集約）
        namespace = "tokyo.isseikuzumaki.shared.ui"
        compileSdk = 36
        minSdk = 24
        // withHostTestBuilder / withDeviceTestBuilder など test 構成は新 DSL に従う 🔎
    }
    // iosArm64(), iosSimulatorArm64(), jvm(), js(), wasmJs() は従来どおり
}
```

ポイント:
- `androidTarget()` 呼び出しと トップレベル `android {}` ブロックは廃止し、`androidLibrary {}` に一本化。
- ソースセット名は引き続き `androidMain` 等を使用（`androidLibrary` のデフォルト）🔎。
- `shared-ui/src/androidMain/AndroidManifest.xml` の扱い（namespace を DSL に集約後の要否）を要確認 🔎。
- **アプリモジュール（`com.android.application`）はこの新プラグインの対象外**。
  アプリは引き続き `androidTarget()` + `android {}` ブロックを使用する。

> アーキテクチャ整合性: `shared-ui` は全アプリの共通依存。プラグイン刷新後も
> 公開 API（atoms / theme）と成果物座標は不変であること（依存側のビルドファイル変更不要が理想）。

### 3.2 ソースセット / ディレクトリ構造

現行は標準的な `src/<sourceSet>/kotlin` レイアウト（`commonMain`, `androidMain`, `iosMain`,
`jvmMain`/`desktopMain`, `jsMain`, `wasmJsMain`, `commonTest` 等）で、新デフォルト構造とおおむね整合する。

確認事項:
- 新 KMP デフォルト構造でディレクトリ命名規約に変更がないか（特に `androidLibrary` 配下のテスト
  ソースセット名: `androidHostTest` / `androidDeviceTest` への改称有無）🔎。
- `mobile-vibe-terminal` の `androidUnitTest` / `androidInstrumentedTest` は **アプリモジュール**
  のため従来名のまま影響なしと想定（ライブラリ側の新 DSL とは別系統）🔎。
- `composeApp` の `webMain` ディレクトリ（`build.gradle.kts` 未参照）の要否を棚卸し。

### 3.3 Gradle Wrapper / JDK

- `gradle/wrapper/gradle-wrapper.properties` の `distributionUrl` を Gradle 9.x に更新。
- CI / 開発環境の JDK を 17 以上に統一（`.github/` ワークフローと各自環境）。
- `org.gradle.configuration-cache` 有効環境のため、AGP 9 / Gradle 9 での
  Configuration Cache 互換性（特に `mobile-vibe-terminal` のカスタムタスク）を再検証。

### 3.4 Version Catalog（`gradle/libs.versions.toml`）

- `[versions]` の `agp`, `kotlin`, `composeMultiplatform`, `ksp`, `composeHotReload` を更新。
- `[plugins]` に新プラグイン `androidKmpLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }` を追加 🔎。
- 既存 `androidLibrary`（`com.android.library`）はアプリ以外で未使用化されるため、
  移行完了後に削除可否を判断。

---

## 4. 技術スタックの変化点

### 4.1 バージョン互換マトリクス（移行時に必ず整合させる）

| コンポーネント | 連動関係 | リスク |
| --- | --- | --- |
| Kotlin ↔ Compose Compiler | `composeCompiler` プラグインは Kotlin と同一版（`version.ref = "kotlin"`） | Kotlin 更新時に自動追従。要 CMP 1.11 対応 Kotlin 確認 🔎 |
| Kotlin ↔ KSP | `ksp = "2.2.20-2.0.4"` は Kotlin 版に厳密追従 | Kotlin 更新時に **必ず** KSP を対応版へ。`mobile-vibe-terminal` の Room ビルドに直結 |
| Kotlin ↔ CMP | CMP 1.11.0 が要求する Kotlin 下限 🔎 | 不一致でコンパイル不能 |
| AGP ↔ Gradle | AGP 9.0 は Gradle 9.x 必須 🔎 | Wrapper 更新必須 |
| CMP ↔ `org.jetbrains.androidx.*` | lifecycle / navigation の Compose 用ライブラリは CMP に追従 | `androidx-lifecycle 2.9.4` / `navigationCompose 2.9.0` の CMP 1.11 対応版へ更新 🔎 |
| CMP ↔ Compose Hot Reload | `composeHotReload 1.0.0-beta07` | CMP 1.11 対応版へ更新（`composeApp` のみ影響） 🔎 |

### 4.2 ライブラリ別の確認事項

| ライブラリ | 現行 | 確認 / 対応 |
| --- | --- | --- |
| Room (KMP) | 2.7.1 + KSP | AGP 9 / 新 Kotlin での KMP 対応版を確認。`mobile-vibe-terminal` のみ 🔎 |
| Koin | 4.1.1 | CMP/Kotlin 互換確認 |
| Voyager | 1.1.0-beta02 | CMP 1.11 のナビゲーション/トランジション互換確認 |
| Coil | 3.3.0 | CMP 1.11 互換確認 |
| Ktor | 3.3.0 | 影響小（Kotlin 版互換のみ） |
| Kover | 0.9.0 | Gradle 9 / Kotlin 互換確認（`composeApp`） |
| Firebase BOM | 33.7.0 + google-services 4.4.2 | AGP 9 での google-services プラグイン互換確認（`nlt-app` / `quick-deploy-app`） 🔎 |
| MINA SSHD | 2.16.0 | JVM ライブラリ。影響小（`mobile-vibe-terminal`） |
| Robolectric | 4.14.1 | AGP 9 / compileSdk 互換確認（`mobile-vibe-terminal` の androidUnitTest） 🔎 |
| Hyperion | 0.9.38 | AGP 9 互換確認。非対応なら代替/削除検討（`mobile-vibe-terminal` debug のみ） 🔎 |

### 4.3 AGP 9.0 の挙動変更で影響しうる点

- **旧 Variant API の削除**: AGP 9.0 でレガシー Variant API は撤廃。
  本リポジトリのビルドスクリプトは Variant API を直接利用していない（カスタムタスクは
  `mobile-vibe-terminal` の `assembleDebug` への `finalizedBy` のみ）ため影響は小と想定。要再検証。
- **`buildConfig` のデフォルト無効**: `quick-deploy-app` は `buildFeatures { buildConfig = true }`
  を明示しているため問題なし。他モジュールは未使用。
- **`android.nonTransitiveRClass`**: `gradle.properties` で `true` 設定済み（AGP 9 デフォルトと整合）。
- **namespace 必須**: 全モジュール設定済み。
- **AIDL / RenderScript**: 本リポジトリで未使用。影響なし。
- **カスタム Gradle タスク**: `mobile-vibe-terminal` の `NotifyApkPathTask` は
  `ExecOperations` 注入済みで Configuration Cache 対応の作法に沿うが、Gradle 9 での
  `afterEvaluate` / `finalizedBy` 挙動を再検証する。

---

## 5. モジュール別 影響評価（完了条件: 全モジュールで不都合がないこと）

| モジュール | プラグイン変更 | バージョン更新影響 | 個別リスク | 想定対応量 |
| --- | --- | --- | --- | --- |
| `shared-ui` | **大**: `com.android.library` → `com.android.kotlin.multiplatform.library` | Kotlin/CMP | 共通依存のため全アプリへ波及。API 不変を担保 | 大 |
| `composeApp` | なし（アプリ） | Kotlin/CMP/Kover/Hot Reload | 最多ターゲット（js/wasmJs 含む）でのビルド確認 | 中 |
| `nlt-app` | なし（アプリ） | Kotlin/CMP/google-services | Firebase + google-services の AGP 9 互換 | 中 |
| `quick-deploy-app` | なし（アプリ） | Kotlin/CMP/google-services | Firebase + buildConfig + Ktor/WorkManager | 中 |
| `unison-app` | なし（アプリ） | Kotlin/CMP | 標準構成。リスク最小 | 小 |
| `mobile-vibe-terminal` | なし（アプリ） | Kotlin/**KSP**/CMP | Room+KSP の Kotlin 厳密追従、Robolectric/Hyperion 互換、カスタムタスク、desktop ターゲット | 大 |

> アーキテクチャ整合性チェック:
> - `shared-ui` のプラグイン刷新は **ライブラリ→アプリ依存方向**（`implementation(project(":shared-ui"))`）に
>   影響を与えないこと（成果物座標・公開 API 不変）。
> - 全アプリは `com.android.application` のままで、新 KMP ライブラリプラグインの対象外。
>   →「ライブラリのみ新 DSL、アプリは従来 DSL」という二層構造で整合する。
> - Version Catalog 一元管理により、バージョン更新は全モジュールへ一括反映され不整合が起きにくい。

---

## 6. 移行手順（推奨順序）

段階的に行い、各段で `./gradlew build` を通すこと。問題の切り分けを容易にするため
**バージョン更新（4 章）とプラグイン刷新（3.1）を別コミットに分離**する。

1. **下準備**: feature ブランチ作成。CI/ローカルの JDK を 17+ に統一。
2. **Gradle Wrapper を 9.x へ更新** し、現行 AGP のまま全モジュールビルドが通るか確認 🔎
   （Gradle 9 単体での非互換を先に潰す）。
3. **AGP を 9.0.x へ更新**（`libs.versions.toml`）。ビルド・Lint を全モジュールで実行。
4. **Kotlin / KSP / Compose Compiler を互換版へ更新**（連動）。`mobile-vibe-terminal` の
   Room/KSP ビルドを重点確認。
5. **Compose Multiplatform を 1.11.0 へ更新**し、連動して `org.jetbrains.androidx.*`
   （lifecycle / navigation）と Compose Hot Reload を対応版へ。
6. **`shared-ui` を新プラグインへ移行**（3.1）。`androidLibrary {}` DSL へ書き換え、
   AndroidManifest / テストソースセットの扱いを確定。依存側アプリのビルド不変を確認。
7. **回帰確認**: 全モジュール `assembleDebug` + 全プラットフォーム
   （ios framework / jvm / js / wasmJs / desktop）ビルド、テスト一式、Kover レポート。
8. **ドキュメント更新**: `docs/MonorepoDevelopmentGuide.md` の AGP バージョン記述
   （「Current version: AGP 8.11.2」）と新規モジュール作成手順を最新化。

---

## 7. TODO リスト

### 事前調査（着手前に確定すべき 🔎 項目）
- [ ] AGP 9.0 が要求する Gradle 最小バージョンと JDK 要件を確定
- [ ] CMP 1.11.0 が要求する Kotlin 下限バージョンを確定
- [ ] 確定した Kotlin に対応する KSP / Compose Compiler 版を確定
- [ ] `com.android.kotlin.multiplatform.library` プラグインの正式 ID・バージョン体系・DSL を確定
- [ ] `androidLibrary {}` 下のテストソースセット命名規約（`androidHostTest`/`androidDeviceTest` 等）を確認
- [ ] google-services / Firebase BOM の AGP 9 互換版を確認
- [ ] Robolectric 4.14.1 / Hyperion 0.9.38 の AGP 9 互換性を確認（非対応なら代替案）
- [ ] Kover 0.9.0 の Gradle 9 互換性を確認

### Version Catalog 更新（`gradle/libs.versions.toml`）
- [ ] `agp` を 9.0.x に更新
- [ ] `kotlin` を互換版に更新
- [ ] `ksp` を Kotlin 連動版に更新
- [ ] `composeMultiplatform` を 1.11.0 に更新
- [ ] `composeHotReload` を CMP 1.11 対応版に更新
- [ ] `androidx-lifecycle` / `navigationCompose` を CMP 1.11 対応版に更新
- [ ] `[plugins]` に `androidKmpLibrary`（新ライブラリプラグイン）を追加

### インフラ
- [ ] `gradle/wrapper/gradle-wrapper.properties` を Gradle 9.x に更新
- [ ] `.github/` ワークフローの JDK / セットアップを 17+ に更新
- [ ] Configuration Cache が AGP 9 / Gradle 9 で機能することを確認

### モジュール対応
- [ ] `shared-ui`: 新プラグイン + `androidLibrary {}` DSL へ移行（AndroidManifest / テスト構成含む）
- [ ] `shared-ui`: 公開 API・成果物座標が不変であることを依存アプリ側で確認
- [ ] `composeApp`: 全ターゲット（android/ios/jvm/js/wasmJs）ビルド + Kover + Hot Reload 確認
- [ ] `nlt-app`: Firebase / google-services ビルド確認
- [ ] `quick-deploy-app`: Firebase / buildConfig / Ktor / WorkManager ビルド確認
- [ ] `unison-app`: ビルド確認
- [ ] `mobile-vibe-terminal`: Room/KSP・Koin・Voyager・SSHD・desktop・カスタムタスク・テスト確認

### 回帰・仕上げ
- [ ] 全モジュール `assembleDebug` 成功
- [ ] iOS framework / desktop / js / wasmJs の各ビルド成功
- [ ] 全テストスイート（`./gradlew test`）と `koverHtmlReport` 成功
- [ ] `docs/MonorepoDevelopmentGuide.md` のバージョン記述・手順を更新
- [ ] 本移行案を実装結果に合わせて追補（差異があれば記録）

---

## 8. リスクと緩和策

| リスク | 影響 | 緩和策 |
| --- | --- | --- |
| `shared-ui` プラグイン刷新で全アプリが連鎖ビルド不能 | 全モジュール | バージョン更新と分離したコミット。API 不変を回帰で担保。問題時は当該コミットのみ revert |
| Kotlin/KSP 版不一致で Room が壊れる | `mobile-vibe-terminal` | KSP を Kotlin と厳密に同期。Room ビルドを最優先で確認 |
| サードパーティ（Hyperion/Robolectric）が AGP 9 未対応 | `mobile-vibe-terminal` | 事前調査で代替/削除を判断。debug 専用のため最悪は一時無効化 |
| Configuration Cache 非互換 | 全モジュール | 一時的に `org.gradle.configuration-cache=false` で切り分け |
| 新 KMP デフォルト構造の細部が想定と異なる | `shared-ui` | 参考リンクを着手直前に再確認し、本書 🔎 箇所を確定してから実装 |

---

## 9. 完了条件（Issue #172）

- [x] 移行案を markdown 形式で `docs/` 配下に出力
- [x] プロジェクト構造上の変化点を記載（3 章）
- [x] 技術スタックの変化点を記載（4 章）
- [x] TODO リストを記載（7 章）
- [x] アーキテクチャ観点の整合性を明記（3.1 / 5 章 — ライブラリは新 DSL・アプリは従来 DSL の二層整合）
- [x] 全 6 モジュールに対する影響評価を記載（5 章）
