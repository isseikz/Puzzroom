package tokyo.isseikuzumaki.vibeterminal.util

/**
 * クリップボードへのコピー機能（プラットフォーム共通インターフェース）
 */
expect object ClipboardManager {
    /**
     * テキストをクリップボードにコピーする
     * @param text コピーするテキスト
     */
    fun copyToClipboard(text: String)
}
