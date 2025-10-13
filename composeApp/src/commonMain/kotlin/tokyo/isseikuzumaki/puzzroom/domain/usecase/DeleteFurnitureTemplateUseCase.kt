package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.FurnitureTemplateRepository

/**
 * 家具テンプレート削除UseCase
 */
class DeleteFurnitureTemplateUseCase(
    private val repository: FurnitureTemplateRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.deleteTemplate(id)
    }
}
