package tokyo.isseikuzumaki.puzzroom.ui.state

import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * プロジェクト画面のUI状態
 */
sealed interface ProjectUiState {
    /**
     * 読み込み中
     */
    data object Loading : ProjectUiState

    /**
     * プロジェクト一覧表示
     */
    data class ProjectList(val projects: List<Project>) : ProjectUiState

    /**
     * プロジェクト編集中
     */
    data class EditingProject(val project: Project) : ProjectUiState

    /**
     * エラー状態
     */
    data class Error(val error: UiError) : ProjectUiState
}
