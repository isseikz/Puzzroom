package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.puzzroom.di.DataModule

/**
 * FurnitureTemplateViewModelを生成
 *
 * プラットフォーム固有のFileStorageは rememberFileStorage() で生成され、
 * それ以降の依存関係は共通コードで構築されます。
 */
@Composable
fun rememberFurnitureTemplateViewModel(): FurnitureTemplateViewModel {
    val fileStorage = rememberFileStorage()
    val repository = remember(fileStorage) {
        DataModule.provideFurnitureTemplateRepository(fileStorage)
    }
    val useCases = remember(repository) {
        DataModule.provideFurnitureTemplateUseCases(repository)
    }

    return viewModel {
        FurnitureTemplateViewModel(
            useCases.saveTemplate,
            useCases.listTemplates,
            useCases.deleteTemplate
        )
    }
}
