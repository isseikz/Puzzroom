# Quickstart: iOS Support for Mobile Vibe Terminal

**Branch**: `006-ios-support` | **Date**: 2026-03-28

---

## 前提条件

- macOS + Xcode 15+（最新安定版推奨）
- Kotlin Multiplatform プラグイン（Android Studio / IntelliJ）
- CMake インストール済み：`brew install cmake`
- Git
- iOS 16+ デバイスまたはシミュレータ（arm64）
- **Kotlin 2.3.x 以上** — `kmp-terminal-input:1.0.3` が KLIB ABI 2.3.0 でコンパイルされているため、`gradle/libs.versions.toml` の `kotlin = "2.3.x"` に更新が必要

---

## Step 1: libssh2 + OpenSSL を xcframework としてビルドする

### 1-1. ビルドスクリプトの取得

```bash
git clone https://github.com/apotocki/libssh2-iosx.git
cd libssh2-iosx
```

### 1-2. ビルド実行

```bash
scripts/build.sh
```

完了後 `frameworks/` に以下が生成される:

```
frameworks/
  libssh2.xcframework/
    ios-arm64/
      libssh2.a
    ios-arm64_x86_64-simulator/
      libssh2.a
  openssl.xcframework/
    ios-arm64/
      libcrypto.a
      libssl.a
    ios-arm64_x86_64-simulator/
      libcrypto.a
      libssl.a
```

---

## Step 2: バイナリとヘッダをプロジェクトに配置する

プロジェクトモジュール `mobile-vibe-terminal/src/nativeInterop/` に以下の構造でファイルを配置する:

```
mobile-vibe-terminal/src/nativeInterop/
  include/
    libssh2.h
    libssh2_sftp.h
    libssh2_publickey.h
  libs/
    ios-arm64/          ← 実機用
      libssh2.a
      libssl.a
      libcrypto.a
    ios-arm64-simulator/ ← シミュレータ用
      libssh2.a
      libssl.a
      libcrypto.a
```

### コマンド例

```bash
# リポジトリルートから実行
INTEROP_DIR=mobile-vibe-terminal/src/nativeInterop
LIBSSH2_IOSX=path/to/libssh2-iosx

# xcframework 内のディレクトリ名を確認
ls $LIBSSH2_IOSX/frameworks/libssh2.xcframework/
ls $LIBSSH2_IOSX/frameworks/openssl.xcframework/

# ヘッダ（libssh2 ソースの include/ から）
cp $LIBSSH2_IOSX/libssh2/include/*.h $INTEROP_DIR/include/

# 実機用ライブラリ（ディレクトリ名は環境によって異なる）
cp $LIBSSH2_IOSX/frameworks/libssh2.xcframework/ios-arm64/libssh2.a          $INTEROP_DIR/libs/ios-arm64/
cp $LIBSSH2_IOSX/frameworks/openssl.xcframework/ios-arm64/libcrypto.a        $INTEROP_DIR/libs/ios-arm64/
cp $LIBSSH2_IOSX/frameworks/openssl.xcframework/ios-arm64/libssl.a           $INTEROP_DIR/libs/ios-arm64/

# シミュレータ用ライブラリ
SIMSUFFIX=$(ls $LIBSSH2_IOSX/frameworks/libssh2.xcframework/ | grep simulator)
cp $LIBSSH2_IOSX/frameworks/libssh2.xcframework/$SIMSUFFIX/libssh2.a          $INTEROP_DIR/libs/ios-arm64-simulator/
cp $LIBSSH2_IOSX/frameworks/openssl.xcframework/$SIMSUFFIX/libcrypto.a        $INTEROP_DIR/libs/ios-arm64-simulator/
cp $LIBSSH2_IOSX/frameworks/openssl.xcframework/$SIMSUFFIX/libssl.a           $INTEROP_DIR/libs/ios-arm64-simulator/
```

> **注意**: `.a` ファイルは `.gitignore` で除外されているため、各開発者が手元でビルドして配置する必要がある。

---

## Step 3: cinterop タスクの実行確認

ファイルを配置後:

```bash
./gradlew :mobile-vibe-terminal:cinteropLibssh2IosArm64
./gradlew :mobile-vibe-terminal:cinteropLibssh2IosSimulatorArm64
```

成功すると以下が生成される:

```
mobile-vibe-terminal/build/classes/kotlin/iosArm64/main/
  mobile-vibe-terminal-cinterop-libssh2.klib
```

### よくあるエラーと対処

| エラー | 原因 | 対処 |
|--------|------|------|
| `Cannot find library` | `.a` ファイルのパスが不正 | `libs/ios-arm64/` の中身を確認 |
| `Header not found` | ヘッダ未配置 | `include/` に `libssh2.h` を配置 |
| `Undefined symbols` | OpenSSL ライブラリ不足 | `libssl.a`, `libcrypto.a` の配置を確認 |
| `ld: symbol(s) not found` for zlib | `-lz` 未設定 | `.def` の `linkerOpts` を確認（設定済み） |
| KLIB ABI version mismatch | `kmp-terminal-input` が Kotlin 2.3+ | `kotlin = "2.3.x"` に更新 |

---

## Step 4: ビルド & 実行

```bash
# KMP xcframework をビルド
./gradlew :mobile-vibe-terminal:assembleReleaseXCFramework

# Xcode プロジェクトを開く
open iosApp/iosApp.xcodeproj

# iOS Simulator ターゲットを選択して Cmd+R
```

---

## ファイル構造（参照）

| 対象 | パス |
|------|------|
| cinterop 定義ファイル | `mobile-vibe-terminal/src/iosMain/cinterop/libssh2.def` |
| ヘッダ配置先 | `mobile-vibe-terminal/src/nativeInterop/include/` |
| 実機用ライブラリ配置先 | `mobile-vibe-terminal/src/nativeInterop/libs/ios-arm64/` |
| シミュレータ用ライブラリ配置先 | `mobile-vibe-terminal/src/nativeInterop/libs/ios-arm64-simulator/` |
| iOS 実装 | `mobile-vibe-terminal/src/iosMain/kotlin/` |
| Koin DI 設定 | `mobile-vibe-terminal/src/iosMain/.../di/PlatformModule.kt` |
| SSH 実装 | `mobile-vibe-terminal/src/iosMain/.../ssh/SshRepositoryIos.kt` |
| Keychain ヘルパー | `mobile-vibe-terminal/src/iosMain/.../security/KeychainHelper.kt` |
| Room DB ビルダー | `mobile-vibe-terminal/src/iosMain/.../data/database/DatabaseBuilder.ios.kt` |
| DataStore ビルダー | `mobile-vibe-terminal/src/iosMain/.../data/datastore/DataStoreBuilder.ios.kt` |

---

## 既知の制約・注意事項

### Kotlin バージョン

`kmp-terminal-input:1.0.3` は Kotlin KLIB ABI 2.3.0 でコンパイルされている。
現状の `kotlin = "2.2.20"` ではネイティブ（iOS/Desktop）ターゲットがコンパイルできない。

```toml
# gradle/libs.versions.toml
kotlin = "2.3.0"  # ← kmp-terminal-input:1.0.3 の要求に合わせて更新
```

### Room マイグレーション（iOS）

iOS の `BundledSQLiteDriver` は Android の `SupportSQLiteDatabase` マイグレーション API を持たない。
スキーマ変更時は `DatabaseBuilder.ios.kt` に以下を追加:

```kotlin
return Room.databaseBuilder<AppDatabase>(name = dbPath)
    .setDriver(BundledSQLiteDriver())
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```

### Magic Deploy

iOS では APK インストール機能（Magic Deploy）は利用不可。
`isMagicDeploySupported = false` により UI 上のインストールボタンは非表示。

---

## テスト実行

```bash
# Android ユニットテスト（既存の CI と同じ）
./gradlew :mobile-vibe-terminal:testDebugUnitTest

# iOS シミュレータテスト（libssh2 配置 + Kotlin 2.3.x が必要）
./gradlew :mobile-vibe-terminal:iosSimulatorArm64Test
```
