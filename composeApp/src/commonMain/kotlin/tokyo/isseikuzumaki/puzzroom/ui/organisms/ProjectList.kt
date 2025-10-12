package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * Project list organism
 */
@Composable
fun ProjectList(
    projects: List<Project>,
    onProjectClick: (String) -> Unit,
    onProjectDelete: (String) -> Unit,
    onProjectRename: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(projects) { project ->
            ProjectCardItem(
                project = project,
                onClick = { onProjectClick(project.id) },
                onDelete = { onProjectDelete(project.id) },
                onRename = { newName -> onProjectRename(project.id, newName) }
            )
        }
    }
}
