package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.puzzroom.data.storage.FileStorage

/**
 * プラットフォーム固有のFileStorageを生成
 *
 * - Android: Contextを使用してFileStorageを初期化
 * - iOS: Contextなしで直接FileStorageを初期化
 */
@Composable
expect fun rememberFileStorage(): FileStorage
