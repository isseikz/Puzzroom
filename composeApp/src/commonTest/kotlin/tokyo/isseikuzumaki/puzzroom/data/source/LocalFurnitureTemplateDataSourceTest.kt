package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.storage.FakeFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import kotlin.test.*

class LocalFurnitureTemplateDataSourceTest {
    private lateinit var storage: FakeFileStorage
    private lateinit var dataSource: LocalFurnitureTemplateDataSource

    @BeforeTest
    fun setup() {
        storage = FakeFileStorage()
        dataSource = LocalFurnitureTemplateDataSourceImpl(storage)
    }

    @AfterTest
    fun tearDown() {
        storage.clear()
    }

    @Test
    fun `getAllTemplates should return empty list when no templates exist`() = runTest {
        // When
        val templates = dataSource.getAllTemplates()

        // Then
        assertTrue(templates.isEmpty())
    }

    @Test
    fun `insertTemplate should save template to storage`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test Sofa",
            category = FurnitureCategory.LIVING,
            width = Centimeter(200),
            depth = Centimeter(90)
        )

        // When
        dataSource.insertTemplate(template)
        val retrieved = dataSource.getTemplateById("test-id")

        // Then
        assertNotNull(retrieved)
        assertEquals(template.id, retrieved.id)
        assertEquals(template.name, retrieved.name)
    }

    @Test
    fun `getAllTemplates should return all saved templates`() = runTest {
        // Given
        val template1 = FurnitureTemplate(
            id = "1",
            name = "Sofa",
            category = FurnitureCategory.LIVING,
            width = Centimeter(200),
            depth = Centimeter(90)
        )
        val template2 = FurnitureTemplate(
            id = "2",
            name = "Bed",
            category = FurnitureCategory.BEDROOM,
            width = Centimeter(140),
            depth = Centimeter(200)
        )

        // When
        dataSource.insertTemplate(template1)
        dataSource.insertTemplate(template2)
        val templates = dataSource.getAllTemplates()

        // Then
        assertEquals(2, templates.size)
        assertTrue(templates.any { it.id == "1" })
        assertTrue(templates.any { it.id == "2" })
    }

    @Test
    fun `updateTemplate should update existing template`() = runTest {
        // Given
        val originalTemplate = FurnitureTemplate(
            id = "test-id",
            name = "Original",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        dataSource.insertTemplate(originalTemplate)

        val updatedTemplate = FurnitureTemplate(
            id = "test-id",
            name = "Updated",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(120),
            depth = Centimeter(60)
        )

        // When
        dataSource.updateTemplate(updatedTemplate)
        val retrieved = dataSource.getTemplateById("test-id")

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved.name)
        assertEquals(Centimeter(120), retrieved.width)
    }

    @Test
    fun `updateTemplate should throw error when template does not exist`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "non-existent",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )

        // When & Then
        assertFailsWith<PersistenceError.NotFoundError> {
            dataSource.updateTemplate(template)
        }
    }

    @Test
    fun `deleteTemplate should remove template from storage`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        dataSource.insertTemplate(template)

        // When
        dataSource.deleteTemplate("test-id")
        val retrieved = dataSource.getTemplateById("test-id")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `observeTemplates should emit updates when templates change`() = runTest {
        // Given
        val flow = dataSource.observeTemplates()
        
        // Initial state should be empty
        assertEquals(0, flow.first().size)

        // When
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        dataSource.insertTemplate(template)

        // Then
        val templates = flow.first()
        assertEquals(1, templates.size)
        assertEquals("test-id", templates[0].id)
    }

    @Test
    fun `observeTemplate should emit updates for specific template`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Original",
            category = FurnitureCategory.CUSTOM,
            width = Centimeter(100),
            depth = Centimeter(50)
        )
        dataSource.insertTemplate(template)
        
        val flow = dataSource.observeTemplate("test-id")

        // When
        val updatedTemplate = template.copy(name = "Updated")
        dataSource.updateTemplate(updatedTemplate)

        // Then
        val observed = flow.first()
        assertNotNull(observed)
        assertEquals("Updated", observed.name)
    }

    @Test
    fun `getTemplateById should return null for non-existent template`() = runTest {
        // When
        val retrieved = dataSource.getTemplateById("non-existent")

        // Then
        assertNull(retrieved)
    }
}
