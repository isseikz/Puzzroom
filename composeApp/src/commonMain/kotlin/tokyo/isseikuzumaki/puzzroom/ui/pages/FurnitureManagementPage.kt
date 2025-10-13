package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.organisms.EmptyState
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureTemplateCard
import tokyo.isseikuzumaki.puzzroom.ui.templates.ListScreenTemplate
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.FurnitureTemplateViewModel

/**
 * Furniture management page - allows users to manage furniture templates
 */
@Composable
fun FurnitureManagementPage(
    appState: AppState,
    furnitureTemplateViewModel: FurnitureTemplateViewModel,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val templates by furnitureTemplateViewModel.allTemplates.collectAsState()
    
    ListScreenTemplate(
        title = "Furniture Library",
        actions = {
            AppIconButton(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Furniture",
                onClick = onCreateNew
            )
        }
    ) { contentModifier ->

        if (templates.isEmpty()) {
            EmptyState(
                title = "No furniture templates",
                message = "Let's create a new furniture template",
                actionText = "Create New",
                onAction = onCreateNew,
                modifier = contentModifier
            )
        } else {
            LazyColumn(
                modifier = contentModifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    FurnitureTemplateCard(
                        template = template,
                        isSelected = false,
                        onClick = {
                            // TODO: Navigate to edit screen or show details
                        }
                    )
                }
            }
        }
    }
}
