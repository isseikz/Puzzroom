package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.ui.atoms.*
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ConfirmationDialog
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ImageWithFallback
import tokyo.isseikuzumaki.puzzroom.ui.molecules.TitleWithSubtitle

/**
 * Project card organism
 */
@Composable
fun ProjectCardItem(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            ImageWithFallback(
                imageUrl = project.layoutUrl,
                fallbackText = project.name
            )

            HorizontalSpacer(width = 16.dp)

            // Title and subtitle
            TitleWithSubtitle(
                title = project.name,
                subtitle = "${project.floorPlans.size} floor plans",
                modifier = Modifier.weight(1f)
            )

            // Edit button
            AppIconButton(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename",
                onClick = { showRenameDialog = true }
            )

            // Delete button
            AppIconButton(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                onClick = { showDeleteDialog = true },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(project.name) }
        
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { AppText("Rename Project") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { AppText("Project Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                AppTextButton(
                    text = "Rename",
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName)
                            showRenameDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                AppTextButton(
                    text = "Cancel",
                    onClick = { showRenameDialog = false }
                )
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Project",
            message = "Are you sure you want to delete \"${project.name}\"? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true
        )
    }
}
