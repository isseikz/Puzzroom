package tokyo.isseikuzumaki.puzzroom.ui.templates

import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.PlacedShapeData
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals

class PlacedShapeConversionTest {

    @Test
    fun testPlacedShapeToDataConversion() {
        // Create a PlacedShape (UI model)
        val placedShape = PlacedShape.createWall(Centimeter(100))
        
        // Convert to Room which includes PlacedShapeData
        val room = convertPlacedShapesToRoom(listOf(placedShape), "Test Room")
        
        // Verify the room has the shapes stored
        assertEquals(1, room.shapes.size)
        val shapeData = room.shapes.first()
        
        // Verify the shape data preserves the structure
        assertEquals(placedShape.shape.points.size, shapeData.shape.points.size)
        assertEquals(placedShape.position, shapeData.position)
        assertEquals(placedShape.rotation.value, shapeData.rotation.value)
    }

    @Test
    fun testPlacedShapeDataToPlacedShapeConversion() {
        // Create a PlacedShapeData (domain model)
        val shapeData = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(0))
                )
            ),
            position = Point(Centimeter(50), Centimeter(50)),
            rotation = Degree(45f),
            colorArgb = 0xFF00FF00.toInt(),
            name = "Test Wall"
        )
        
        // Convert to PlacedShape (UI model)
        val placedShape = PlacedShape.fromData(shapeData)
        
        // Verify the conversion preserves data
        assertEquals(shapeData.shape.points.size, placedShape.shape.points.size)
        assertEquals(shapeData.position, placedShape.position)
        assertEquals(shapeData.rotation.value, placedShape.rotation.value)
        assertEquals(shapeData.name, placedShape.name)
    }

    @Test
    fun testRoundTripConversion() {
        // Create original PlacedShape
        val original = PlacedShape(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(200), Centimeter(0))
                )
            ),
            position = Point(Centimeter(100), Centimeter(100)),
            rotation = Degree(90f),
            name = "Wall 1"
        )
        
        // Convert to Room and back
        val room = convertPlacedShapesToRoom(listOf(original), "Test Room")
        val restored = room.shapes.map { PlacedShape.fromData(it) }.first()
        
        // Verify round-trip preserves essential data
        assertEquals(original.shape.points, restored.shape.points)
        assertEquals(original.position, restored.position)
        assertEquals(original.rotation.value, restored.rotation.value)
        assertEquals(original.name, restored.name)
    }
}
