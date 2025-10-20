package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.puzzroom.data.source.LocalFurnitureTemplateDataSourceImpl
import tokyo.isseikuzumaki.puzzroom.data.storage.FakeFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.Length
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import kotlin.test.*

class FurnitureTemplateRepositoryTest {
    private lateinit var storage: FakeFileStorage
    private lateinit var repository: FurnitureTemplateRepository

    @BeforeTest
    fun setup() {
        storage = FakeFileStorage()
        val dataSource = LocalFurnitureTemplateDataSourceImpl(storage)
        repository = FurnitureTemplateRepositoryImpl(dataSource)
    }

    @AfterTest
    fun tearDown() {
        storage.clear()
    }

    @Test
    fun `getAllTemplates should return success with empty list initially`() = runTest {
        // When
        val result = repository.getAllTemplates()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `saveTemplate should save template successfully`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test Sofa",
            category = FurnitureCategory.LIVING,
            width = Length(200),
            depth = Length(90)
        )

        // When
        val saveResult = repository.saveTemplate(template)
        val getResult = repository.getTemplateById("test-id")

        // Then
        assertTrue(saveResult.isSuccess)
        assertTrue(getResult.isSuccess)
        assertEquals(template.id, getResult.getOrNull()?.id)
    }

    @Test
    fun `getAllTemplates should return all saved templates`() = runTest {
        // Given
        val template1 = FurnitureTemplate(
            id = "1",
            name = "Sofa",
            category = FurnitureCategory.LIVING,
            width = Length(200),
            depth = Length(90)
        )
        val template2 = FurnitureTemplate(
            id = "2",
            name = "Bed",
            category = FurnitureCategory.BEDROOM,
            width = Length(140),
            depth = Length(200)
        )

        // When
        repository.saveTemplate(template1)
        repository.saveTemplate(template2)
        val result = repository.getAllTemplates()

        // Then
        assertTrue(result.isSuccess)
        val templates = result.getOrNull()!!
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
            width = Length(100),
            depth = Length(50)
        )
        repository.saveTemplate(originalTemplate)

        val updatedTemplate = FurnitureTemplate(
            id = "test-id",
            name = "Updated",
            category = FurnitureCategory.CUSTOM,
            width = Length(120),
            depth = Length(60)
        )

        // When
        val updateResult = repository.updateTemplate(updatedTemplate)
        val getResult = repository.getTemplateById("test-id")

        // Then
        assertTrue(updateResult.isSuccess)
        assertTrue(getResult.isSuccess)
        assertEquals("Updated", getResult.getOrNull()?.name)
    }

    @Test
    fun `deleteTemplate should remove template`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Length(100),
            depth = Length(50)
        )
        repository.saveTemplate(template)

        // When
        val deleteResult = repository.deleteTemplate("test-id")
        val getResult = repository.getTemplateById("test-id")

        // Then
        assertTrue(deleteResult.isSuccess)
        assertTrue(getResult.isSuccess)
        assertNull(getResult.getOrNull())
    }

    @Test
    fun `observeTemplates should emit updates when templates change`() = runTest {
        // Given
        val flow = repository.observeTemplates()
        
        // Initial state
        assertEquals(0, flow.first().size)

        // When
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Length(100),
            depth = Length(50)
        )
        repository.saveTemplate(template)

        // Then
        val templates = flow.first()
        assertEquals(1, templates.size)
        assertEquals("test-id", templates[0].id)
    }

    @Test
    fun `getTemplateById should return null for non-existent template`() = runTest {
        // When
        val result = repository.getTemplateById("non-existent")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `cache should be used for subsequent getTemplateById calls`() = runTest {
        // Given
        val template = FurnitureTemplate(
            id = "test-id",
            name = "Test",
            category = FurnitureCategory.CUSTOM,
            width = Length(100),
            depth = Length(50)
        )
        repository.saveTemplate(template)

        // When - first call loads from storage
        val result1 = repository.getTemplateById("test-id")
        
        // Delete from storage directly (bypassing repository)
        storage.deleteFurnitureTemplate("test-id")
        
        // Second call should use cache
        val result2 = repository.getTemplateById("test-id")

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertNotNull(result1.getOrNull())
        assertNotNull(result2.getOrNull()) // Should still be in cache
        assertEquals(template.id, result2.getOrNull()?.id)
    }
}
