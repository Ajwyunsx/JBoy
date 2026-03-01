# JBOY GBA Emulator

<p align="center">
  <img src="docs/images/logo.png" alt="JBOY Logo" width="200"/>
</p>

<p align="center">
  <a href="https://github.com/jboy-emulator/jboy-android/releases">
    <img src="https://img.shields.io/github/v/release/jboy-emulator/jboy-android" alt="GitHub release"/>
  </a>
  <a href="https://github.com/jboy-emulator/jboy-android/releases">
    <img src="https://img.shields.io/github/downloads/jboy-emulator/jboy-android/total" alt="GitHub downloads"/>
  </a>
  <a href="https://github.com/jboy-emulator/jboy-android/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/jboy-emulator/jboy-android" alt="License"/>
  </a>
</p>

## 简介

**JBOY** 是一款运行在 Android 平台上的 GBA (Game Boy Advance) 模拟器，采用 Kotlin 开发，遵循 Material Design 3 设计规范，拥有精美的青色主题界面。

## 特性

### 核心功能
- ✅ 完整的 GBA 游戏兼容性
- ✅ 高性能模拟 (基于 mGBA 核心)
- ✅ 实时存档/读档 (4个槽位)
- ✅ 快进功能 (最高 16x 速度)
- ✅ 自定义金手指 (GameShark/Action Replay)

### 视频功能
- ✅ OpenGL ES 2.0 硬件加速渲染
- ✅ 多种视频滤镜 (原始/线性/CRT)
- ✅ 多种屏幕比例 (适应/拉伸/整数缩放)
- ✅ 帧率显示

### 音频功能
- ✅ OpenSL ES 低延迟音频输出
- ✅ 44.1kHz 立体声
- ✅ 音量控制

### 控制功能
- ✅ 可自定义虚拟手柄
- ✅ 按钮位置/大小调整
- ✅ 透明度调节
- ✅ 触摸振动反馈

### 界面功能
- ✅ Material Design 3 青色主题
- ✅ 深色/浅色模式
- ✅ 游戏库管理
- ✅ 最近游戏记录
- ✅ 游戏收藏

## 系统要求

- Android 7.0 (API 24) 或更高版本
- 64位处理器 (ARM64)
- 约 50MB 可用存储空间

## 安装

### 从 Release 下载
1. 前往 [Releases]([https://github.com/Ajwyunsx/JBoy/releases) 页面
2. 下载最新的 APK 文件
3. 安装到您的 Android 设备

### 从源码构建
```bash
# 克隆仓库
git clone https://github.com/Ajwyunsx/JBoy
cd jboy-android

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

## 使用指南

### 添加游戏
1. 点击主界面底部的 "+" 按钮
2. 选择包含 GBA ROM 的文件夹
3. 等待扫描完成

### 开始游戏
1. 在游戏库中点击游戏卡片
2. 等待游戏加载

### 虚拟手柄
- 方向键: 屏幕左下角
- A/B 按钮: 屏幕右下角
- L/R 肩键: 屏幕上边缘
- Start/Select: 屏幕底部中央

### 快捷操作
- 点击屏幕显示/隐藏虚拟手柄
- 双指捏合调整按钮大小
- 长按屏幕打开游戏菜单

## 项目结构

```
JBOY/
├── app/
│   ├── src/main/
│   │   ├── cpp/                    # NDK 原生代码
│   │   │   ├── jni_bridge.cpp     # JNI 接口层
│   │   │   ├── video_renderer.cpp # OpenGL 渲染器
│   │   │   ├── audio_output.cpp   # 音频输出
│   │   │   └── emulator_core.cpp  # 模拟器核心
│   │   ├── java/com/jboy/emulator/
│   │   │   ├── core/              # 核心模块
│   │   │   ├── ui/                # UI 组件
│   │   │   ├── data/              # 数据层
│   │   │   └── viewmodel/         # ViewModel
│   │   └── res/                   # 资源文件
│   └── build.gradle.kts
├── docs/                          # 文档
└── README.md
```

## 技术栈

- **语言**: Kotlin, C/C++
- **UI 框架**: Jetpack Compose, Material Design 3
- **核心模拟**: mGBA
- **渲染**: OpenGL ES 2.0
- **音频**: OpenSL ES
- **架构**: MVVM, Clean Architecture
- **构建**: Gradle, CMake, NDK

## 致谢

- [mGBA](https://github.com/mgba-emu/mgba) - 高性能 GBA 模拟器核心
- [Android Open Source Project](https://source.android.com/) - Android 平台
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 UI 工具包

## 许可证

本项目基于 MPL-2.0 许可证开源，详见 [LICENSE](LICENSE) 文件。

## 声明

本项目仅供学习交流使用。请确保您拥有所使用 ROM 的合法版权。
