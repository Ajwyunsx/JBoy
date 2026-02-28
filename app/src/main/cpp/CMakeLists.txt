cmake_minimum_required(VERSION 3.22.1)

project("jboy-core")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# mGBA 配置
set(LIBMGBA_ONLY ON)
set(BUILD_STATIC ON)
set(BUILD_SHARED OFF)
set(M_CORE_GBA ON)
set(M_CORE_GB ON)
set(DISABLE_FRONTENDS ON)
set(DISABLE_DEPS ON)
set(ENABLE_VFS ON)
set(ENABLE_DIRECTORIES ON)

# 添加编译定义
add_definitions(-DENABLE_VFS)
add_definitions(-DENABLE_DIRECTORIES)

# 添加 mGBA 子目录
add_subdirectory(mgba)

# 添加头文件搜索路径
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/mgba/include
)

# 创建 JBOY 共享库
add_library(
    jboy-core
    SHARED
    video_renderer.cpp
    audio_output.cpp
    emulator_core.cpp
)

# 链接 Android NDK 库和 mGBA
target_link_libraries(
    jboy-core
    android
    EGL
    GLESv2
    OpenSLES
    log
    mgba
)
