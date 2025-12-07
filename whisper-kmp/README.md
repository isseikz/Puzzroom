# Whisper KMP - Kotlin Multiplatform Library for Whisper.cpp

A Kotlin Multiplatform library that provides bindings to [whisper.cpp](https://github.com/ggml-org/whisper.cpp), enabling high-performance speech recognition across Android platforms (with iOS and JVM support planned).

## Features

- **High Performance**: Direct bindings to whisper.cpp for efficient speech recognition
- **CPU Optimization**: Automatic detection and loading of optimized libraries (NEON-VFPv4, ARMv8.2-FP16)
- **Asset Loading**: Load models directly from Android assets (no need to copy to storage)
- **Kotlin Multiplatform**: Clean, type-safe API for Android (iOS/JVM coming soon)
- **Thread-Safe**: Coroutine-based API with automatic thread management
- **Easy to Use**: Simple, intuitive API for speech transcription
- **Flexible**: Support for multiple languages, translation, and custom parameters

## Setup

### 1. Add the dependency

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":whisper-kmp"))
}
```

### 2. Download a Whisper Model

Download a GGML model from the [whisper.cpp models repository](https://huggingface.co/ggerganov/whisper.cpp):

```bash
# Download base English model (74 MB)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin

# Or download multilingual base model (142 MB)
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
```

Available models:
- `ggml-tiny.en.bin` (75 MB) - Tiny English-only model
- `ggml-base.en.bin` (142 MB) - Base English-only model
- `ggml-small.en.bin` (466 MB) - Small English-only model
- `ggml-base.bin` (142 MB) - Base multilingual model
- `ggml-medium.bin` (1.5 GB) - Medium multilingual model

### 3. Place the model in your app

**Option A: Android Assets (Recommended)**

Place the model in your app's assets directory:
```
unison-app/src/androidMain/assets/models/ggml-base.en.bin
```

**Option B: Internal Storage**

Copy the model file to internal storage:

```kotlin
// Example: Copy from assets to internal storage
fun copyModelToInternalStorage(context: Context, assetFileName: String): String {
    val file = File(context.filesDir, assetFileName)
    if (!file.exists()) {
        context.assets.open(assetFileName).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return file.absolutePath
}
```

## Usage

### Android - Load from Assets (Recommended)

```kotlin
import com.puzzroom.whisper.Whisper
import com.puzzroom.whisper.WhisperAndroid
import com.puzzroom.whisper.TranscriptionParams

// Initialize Whisper
val whisper = Whisper.create() as WhisperAndroid

// Load model from assets
val context = whisper.initFromAsset(
    assetManager = context.assets,
    assetPath = "models/ggml-base.en.bin"
)

// Prepare audio data (16kHz, mono, float32)
val audioData: FloatArray = loadAudioData() // Your audio loading logic

// Transcribe
val result = context.transcribe(audioData)

// Get full transcription
println("Transcription: ${result.fullText}")

// Get segments with timestamps
result.segments.forEach { segment ->
    println("[${segment.startTimeMs}ms - ${segment.endTimeMs}ms]: ${segment.text}")
}

// Clean up
context.close()
```

### Android - Load from File Path

```kotlin
import com.puzzroom.whisper.Whisper

// Initialize Whisper
val whisper = Whisper.create()

// Load model from file path
val modelPath = "/path/to/ggml-base.en.bin"
val context = whisper.initFromFile(modelPath)

// Transcribe
val result = context.transcribe(audioData)

// Clean up
context.close()
```

### Advanced Parameters

```kotlin
val params = TranscriptionParams(
    language = "en",           // Target language (ISO 639-1 code)
    translate = false,         // Translate to English
    threads = 0,               // Number of threads (0 = auto-detect)
    samplingStrategy = SamplingStrategy.GREEDY, // or BEAM_SEARCH
    printProgress = false,     // Print progress to console
    printTimestamps = true     // Include timestamps in output
)

val result = context.transcribe(audioData, params)
```

### CPU Optimization

The library automatically detects your device's CPU capabilities and loads the optimal native library:

```kotlin
import com.puzzroom.whisper.WhisperCpuConfig

// Get device info
println(WhisperCpuConfig.deviceInfo)
// Output:
// Device: Google Pixel 6
// Android: 13 (API 33)
// ABI: arm64-v8a, armeabi-v7a, armeabi
// Cores: 8
// Preferred threads: 7

// Get recommended thread count
val threads = WhisperCpuConfig.preferredThreadCount

// Use in transcription
val result = context.transcribe(
    audioData,
    TranscriptionParams(threads = threads) // or threads = 0 for auto
)
```

**Optimized Libraries:**
- ARMv7 with VFPv4: Loads `libwhisper_vfpv4.so` (NEON optimizations)
- ARMv8.2+ with FP16: Loads `libwhisper_v8fp16_va.so` (FP16 arithmetic)
- Default: Loads `libwhisper.so` (standard build)

### Supported Languages

Check if a language is supported:

```kotlin
val isSupported = whisper.isLanguageSupported("es") // Spanish
```

Supported languages include: en, zh, de, es, ru, ko, fr, ja, pt, tr, pl, ca, nl, ar, sv, it, id, hi, fi, vi, he, uk, el, ms, cs, ro, da, hu, ta, no, th, ur, hr, bg, lt, la, mi, ml, cy, sk, te, fa, lv, bn, sr, az, sl, kn, et, mk, br, eu, is, hy, ne, mn, bs, kk, sq, sw, gl, mr, pa, si, km, sn, yo, so, af, oc, ka, be, tg, sd, gu, am, yi, lo, uz, fo, ht, ps, tk, nn, mt, sa, lb, my, bo, tl, mg, as, tt, haw, ln, ha, ba, jw, su

### Audio Format Requirements

Whisper.cpp expects audio in a specific format:
- **Sample rate**: 16kHz
- **Channels**: Mono (1 channel)
- **Format**: Float32 array, normalized to [-1.0, 1.0]

Example conversion from PCM16 to Float32:

```kotlin
fun convertPcm16ToFloat32(pcm16: ShortArray): FloatArray {
    return FloatArray(pcm16.size) { i ->
        pcm16[i] / 32768.0f
    }
}
```

### Example: Recording and Transcribing Audio

```kotlin
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioTranscriber(private val modelPath: String) {
    private val whisper = Whisper.create()
    private lateinit var context: WhisperContext

    fun initialize() {
        context = whisper.initFromFile(modelPath)
    }

    fun recordAndTranscribe(durationSeconds: Int): String {
        val sampleRate = 16000
        val bufferSize = sampleRate * durationSeconds

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        val audioBuffer = ShortArray(bufferSize)
        audioRecord.startRecording()
        audioRecord.read(audioBuffer, 0, bufferSize)
        audioRecord.stop()
        audioRecord.release()

        // Convert to float32
        val audioData = convertPcm16ToFloat32(audioBuffer)

        // Transcribe
        val result = context.transcribe(audioData)
        return result.fullText
    }

    fun close() {
        context.close()
    }

    private fun convertPcm16ToFloat32(pcm16: ShortArray): FloatArray {
        return FloatArray(pcm16.size) { i ->
            pcm16[i] / 32768.0f
        }
    }
}
```

## Building from Source

### Prerequisites

- Android Studio Arctic Fox or later
- CMake 3.22.1 or later
- NDK r21 or later
- Git (for submodules)

### Build Steps

1. Clone the repository with submodules:
```bash
git clone --recurse-submodules https://github.com/yourusername/puzzroom.git
cd puzzroom
```

2. Build the project:
```bash
./gradlew :whisper-kmp:build
```

This will:
- Compile whisper.cpp native library
- Build JNI wrapper
- Create the Kotlin library
- Generate AAR for Android

## Performance Tips

1. **Use appropriate model size**: Smaller models are faster but less accurate
2. **Optimize thread count**: Set `threads` parameter based on device capabilities
3. **Hardware acceleration**: The library enables NNAPI by default on Android
4. **Audio preprocessing**: Ensure audio is properly normalized and denoised
5. **Model caching**: Load the model once and reuse the context

## Architecture

```
whisper-kmp/
├── src/
│   ├── commonMain/kotlin/        # Platform-agnostic API
│   │   └── com/puzzroom/whisper/
│   │       ├── Whisper.kt        # Main interface
│   │       ├── WhisperModels.kt  # Data models
│   │       └── WhisperContext.kt # Context interface
│   ├── androidMain/kotlin/       # Android implementation
│   │   └── com/puzzroom/whisper/
│   │       └── WhisperAndroid.kt # Android bindings
│   └── androidMain/cpp/          # Native JNI layer
│       ├── CMakeLists.txt        # CMake build config
│       └── whisper_jni.cpp       # JNI wrapper
└── whisper.cpp/                  # Git submodule

```

## Troubleshooting

### Library not found error

If you see `UnsatisfiedLinkError`, ensure:
1. NDK is properly installed
2. CMake version is 3.22.1 or later
3. Build the project with `./gradlew :whisper-kmp:build`

### Model loading fails

Ensure:
1. Model file path is correct and accessible
2. Model file is a valid GGML format
3. App has READ permission for the model file location

### Poor transcription quality

Try:
1. Using a larger model (e.g., small or medium)
2. Ensuring audio is 16kHz mono
3. Reducing background noise
4. Using beam search sampling strategy

## License

This library is a wrapper around whisper.cpp. Please see:
- [whisper.cpp license](https://github.com/ggml-org/whisper.cpp/blob/master/LICENSE)

## Contributing

Contributions are welcome! Areas for improvement:
- iOS support using cinterop
- JVM desktop support
- Additional language bindings
- Performance optimizations
- Example applications

## Credits

- [whisper.cpp](https://github.com/ggml-org/whisper.cpp) by Georgi Gerganov
- [OpenAI Whisper](https://github.com/openai/whisper) - Original model
