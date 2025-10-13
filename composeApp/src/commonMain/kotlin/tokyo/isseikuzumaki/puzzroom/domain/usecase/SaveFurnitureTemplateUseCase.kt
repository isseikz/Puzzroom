package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.FurnitureTemplateRepository
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * 家具テンプレート保存UseCase
 * 
 * Note: バリデーションは FurnitureTemplate の init ブロックで行われます
 */
class SaveFurnitureTemplateUseCase(
    private val repository: FurnitureTemplateRepository
) {
    suspend operator fun invoke(template: FurnitureTemplate): Result<Unit> {
        return repository.saveTemplate(template)
    }
}
