package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.ui.molecules.EmptyProjectListMessage
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ErrorMessage
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ProjectList
import tokyo.isseikuzumaki.puzzroom.ui.state.ProjectUiState
import tokyo.isseikuzumaki.puzzroom.ui.templates.ProjectListTemplate
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

/**
 * Project list page (Atomic Design - Page layer)
 * プロジェクト一覧ページ
 * 
 * This is the Page layer in Atomic Design, which:
 * - Connects to ViewModel for state and events
 * - Uses Templates for layout structure
 * - Passes data down to Organisms, Molecules, and Atoms
 */
@Composable
fun ProjectListPage(
    viewModel: ProjectViewModel,
    onProjectClick: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    ProjectListTemplate(
        onCreateNew = onCreateNew
    ) { paddingValues ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProjectUiState.ProjectList -> {
                if (state.projects.isEmpty()) {
                    EmptyProjectListMessage(
                        onCreateNew = onCreateNew,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    ProjectList(
                        projects = state.projects,
                        onProjectClick = onProjectClick,
                        onProjectDelete = { projectId ->
                            viewModel.deleteProject(projectId)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
            is ProjectUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.loadProjects() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is ProjectUiState.EditingProject -> {
                // 編集画面は別のルートで表示
            }
        }
    }
}
