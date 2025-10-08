package tokyo.isseikuzumaki.puzzroom.data.storage

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
}

/**
 * プラットフォーム固有のファイルストレージ
 *
 * Android: Context.filesDir/projects/
 * iOS: NSDocumentDirectory/projects/
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
}
