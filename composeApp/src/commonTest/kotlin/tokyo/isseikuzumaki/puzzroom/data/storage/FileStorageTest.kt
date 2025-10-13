package tokyo.isseikuzumaki.puzzroom.data.storage

import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.Project
import kotlin.test.*

class FileStorageTest {
    private lateinit var storage: FakeFileStorage

    @BeforeTest
    fun setup() {
        storage = FakeFileStorage()
    }

    @AfterTest
    fun tearDown() {
        storage.clear()
    }

    @Test
    fun `writeProject and readProject should save and retrieve project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")

        // When
        storage.writeProject(project, "test-project")
        val retrieved = storage.readProject("test-project")

        // Then
        assertNotNull(retrieved)
        assertEquals(project.id, retrieved.id)
        assertEquals(project.name, retrieved.name)
    }

    @Test
    fun `readProject should return null for non-existent file`() = runTest {
        // When
        val retrieved = storage.readProject("non-existent")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `listProjects should return all saved project file names`() = runTest {
        // Given
        storage.writeProject(Project(id = "1", name = "Project 1"), "proj-1")
        storage.writeProject(Project(id = "2", name = "Project 2"), "proj-2")
        storage.writeProject(Project(id = "3", name = "Project 3"), "proj-3")

        // When
        val list = storage.listProjects()

        // Then
        assertEquals(3, list.size)
        assertTrue(list.contains("proj-1"))
        assertTrue(list.contains("proj-2"))
        assertTrue(list.contains("proj-3"))
    }

    @Test
    fun `deleteProject should remove file`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test")
        storage.writeProject(project, "test")

        // When
        storage.deleteProject("test")
        val retrieved = storage.readProject("test")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `isInitialized should return true`() {
        // When & Then
        assertTrue(storage.isInitialized())
    }

    @Test
    fun `writeProject should overwrite existing project`() = runTest {
        // Given
        val project1 = Project(id = "test-id", name = "Original Name")
        val project2 = Project(id = "test-id", name = "Updated Name")

        // When
        storage.writeProject(project1, "test-project")
        storage.writeProject(project2, "test-project")
        val retrieved = storage.readProject("test-project")

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
    }

    @Test
    fun `writeFurnitureTemplate and readFurnitureTemplate should save and retrieve template`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test Sofa",
            category = FurnitureCategory.LIVING,
            width = Centimeter(200),
            depth = Centimeter(90)
        )

        // When
        storage.writeFurnitureTemplate(template, "test-template")
        val retrieved = storage.readFurnitureTemplate("test-template")

        // Then
        assertNotNull(retrieved)
        assertEquals(template.id, retrieved.id)
        assertEquals(template.name, retrieved.name)
        assertEquals(template.category, retrieved.category)
        assertEquals(template.width, retrieved.width)
        assertEquals(template.depth, retrieved.depth)
    }

    @Test
    fun `readFurnitureTemplate should return null for non-existent file`() = runTest {
        // When
        val retrieved = storage.readFurnitureTemplate("non-existent")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `listFurnitureTemplates should return all saved template file names`() = runTest {
        // Given
        val template1 = FurnitureTemplate(
            id = "1",
            name = "Template 1",
            category = FurnitureCategory.LIVING,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        val template2 = FurnitureTemplate(
            id = "2",
            name = "Template 2",
            category = FurnitureCategory.BEDROOM,
            width = Centimeter(120),
            depth = Centimeter(60)
        )
        val template3 = FurnitureTemplate(
            id = "3",
            name = "Template 3",
            category = FurnitureCategory.KITCHEN,
            width = Centimeter(80),
            depth = Centimeter(40)
        )

        storage.writeFurnitureTemplate(template1, "template-1")
        storage.writeFurnitureTemplate(template2, "template-2")
        storage.writeFurnitureTemplate(template3, "template-3")

        // When
        val list = storage.listFurnitureTemplates()

        // Then
        assertEquals(3, list.size)
        assertTrue(list.contains("template-1"))
        assertTrue(list.contains("template-2"))
        assertTrue(list.contains("template-3"))
    }

    @Test
    fun `deleteFurnitureTemplate should remove file`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        storage.writeFurnitureTemplate(template, "test")

        // When
        storage.deleteFurnitureTemplate("test")
        val retrieved = storage.readFurnitureTemplate("test")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `writeFurnitureTemplate should overwrite existing template`() = runTest {
        // Given
        val template1 = FurnitureTemplate(
            id = "test-id",
            name = "Original Name",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        val template2 = FurnitureTemplate(
            id = "test-id",
            name = "Updated Name",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(120),
            depth = Centimeter(60)
        )

        // When
        storage.writeFurnitureTemplate(template1, "test-template")
        storage.writeFurnitureTemplate(template2, "test-template")
        val retrieved = storage.readFurnitureTemplate("test-template")

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
        assertEquals(Centimeter(120), retrieved.width)
        assertEquals(Centimeter(60), retrieved.depth)
    }

    @Test
    fun `projects and furniture templates should be stored separately`() = runTest {
        // Given
        val project = Project(id = "proj-id", name = "Test Project")
        val template = FurnitureTemplate(
            id = "template-id",
            name = "Test Template",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )

        // When
        storage.writeProject(project, "test")
        storage.writeFurnitureTemplate(template, "test")

        // Then
        val retrievedProject = storage.readProject("test")
        val retrievedTemplate = storage.readFurnitureTemplate("test")
        
        assertNotNull(retrievedProject)
        assertNotNull(retrievedTemplate)
        assertEquals(project.id, retrievedProject.id)
        assertEquals(template.id, retrievedTemplate.id)
        
        // Lists should not overlap
        assertEquals(1, storage.listProjects().size)
        assertEquals(1, storage.listFurnitureTemplates().size)
    }

    @Test
    fun `FurnitureTemplate should fail with blank name`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            FurnitureTemplate(
                name = "",
                category = FurnitureCategory.CUSTOM,
                width = Centimeter(100),
                depth = Centimeter(50)
            )
        }
    }

    @Test
    fun `FurnitureTemplate should fail with zero width`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            FurnitureTemplate(
                name = "Test",
                category = FurnitureCategory.CUSTOM,
                width = Centimeter(0),
                depth = Centimeter(50)
            )
        }
    }

    @Test
    fun `FurnitureTemplate should fail with negative depth`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            FurnitureTemplate(
                name = "Test",
                category = FurnitureCategory.CUSTOM,
                width = Centimeter(100),
                depth = Centimeter(-10)
            )
        }
    }
}
