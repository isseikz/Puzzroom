# Quick Start Guide

## Initial Setup

### 1. Initialize the whisper.cpp submodule

```bash
cd whisper-kmp
git submodule update --init --recursive
cd ..
```

### 2. Sync the project

```bash
./gradlew :whisper-kmp:build
```

This will:
- Download and compile whisper.cpp
- Build the JNI wrapper
- Create the Android library

## Download a Model

Download a Whisper model to test with:

```bash
# Create a directory for models
mkdir -p models

# Download the base English model (142 MB)
curl -L "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin" \
  -o models/ggml-base.en.bin
```

## Basic Usage Example

```kotlin
import com.puzzroom.whisper.Whisper
import com.puzzroom.whisper.TranscriptionParams
import com.puzzroom.whisper.AudioUtils

// Copy model to internal storage
val modelPath = AudioUtils.copyModelFromAssets(
    context,
    "ggml-base.en.bin"
)

// Initialize Whisper
val whisper = Whisper.create()
val context = whisper.initFromFile(modelPath)

// Prepare audio (16kHz, mono, float32)
val audioData: FloatArray = prepareAudio()

// Transcribe
val result = context.transcribe(audioData)
println(result.fullText)

// Clean up
context.close()
```

## Testing the Library

### Option 1: Use in your existing app module

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":whisper-kmp"))
}
```

### Option 2: Create a simple test app

Create a new activity to test:

```kotlin
class WhisperTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val whisper = Whisper.create()
                val version = whisper.getVersion()
                Log.i("WhisperTest", "Whisper version: $version")

                // Test model loading
                val modelPath = "/path/to/model.bin"
                if (AudioUtils.isModelFileValid(modelPath)) {
                    val context = whisper.initFromFile(modelPath)
                    Log.i("WhisperTest", "Model loaded successfully!")
                    context.close()
                }
            } catch (e: Exception) {
                Log.e("WhisperTest", "Error: ${e.message}", e)
            }
        }
    }
}
```

## Next Steps

1. **Add to your Unison app**: Integrate with `whisper-kmp/src/androidMain/kotlin/com/puzzroom/whisper/AudioUtils.kt`
2. **Real-time transcription**: Use `AudioRecord` to capture audio and transcribe in chunks
3. **Optimize performance**: Experiment with different models and thread counts
4. **Add UI**: Create a transcription interface in your app

## Troubleshooting

### Build fails with CMake errors

Ensure you have:
- Android SDK with NDK installed
- CMake 3.22.1 or later
- Initialized the whisper.cpp submodule

```bash
# Check submodule status
git submodule status

# If not initialized
git submodule update --init --recursive
```

### Library load fails at runtime

Check logcat for errors:

```bash
adb logcat | grep -i whisper
```

Common issues:
- Model file path is incorrect
- Model file is corrupted or wrong format
- Native library not built for device ABI

### Poor performance

Try:
- Using a smaller model (tiny or base)
- Reducing thread count
- Ensuring audio is properly formatted (16kHz, mono)
- Testing on a device with NNAPI support

## Development Tips

- The native code is in `src/androidMain/cpp/`
- Kotlin interfaces are in `src/commonMain/kotlin/`
- Android implementation is in `src/androidMain/kotlin/`
- For debugging native code, enable NDK debugging in Android Studio

## Resources

- [Whisper.cpp GitHub](https://github.com/ggml-org/whisper.cpp)
- [Whisper Models](https://huggingface.co/ggerganov/whisper.cpp)
- [OpenAI Whisper Paper](https://arxiv.org/abs/2212.04356)
