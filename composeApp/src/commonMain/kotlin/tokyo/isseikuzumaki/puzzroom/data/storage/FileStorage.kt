package tokyo.isseikuzumaki.puzzroom.data.storage

import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * ファイルストレージの共通インターフェース
 */
interface IFileStorage {
    /**
     * プロジェクトをJSONファイルとして保存
     * @param project 保存するプロジェクト
     * @param fileName ファイル名（拡張子なし、自動的に .json が付与される）
     */
    suspend fun writeProject(project: Project, fileName: String)

    /**
     * JSONファイルからプロジェクトを読み込み
     * @param fileName ファイル名（拡張子なし）
     * @return プロジェクト、存在しない場合はnull
     */
    suspend fun readProject(fileName: String): Project?

    /**
     * プロジェクトファイルを削除
     * @param fileName ファイル名（拡張子なし）
     */
    suspend fun deleteProject(fileName: String)

    /**
     * 保存されているすべてのプロジェクトファイル名を取得
     * @return ファイル名のリスト（拡張子なし）
     */
    suspend fun listProjects(): List<String>

    /**
     * ストレージの初期化状態を確認
     * @return 初期化済みの場合true
     */
    fun isInitialized(): Boolean

    /**
     * 家具テンプレートをJSONファイルとして保存
     * @param template 保存する家具テンプレート
     * @param fileName ファイル名（拡張子なし、自動的に .json が付与される）
     */
    suspend fun writeFurnitureTemplate(template: FurnitureTemplate, fileName: String)

    /**
     * JSONファイルから家具テンプレートを読み込み
     * @param fileName ファイル名（拡張子なし）
     * @return 家具テンプレート、存在しない場合はnull
     */
    suspend fun readFurnitureTemplate(fileName: String): FurnitureTemplate?

    /**
     * 家具テンプレートファイルを削除
     * @param fileName ファイル名（拡張子なし）
     */
    suspend fun deleteFurnitureTemplate(fileName: String)

    /**
     * 保存されているすべての家具テンプレートファイル名を取得
     * @return ファイル名のリスト（拡張子なし）
     */
    suspend fun listFurnitureTemplates(): List<String>
}

/**
 * プラットフォーム固有のファイルストレージ
 *
 * Android: Context.filesDir/projects/ (プロジェクト), Context.filesDir/furniture_templates/ (家具テンプレート)
 * iOS: NSDocumentDirectory/projects/ (プロジェクト), NSDocumentDirectory/furniture_templates/ (家具テンプレート)
 */
expect class FileStorage : IFileStorage {
    /**
     * プロジェクトをJSONファイルとして保存
     * @param project 保存するプロジェクト
     * @param fileName ファイル名（拡張子なし、自動的に .json が付与される）
     */
    override suspend fun writeProject(project: Project, fileName: String)

    /**
     * JSONファイルからプロジェクトを読み込み
     * @param fileName ファイル名（拡張子なし）
     * @return プロジェクト、存在しない場合はnull
     */
    override suspend fun readProject(fileName: String): Project?

    /**
     * プロジェクトファイルを削除
     * @param fileName ファイル名（拡張子なし）
     */
    override suspend fun deleteProject(fileName: String)

    /**
     * 保存されているすべてのプロジェクトファイル名を取得
     * @return ファイル名のリスト（拡張子なし）
     */
    override suspend fun listProjects(): List<String>

    /**
     * ストレージの初期化状態を確認
     * @return 初期化済みの場合true
     */
    override fun isInitialized(): Boolean

    /**
     * 家具テンプレートをJSONファイルとして保存
     * @param template 保存する家具テンプレート
     * @param fileName ファイル名（拡張子なし、自動的に .json が付与される）
     */
    override suspend fun writeFurnitureTemplate(template: FurnitureTemplate, fileName: String)

    /**
     * JSONファイルから家具テンプレートを読み込み
     * @param fileName ファイル名（拡張子なし）
     * @return 家具テンプレート、存在しない場合はnull
     */
    override suspend fun readFurnitureTemplate(fileName: String): FurnitureTemplate?

    /**
     * 家具テンプレートファイルを削除
     * @param fileName ファイル名（拡張子なし）
     */
    override suspend fun deleteFurnitureTemplate(fileName: String)

    /**
     * 保存されているすべての家具テンプレートファイル名を取得
     * @return ファイル名のリスト（拡張子なし）
     */
    override suspend fun listFurnitureTemplates(): List<String>
}
