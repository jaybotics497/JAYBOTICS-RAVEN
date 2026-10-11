cmake_minimum_required(VERSION 3.22.1)
project(raven_qwen LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

set(RAVEN_LLAMA_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../native/llama.cpp" CACHE PATH "Pinned llama.cpp source root")
if(NOT EXISTS "${RAVEN_LLAMA_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Pinned llama.cpp source missing. Expected: ${RAVEN_LLAMA_DIR}")
endif()

add_subdirectory("${RAVEN_LLAMA_DIR}" "${CMAKE_BINARY_DIR}/llama" EXCLUDE_FROM_ALL)

add_library(raven_qwen SHARED raven_qwen_jni.cpp)

target_include_directories(raven_qwen PRIVATE
    "${RAVEN_LLAMA_DIR}/include"
    "${RAVEN_LLAMA_DIR}/ggml/include"
    "${RAVEN_LLAMA_DIR}/common"
)

# LLAMA_BUILD_COMMON is enabled above. Resolve the exact target exported by
# this pinned llama.cpp revision rather than silently linking the wrong one.
if(TARGET llama-common)
    set(RAVEN_LLAMA_COMMON_TARGET llama-common)
elseif(TARGET common)
    set(RAVEN_LLAMA_COMMON_TARGET common)
else()
    message(FATAL_ERROR
        "Pinned llama.cpp did not expose its common/Jinja target")
endif()

target_link_libraries(
    raven_qwen
    PRIVATE
        llama
        ${RAVEN_LLAMA_COMMON_TARGET}
        android
        log
)

target_compile_options(
    raven_qwen
    PRIVATE
        -Wall
        -Wextra
        -Werror=return-type
)

# Pinned whisper.cpp source is admitted independently from the Raven application.
set(RAVEN_WHISPER_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../native/whisper.cpp" CACHE PATH "Pinned whisper.cpp source root")
if(NOT EXISTS "${RAVEN_WHISPER_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Pinned whisper.cpp source missing. Expected: ${RAVEN_WHISPER_DIR}")
endif()
# llama.cpp establishes Raven's single GGML target above.
# whisper.cpp must consume that target rather than creating a second GGML copy.
if(NOT TARGET ggml)
    message(FATAL_ERROR "Raven shared GGML authority was not established by llama.cpp")
endif()

set(GGML_FOUND TRUE CACHE BOOL "" FORCE)
set(GGML_INCLUDE_DIRS
    "${RAVEN_LLAMA_DIR}/ggml/include"
    "${RAVEN_LLAMA_DIR}/ggml/src"
    CACHE STRING "" FORCE
)

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory("${RAVEN_WHISPER_DIR}" "${CMAKE_BINARY_DIR}/whisper" EXCLUDE_FROM_ALL)
add_library(raven_whisper SHARED raven_whisper_jni.cpp)
target_include_directories(raven_whisper PRIVATE "${RAVEN_WHISPER_DIR}/include")
target_link_libraries(raven_whisper PRIVATE whisper android log)
target_compile_options(raven_whisper PRIVATE -Wall -Wextra -Werror=return-type)
