package tokyo.isseikuzumaki.puzzroom.domain.usecase

/**
 * プロジェクト関連のUseCaseをまとめたコンテナ
 */
data class ProjectUseCases(
    val saveProject: SaveProjectUseCase,
    val loadProject: LoadProjectUseCase,
    val listProjects: ListProjectsUseCase,
    val deleteProject: DeleteProjectUseCase,
)
