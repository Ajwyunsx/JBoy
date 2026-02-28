# JBOY Architecture

## Overview

JBOY is a GBA emulator for Android built with Clean Architecture principles.

## Architecture Layers

```
┌─────────────────────────────────────────┐
│           Presentation Layer             │
│  (Compose UI, ViewModels, Navigation)    │
├─────────────────────────────────────────┤
│             Domain Layer                 │
│     (Use Cases, Repository Interfaces)   │
├─────────────────────────────────────────┤
│              Data Layer                  │
│  (Repository Implementations, DataStore) │
├─────────────────────────────────────────┤
│            Native Layer                  │
│   (JNI Bridge, mGBA Core, OpenGL ES)    │
└─────────────────────────────────────────┘
```

## Module Structure

### Core Module (`core/`)
- `EmulatorCore`: Main emulator controller
- `VideoRenderer`: OpenGL ES rendering
- `AudioOutput`: OpenSL ES audio
- `InputHandler`: Touch input processing

### Data Module (`data/`)
- `GameDatabase`: Room database
- `SettingsRepository`: DataStore preferences
- `RomRepository`: File system access

### UI Module (`ui/`)
- `gamelist/`: Game library screen
- `game/`: Gameplay screen
- `gamepad/`: Virtual controller
- `settings/`: Settings screen
- `theme/`: Material 3 theming

## JNI Interface

```cpp
// Key JNI functions
Java_com_jboy_emulator_core_EmulatorCore_nativeInit()
Java_com_jboy_emulator_core_EmulatorCore_nativeLoadRom()
Java_com_jboy_emulator_core_EmulatorCore_nativeRunFrame()
Java_com_jboy_emulator_core_EmulatorCore_nativeSetInput()
Java_com_jboy_emulator_core_EmulatorCore_nativeSaveState()
Java_com_jboy_emulator_core_EmulatorCore_nativeLoadState()
```

## Rendering Pipeline

1. GBA framebuffer in native memory
2. Copy to OpenGL texture
3. Render to screen with vertex shaders
4. Apply video filters (optional)

## State Management

- Compose StateFlow for UI state
- DataStore for persistent settings
- Room for game metadata
- File system for ROMs and save states
