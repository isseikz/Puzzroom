package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.PreviewTemplate
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

@Composable
fun RoomSelectionItemCard(
    room: Room,
    onClick: () -> Unit = { },
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                room.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Number of vertices: ${room.shape.points.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RoomSelectionList(
    rooms: List<Room>,
    onRoomSelected: (Room) -> Unit,
) {
    if (rooms.isEmpty()) {
        Text(
            "No rooms are registered. Please create a room on the Room screen.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rooms) { room ->
                RoomSelectionItemCard(room) {
                    onRoomSelected(room)
                }
            }
        }
    }
}

@Preview
@Composable
fun RoomSelectionItemCardPreview() {
    PreviewTemplate {
        PuzzroomTheme {
            RoomSelectionItemCard(
                room = Room(
                    id = "room1",
                    name = "Living Room",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm, 0.cm),
                            Point(400.cm, 0.cm),
                            Point(400.cm, 300.cm),
                            Point(0.cm, 300.cm)
                        )
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun RoomSelectionListPreview() {
    PreviewTemplate {
        RoomSelectionList(
            rooms = listOf(
                Room(
                    id = "room1",
                    name = "Living Room",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm, 0.cm),
                            Point(400.cm, 0.cm),
                            Point(400.cm, 300.cm),
                            Point(0.cm, 300.cm)
                        )
                    )
                ),
                Room(
                    id = "room2",
                    name = "Bedroom",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm, 0.cm),
                            Point(300.cm, 0.cm),
                            Point(300.cm, 400.cm),
                            Point(0.cm, 400.cm)
                        )
                    )
                )
            ),
            onRoomSelected = {}
        )
    }
}
