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

if(TARGET llama-common)
    set(RAVEN_LLAMA_COMMON_TARGET llama-common)
elseif(TARGET common)
    set(RAVEN_LLAMA_COMMON_TARGET common)
else()
    message(FATAL_ERROR "Pinned llama.cpp did not expose its common/Jinja target")
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

set(RAVEN_WHISPER_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../native/whisper.cpp" CACHE PATH "Pinned whisper.cpp source root")
if(NOT EXISTS "${RAVEN_WHISPER_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Pinned whisper.cpp source missing. Expected: ${RAVEN_WHISPER_DIR}")
endif()

# llama.cpp is Raven's sole GGML authority.
if(NOT TARGET ggml)
    message(FATAL_ERROR "Raven shared GGML authority was not established by llama.cpp")
endif()

# Do not add whisper.cpp's root CMake project here. Its fallback path can add
# its bundled GGML tree, producing duplicate ggml/ggml-base targets. Build the
# pinned Whisper library source directory directly against llama's ggml target.
set(WHISPER_VERSION_BASE "1.9.5")
set(WHISPER_VERSION_MAJOR "1")
set(WHISPER_EXTRA_FLAGS "")
set(PARAKEET_EXTRA_FLAGS "")
set(WHISPER_COREML OFF)
set(WHISPER_OPENVINO OFF)
set(WHISPER_VITISAI OFF)
set(WHISPER_MKL OFF)
set(WHISPER_ALL_WARNINGS OFF)

add_subdirectory(
    "${RAVEN_WHISPER_DIR}/src"
    "${CMAKE_BINARY_DIR}/whisper-src"
    EXCLUDE_FROM_ALL
)

add_library(raven_whisper SHARED raven_whisper_jni.cpp)
target_include_directories(raven_whisper PRIVATE "${RAVEN_WHISPER_DIR}/include")
target_link_libraries(raven_whisper PRIVATE whisper android log)
target_compile_options(raven_whisper PRIVATE -Wall -Wextra -Werror=return-type)
