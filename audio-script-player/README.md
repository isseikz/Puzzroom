# Audio-Script Alignment Player

A prototype Android application for synchronizing audio playback with text highlighting.

## Overview

This app demonstrates:
- **Audio-script alignment**: Using Whisper timestamps to map recognized speech to ground truth text
- **Synchronized highlighting**: Real-time word highlighting during audio playback
- **Touch-to-seek**: Tap on any word to jump to that position in the audio

## Architecture

### System Components

```
┌──────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                │
│  ┌─────────────┬──────────────┬──────────────────────────┐  │
│  │ PlayerHeader│  ScriptView  │    PlayerControls        │  │
│  └─────────────┴──────────────┴──────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│               AndroidAlignmentViewModel                      │
│  - Model loading state                                       │
│  - Alignment processing                                      │
│  - ExoPlayer integration for audio playback                  │
└──────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌──────────────┐  ┌─────────────┐  ┌─────────────┐
    │WhisperJniWrap│  │ TextAligner │  │ExoAudioPlayer
    │   (JNI)      │  │ (Levenshtein)│  │ (Media3)    │
    └──────────────┘  └─────────────┘  └─────────────┘
              │
              ▼
    ┌──────────────┐
    │native-lib.cpp│
    │ (whisper.cpp)│
    └──────────────┘
```

### Data Flow

1. **Model Loading**: Load Whisper model via JNI (ggml-tiny.en.bin)
2. **Transcription**: Get timestamped tokens from audio via whisper.cpp
3. **Alignment**: Match tokens with script using Levenshtein distance
4. **Playback**: ExoPlayer plays audio while highlighting words

## Implementation Details

### ExoPlayer Integration

The `ExoAudioPlayer` class provides real audio playback:
- Media3 ExoPlayer for efficient audio decoding
- StateFlow-based playback state management
- Support for file://, content://, and asset:// URIs

### Whisper.cpp JNI Integration

The `WhisperJniWrapper` provides:
- Model loading from assets or internal storage
- Transcription with `token_timestamps = true`
- JSON-formatted results with word-level timing

### Native Code (C++)

The `native-lib.cpp` implements JNI methods:
- `nativeLoadModel()` - Load ggml model file
- `nativeTranscribeWithTimestamps()` - Run inference with timestamps
- `nativeFreeModel()` - Release model memory

## Data Models

### WhisperRawToken
```kotlin
data class WhisperRawToken(
    val text: String,      // Recognized text
    val startMs: Long,     // Start time in milliseconds
    val endMs: Long,       // End time in milliseconds
    val prob: Float        // Confidence probability
)
```

### AlignedWord
```kotlin
data class AlignedWord(
    val originalWord: String,  // Display text from script
    val startTime: Long,       // Mapped start time
    val endTime: Long,         // Mapped end time
    val isHighlighted: Boolean // Currently playing
)
```

## Text Alignment Algorithm

The `TextAligner` class implements a greedy forward-matching algorithm:

1. **Tokenization**: Split both texts into word lists
2. **Normalization**: Lowercase and remove punctuation for comparison
3. **Matching**: Find best match using Levenshtein distance with tolerance
4. **Interpolation**: Fill in timestamps for unmatched words

## UI Components (Atomic Design)

### Organisms
- **PlayerHeader**: Model status and process button
- **ScriptView**: FlowRow of words with highlighting
- **PlayerControls**: Play/pause and seek bar

### Pages
- **AlignmentPlayerPage**: Main screen composing all organisms

## Building

```bash
./gradlew :audio-script-player:assembleDebug
```

## Whisper.cpp Integration Steps

To complete the whisper.cpp integration:

### 1. Build whisper.cpp for Android

```bash
git clone https://github.com/ggerganov/whisper.cpp
cd whisper.cpp
# Follow Android build instructions in whisper.cpp/examples/android
```

### 2. Copy Native Libraries

Copy the built `.so` files to:
```
audio-script-player/src/androidMain/jniLibs/
├── arm64-v8a/
│   └── libwhisper.so
├── armeabi-v7a/
│   └── libwhisper.so
└── x86_64/
    └── libwhisper.so
```

### 3. Add Model File

Add the Whisper model to assets:
```
audio-script-player/src/androidMain/assets/
└── ggml-tiny.en.bin
```

### 4. Enable Real Implementation

In `MainActivity.kt`, set `useRealImplementations = true`:
```kotlin
AndroidAlignmentViewModel.Factory(
    context = applicationContext,
    useRealImplementations = true  // Enable real Whisper + ExoPlayer
)
```

### 5. Uncomment Native Code

Uncomment the whisper.cpp integration code in `native-lib.cpp`.

## Requirements

- Android 8.0 (API 26) or higher
- ARM64-v8a, armeabi-v7a, or x86_64 device
- ~75MB for ggml-tiny.en.bin model

## License

Part of the Puzzroom project.
