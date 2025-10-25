package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.templates.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.templates.RoomCreationTemplate
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ShapePlacementViewModel

/**
 * Room creation page - allows users to create rooms by placing walls and doors
 * 
 * State management for room creation, following the same pattern as FurniturePlacementPage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCreationPage(
    backgroundImageUrl: String? = null,
    onRoomCreated: (List<PlacedShape>) -> Unit = { _ -> },
    onCancel: () -> Unit = { },
    modifier: Modifier = Modifier,
    viewModel: ShapePlacementViewModel = viewModel { ShapePlacementViewModel() }
) {
    val placedShapes by viewModel.placedShapes.collectAsState()
    
    RoomCreationTemplate(
        backgroundImageUrl = backgroundImageUrl,
        placedShapes = placedShapes,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoomCreationPagePreview() {
    PuzzroomTheme {
        RoomCreationPage()
    }
}
