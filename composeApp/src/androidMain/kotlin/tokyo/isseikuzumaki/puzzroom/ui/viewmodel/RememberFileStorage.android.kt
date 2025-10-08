package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import tokyo.isseikuzumaki.puzzroom.data.storage.FileStorage

/**
 * Android版のFileStorage生成
 *
 * LocalContextからContextを取得してFileStorageを初期化
 */
@Composable
actual fun rememberFileStorage(): FileStorage {
    val context = LocalContext.current
    return remember { FileStorage(context) }
}
