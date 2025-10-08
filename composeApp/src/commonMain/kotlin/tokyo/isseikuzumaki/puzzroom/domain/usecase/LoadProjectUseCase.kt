package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepository
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * プロジェクト読み込みUseCase
 */
class LoadProjectUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Result<Project> {
        return repository.getProjectById(projectId)
            .mapCatching { project ->
                project ?: throw ProjectNotFoundException(projectId)
            }
    }
}

class ProjectNotFoundException(projectId: String) :
    Exception("Project not found: $projectId")
