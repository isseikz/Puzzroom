package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.data.source.LocalProjectDataSourceImpl
import tokyo.isseikuzumaki.puzzroom.data.storage.FakeFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.Project
import kotlin.test.*

class ProjectRepositoryTest {
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
    fun `saveProject and getProjectById should save and retrieve project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")

        // When
        val saveResult = repository.saveProject(project)
        val retrieveResult = repository.getProjectById("test-id")

        // Then
        assertTrue(saveResult.isSuccess)
        assertTrue(retrieveResult.isSuccess)
        assertNotNull(retrieveResult.getOrNull())
        assertEquals(project.id, retrieveResult.getOrNull()?.id)
        assertEquals(project.name, retrieveResult.getOrNull()?.name)
    }

    @Test
    fun `getAllProjects should return all saved projects`() = runTest {
        // Given
        val project1 = Project(id = "1", name = "Project 1")
        val project2 = Project(id = "2", name = "Project 2")
        repository.saveProject(project1)
        repository.saveProject(project2)

        // When
        val result = repository.getAllProjects()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `updateProject should modify existing project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Original Name")
        repository.saveProject(project)

        // When
        val updated = project.copy(name = "Updated Name")
        val updateResult = repository.updateProject(updated)
        val retrieveResult = repository.getProjectById("test-id")

        // Then
        assertTrue(updateResult.isSuccess)
        assertTrue(retrieveResult.isSuccess)
        assertEquals("Updated Name", retrieveResult.getOrNull()?.name)
    }

    @Test
    fun `deleteProject should remove project from storage`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")
        repository.saveProject(project)

        // When
        val deleteResult = repository.deleteProject("test-id")
        val retrieveResult = repository.getProjectById("test-id")

        // Then
        assertTrue(deleteResult.isSuccess)
        assertTrue(retrieveResult.isSuccess)
        assertNull(retrieveResult.getOrNull())
    }

    @Test
    fun `observeProjects should emit changes when projects are modified`() = runTest {
        // Given
        val project1 = Project(id = "1", name = "Project 1")

        // When
        repository.saveProject(project1)
        val projects1 = repository.observeProjects().first()

        val project2 = Project(id = "2", name = "Project 2")
        repository.saveProject(project2)
        val projects2 = repository.observeProjects().first()

        // Then
        assertEquals(1, projects1.size)
        assertEquals(2, projects2.size)
    }

    @Test
    fun `observeProject should emit specific project changes`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Original Name")
        repository.saveProject(project)

        // When
        val observed1 = repository.observeProject("test-id").first()

        val updated = project.copy(name = "Updated Name")
        repository.updateProject(updated)
        val observed2 = repository.observeProject("test-id").first()

        // Then
        assertNotNull(observed1)
        assertEquals("Original Name", observed1.name)
        assertNotNull(observed2)
        assertEquals("Updated Name", observed2.name)
    }

    @Test
    fun `getProjectById should use cache for subsequent requests`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")
        repository.saveProject(project)

        // When - First call populates cache
        val result1 = repository.getProjectById("test-id")

        // Clear file storage but cache should still have the project
        fileStorage.clear()

        // Second call should return from cache
        val result2 = repository.getProjectById("test-id")

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(project.id, result2.getOrNull()?.id)
    }

    @Test
    fun `getAllProjects should update cache`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")
        repository.saveProject(project)

        // When
        val getAllResult = repository.getAllProjects()

        // Clear file storage
        fileStorage.clear()

        // Try to get from cache
        val getByIdResult = repository.getProjectById("test-id")

        // Then
        assertTrue(getAllResult.isSuccess)
        assertTrue(getByIdResult.isSuccess)
        assertNotNull(getByIdResult.getOrNull())
    }
}
