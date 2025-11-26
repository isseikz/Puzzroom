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
│                    AlignmentViewModel                        │
│  - Model loading state                                       │
│  - Alignment processing                                      │
│  - Playback control (simulated)                             │
└──────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
        ┌───────────────────┐   ┌────────────────┐
        │   WhisperWrapper  │   │   TextAligner  │
        │  (Mock for now)   │   │  (Levenshtein) │
        └───────────────────┘   └────────────────┘
```

### Data Flow

1. **Model Loading**: Load Whisper model (mock in prototype)
2. **Transcription**: Get timestamped tokens from audio
3. **Alignment**: Match tokens with script using edit distance
4. **Playback**: Highlight words based on current position

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

## Future Work (JNI Integration)

To integrate real Whisper.cpp:

1. Clone whisper.cpp repository
2. Build native library for Android
3. Implement JNI bindings in `WhisperWrapper`
4. Add model file to assets

### JNI Changes Required

```cpp
// native-lib.cpp
whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
params.token_timestamps = true;  // Enable timestamps
```

## Requirements

- Android 8.0 (API 26) or higher
- ARM64-v8a or x86_64 device for Whisper inference

## License

Part of the Puzzroom project.
