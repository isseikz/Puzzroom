#!/bin/bash

# Analyze Whisper logs - run this AFTER you've tested the app
# This will show the last 500 lines of relevant logs

echo "==================================================="
echo "Whisper Transcription Log Analysis"
echo "==================================================="
echo ""

echo "📱 Analyzing recent logs from device..."
echo ""

# Get logs and filter for Whisper-related entries
adb logcat -d -v time | grep -E "(WhisperAndroid|JNI|SessionViewModel|AndroidAudioRepository|transcribe|Whisper)" | tail -500 > whisper_analysis.txt

echo "✅ Logs saved to: whisper_analysis.txt"
echo ""
echo "Key sections to look for:"
echo "  1. '===== STARTING TRANSCRIPTION =====' - Shows when transcription starts"
echo "  2. 'Calling JNI fullTranscribe...' - Before JNI call"
echo "  3. 'fullTranscribe called' - JNI function entry"
echo "  4. 'About to run whisper_full' - Before Whisper C++ call"
echo "  5. 'whisper_full returned' - After Whisper C++ call"
echo "  6. 'JNI fullTranscribe returned' - After JNI call"
echo "  7. 'Number of segments' - How many segments found"
echo ""
echo "If you see steps 1-2 but NOT 3-7, the JNI call failed"
echo "If you see 1-5 but NOT 6-7, Whisper C++ is hanging"
echo "If you see all steps with 0 segments, the model isn't detecting speech"
echo ""
echo "==================================================="
echo ""
echo "Opening log file for review..."
cat whisper_analysis.txt
