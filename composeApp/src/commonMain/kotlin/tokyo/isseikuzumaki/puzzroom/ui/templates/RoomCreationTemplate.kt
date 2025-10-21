package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ButtonToCreate
import tokyo.isseikuzumaki.puzzroom.ui.organisms.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomShapeSelector
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeAttributeForm
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
import androidx.compose.ui.graphics.Color

/**
 * 部屋作成テンプレート（Template）
 * 壁や扉を配置して部屋を作成するためのテンプレート
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCreationTemplate(
    backgroundImageUrl: String? = null,
    shapeTypeToPlace: RoomShapeType? = null,
    shapeRotation: Float = 0f,
    placeableShapeTypes: List<RoomShapeType> = listOf(RoomShapeType.WALL, RoomShapeType.DOOR),
    placedShapes: List<PlacedShape> = emptyList(),
    selectedShapeIndex: Int? = null,
    onPositionUpdate: (position: Point?) -> Unit = { _ -> },
    onShapeSelected: (index: Int?) -> Unit = { _ -> },
    onShapeMoved: (index: Int, newPosition: Point) -> Unit = { _, _ -> },
    onShapeTypeSelected: (RoomShapeType) -> Unit = { _ -> },
    bottomSheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    ),
    onCancelBottomSheet: () -> Unit = { },
    onSaveBottomSheet: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Bottom
    ) {
        ShapeLayoutCanvas(
            backgroundImageUrl = backgroundImageUrl,
            shapeToPlace = null, // TODO: Create shape from shapeTypeToPlace
            shapeRotation = shapeRotation,
            placedShapes = placedShapes,
            selectedShapeIndex = selectedShapeIndex,
            onPositionUpdate = onPositionUpdate,
            onShapeSelected = onShapeSelected,
            onShapeMoved = onShapeMoved,
            modifier = modifier.weight(1f)
        )

        RoomShapeSelector(
            items = placeableShapeTypes,
            selectedShape = shapeTypeToPlace,
            onShapeSelected = onShapeTypeSelected,
            modifier = Modifier.fillMaxWidth().height(100.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )
        
        ButtonToCreate(
            onClick = { showBottomSheet = true },
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = bottomSheetState
            ) {
                ShapeAttributeForm(
                    onDismiss = {
                        onCancelBottomSheet()
                        showBottomSheet = false
                    },
                    onSave = { _ ->
                        onSaveBottomSheet()
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoomCreationTemplatePreview() {
    PuzzroomTheme {
        RoomCreationTemplate(
            placeableShapeTypes = listOf(
                RoomShapeType.WALL,
                RoomShapeType.DOOR,
                RoomShapeType.WINDOW
            ),
            placedShapes = listOf(
                PlacedShape(
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(200.cm(), 0.cm())
                        )
                    ),
                    position = Point(100.cm(), 100.cm()),
                    rotation = 0f.degree(),
                    color = Color.Gray,
                    name = "Wall 1"
                ),
                PlacedShape(
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(80.cm(), 0.cm()),
                            Point(80.cm(), 20.cm()),
                            Point(0.cm(), 20.cm())
                        )
                    ),
                    position = Point(250.cm(), 150.cm()),
                    rotation = 45f.degree(),
                    color = Color.Red,
                    name = "Door 1"
                )
            )
        )
    }
}
