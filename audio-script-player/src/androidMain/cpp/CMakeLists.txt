# CMakeLists.txt for whisper.cpp JNI integration
#
# Build instructions:
# 1. Download or clone whisper.cpp: https://github.com/ggerganov/whisper.cpp
# 2. Build whisper.cpp for Android (follow whisper.cpp Android example)
# 3. Copy libwhisper.so to jniLibs/<abi>/
# 4. Uncomment the whisper.cpp integration in native-lib.cpp

cmake_minimum_required(VERSION 3.22.1)

project("audioscriptplayer")

# For now, build just the JNI wrapper
# When whisper.cpp is integrated, link against libwhisper.so

add_library(${CMAKE_PROJECT_NAME} SHARED
    native-lib.cpp
)

# Find and link required libraries
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(${CMAKE_PROJECT_NAME}
    ${log-lib}
    ${android-lib}
)

# When whisper.cpp is available, add these:
# set(WHISPER_DIR "${CMAKE_SOURCE_DIR}/../../../../whisper.cpp")
# 
# include_directories(
#     ${WHISPER_DIR}
#     ${WHISPER_DIR}/include
# )
# 
# # Link against pre-built whisper library
# add_library(whisper SHARED IMPORTED)
# set_target_properties(whisper PROPERTIES
#     IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libwhisper.so
# )
# 
# target_link_libraries(${CMAKE_PROJECT_NAME}
#     whisper
#     ${log-lib}
#     ${android-lib}
# )
