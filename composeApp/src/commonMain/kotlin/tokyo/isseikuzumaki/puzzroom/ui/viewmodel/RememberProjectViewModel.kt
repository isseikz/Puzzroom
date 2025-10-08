package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.puzzroom.di.DataModule

/**
 * ProjectViewModelを生成
 *
 * プラットフォーム固有のFileStorageは rememberFileStorage() で生成され、
 * それ以降の依存関係は共通コードで構築されます。
 */
@Composable
fun rememberProjectViewModel(): ProjectViewModel {
    val fileStorage = rememberFileStorage()
    val repository = remember(fileStorage) {
        DataModule.provideProjectRepository(fileStorage)
    }
    val useCases = remember(repository) {
        DataModule.provideUseCases(repository)
    }

    return viewModel {
        ProjectViewModel(
            useCases.saveProject,
            useCases.loadProject,
            useCases.listProjects,
            useCases.deleteProject
        )
    }
}
