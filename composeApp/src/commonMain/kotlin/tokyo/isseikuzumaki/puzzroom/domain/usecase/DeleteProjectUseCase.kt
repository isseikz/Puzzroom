package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepository

/**
 * プロジェクト削除UseCase
 */
class DeleteProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Result<Unit> {
        return repository.deleteProject(projectId)
    }
}
