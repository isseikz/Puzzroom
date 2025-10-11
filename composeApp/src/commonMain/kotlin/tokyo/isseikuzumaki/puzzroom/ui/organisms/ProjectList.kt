package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ProjectCardItem

/**
 * Project list organism
 * プロジェクト一覧を表示する有機体コンポーネント
 */
@Composable
fun ProjectList(
    projects: List<Project>,
    onProjectClick: (String) -> Unit,
    onProjectDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(projects) { project ->
            ProjectCardItem(
                project = project,
                onClick = { onProjectClick(project.id) },
                onDelete = { onProjectDelete(project.id) }
            )
        }
    }
}
