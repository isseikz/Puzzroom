#!/bin/bash

# Script to capture and analyze Whisper transcription logs
# Usage: ./capture_logs.sh

echo "==================================================="
echo "Whisper Transcription Log Capture"
echo "==================================================="
echo ""
echo "Instructions:"
echo "1. Make sure your device is connected via ADB"
echo "2. Open the Unison app"
echo "3. Select an audio file"
echo "4. Press record and then stop"
echo "5. The logs will be captured automatically"
echo ""
echo "Press Ctrl+C to stop logging"
echo ""
echo "==================================================="
echo ""

# Clear previous logs
adb logcat -c

# Start capturing logs with relevant filters
adb logcat -v time \
  WhisperAndroid:I \
  JNI:I \
  SessionViewModel:I \
  AndroidAudioRepository:I \
  *:E \
  | grep -E "(WhisperAndroid|JNI|SessionViewModel|AndroidAudioRepository|FATAL|ERROR)" \
  | tee whisper_logs_$(date +%Y%m%d_%H%M%S).txt
