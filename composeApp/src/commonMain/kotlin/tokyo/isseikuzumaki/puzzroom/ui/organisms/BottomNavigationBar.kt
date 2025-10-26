package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tokyo.isseikuzumaki.puzzroom.AppScreen
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText

/**
 * Navigation item data
 */
private data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

/**
 * Bottom navigation bar organism
 * 
 * Provides main navigation between app sections using Material3 NavigationBar
 */
@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem(
            screen = AppScreen.ProjectList,
            label = "Projects",
            icon = Icons.Default.Home
        ),
        NavigationItem(
            screen = AppScreen.FurnitureManagement,
            label = "Library",
            icon = Icons.Default.List
        ),
        NavigationItem(
            screen = AppScreen.Room,
            label = "Room",
            icon = Icons.Default.Edit
        ),
        NavigationItem(
            screen = AppScreen.Furniture,
            label = "Place",
            icon = Icons.Default.MoreVert
        )
    )

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.screen,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { AppText(item.label) }
            )
        }
    }
}
