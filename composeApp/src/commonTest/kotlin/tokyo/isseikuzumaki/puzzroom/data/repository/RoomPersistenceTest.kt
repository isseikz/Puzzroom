package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.data.source.LocalProjectDataSourceImpl
import tokyo.isseikuzumaki.puzzroom.data.storage.FakeFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.*
import kotlin.test.*

/**
 * Test for Room information persistence
 * 
 * This test verifies that:
 * 1. Rotation information is preserved when saving and loading
 * 2. Multiple background shapes are stored separately (not merged)
 */
class RoomPersistenceTest {
    private lateinit var fileStorage: FakeFileStorage
    private lateinit var localDataSource: LocalProjectDataSourceImpl
    private lateinit var repository: ProjectRepositoryImpl

    @BeforeTest
    fun setup() {
        fileStorage = FakeFileStorage()
        localDataSource = LocalProjectDataSourceImpl(fileStorage)
        repository = ProjectRepositoryImpl(localDataSource)
    }

    @AfterTest
    fun tearDown() {
        fileStorage.clear()
    }

    @Test
    fun `Room with rotation should preserve rotation after save and load`() = runTest {
        // Given - Create a room with a shape that has rotation
        val shapeWithRotation = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(50)),
                    Point(Centimeter(0), Centimeter(50))
                )
            ),
            position = Point(Centimeter(50), Centimeter(50)),
            rotation = Degree(45f),
            colorArgb = 0xFF00FF00.toInt(),
            name = "Rotated Wall"
        )

        val room = Room(
            name = "Test Room with Rotation",
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
            id = "rotation-test",
            name = "Rotation Test Project",
            floorPlans = listOf(
                FloorPlan(
                    name = "Floor 1",
                    rooms = listOf(room)
                )
            )
        )

        // When - Save and reload
        repository.saveProject(project)
        val loadedProject = repository.getProjectById("rotation-test").getOrNull()

        // Then - Verify rotation is preserved
        assertNotNull(loadedProject)
        assertEquals(1, loadedProject.floorPlans.size)
        assertEquals(1, loadedProject.floorPlans[0].rooms.size)
        
        val loadedRoom = loadedProject.floorPlans[0].rooms[0]
        assertEquals(1, loadedRoom.shapes.size)
        
        val loadedShape = loadedRoom.shapes[0]
        assertEquals(45f, loadedShape.rotation.value, "Rotation should be preserved")
        assertEquals("Rotated Wall", loadedShape.name)
    }

    @Test
    fun `Room with multiple shapes should preserve all shapes separately`() = runTest {
        // Given - Create a room with multiple background shapes
        val shape1 = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(0))
                )
            ),
            position = Point(Centimeter(10), Centimeter(10)),
            rotation = Degree(0f),
            colorArgb = 0xFFFF0000.toInt(),
            name = "Wall 1"
        )

        val shape2 = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(150), Centimeter(0))
                )
            ),
            position = Point(Centimeter(50), Centimeter(50)),
            rotation = Degree(90f),
            colorArgb = 0xFF00FF00.toInt(),
            name = "Wall 2"
        )

        val shape3 = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(80), Centimeter(0))
                )
            ),
            position = Point(Centimeter(100), Centimeter(100)),
            rotation = Degree(180f),
            colorArgb = 0xFF0000FF.toInt(),
            name = "Wall 3"
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
            shapes = listOf(shape1, shape2, shape3)
        )

        val project = Project(
            id = "multi-shape-test",
            name = "Multi-Shape Test Project",
            floorPlans = listOf(
                FloorPlan(
                    name = "Floor 1",
                    rooms = listOf(room)
                )
            )
        )

        // When - Save and reload
        repository.saveProject(project)
        val loadedProject = repository.getProjectById("multi-shape-test").getOrNull()

        // Then - Verify all shapes are preserved separately
        assertNotNull(loadedProject)
        val loadedRoom = loadedProject.floorPlans[0].rooms[0]
        
        assertEquals(3, loadedRoom.shapes.size, "All three shapes should be preserved separately")
        
        // Verify first shape
        assertEquals("Wall 1", loadedRoom.shapes[0].name)
        assertEquals(0f, loadedRoom.shapes[0].rotation.value)
        assertEquals(Point(Centimeter(10), Centimeter(10)), loadedRoom.shapes[0].position)
        
        // Verify second shape
        assertEquals("Wall 2", loadedRoom.shapes[1].name)
        assertEquals(90f, loadedRoom.shapes[1].rotation.value)
        assertEquals(Point(Centimeter(50), Centimeter(50)), loadedRoom.shapes[1].position)
        
        // Verify third shape
        assertEquals("Wall 3", loadedRoom.shapes[2].name)
        assertEquals(180f, loadedRoom.shapes[2].rotation.value)
        assertEquals(Point(Centimeter(100), Centimeter(100)), loadedRoom.shapes[2].position)
    }

    @Test
    fun `Room shapes with different rotations should all be preserved`() = runTest {
        // Given - Create shapes with various rotation angles
        val rotations = listOf(0f, 30f, 45f, 90f, 135f, 180f, 270f, 315f)
        val shapes = rotations.mapIndexed { index, angle ->
            PlacedShapeData(
                shape = Polygon(
                    points = listOf(
                        Point(Centimeter(0), Centimeter(0)),
                        Point(Centimeter(50), Centimeter(0))
                    )
                ),
                position = Point(Centimeter(index * 20), Centimeter(index * 20)),
                rotation = Degree(angle),
                name = "Shape $index with ${angle}°"
            )
        }

        val room = Room(
            name = "Rotation Variety Room",
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(400), Centimeter(0)),
                    Point(Centimeter(400), Centimeter(400)),
                    Point(Centimeter(0), Centimeter(400))
                )
            ),
            shapes = shapes
        )

        val project = Project(
            id = "rotation-variety-test",
            name = "Rotation Variety Test",
            floorPlans = listOf(FloorPlan(name = "Floor 1", rooms = listOf(room)))
        )

        // When - Save and reload
        repository.saveProject(project)
        val loadedProject = repository.getProjectById("rotation-variety-test").getOrNull()

        // Then - Verify all rotation values are preserved
        assertNotNull(loadedProject)
        val loadedRoom = loadedProject.floorPlans[0].rooms[0]
        
        assertEquals(rotations.size, loadedRoom.shapes.size)
        
        rotations.forEachIndexed { index, expectedAngle ->
            val loadedShape = loadedRoom.shapes[index]
            assertEquals(
                expectedAngle, 
                loadedShape.rotation.value, 
                "Rotation ${expectedAngle}° should be preserved for shape $index"
            )
        }
    }

    @Test
    fun `Room shape polygon points should be preserved independently from background shapes`() = runTest {
        // Given - A room with a complex polygon AND background shapes
        val roomPolygon = Polygon(
            points = listOf(
                Point(Centimeter(0), Centimeter(0)),
                Point(Centimeter(400), Centimeter(0)),
                Point(Centimeter(400), Centimeter(300)),
                Point(Centimeter(250), Centimeter(300)),
                Point(Centimeter(250), Centimeter(500)),
                Point(Centimeter(0), Centimeter(500))
            )
        )

        val backgroundShape = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(0))
                )
            ),
            position = Point(Centimeter(50), Centimeter(50)),
            rotation = Degree(45f),
            name = "Background Wall"
        )

        val room = Room(
            name = "L-Shaped Room with Background",
            shape = roomPolygon,
            shapes = listOf(backgroundShape)
        )

        val project = Project(
            id = "polygon-test",
            name = "Polygon Test",
            floorPlans = listOf(FloorPlan(name = "Floor 1", rooms = listOf(room)))
        )

        // When - Save and reload
        repository.saveProject(project)
        val loadedProject = repository.getProjectById("polygon-test").getOrNull()

        // Then - Verify both room polygon and background shapes are preserved
        assertNotNull(loadedProject)
        val loadedRoom = loadedProject.floorPlans[0].rooms[0]
        
        // Room polygon should be preserved
        assertEquals(6, loadedRoom.shape.points.size, "Room polygon should have 6 points")
        assertEquals(roomPolygon.points, loadedRoom.shape.points)
        
        // Background shapes should be preserved separately
        assertEquals(1, loadedRoom.shapes.size)
        assertEquals("Background Wall", loadedRoom.shapes[0].name)
        assertEquals(2, loadedRoom.shapes[0].shape.points.size, "Background shape should have 2 points")
    }

    @Test
    fun `Room with zero rotation should preserve zero value`() = runTest {
        // Given - Shape with explicit 0 degree rotation
        val shapeWithZeroRotation = PlacedShapeData(
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(100), Centimeter(0))
                )
            ),
            position = Point(Centimeter(0), Centimeter(0)),
            rotation = Degree(0f),
            name = "Zero Rotation Shape"
        )

        val room = Room(
            name = "Zero Rotation Test",
            shape = Polygon(
                points = listOf(
                    Point(Centimeter(0), Centimeter(0)),
                    Point(Centimeter(200), Centimeter(0)),
                    Point(Centimeter(200), Centimeter(200)),
                    Point(Centimeter(0), Centimeter(200))
                )
            ),
            shapes = listOf(shapeWithZeroRotation)
        )

        val project = Project(
            id = "zero-rotation-test",
            name = "Zero Rotation Test",
            floorPlans = listOf(FloorPlan(name = "Floor 1", rooms = listOf(room)))
        )

        // When - Save and reload
        repository.saveProject(project)
        val loadedProject = repository.getProjectById("zero-rotation-test").getOrNull()

        // Then - Verify zero rotation is explicitly preserved
        assertNotNull(loadedProject)
        val loadedRoom = loadedProject.floorPlans[0].rooms[0]
        val loadedShape = loadedRoom.shapes[0]
        
        assertEquals(0f, loadedShape.rotation.value, "Zero rotation should be preserved")
    }
}
