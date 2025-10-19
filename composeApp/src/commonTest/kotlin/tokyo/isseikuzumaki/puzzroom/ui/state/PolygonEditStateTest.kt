package tokyo.isseikuzumaki.puzzroom.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolygonEditStateTest {

    @Test
    fun testToggleEdgeLock() {
        val state = PolygonEditState()
        
        // Lock an edge
        val lockedState = state.toggleEdgeLock(0)
        assertTrue(lockedState.isEdgeLocked(0))
        assertEquals(1, lockedState.lockedEdges.size)
        
        // Unlock the edge
        val unlockedState = lockedState.toggleEdgeLock(0)
        assertFalse(unlockedState.isEdgeLocked(0))
        assertEquals(0, unlockedState.lockedEdges.size)
    }

    @Test
    fun testToggleAngleLock() {
        val state = PolygonEditState()
        
        // Lock an angle
        val lockedState = state.toggleAngleLock(0)
        assertTrue(lockedState.isAngleLocked(0))
        assertEquals(1, lockedState.lockedAngles.size)
        
        // Unlock the angle
        val unlockedState = lockedState.toggleAngleLock(0)
        assertFalse(unlockedState.isAngleLocked(0))
        assertEquals(0, unlockedState.lockedAngles.size)
    }

    @Test
    fun testLockAngleTo90() {
        val state = PolygonEditState()
        
        val lockedState = state.lockAngleTo90(1)
        assertTrue(lockedState.isAngleLocked(1))
        assertEquals(1, lockedState.lockedAngles.size)
    }

    @Test
    fun testMarkSimilarityApplied() {
        val state = PolygonEditState()
        assertFalse(state.hasAppliedSimilarity)
        
        val appliedState = state.markSimilarityApplied()
        assertTrue(appliedState.hasAppliedSimilarity)
    }

    @Test
    fun testIsFullyConstrained_AllEdgesLocked() {
        val state = PolygonEditState()
        
        // Square (4 vertices)
        val vertexCount = 4
        
        // Lock all edges
        var lockedState = state
        for (i in 0 until vertexCount) {
            lockedState = lockedState.toggleEdgeLock(i)
        }
        
        assertTrue(lockedState.isFullyConstrained(vertexCount))
    }

    @Test
    fun testIsFullyConstrained_EdgeAndAngleLocked() {
        val state = PolygonEditState()
        
        // Square (4 vertices)
        val vertexCount = 4
        
        // Lock 3 edges and 1 angle (should be constrained: n-1 edges + n-3 angles)
        var lockedState = state
            .toggleEdgeLock(0)
            .toggleEdgeLock(1)
            .toggleEdgeLock(2)
            .toggleAngleLock(0)
        
        assertTrue(lockedState.isFullyConstrained(vertexCount))
    }

    @Test
    fun testIsFullyConstrained_NotFullyConstrained() {
        val state = PolygonEditState()
        
        // Square (4 vertices)
        val vertexCount = 4
        
        // Lock only 2 edges
        val lockedState = state
            .toggleEdgeLock(0)
            .toggleEdgeLock(1)
        
        assertFalse(lockedState.isFullyConstrained(vertexCount))
    }

    @Test
    fun testMultipleLocks() {
        val state = PolygonEditState()
        
        val lockedState = state
            .toggleEdgeLock(0)
            .toggleEdgeLock(2)
            .toggleAngleLock(1)
            .toggleAngleLock(3)
        
        assertTrue(lockedState.isEdgeLocked(0))
        assertFalse(lockedState.isEdgeLocked(1))
        assertTrue(lockedState.isEdgeLocked(2))
        
        assertTrue(lockedState.isAngleLocked(1))
        assertTrue(lockedState.isAngleLocked(3))
        assertFalse(lockedState.isAngleLocked(0))
        
        assertEquals(2, lockedState.lockedEdges.size)
        assertEquals(2, lockedState.lockedAngles.size)
    }
}
