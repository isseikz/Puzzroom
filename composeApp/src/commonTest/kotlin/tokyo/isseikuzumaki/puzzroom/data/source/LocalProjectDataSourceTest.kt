package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.storage.FakeFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.Project
import kotlin.test.*

class LocalProjectDataSourceTest {
    private lateinit var fileStorage: FakeFileStorage
    private lateinit var dataSource: LocalProjectDataSourceImpl

    @BeforeTest
    fun setup() {
        fileStorage = FakeFileStorage()
        dataSource = LocalProjectDataSourceImpl(fileStorage)
    }

    @AfterTest
    fun tearDown() {
        fileStorage.clear()
    }

    @Test
    fun `getAllProjects should return empty list when no projects exist`() = runTest {
        // When
        val projects = dataSource.getAllProjects()

        // Then
        assertTrue(projects.isEmpty())
    }

    @Test
    fun `insertProject and getAllProjects should save and retrieve project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")

        // When
        dataSource.insertProject(project)
        val projects = dataSource.getAllProjects()

        // Then
        assertEquals(1, projects.size)
        assertEquals(project.id, projects[0].id)
        assertEquals(project.name, projects[0].name)
    }

    @Test
    fun `getProjectById should return project when it exists`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")
        dataSource.insertProject(project)

        // When
        val retrieved = dataSource.getProjectById("test-id")

        // Then
        assertNotNull(retrieved)
        assertEquals(project.id, retrieved.id)
        assertEquals(project.name, retrieved.name)
    }

    @Test
    fun `getProjectById should return null when project does not exist`() = runTest {
        // When
        val retrieved = dataSource.getProjectById("non-existent")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `updateProject should modify existing project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Original Name")
        dataSource.insertProject(project)

        // When
        val updated = project.copy(name = "Updated Name")
        dataSource.updateProject(updated)
        val retrieved = dataSource.getProjectById("test-id")

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
    }

    @Test
    fun `updateProject should throw NotFoundError when project does not exist`() = runTest {
        // Given
        val project = Project(id = "non-existent", name = "Test")

        // When & Then
        assertFailsWith<PersistenceError.NotFoundError> {
            dataSource.updateProject(project)
        }
    }

    @Test
    fun `deleteProject should remove project`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Test Project")
        dataSource.insertProject(project)

        // When
        dataSource.deleteProject("test-id")
        val retrieved = dataSource.getProjectById("test-id")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `observeProjects should emit changes when projects are added`() = runTest {
        // Given
        val project1 = Project(id = "1", name = "Project 1")
        val project2 = Project(id = "2", name = "Project 2")

        // When
        dataSource.insertProject(project1)
        val projects1 = dataSource.observeProjects().first()

        dataSource.insertProject(project2)
        val projects2 = dataSource.observeProjects().first()

        // Then
        assertEquals(1, projects1.size)
        assertEquals(2, projects2.size)
    }

    @Test
    fun `observeProject should emit specific project changes`() = runTest {
        // Given
        val project = Project(id = "test-id", name = "Original Name")
        dataSource.insertProject(project)

        // When
        val observed1 = dataSource.observeProject("test-id").first()

        val updated = project.copy(name = "Updated Name")
        dataSource.updateProject(updated)
        val observed2 = dataSource.observeProject("test-id").first()

        // Then
        assertNotNull(observed1)
        assertEquals("Original Name", observed1.name)
        assertNotNull(observed2)
        assertEquals("Updated Name", observed2.name)
    }

    @Test
    fun `observeProject should emit null when project does not exist`() = runTest {
        // When
        val observed = dataSource.observeProject("non-existent").first()

        // Then
        assertNull(observed)
    }
}
