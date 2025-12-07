# Test Audio Files

This directory contains audio files used for instrumented tests.

## Files

### test_audio.mp3
- **Source**: BBC Learning English - "Work in the Future: Cyber Security"
- **URL**: https://downloads.bbc.co.uk/learningenglish/features/work_in_the_future/251201_cyber_security_download.mp3
- **Size**: ~19 MB
- **Duration**: ~6 minutes
- **Purpose**: Integration test for Whisper.cpp transcription and OutOfMemoryError detection

## Downloading Test Files

### Local Development
```bash
cd unison-app/src/androidTest/assets
curl -O https://downloads.bbc.co.uk/learningenglish/features/work_in_the_future/251201_cyber_security_download.mp3 \
  -o test_audio.mp3
```

### CI/CD (GitHub Actions)
The workflow should download test audio files before running instrumented tests:

```yaml
- name: Download test audio files
  run: |
    mkdir -p unison-app/src/androidTest/assets
    curl -o unison-app/src/androidTest/assets/test_audio.mp3 \
      https://downloads.bbc.co.uk/learningenglish/features/work_in_the_future/251201_cyber_security_download.mp3
```

## Note

These files are excluded from git (.gitignore) due to their large size.
Tests will download them automatically when needed.
