# Whisper Integration Guide for Unison App

## Model Setup ✅

The Whisper model is now installed at:
```
unison-app/src/androidMain/assets/models/ggml-base.en.bin (141 MB)
```

## Quick Start Example

### 1. Initialize Whisper in Your Activity/ViewModel

```kotlin
import com.puzzroom.whisper.Whisper
import com.puzzroom.whisper.WhisperAndroid
import com.puzzroom.whisper.WhisperContext
import com.puzzroom.whisper.TranscriptionParams

class YourViewModel(
    private val context: Context
) : ViewModel() {

    private var whisperContext: WhisperContext? = null

    fun initializeWhisper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val whisper = Whisper.create() as WhisperAndroid

                // Load model from assets
                whisperContext = whisper.initFromAsset(
                    assetManager = context.assets,
                    assetPath = "models/ggml-base.en.bin"
                )

                Log.i("Whisper", "Model loaded successfully!")
            } catch (e: Exception) {
                Log.e("Whisper", "Failed to load model", e)
            }
        }
    }

    fun transcribeAudio(audioData: FloatArray): String? {
        return try {
            val result = whisperContext?.transcribe(
                audioData = audioData,
                params = TranscriptionParams(
                    language = "en",
                    threads = 0 // Auto-detect
                )
            )
            result?.fullText
        } catch (e: Exception) {
            Log.e("Whisper", "Transcription failed", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        whisperContext?.close()
    }
}
```

### 2. Convert Audio to Required Format

Whisper expects: **16kHz, Mono, Float32 [-1.0 to 1.0]**

```kotlin
/**
 * Convert PCM16 audio to Float32 format required by Whisper
 */
fun convertPcm16ToFloat32(pcm16: ShortArray): FloatArray {
    return FloatArray(pcm16.size) { i ->
        pcm16[i] / 32768.0f  // Normalize to [-1.0, 1.0]
    }
}

/**
 * Resample audio to 16kHz if needed
 * (Use a resampling library like Oboe or FFmpeg for production)
 */
fun resampleTo16kHz(
    audioData: FloatArray,
    originalSampleRate: Int
): FloatArray {
    if (originalSampleRate == 16000) {
        return audioData
    }

    // Simple linear interpolation (use proper resampling in production)
    val ratio = originalSampleRate / 16000.0
    val newSize = (audioData.size / ratio).toInt()

    return FloatArray(newSize) { i ->
        val srcIndex = (i * ratio).toInt()
        if (srcIndex < audioData.size) audioData[srcIndex] else 0f
    }
}
```

### 3. Example: Transcribe from AudioRecord

```kotlin
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecorder(private val viewModel: YourViewModel) {

    private val sampleRate = 16000  // Whisper requires 16kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun recordAndTranscribe(durationSeconds: Int): String? {
        val bufferSize = sampleRate * durationSeconds

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecorder", "Failed to initialize AudioRecord")
            return null
        }

        val audioBuffer = ShortArray(bufferSize)

        audioRecord.startRecording()
        val bytesRead = audioRecord.read(audioBuffer, 0, bufferSize)
        audioRecord.stop()
        audioRecord.release()

        if (bytesRead <= 0) {
            Log.e("AudioRecorder", "No audio data recorded")
            return null
        }

        // Convert to Float32
        val audioFloat = convertPcm16ToFloat32(audioBuffer)

        // Transcribe
        return viewModel.transcribeAudio(audioFloat)
    }
}
```

### 4. Example: Transcribe from Audio File

```kotlin
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun transcribeAudioFile(filePath: String, viewModel: YourViewModel): String? {
    // This is a simplified example - in production use a proper audio decoder
    val file = File(filePath)
    val bytes = file.readBytes()

    // Assuming raw PCM16 format
    val shorts = ShortArray(bytes.size / 2)
    ByteBuffer.wrap(bytes)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asShortBuffer()
        .get(shorts)

    val audioFloat = convertPcm16ToFloat32(shorts)

    return viewModel.transcribeAudio(audioFloat)
}
```

### 5. Using with Compose UI

```kotlin
@Composable
fun TranscriptionScreen(viewModel: YourViewModel = viewModel()) {
    var transcription by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initializeWhisper()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = transcription.ifEmpty { "Press button to record" },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isRecording = true
                // Start recording in background
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val recorder = AudioRecorder(viewModel)
                    val result = recorder.recordAndTranscribe(5) // 5 seconds
                    withContext(Dispatchers.Main) {
                        transcription = result ?: "Transcription failed"
                        isRecording = false
                    }
                }
            },
            enabled = !isRecording
        ) {
            Text(if (isRecording) "Recording..." else "Record 5 seconds")
        }
    }
}
```

## Performance Tips

### 1. Initialize Once
```kotlin
// ✅ GOOD: Initialize in Application.onCreate() or ViewModel
class YourApplication : Application() {
    lateinit var whisperContext: WhisperContext

    override fun onCreate() {
        super.onCreate()
        val whisper = Whisper.create() as WhisperAndroid
        whisperContext = whisper.initFromAsset(assets, "models/ggml-base.en.bin")
    }
}

// ❌ BAD: Don't initialize for every transcription
fun transcribe(audio: FloatArray) {
    val whisper = Whisper.create()  // Slow!
    val context = whisper.initFromFile("...")  // Very slow!
    context.transcribe(audio)
}
```

### 2. Use Optimal Thread Count
```kotlin
import com.puzzroom.whisper.WhisperCpuConfig

// Auto-detect (recommended)
TranscriptionParams(threads = 0)

// Or use recommended count
TranscriptionParams(threads = WhisperCpuConfig.preferredThreadCount)
```

### 3. Process on Background Thread
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val result = whisperContext.transcribe(audioData)
    withContext(Dispatchers.Main) {
        // Update UI with result
        updateUI(result.fullText)
    }
}
```

## Debugging

### Check Device Info
```kotlin
import com.puzzroom.whisper.WhisperCpuConfig

Log.d("Whisper", WhisperCpuConfig.deviceInfo)
// Output:
// Device: Google Pixel 6
// Android: 13 (API 33)
// ABI: arm64-v8a, armeabi-v7a
// Cores: 8
// Preferred threads: 7
```

### Enable Logcat Filtering
```bash
adb logcat | grep -E "Whisper|WhisperJNI"
```

### Check Model Loading
```kotlin
try {
    val whisper = Whisper.create() as WhisperAndroid
    val context = whisper.initFromAsset(assets, "models/ggml-base.en.bin")
    Log.i("Whisper", "✅ Model loaded successfully")

    // Test with empty audio
    val testAudio = FloatArray(16000) // 1 second of silence
    val result = context.transcribe(testAudio)
    Log.i("Whisper", "✅ Transcription test: ${result.fullText}")

    context.close()
} catch (e: Exception) {
    Log.e("Whisper", "❌ Error: ${e.message}", e)
}
```

## Common Issues

### Issue: "Failed to load native library"
**Solution**: Clean and rebuild
```bash
./gradlew clean :whisper-kmp:assembleDebug :unison-app:assembleDebug
```

### Issue: "Couldn't create context from asset"
**Solution**: Verify model path
```kotlin
// Check if asset exists
val modelExists = try {
    assets.open("models/ggml-base.en.bin").close()
    true
} catch (e: Exception) {
    false
}
Log.d("Whisper", "Model exists: $modelExists")
```

### Issue: Poor transcription quality
**Solutions**:
1. Ensure audio is 16kHz mono
2. Reduce background noise
3. Use a larger model (small or medium)
4. Check audio normalization [-1.0, 1.0]

## Next Steps

1. **Add Permissions** to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

2. **Request Runtime Permission**:
```kotlin
requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
```

3. **Integrate with Your Audio Pipeline**:
   - Update your `AudioEngine` or `AudioRepository` to use Whisper
   - Add transcription callbacks
   - Handle errors gracefully

4. **Test on Real Device**:
   - Asset loading works better on real devices
   - Test with different recording durations
   - Monitor memory usage

## References

- [Whisper KMP README](whisper-kmp/README.md)
- [whisper.cpp Documentation](https://github.com/ggerganov/whisper.cpp)
- [Whisper Models](https://huggingface.co/ggerganov/whisper.cpp)
