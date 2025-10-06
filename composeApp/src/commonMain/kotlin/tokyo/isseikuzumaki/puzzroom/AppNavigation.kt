package tokyo.isseikuzumaki.puzzroom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

enum class AppScreen {
    Room,
    Furniture,
    File
}

@Composable
fun NavigationButton(
    text: String,
    targetScreen: AppScreen,
    currentScreen: AppScreen,
    onClick: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onClick(targetScreen) },
        enabled = currentScreen != targetScreen,
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun AppBar(
    navController: NavHostController,
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        NavigationButton(
            text = "Room",
            targetScreen = AppScreen.Room,
            currentScreen = currentScreen,
            onClick = { screen -> navController.navigate(screen.name) },
        )

        NavigationButton(
            text = "Furniture",
            targetScreen = AppScreen.Furniture,
            currentScreen = currentScreen,
            onClick = { screen -> navController.navigate(screen.name) },
        )
    }
}