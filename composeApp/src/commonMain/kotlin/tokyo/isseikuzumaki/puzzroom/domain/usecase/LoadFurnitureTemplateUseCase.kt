package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.FurnitureTemplateRepository
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * 家具テンプレート読み込みUseCase
 */
class LoadFurnitureTemplateUseCase(
    private val repository: FurnitureTemplateRepository
) {
    suspend operator fun invoke(id: String): Result<FurnitureTemplate?> {
        return repository.getTemplateById(id)
    }
}
