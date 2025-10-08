package tokyo.isseikuzumaki.puzzroom.ui.state

/**
 * 保存状態
 */
sealed interface SaveState {
    /**
     * 保存済み
     */
    data object Saved : SaveState

    /**
     * 保存中
     */
    data object Saving : SaveState

    /**
     * 保存失敗
     */
    data class Failed(val error: UiError) : SaveState
}
