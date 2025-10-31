package tokyo.isseikuzumaki.puzzroom.data.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import tokyo.isseikuzumaki.puzzroom.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test to verify JSON serialization/deserialization of Room data
 * This specifically tests that rotation and multiple shapes are preserved
 */
class RoomSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `PlacedShapeData with rotation should serialize and deserialize correctly`() {
        // Given
        val original = PlacedShapeData(
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

        // When
        val jsonString = json.encodeToString(original)
        println("Serialized PlacedShapeData:")
        println(jsonString)
        
        val deserialized = json.decodeFromString<PlacedShapeData>(jsonString)

        // Then
        assertEquals(original.shape.points, deserialized.shape.points)
        assertEquals(original.position, deserialized.position)
        assertEquals(original.rotation.value, deserialized.rotation.value, "Rotation should be preserved")
        assertEquals(original.colorArgb, deserialized.colorArgb)
        assertEquals(original.name, deserialized.name)
    }

    @Test
    fun `Room with multiple shapes should serialize all shapes`() {
        // Given
        val shape1 = PlacedShapeData(
            shape = Polygon(points = listOf(Point(Centimeter(0), Centimeter(0)), Point(Centimeter(100), Centimeter(0)))),
            position = Point(Centimeter(10), Centimeter(10)),
            rotation = Degree(0f),
            name = "Wall 1"
        )

        val shape2 = PlacedShapeData(
            shape = Polygon(points = listOf(Point(Centimeter(0), Centimeter(0)), Point(Centimeter(150), Centimeter(0)))),
            position = Point(Centimeter(50), Centimeter(50)),
            rotation = Degree(90f),
            name = "Wall 2"
        )

        val room = Room(
            name = "Multi-Shape Room",
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(300), Centimeter(0)),
                    Point(Centimeter(300), Centimeter(300)),
                    Point(Centimeter(0), Centimeter(300))
                )
            ),
            shapes = listOf(shape1, shape2)
        )

        // When
        val jsonString = json.encodeToString(room)
        println("\nSerialized Room:")
        println(jsonString)
        
        val deserialized = json.decodeFromString<Room>(jsonString)

        // Then
        assertEquals(2, deserialized.shapes.size, "Both shapes should be in the deserialized room")
        assertEquals("Wall 1", deserialized.shapes[0].name)
        assertEquals(0f, deserialized.shapes[0].rotation.value)
        assertEquals("Wall 2", deserialized.shapes[1].name)
        assertEquals(90f, deserialized.shapes[1].rotation.value)
    }

    @Test
    fun `Project with Room containing shapes should serialize completely`() {
        // Given
        val shapeWithRotation = PlacedShapeData(
            shape = Polygon(points = listOf(Point(Centimeter(0), Centimeter(0)), Point(Centimeter(80), Centimeter(0)))),
            position = Point(Centimeter(100), Centimeter(100)),
            rotation = Degree(135f),
            colorArgb = 0xFFFF0000.toInt(),
            name = "Diagonal Wall"
        )

        val room = Room(
            name = "Test Room",
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(200), Centimeter(0)),
                    Point(Centimeter(200), Centimeter(200)),
                    Point(Centimeter(0), Centimeter(200))
                )
            ),
            shapes = listOf(shapeWithRotation)
        )

        val project = Project(
            id = "test-project",
            name = "Test Project",
            floorPlans = listOf(
                FloorPlan(
                    name = "Floor 1",
                    rooms = listOf(room)
                )
            )
        )

        // When
        val jsonString = json.encodeToString(project)
        println("\nSerialized Project:")
        println(jsonString)
        
        val deserialized = json.decodeFromString<Project>(jsonString)

        // Then
        assertEquals(1, deserialized.floorPlans.size)
        assertEquals(1, deserialized.floorPlans[0].rooms.size)
        
        val deserializedRoom = deserialized.floorPlans[0].rooms[0]
        assertEquals(1, deserializedRoom.shapes.size)
        assertEquals("Diagonal Wall", deserializedRoom.shapes[0].name)
        assertEquals(135f, deserializedRoom.shapes[0].rotation.value, "Rotation should be preserved in full project")
    }

    @Test
    fun `Zero rotation should be explicitly serialized`() {
        // Given
        val shapeWithZeroRotation = PlacedShapeData(
            shape = Polygon(points = listOf(Point(Centimeter(0), Centimeter(0)), Point(Centimeter(50), Centimeter(0)))),
            position = Point(Centimeter(0), Centimeter(0)),
            rotation = Degree(0f),
            name = "Zero Rotation"
        )

        // When
        val jsonString = json.encodeToString(shapeWithZeroRotation)
        println("\nSerialized shape with zero rotation:")
        println(jsonString)

        // Then - Check that rotation field exists in JSON
        assert(jsonString.contains("\"rotation\"")) { "JSON should contain rotation field" }
        
        val deserialized = json.decodeFromString<PlacedShapeData>(jsonString)
        assertEquals(0f, deserialized.rotation.value)
    }

    @Test
    fun `Different rotation angles should all serialize correctly`() {
        // Given
        val rotations = listOf(0f, 30f, 45f, 90f, 135f, 180f, 270f, 315f, 359f)
        
        rotations.forEach { angle ->
            val shape = PlacedShapeData(
                shape = Polygon(points = listOf(Point(Centimeter(0), Centimeter(0)), Point(Centimeter(10), Centimeter(0)))),
                position = Point(Centimeter(0), Centimeter(0)),
                rotation = Degree(angle),
                name = "Shape $angle"
            )

            // When
            val jsonString = json.encodeToString(shape)
            val deserialized = json.decodeFromString<PlacedShapeData>(jsonString)

            // Then
            assertEquals(angle, deserialized.rotation.value, "Rotation $angle should be preserved")
        }
    }
}
