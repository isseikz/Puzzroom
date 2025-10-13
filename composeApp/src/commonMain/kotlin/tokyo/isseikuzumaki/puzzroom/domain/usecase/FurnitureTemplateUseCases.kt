package tokyo.isseikuzumaki.puzzroom.domain.usecase

/**
 * 家具テンプレート関連のUseCaseをまとめたクラス
 */
data class FurnitureTemplateUseCases(
    val saveTemplate: SaveFurnitureTemplateUseCase,
    val listTemplates: ListFurnitureTemplatesUseCase,
    val deleteTemplate: DeleteFurnitureTemplateUseCase,
)
