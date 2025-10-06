package tokyo.isseikuzumaki.puzzroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.screen.FurnitureScreen
import tokyo.isseikuzumaki.puzzroom.ui.screen.RoomScreen

@Composable
@Preview
fun App(
    navController: NavHostController = rememberNavController()
) {
    val appState = remember { AppState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination?.route

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppScreen.Room.name,
                modifier = Modifier.fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                composable(route = AppScreen.Room.name) {
                    RoomScreen(appState = appState)
                }
                composable(route = AppScreen.Furniture.name) {
                    FurnitureScreen(appState = appState)
                }
                composable(route = AppScreen.File.name) {  }
            }

            AppBar(
                navController = navController,
                currentScreen = AppScreen.valueOf(currentDestination ?: AppScreen.Room.name),
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(color = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}