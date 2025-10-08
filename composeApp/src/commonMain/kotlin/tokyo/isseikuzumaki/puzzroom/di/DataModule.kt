package tokyo.isseikuzumaki.puzzroom.di

import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepository
import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepositoryImpl
import tokyo.isseikuzumaki.puzzroom.data.source.LocalProjectDataSourceImpl
import tokyo.isseikuzumaki.puzzroom.data.storage.IFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.usecase.*

/**
 * データレイヤーのDI設定
 */
object DataModule {
    /**
     * ProjectRepositoryを提供
     */
    fun provideProjectRepository(fileStorage: IFileStorage): ProjectRepository {
        val localDataSource = LocalProjectDataSourceImpl(fileStorage)
        return ProjectRepositoryImpl(localDataSource)
    }

    /**
     * UseCasesを提供
     */
    fun provideUseCases(repository: ProjectRepository): ProjectUseCases {
        return ProjectUseCases(
            saveProject = SaveProjectUseCase(repository),
            loadProject = LoadProjectUseCase(repository),
            listProjects = ListProjectsUseCase(repository),
            deleteProject = DeleteProjectUseCase(repository)
        )
    }
}
