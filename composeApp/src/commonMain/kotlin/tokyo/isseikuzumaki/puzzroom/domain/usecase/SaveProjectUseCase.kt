package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepository
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * プロジェクト保存UseCase
 */
class SaveProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(project: Project): Result<Unit> {
        // バリデーション
        if (project.name.isBlank()) {
            return Result.failure(InvalidProjectException("プロジェクト名は必須です"))
        }

        return repository.saveProject(project)
    }
}

class InvalidProjectException(message: String) : Exception(message)
