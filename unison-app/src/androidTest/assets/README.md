# Test Audio Files

This directory contains audio files used for instrumented tests.

## Files

### test_audio.mp3
- **Source**: BBC Learning English - "Work in the Future: Cyber Security"
- **URL**: https://downloads.bbc.co.uk/learningenglish/features/work_in_the_future/251201_cyber_security_download.mp3
- **Size**: ~19 MB
- **Duration**: ~6 minutes
- **Purpose**: Integration test for Whisper.cpp transcription and OutOfMemoryError detection

## ⚠️ LOCAL EXECUTION ONLY

**This test is NOT run in CI/CD** to avoid:
- Accessing third-party sites from GitHub Actions
- Long execution times (~5-10 minutes)
- Unnecessary load on BBC servers

The test is intended for **manual verification before releases**.

## Downloading Test Files

### Local Development

1. Navigate to the assets directory:
```bash
cd unison-app/src/androidTest/assets
```

2. Download the test audio:
```bash
curl -o test_audio.mp3 \
  https://downloads.bbc.co.uk/learningenglish/features/work_in_the_future/251201_cyber_security_download.mp3
```

3. Verify the file:
```bash
ls -lh test_audio.mp3
# Should show ~19MB
```

### Running the Test

```bash
# From project root
./gradlew :unison-app:connectedAndroidTest
```

## Automatic Skipping

The test will automatically skip if:
- Running in CI environment (`CI`, `GITHUB_ACTIONS`, `JENKINS_HOME` env vars)
- Test audio file is not present in assets

When skipped, you'll see:
```
org.junit.AssumptionViolatedException: Skipping test in CI environment
```

This is **expected behavior** and not a test failure.

## Note

These files are excluded from git (.gitignore) due to their large size.
Manual download is required for local testing.
