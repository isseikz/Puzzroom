package tokyo.isseikuzumaki.puzzroom.ui.state

/**
 * ポリゴン編集の状態管理
 *
 * 辺や角度のロック状態、相似変換の適用状態を管理
 */
data class PolygonEditState(
    /**
     * ロックされた辺のインデックスのセット
     */
    val lockedEdges: Set<Int> = emptySet(),
    
    /**
     * ロックされた角度（頂点）のインデックスのセット
     */
    val lockedAngles: Set<Int> = emptySet(),
    
    /**
     * 相似変換が適用されたかどうか（初回の辺の長さ調整）
     */
    val hasAppliedSimilarity: Boolean = false
) {
    /**
     * 辺をロック/アンロック
     */
    fun toggleEdgeLock(edgeIndex: Int): PolygonEditState {
        return if (edgeIndex in lockedEdges) {
            copy(lockedEdges = lockedEdges - edgeIndex)
        } else {
            copy(lockedEdges = lockedEdges + edgeIndex)
        }
    }
    
    /**
     * 角度をロック/アンロック
     */
    fun toggleAngleLock(vertexIndex: Int): PolygonEditState {
        return if (vertexIndex in lockedAngles) {
            copy(lockedAngles = lockedAngles - vertexIndex)
        } else {
            copy(lockedAngles = lockedAngles + vertexIndex)
        }
    }
    
    /**
     * 角度を90度にロック
     */
    fun lockAngleTo90(vertexIndex: Int): PolygonEditState {
        return copy(lockedAngles = lockedAngles + vertexIndex)
    }
    
    /**
     * 相似変換を適用済みとしてマーク
     */
    fun markSimilarityApplied(): PolygonEditState {
        return copy(hasAppliedSimilarity = true)
    }
    
    /**
     * 辺がロックされているか判定
     */
    fun isEdgeLocked(edgeIndex: Int): Boolean = edgeIndex in lockedEdges
    
    /**
     * 角度がロックされているか判定
     */
    fun isAngleLocked(vertexIndex: Int): Boolean = vertexIndex in lockedAngles
    
    /**
     * 完全拘束判定: すべての辺と角度がロックされているか
     * （ただし、n角形では n-3 個の角度が決まれば残りの角度も自動的に決まる）
     */
    fun isFullyConstrained(vertexCount: Int): Boolean {
        val edgeCount = vertexCount
        // 完全拘束条件: すべての辺がロックされている、または
        // 十分な数の辺と角度がロックされている
        return lockedEdges.size == edgeCount || 
               (lockedEdges.size >= edgeCount - 1 && lockedAngles.size >= vertexCount - 3)
    }
}
