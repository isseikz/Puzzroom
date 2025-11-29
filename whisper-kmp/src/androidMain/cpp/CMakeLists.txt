cmake_minimum_required(VERSION 3.22.1)
project(whisper_android)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Path to whisper.cpp source
set(WHISPER_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../whisper.cpp)

# Include whisper.cpp headers
include_directories(
    ${WHISPER_CPP_DIR}
    ${WHISPER_CPP_DIR}/ggml/include
    ${WHISPER_CPP_DIR}/include
)

# Add whisper.cpp source files
set(WHISPER_SOURCES
    ${WHISPER_CPP_DIR}/src/whisper.cpp
    ${WHISPER_CPP_DIR}/ggml/src/ggml.c
    ${WHISPER_CPP_DIR}/ggml/src/ggml-alloc.c
    ${WHISPER_CPP_DIR}/ggml/src/ggml-backend.c
    ${WHISPER_CPP_DIR}/ggml/src/ggml-quants.c
)

# Platform-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "armeabi-v7a")
    # Enable NEON optimizations for ARM
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfpu=neon")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
endif()

# Build whisper.cpp as a static library
add_library(whisper STATIC ${WHISPER_SOURCES})

target_compile_definitions(whisper PRIVATE
    GGML_USE_CPU
)

# Optionally enable NNAPI support
if(WHISPER_NNAPI)
    target_compile_definitions(whisper PRIVATE GGML_USE_NNAPI)
    target_sources(whisper PRIVATE ${WHISPER_CPP_DIR}/ggml/src/ggml-nnapi.c)
endif()

# Find required libraries
find_library(LOG_LIB log)
find_library(ANDROID_LIB android)

# Build JNI wrapper
add_library(whisper_android SHARED
    whisper_jni.cpp
)

target_link_libraries(whisper_android
    whisper
    ${LOG_LIB}
    ${ANDROID_LIB}
)
