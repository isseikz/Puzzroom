package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.ProjectRepository
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * プロジェクト一覧取得UseCase
 */
class ListProjectsUseCase(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(): Result<List<Project>> {
        return repository.getAllProjects()
            .map { projects ->
                // IDの降順でソート（新しいものが上）
                projects.sortedByDescending { it.id }
            }
    }
}
