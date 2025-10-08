package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import tokyo.isseikuzumaki.puzzroom.data.storage.FileStorage

/**
 * iOS版のFileStorage生成
 *
 * Contextなしで直接FileStorageを初期化
 */
@Composable
actual fun rememberFileStorage(): FileStorage {
    return remember { FileStorage() }
}
