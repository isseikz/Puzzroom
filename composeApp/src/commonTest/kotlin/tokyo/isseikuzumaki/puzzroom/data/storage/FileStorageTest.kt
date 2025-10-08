package tokyo.isseikuzumaki.puzzroom.data.storage

import kotlinx.coroutines.test.runTest
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
}
