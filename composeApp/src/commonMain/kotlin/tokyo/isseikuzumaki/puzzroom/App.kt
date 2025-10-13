package tokyo.isseikuzumaki.puzzroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import tokyo.isseikuzumaki.puzzroom.ui.pages.FurnitureCreationPage
import tokyo.isseikuzumaki.puzzroom.ui.pages.FurnitureManagementPage
import tokyo.isseikuzumaki.puzzroom.ui.pages.ProjectListPage
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.rememberProjectViewModel
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.rememberFurnitureTemplateViewModel

@Composable
@Preview
fun App(
    navController: NavHostController = rememberNavController()
) {
    val appState = remember { AppState() }
    val projectViewModel = rememberProjectViewModel()
    val furnitureTemplateViewModel = rememberFurnitureTemplateViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination?.route

    PuzzroomTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = AppScreen.ProjectList.name,
                modifier = Modifier.fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                composable(route = AppScreen.ProjectList.name) {
                    ProjectListPage(
                        viewModel = projectViewModel,
                        onProjectClick = { projectId ->
                            projectViewModel.openProject(projectId)
                            navController.navigate(AppScreen.Room.name)
                        },
                        onCreateNew = {
                            projectViewModel.createNewProject()
                            navController.navigate(AppScreen.Room.name)
                        }
                    )
                }
                composable(route = AppScreen.FurnitureManagement.name) {
                    FurnitureManagementPage(
                        appState = appState,
                        furnitureTemplateViewModel = furnitureTemplateViewModel,
                        onCreateNew = {
                            navController.navigate(AppScreen.FurnitureCreation.name)
                        }
                    )
                }
                composable(route = AppScreen.FurnitureCreation.name) {
                    FurnitureCreationPage(
                        appState = appState,
                        furnitureTemplateViewModel = furnitureTemplateViewModel,
                        onFurnitureCreated = {
                            navController.navigateUp()
                        },
                        onCancel = {
                            navController.navigateUp()
                        }
                    )
                }
                composable(route = AppScreen.Room.name) {
                    RoomScreen(
                        appState = appState,
                        viewModel = projectViewModel
                    )
                }
                composable(route = AppScreen.Furniture.name) {
                    FurnitureScreen(
                        appState = appState,
                        viewModel = projectViewModel,
                        furnitureTemplateViewModel = furnitureTemplateViewModel
                    )
                }
                composable(route = AppScreen.File.name) {  }
            }

            // プロジェクト一覧以外の画面でナビゲーションバーを表示
            if (currentDestination != AppScreen.ProjectList.name) {
                AppBar(
                    navController = navController,
                    currentScreen = AppScreen.valueOf(currentDestination ?: AppScreen.ProjectList.name),
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(color = MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
    }
}