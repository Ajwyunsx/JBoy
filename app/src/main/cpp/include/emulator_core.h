#ifndef EMULATOR_CORE_H
#define EMULATOR_CORE_H

#include <cstdint>
#include <cstddef>

// 按钮定义
enum GBButton {
    GB_BUTTON_A      = 0x01,
    GB_BUTTON_B      = 0x02,
    GB_BUTTON_SELECT = 0x04,
    GB_BUTTON_START  = 0x08,
    GB_BUTTON_RIGHT  = 0x10,
    GB_BUTTON_LEFT   = 0x20,
    GB_BUTTON_UP     = 0x40,
    GB_BUTTON_DOWN   = 0x80
};

// 模拟器核心类
class EmulatorCore {
public:
    EmulatorCore();
    ~EmulatorCore();
    
    // 初始化和清理
    bool init();
    void cleanup();
    
    // ROM加载
    bool loadRom(const char* romPath);
    void unloadRom();
    bool isRomLoaded() const;
    
    // 运行一帧
    void runFrame();
    
    // 输入控制
    void setInput(uint8_t buttons);
    uint8_t getInput() const;
    
    // 存档/读档
    bool saveState(int slot);
    bool loadState(int slot);
    bool hasSaveState(int slot) const;
    
    // 获取帧缓冲区
    const uint8_t* getFrameBuffer() const;
    
    // 获取音频缓冲区
    const int16_t* getAudioBuffer() const;
    int getAudioSamples() const;
    
    // 控制
    void pause();
    void resume();
    void reset();
    bool isPaused() const;
    
    // ROM信息
    const char* getRomTitle() const;
    
private:
    class Impl;
    Impl* m_impl;
};

#endif // EMULATOR_CORE_H
