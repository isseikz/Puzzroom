package tokyo.isseikuzumaki.puzzroom.domain

import kotlinx.serialization.Serializable

/**
 * A shape that has been placed in a room with position and rotation
 * This is a serializable domain model for storing placed shapes
 */
@Serializable
data class PlacedShapeData(
    val shape: Polygon,
    val position: Point,
    val rotation: Degree = Degree(0f),
    val colorArgb: Int = 0xFF4CAF50.toInt(), // Default green color as ARGB
    val name: String = "",
)
