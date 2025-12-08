package tokyo.isseikuzumaki.unison.screens.shadowing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import tokyo.isseikuzumaki.unison.data.AudioEngine
import tokyo.isseikuzumaki.unison.data.AudioRepository
import tokyo.isseikuzumaki.unison.screens.session.SessionUiState
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel
import tokyo.isseikuzumaki.unison.screens.session.ShadowingData

/**
 * Integration test for ShadowingScreen
 * Verifies that ViewModel flow is correctly displayed in UI
 */
class ShadowingScreenTest : KoinTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testViewModel: TestSessionViewModel
    private val testUri = "file:///test/audio.mp3"

    @Before
    fun setup() {
        stopKoin()

        testViewModel = TestSessionViewModel()

        val testModule = module {
            single<SessionViewModel> { testViewModel }
        }

        startKoin {
            modules(testModule)
        }
    }

    @Test
    fun whenShadowingDataIsNull_showsLoadingState() {
        // Given: ViewModel emits null shadowing data
        testViewModel.setShadowingData(null)

        // When: Screen is displayed
        composeTestRule.setContent {
            ShadowingScreen(
                uri = testUri,
                onNavigateBack = {}
            )
        }

        // Then: Loading text is displayed
        composeTestRule
            .onNodeWithText("Loading transcript...")
            .assertIsDisplayed()
    }

    @Test
    fun whenShadowingDataIsAvailable_displaysTranscript() {
        // Given: ViewModel emits valid shadowing data
        val testData = ShadowingData(
            pcmData = ByteArray(1000),
            transcript = "Hello, this is a test transcript.",
            durationMs = 5000L,
            fileName = "test_audio"
        )
        testViewModel.setShadowingData(testData)

        // When: Screen is displayed
        composeTestRule.setContent {
            ShadowingScreen(
                uri = testUri,
                onNavigateBack = {}
            )
        }

        // Then: File name is displayed
        composeTestRule
            .onNodeWithText("test_audio")
            .assertIsDisplayed()

        // And: Duration is displayed
        composeTestRule
            .onNodeWithText("Duration: 00:05")
            .assertIsDisplayed()

        // And: Transcript is displayed
        composeTestRule
            .onNodeWithText("Hello, this is a test transcript.")
            .assertIsDisplayed()

        // And: Instructions are displayed
        composeTestRule
            .onNodeWithText("How to Practice")
            .assertIsDisplayed()
    }

    @Test
    fun whenShadowingDataChanges_uiUpdates() {
        // Given: Initial shadowing data
        val initialData = ShadowingData(
            pcmData = ByteArray(1000),
            transcript = "Initial transcript",
            durationMs = 3000L,
            fileName = "initial_audio"
        )
        testViewModel.setShadowingData(initialData)

        composeTestRule.setContent {
            ShadowingScreen(
                uri = testUri,
                onNavigateBack = {}
            )
        }

        // When: Shadowing data changes
        val updatedData = ShadowingData(
            pcmData = ByteArray(2000),
            transcript = "Updated transcript",
            durationMs = 6000L,
            fileName = "updated_audio"
        )
        testViewModel.setShadowingData(updatedData)

        composeTestRule.waitForIdle()

        // Then: UI updates to show new data
        composeTestRule
            .onNodeWithText("updated_audio")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Duration: 00:06")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Updated transcript")
            .assertIsDisplayed()
    }

    /**
     * Test implementation of SessionViewModel
     * Allows manual control of StateFlow for testing
     */
    private class TestSessionViewModel : SessionViewModel(
        uri = "test://uri",
        audioRepository = object : AudioRepository {
            override suspend fun loadPcmData(uri: String): Result<ByteArray> {
                return Result.success(ByteArray(1000))
            }

            override suspend fun validateAudioFile(uri: String): Result<Unit> {
                return Result.success(Unit)
            }

            override suspend fun transcribeAudio(
                pcmData: ByteArray,
                sampleRate: Int
            ): Result<List<com.puzzroom.whisper.TranscriptionSegment>> {
                return Result.success(emptyList())
            }
        },
        audioEngine = object : AudioEngine {
            override fun startRecording(): kotlinx.coroutines.flow.Flow<ByteArray> {
                return kotlinx.coroutines.flow.emptyFlow()
            }

            override suspend fun playOriginal(pcmData: ByteArray): Result<Unit> {
                return Result.success(Unit)
            }

            override suspend fun playDual(
                original: ByteArray,
                recorded: ByteArray,
                offsetMs: Int,
                balance: Float
            ): Result<Unit> {
                return Result.success(Unit)
            }

            override fun stopRecording() {}

            override fun stopPlayback() {}

            override fun getRecordedData(): ByteArray? = null

            override fun getCurrentPositionMs(): Long = 0L

            override fun getDurationMs(pcmData: ByteArray): Long = 1000L

            override fun release() {}
        }
    ) {
        private val _testShadowingData = MutableStateFlow<ShadowingData?>(null)
        override val shadowingData get() = _testShadowingData

        fun setShadowingData(data: ShadowingData?) {
            _testShadowingData.value = data
        }
    }
}
