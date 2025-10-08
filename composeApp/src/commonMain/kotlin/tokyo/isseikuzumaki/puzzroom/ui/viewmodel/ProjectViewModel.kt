package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.usecase.DeleteProjectUseCase
import tokyo.isseikuzumaki.puzzroom.domain.usecase.ListProjectsUseCase
import tokyo.isseikuzumaki.puzzroom.domain.usecase.LoadProjectUseCase
import tokyo.isseikuzumaki.puzzroom.domain.usecase.SaveProjectUseCase
import tokyo.isseikuzumaki.puzzroom.ui.state.ProjectUiState
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState
import tokyo.isseikuzumaki.puzzroom.ui.state.toUiError
import kotlin.time.Duration.Companion.milliseconds

/**
 * プロジェクト管理のViewModel
 *
 * 自動保存、状態管理、エラーハンドリングを担当
 */
class ProjectViewModel(
    private val saveProjectUseCase: SaveProjectUseCase,
    private val loadProjectUseCase: LoadProjectUseCase,
    private val listProjectsUseCase: ListProjectsUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // 現在のプロジェクト
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // 保存状態
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Saved)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // プロジェクト変更検知
    private val projectChanges = MutableSharedFlow<Project>()

    init {
        // 自動保存の設定（500ms debounce）
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            projectChanges
                .debounce(500.milliseconds)
                .collect { project ->
                    autoSave(project)
                }
        }
    }

    /**
     * プロジェクト一覧を読み込み
     */
    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectUiState.Loading
            listProjectsUseCase()
                .onSuccess { projects ->
                    _uiState.value = ProjectUiState.ProjectList(projects)
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }

    /**
     * プロジェクトを開く
     */
    fun openProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectUiState.Loading
            loadProjectUseCase(projectId)
                .onSuccess { project ->
                    _currentProject.value = project
                    _uiState.value = ProjectUiState.EditingProject(project)
                    _saveState.value = SaveState.Saved
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }

    /**
     * 新規プロジェクトを作成
     */
    fun createNewProject(name: String = "新規プロジェクト") {
        val newProject = Project(name = name)
        _currentProject.value = newProject
        _uiState.value = ProjectUiState.EditingProject(newProject)
        _saveState.value = SaveState.Saved

        // 即座に保存
        viewModelScope.launch {
            autoSave(newProject)
        }
    }

    /**
     * プロジェクトを更新（自動保存トリガー）
     */
    fun updateProject(project: Project) {
        _currentProject.value = project

        // 編集中の状態も更新
        if (_uiState.value is ProjectUiState.EditingProject) {
            _uiState.value = ProjectUiState.EditingProject(project)
        }

        viewModelScope.launch {
            projectChanges.emit(project)
        }
    }

    /**
     * 自動保存
     */
    private suspend fun autoSave(project: Project) {
        _saveState.value = SaveState.Saving
        saveProjectUseCase(project)
            .onSuccess {
                _saveState.value = SaveState.Saved
            }
            .onFailure { error ->
                _saveState.value = SaveState.Failed(error.toUiError())
            }
    }

    /**
     * 手動保存（即座に保存）
     */
    fun saveNow() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            autoSave(project)
        }
    }

    /**
     * プロジェクトを削除
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            deleteProjectUseCase(projectId)
                .onSuccess {
                    // 削除後、一覧を再読み込み
                    loadProjects()
                }
                .onFailure { error ->
                    _uiState.value = ProjectUiState.Error(error.toUiError())
                }
        }
    }

    /**
     * プロジェクト一覧画面に戻る
     */
    fun backToProjectList() {
        _currentProject.value = null
        loadProjects()
    }

    /**
     * エラーをクリア
     */
    fun clearError() {
        if (_uiState.value is ProjectUiState.Error) {
            _uiState.value = ProjectUiState.Loading
        }
    }
}
