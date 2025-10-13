package tokyo.isseikuzumaki.puzzroom.domain.usecase

import tokyo.isseikuzumaki.puzzroom.data.repository.FurnitureTemplateRepository
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * 家具テンプレート保存UseCase
 */
class SaveFurnitureTemplateUseCase(
    private val repository: FurnitureTemplateRepository
) {
    suspend operator fun invoke(template: FurnitureTemplate): Result<Unit> {
        // バリデーション
        if (template.name.isBlank()) {
            return Result.failure(InvalidFurnitureTemplateException("家具テンプレート名は必須です"))
        }
        if (template.width.value <= 0) {
            return Result.failure(InvalidFurnitureTemplateException("幅は0より大きい値を指定してください"))
        }
        if (template.depth.value <= 0) {
            return Result.failure(InvalidFurnitureTemplateException("奥行きは0より大きい値を指定してください"))
        }

        return repository.saveTemplate(template)
    }
}

class InvalidFurnitureTemplateException(message: String) : Exception(message)
