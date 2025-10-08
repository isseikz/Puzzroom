package tokyo.isseikuzumaki.puzzroom.ui.state

import tokyo.isseikuzumaki.puzzroom.data.PersistenceError

/**
 * UI表示用のエラー情報
 */
data class UiError(
    val message: String,
    val retryAction: (() -> Unit)? = null,
    val severity: Severity = Severity.Error
) {
    enum class Severity { Info, Warning, Error }
}

/**
 * PersistenceErrorをUiErrorに変換
 */
fun Throwable.toUiError(): UiError {
    return when (this) {
        is PersistenceError.IOError -> UiError(
            message = "保存できませんでした。ストレージを確認してください",
            severity = UiError.Severity.Error
        )
        is PersistenceError.QuotaExceededError -> UiError(
            message = "容量不足です（${currentSize / 1024 / 1024}MB / ${maxSize / 1024 / 1024}MB）",
            severity = UiError.Severity.Warning
        )
        is PersistenceError.CorruptedDataError -> UiError(
            message = "ファイル「$fileName」が破損しています",
            severity = UiError.Severity.Error
        )
        is PersistenceError.NotFoundError -> UiError(
            message = "プロジェクト（ID: $id）が見つかりませんでした",
            severity = UiError.Severity.Warning
        )
        else -> UiError(
            message = "エラーが発生しました: ${this.message}",
            severity = UiError.Severity.Error
        )
    }
}
