package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.FurnitureTemplateRepository
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * 家具テンプレート一覧取得UseCase
 */
class ListFurnitureTemplatesUseCase(
    private val repository: FurnitureTemplateRepository
) {
    suspend operator fun invoke(): Result<List<FurnitureTemplate>> {
        return repository.getAllTemplates()
    }
}
