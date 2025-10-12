package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.organisms.EmptyState
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ErrorDisplay
import tokyo.isseikuzumaki.puzzroom.ui.organisms.LoadingIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ProjectList
import tokyo.isseikuzumaki.puzzroom.ui.state.ProjectUiState
import tokyo.isseikuzumaki.puzzroom.ui.templates.ListScreenTemplate
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

/**
 * Project list page - combines template with data
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

    ListScreenTemplate(
        title = "My Projects",
        actions = {
            AppIconButton(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New",
                onClick = onCreateNew
            )
        }
    ) { modifier ->
        when (val state = uiState) {
            is ProjectUiState.Loading -> {
                LoadingIndicator(modifier = modifier)
            }
            is ProjectUiState.ProjectList -> {
                if (state.projects.isEmpty()) {
                    EmptyState(
                        title = "No projects",
                        message = "Let's create a new project",
                        actionText = "Create New",
                        onAction = onCreateNew,
                        modifier = modifier
                    )
                } else {
                    ProjectList(
                        projects = state.projects,
                        onProjectClick = onProjectClick,
                        onProjectDelete = { projectId -> viewModel.deleteProject(projectId) },
                        onProjectRename = { projectId, newName -> viewModel.renameProject(projectId, newName) },
                        modifier = modifier
                    )
                }
            }
            is ProjectUiState.Error -> {
                ErrorDisplay(
                    error = state.error,
                    onRetry = { viewModel.loadProjects() },
                    modifier = modifier
                )
            }
            is ProjectUiState.EditingProject -> {
                // The editing screen is displayed on a different route
            }
        }
    }
}
