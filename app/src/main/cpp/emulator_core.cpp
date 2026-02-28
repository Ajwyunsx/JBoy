#include <android/log.h>
#include <jni.h>
#include <cstring>
#include <cstdint>
#include <string>
#include <cstdio>
#include <cctype>
#include <mutex>
#include <fcntl.h>
#include <vector>

#include <mgba/core/core.h>
#include <mgba/core/cheats.h>
#include <mgba/core/config.h>
#include <mgba/core/interface.h>
#include <mgba/core/serialize.h>
#include <mgba-util/audio-buffer.h>
#include <mgba-util/image.h>
#include <mgba-util/vfs.h>

#define LOG_TAG "JBOY_Core"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const uint16_t GBA_SCREEN_WIDTH = 240;
const uint16_t GBA_SCREEN_HEIGHT = 160;

enum GBAButton {
    GBA_BUTTON_A      = 0x001,
    GBA_BUTTON_B      = 0x002,
    GBA_BUTTON_SELECT = 0x004,
    GBA_BUTTON_START  = 0x008,
    GBA_BUTTON_RIGHT  = 0x010,
    GBA_BUTTON_LEFT   = 0x020,
    GBA_BUTTON_UP     = 0x040,
    GBA_BUTTON_DOWN   = 0x080,
    GBA_BUTTON_R      = 0x100,
    GBA_BUTTON_L      = 0x200
};

class JboyCore {
public:
    JboyCore();
    ~JboyCore();

    bool init();
    void cleanup();

    bool loadRom(const char* romPath);
    void unloadRom();
    bool isRomLoaded() const { return m_core != nullptr && m_romLoaded; }

    void runFrame();
    void setInput(int buttons);
    int getInput() const { return m_buttons; }

    bool saveState(int slot);
    bool loadState(int slot);
    bool hasSaveState(int slot) const;

    const uint8_t* getFrameBuffer() const { return m_frameBuffer; }
    int getFrameBufferSize() const { return GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT * 2; }

    void pause();
    void resume();
    void reset();
    bool isPaused() const { return m_paused; }

    const char* getRomTitle() const { return m_romTitle.c_str(); }
    void setAudioConfig(int sampleRate, int bufferSize);
    void setGameOptions(bool frameSkipEnabled, int frameSkipThrottlePercent, int frameSkipInterval,
                        bool interframeBlending, int idleLoopMode, bool gbControllerRumble);
    int getAudioRate() const;
    int consumeAudioSamples(int16_t* out, int maxSamples);
    bool clearCheats();
    bool addCheatCode(const char* code);
    uint8_t* getVideoBuffer() { return m_videoBuffer; }
    void appendAudioFrame(int16_t left, int16_t right);

private:
    static constexpr int AUDIO_BUFFER_CAPACITY = 16384;

    std::string getStatePath(int slot) const;
    std::string getSavePath() const;
    void appendAudioSamples(const int16_t* samples, int sampleCount);
    bool createCoreLocked();
    bool performCoreResetLocked();

    struct mCore* m_core = nullptr;
    int m_buttons = 0;
    bool m_paused = false;
    bool m_romLoaded = false;
    bool m_coreReady = false;
    unsigned m_targetSampleRate = 44100;
    size_t m_targetAudioBufferSize = 8192;
    bool m_frameSkipEnabled = false;
    int m_frameSkipThrottlePercent = 33;
    int m_frameSkipInterval = 0;
    bool m_interframeBlending = false;
    int m_idleLoopMode = 0;
    bool m_gbControllerRumble = false;
    std::string m_romTitle;
    std::string m_romPath;

    mColor m_coreVideoBuffer[GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT];
    uint8_t m_videoBuffer[GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT * 2];
    int16_t m_audioBuffer[AUDIO_BUFFER_CAPACITY];
    int m_audioReadIndex = 0;
    int m_audioWriteIndex = 0;
    int m_audioCount = 0;
    uint8_t m_frameBuffer[GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT];
    struct mAVStream m_avStream{};
    mutable std::recursive_mutex m_coreMutex;
};

static JboyCore* g_jboyCore = nullptr;

static void onAudioRateChanged(struct mAVStream* stream, unsigned rate) {
    (void) stream;
    LOGD("Audio rate changed: %u", rate);
}

static void onPostAudioFrame(struct mAVStream* stream, int16_t left, int16_t right) {
    (void) stream;
    if (g_jboyCore) {
        g_jboyCore->appendAudioFrame(left, right);
    }
}

JboyCore::JboyCore() {
    memset(m_coreVideoBuffer, 0, sizeof(m_coreVideoBuffer));
    memset(m_videoBuffer, 0, sizeof(m_videoBuffer));
    memset(m_audioBuffer, 0, sizeof(m_audioBuffer));
    memset(m_frameBuffer, 0, sizeof(m_frameBuffer));
}

std::string JboyCore::getStatePath(int slot) const {
    return m_romPath + ".slot" + std::to_string(slot) + ".ss";
}

std::string JboyCore::getSavePath() const {
    if (m_romPath.empty()) {
        return "";
    }
    std::string savePath = m_romPath;
    size_t dot = savePath.find_last_of('.');
    if (dot != std::string::npos) {
        savePath = savePath.substr(0, dot);
    }
    savePath += ".sav";
    return savePath;
}

static std::string trimCheatLine(const std::string& input) {
    size_t start = 0;
    while (start < input.size() && std::isspace(static_cast<unsigned char>(input[start]))) {
        ++start;
    }
    if (start >= input.size()) {
        return "";
    }
    size_t end = input.size();
    while (end > start && std::isspace(static_cast<unsigned char>(input[end - 1]))) {
        --end;
    }
    return input.substr(start, end - start);
}

void JboyCore::appendAudioSamples(const int16_t* samples, int sampleCount) {
    if (!samples || sampleCount <= 0) {
        return;
    }
    for (int i = 0; i < sampleCount; ++i) {
        m_audioBuffer[m_audioWriteIndex] = samples[i];
        m_audioWriteIndex = (m_audioWriteIndex + 1) % AUDIO_BUFFER_CAPACITY;
        if (m_audioCount < AUDIO_BUFFER_CAPACITY) {
            ++m_audioCount;
        } else {
            m_audioReadIndex = (m_audioReadIndex + 1) % AUDIO_BUFFER_CAPACITY;
        }
    }
}

void JboyCore::appendAudioFrame(int16_t left, int16_t right) {
    const int16_t pair[2] = { left, right };
    appendAudioSamples(pair, 2);
}

void JboyCore::setAudioConfig(int sampleRate, int bufferSize) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    const int clampedRate = sampleRate < 8000 ? 8000 : (sampleRate > 96000 ? 96000 : sampleRate);
    const int clampedBuffer = bufferSize < 1024 ? 1024 : (bufferSize > 65536 ? 65536 : bufferSize);
    m_targetSampleRate = static_cast<unsigned>(clampedRate);
    m_targetAudioBufferSize = static_cast<size_t>(clampedBuffer);

    if (m_core) {
        m_core->opts.sampleRate = m_targetSampleRate;
        m_core->opts.audioBuffers = m_targetAudioBufferSize;
        m_core->setAudioBufferSize(m_core, m_targetAudioBufferSize);
        if (m_core->reloadConfigOption) {
            m_core->reloadConfigOption(m_core, nullptr, &m_core->config);
        }
    }
    LOGD("Audio config updated sampleRate=%u buffer=%zu", m_targetSampleRate, m_targetAudioBufferSize);
}

void JboyCore::setGameOptions(bool frameSkipEnabled, int frameSkipThrottlePercent, int frameSkipInterval,
                              bool interframeBlending, int idleLoopMode, bool gbControllerRumble) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    m_frameSkipEnabled = frameSkipEnabled;
    m_frameSkipThrottlePercent = frameSkipThrottlePercent < 0 ? 0 : (frameSkipThrottlePercent > 100 ? 100 : frameSkipThrottlePercent);
    m_frameSkipInterval = frameSkipInterval < 0 ? 0 : (frameSkipInterval > 12 ? 12 : frameSkipInterval);
    m_interframeBlending = interframeBlending;
    m_idleLoopMode = idleLoopMode;
    m_gbControllerRumble = gbControllerRumble;

    if (m_core) {
        m_core->opts.frameskip = (m_frameSkipEnabled && m_frameSkipInterval > 0) ? m_frameSkipInterval : 0;
        m_core->opts.interframeBlending = m_interframeBlending;

        const char* idleOption = "remove";
        if (m_idleLoopMode == 1) {
            idleOption = "detect";
        } else if (m_idleLoopMode == 2) {
            idleOption = "ignore";
        }

        mCoreConfigSetValue(&m_core->config, "idleOptimization", idleOption);
        mCoreConfigSetIntValue(&m_core->config, "frameskip", m_core->opts.frameskip);
        mCoreConfigSetIntValue(&m_core->config, "interframeBlending", m_interframeBlending ? 1 : 0);
        mCoreConfigSetIntValue(&m_core->config, "frameskipThrottlePercent", m_frameSkipThrottlePercent);
        mCoreConfigSetIntValue(&m_core->config, "gbControllerRumble", m_gbControllerRumble ? 1 : 0);

        if (m_core->reloadConfigOption) {
            m_core->reloadConfigOption(m_core, nullptr, &m_core->config);
        }
    }

    LOGD("Game options updated fs=%d throttle=%d interval=%d blend=%d idleMode=%d gbRumble=%d",
         m_frameSkipEnabled ? 1 : 0,
         m_frameSkipThrottlePercent,
         m_frameSkipInterval,
         m_interframeBlending ? 1 : 0,
         m_idleLoopMode,
         m_gbControllerRumble ? 1 : 0);
}

int JboyCore::getAudioRate() const {
    if (!m_core || !m_core->audioSampleRate) {
        return 0;
    }
    return static_cast<int>(m_core->audioSampleRate(m_core));
}

int JboyCore::consumeAudioSamples(int16_t* out, int maxSamples) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!out || maxSamples <= 0) {
        return 0;
    }

    int preferredBacklog = static_cast<int>((static_cast<uint64_t>(m_targetSampleRate) * 2ULL * 65ULL) / 1000ULL);
    const int minBacklog = maxSamples * 2;
    int maxBacklog = AUDIO_BUFFER_CAPACITY - maxSamples;
    if (maxBacklog < minBacklog) {
        maxBacklog = minBacklog;
    }
    if (preferredBacklog < minBacklog) {
        preferredBacklog = minBacklog;
    } else if (preferredBacklog > maxBacklog) {
        preferredBacklog = maxBacklog;
    }
    if (m_audioCount > preferredBacklog) {
        int drop = m_audioCount - preferredBacklog;
        if (drop & 1) {
            --drop;
        }
        if (drop > 0) {
            m_audioReadIndex = (m_audioReadIndex + drop) % AUDIO_BUFFER_CAPACITY;
            m_audioCount -= drop;
        }
    }

    int count = m_audioCount < maxSamples ? m_audioCount : maxSamples;
    if (count & 1) {
        --count;
    }
    if (count <= 0) {
        return 0;
    }
    for (int i = 0; i < count; ++i) {
        out[i] = m_audioBuffer[m_audioReadIndex];
        m_audioReadIndex = (m_audioReadIndex + 1) % AUDIO_BUFFER_CAPACITY;
    }
    m_audioCount -= count;
    return count;
}

bool JboyCore::clearCheats() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || !m_romLoaded || !m_core->cheatDevice) {
        return false;
    }
    struct mCheatDevice* device = m_core->cheatDevice(m_core);
    if (!device) {
        return false;
    }
    mCheatDeviceClear(device);
    return true;
}

bool JboyCore::addCheatCode(const char* code) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || !m_romLoaded || !m_core->cheatDevice || !code) {
        return false;
    }

    struct mCheatDevice* device = m_core->cheatDevice(m_core);
    if (!device) {
        return false;
    }

    struct mCheatSet* cheatSet = nullptr;
    if (mCheatSetsSize(&device->cheats)) {
        cheatSet = *mCheatSetsGetPointer(&device->cheats, 0);
    } else {
        cheatSet = device->createSet(device, "JBOY");
        if (!cheatSet) {
            return false;
        }
        mCheatAddSet(device, cheatSet);
    }

    bool added = false;
    std::string codeText(code);
    for (char& c : codeText) {
        if (c == ';' || c == '+') {
            c = '\n';
        }
    }

    size_t cursor = 0;
    while (cursor <= codeText.size()) {
        const size_t lineEnd = codeText.find('\n', cursor);
        const size_t endPos = (lineEnd == std::string::npos) ? codeText.size() : lineEnd;
        const std::string line = trimCheatLine(codeText.substr(cursor, endPos - cursor));
        if (!line.empty()) {
            if (mCheatAddLine(cheatSet, line.c_str(), 0)) {
                added = true;
            }
        }
        if (lineEnd == std::string::npos) {
            break;
        }
        cursor = lineEnd + 1;
    }

    if (!added) {
        return false;
    }

    cheatSet->enabled = true;
    if (cheatSet->refresh) {
        cheatSet->refresh(cheatSet, device);
    } else {
        mCheatRefresh(device, cheatSet);
    }
    return true;
}

bool JboyCore::createCoreLocked() {
    if (m_core) {
        m_core->deinit(m_core);
        m_core = nullptr;
    }

    m_core = mCoreCreate(mPLATFORM_GBA);
    if (!m_core) {
        LOGE("Failed to create mCore");
        return false;
    }

    m_core->init(m_core);
    mCoreInitConfig(m_core, "jboy");

    // Set sane defaults before loading user config.
    m_core->opts.volume = 0x100;
    m_core->opts.mute = false;
    m_core->opts.frameskip = (m_frameSkipEnabled && m_frameSkipInterval > 0) ? m_frameSkipInterval : 0;
    m_core->opts.interframeBlending = m_interframeBlending;
    m_core->opts.sampleRate = m_targetSampleRate;
    m_core->opts.audioBuffers = m_targetAudioBufferSize;

    mCoreLoadConfig(m_core);

    m_core->opts.useBios = false;
    m_core->opts.skipBios = true;
    m_core->opts.sampleRate = m_targetSampleRate;
    m_core->opts.mute = false;
    if (m_core->opts.volume <= 0) {
        m_core->opts.volume = 0x100;
    }
    if (m_core->opts.audioBuffers == 0) {
        m_core->opts.audioBuffers = m_targetAudioBufferSize;
    }
    m_core->opts.frameskip = (m_frameSkipEnabled && m_frameSkipInterval > 0) ? m_frameSkipInterval : 0;
    m_core->opts.interframeBlending = m_interframeBlending;

    const char* idleOption = "remove";
    if (m_idleLoopMode == 1) {
        idleOption = "detect";
    } else if (m_idleLoopMode == 2) {
        idleOption = "ignore";
    }
    mCoreConfigSetValue(&m_core->config, "idleOptimization", idleOption);
    mCoreConfigSetIntValue(&m_core->config, "frameskip", m_core->opts.frameskip);
    mCoreConfigSetIntValue(&m_core->config, "interframeBlending", m_interframeBlending ? 1 : 0);
    mCoreConfigSetIntValue(&m_core->config, "frameskipThrottlePercent", m_frameSkipThrottlePercent);
    mCoreConfigSetIntValue(&m_core->config, "gbControllerRumble", m_gbControllerRumble ? 1 : 0);

    // Apply current opts (volume/mute/frameskip) to running GBA core.
    if (m_core->reloadConfigOption) {
        m_core->reloadConfigOption(m_core, nullptr, &m_core->config);
    }

    memset(&m_avStream, 0, sizeof(m_avStream));
    m_avStream.audioRateChanged = onAudioRateChanged;
    // Prefer pulling from mCore audio buffer in runFrame.
    m_avStream.postAudioFrame = nullptr;
    m_core->setAVStream(m_core, &m_avStream);

    m_core->setVideoBuffer(m_core, m_coreVideoBuffer, GBA_SCREEN_WIDTH);
    m_core->setAudioBufferSize(m_core, m_targetAudioBufferSize);

    m_romLoaded = false;
    m_coreReady = false;
    m_paused = false;
    m_audioReadIndex = 0;
    m_audioWriteIndex = 0;
    m_audioCount = 0;
    return true;
}

bool JboyCore::performCoreResetLocked() {
    if (!m_core || !m_romLoaded) {
        m_coreReady = false;
        return false;
    }
    if (!m_core->reset) {
        LOGE("reset callback is null");
        m_coreReady = false;
        return false;
    }

    m_core->opts.useBios = false;
    m_core->opts.skipBios = true;
    m_core->opts.sampleRate = m_targetSampleRate;
    m_core->opts.mute = false;
    if (m_core->opts.volume <= 0) {
        m_core->opts.volume = 0x100;
    }
    if (m_core->opts.audioBuffers == 0) {
        m_core->opts.audioBuffers = m_targetAudioBufferSize;
    }
    m_core->opts.frameskip = (m_frameSkipEnabled && m_frameSkipInterval > 0) ? m_frameSkipInterval : 0;
    m_core->opts.interframeBlending = m_interframeBlending;

    const char* idleOption = "remove";
    if (m_idleLoopMode == 1) {
        idleOption = "detect";
    } else if (m_idleLoopMode == 2) {
        idleOption = "ignore";
    }
    mCoreConfigSetValue(&m_core->config, "idleOptimization", idleOption);
    mCoreConfigSetIntValue(&m_core->config, "frameskip", m_core->opts.frameskip);
    mCoreConfigSetIntValue(&m_core->config, "interframeBlending", m_interframeBlending ? 1 : 0);
    mCoreConfigSetIntValue(&m_core->config, "frameskipThrottlePercent", m_frameSkipThrottlePercent);
    mCoreConfigSetIntValue(&m_core->config, "gbControllerRumble", m_gbControllerRumble ? 1 : 0);

    if (m_core->reloadConfigOption) {
        m_core->reloadConfigOption(m_core, nullptr, &m_core->config);
    }
    m_core->setVideoBuffer(m_core, m_coreVideoBuffer, GBA_SCREEN_WIDTH);
    m_core->setAudioBufferSize(m_core, m_targetAudioBufferSize);
    m_core->setAVStream(m_core, &m_avStream);
    m_core->reset(m_core);

    m_audioReadIndex = 0;
    m_audioWriteIndex = 0;
    m_audioCount = 0;
    m_coreReady = true;
    m_paused = false;
    return true;
}

JboyCore::~JboyCore() {
    cleanup();
}

bool JboyCore::init() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    LOGD("Initializing JBOY core with mGBA");
    const bool ok = createCoreLocked();
    if (ok) {
        LOGD("JBOY core initialized successfully");
    }
    return ok;
}

void JboyCore::cleanup() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    LOGD("Cleaning up JBOY core");
    if (m_core) {
        if (m_romLoaded && m_core->unloadROM) {
            m_core->unloadROM(m_core);
        }
        m_core->deinit(m_core);
        m_core = nullptr;
    }
    m_romLoaded = false;
    m_coreReady = false;
    m_audioReadIndex = 0;
    m_audioWriteIndex = 0;
    m_audioCount = 0;
}

bool JboyCore::loadRom(const char* romPath) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    LOGD("Loading ROM: %s", romPath);
    if (!m_core || m_romLoaded) {
        if (!createCoreLocked()) {
            LOGE("Core reinitialization failed");
            return false;
        }
    }
    m_coreReady = false;
    m_romPath = romPath;
    
    struct VFile* vf = VFileOpen(romPath, O_RDONLY);
    if (!vf) {
        LOGE("Failed to open ROM file: %s", romPath);
        return false;
    }
    
    if (!m_core->loadROM(m_core, vf)) {
        LOGE("Failed to load ROM: %s", romPath);
        vf->close(vf);
        return false;
    }
    // IMPORTANT: mGBA core takes ownership of vf after loadROM succeeds.
    // Do not close here; it will be handled by core unload/deinit.

    const std::string savePath = getSavePath();
    if (!savePath.empty()) {
        if (mCoreLoadSaveFile(m_core, savePath.c_str(), false)) {
            LOGD("Save data attached: %s", savePath.c_str());
        } else {
            LOGE("Failed to attach save data file: %s", savePath.c_str());
        }
    }
    
    struct mGameInfo info;
    memset(&info, 0, sizeof(info));
    m_core->getGameInfo(m_core, &info);
    if (info.title[0] != '\0') {
        m_romTitle = info.title;
    } else {
        const char* name = strrchr(romPath, '/');
        if (!name) name = strrchr(romPath, '\\');
        if (name) name++;
        else name = romPath;
        m_romTitle = name;
    }
    size_t extPos = m_romTitle.rfind(".gba");
    if (extPos != std::string::npos) {
        m_romTitle = m_romTitle.substr(0, extPos);
    }
    LOGD("ROM loaded: %s", m_romTitle.c_str());
    m_romLoaded = true;
    if (!performCoreResetLocked()) {
        LOGE("Core reset failed after loading ROM");
        m_core->unloadROM(m_core);
        m_romLoaded = false;
        return false;
    }
    return true;
}

void JboyCore::unloadRom() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (m_core && m_romLoaded && m_core->unloadROM) {
        m_core->unloadROM(m_core);
    }
    m_romLoaded = false;
    m_coreReady = false;
    m_audioReadIndex = 0;
    m_audioWriteIndex = 0;
    m_audioCount = 0;
    m_romTitle.clear();
}

void JboyCore::runFrame() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || !m_romLoaded || !m_coreReady || m_paused) {
        return;
    }
    if (!m_core->runFrame) {
        LOGE("runFrame callback is null");
        return;
    }
    m_core->runFrame(m_core);

    const size_t pixelCount = GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT;
    if (sizeof(mColor) == 2) {
        memcpy(m_videoBuffer, m_coreVideoBuffer, pixelCount * 2);
    } else {
        for (size_t i = 0; i < pixelCount; ++i) {
            const uint32_t c = static_cast<uint32_t>(m_coreVideoBuffer[i]);
            // mGBA native 32-bit color is XBGR8 by default:
            // bits 0-7: R, 8-15: G, 16-23: B.
            const uint8_t r = c & 0xFF;
            const uint8_t g = (c >> 8) & 0xFF;
            const uint8_t b = (c >> 16) & 0xFF;
            const uint16_t rgb565 = ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3);
            m_videoBuffer[i * 2] = static_cast<uint8_t>(rgb565 & 0xFF);
            m_videoBuffer[i * 2 + 1] = static_cast<uint8_t>((rgb565 >> 8) & 0xFF);
        }
    }

    if (m_core->getAudioBuffer) {
        struct mAudioBuffer* audioBuffer = m_core->getAudioBuffer(m_core);
        if (audioBuffer) {
            size_t loops = 0;
            while (loops < 8) {
                const size_t availableFrames = mAudioBufferAvailable(audioBuffer);
                if (!availableFrames) {
                    break;
                }
                const size_t maxFrames = 1024;
                const size_t framesToRead = availableFrames < maxFrames ? availableFrames : maxFrames;
                int16_t temp[1024 * 2];
                const size_t readFrames = mAudioBufferRead(audioBuffer, temp, framesToRead);
                if (!readFrames) {
                    break;
                }
                appendAudioSamples(temp, static_cast<int>(readFrames * 2));
                ++loops;
            }
        }
    }
}

void JboyCore::setInput(int buttons) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    m_buttons = buttons;
    if (!m_core) return;
    uint32_t keys = 0;
    if (buttons & GBA_BUTTON_A) keys |= 1 << 0;
    if (buttons & GBA_BUTTON_B) keys |= 1 << 1;
    if (buttons & GBA_BUTTON_SELECT) keys |= 1 << 2;
    if (buttons & GBA_BUTTON_START) keys |= 1 << 3;
    if (buttons & GBA_BUTTON_RIGHT) keys |= 1 << 4;
    if (buttons & GBA_BUTTON_LEFT) keys |= 1 << 5;
    if (buttons & GBA_BUTTON_UP) keys |= 1 << 6;
    if (buttons & GBA_BUTTON_DOWN) keys |= 1 << 7;
    if (buttons & GBA_BUTTON_R) keys |= 1 << 8;
    if (buttons & GBA_BUTTON_L) keys |= 1 << 9;
    m_core->setKeys(m_core, keys);
}

bool JboyCore::saveState(int slot) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || !m_romLoaded || slot < 0) return false;
    LOGD("Saving state to slot: %d", slot);

    if (!m_core->stateSize || !m_core->saveState) {
        LOGE("Core state callbacks unavailable");
        return false;
    }

    const size_t stateSize = m_core->stateSize(m_core);
    if (!stateSize) {
        LOGE("Invalid state size: 0");
        return false;
    }

    std::vector<uint8_t> stateData(stateSize);
    bool ok = m_core->saveState(m_core, stateData.data());
    if (!ok) {
        LOGE("Core saveState callback failed, trying mCoreSaveState fallback");
        ok = mCoreSaveState(m_core, slot, 0);
        return ok;
    }

    const std::string statePath = getStatePath(slot);
    LOGD("Save state path: %s", statePath.c_str());
    FILE* fp = fopen(statePath.c_str(), "wb");
    if (!fp) {
        LOGE("Failed to fopen state file for writing: %s", statePath.c_str());
        const bool fallbackOk = mCoreSaveState(m_core, slot, 0);
        LOGD("Save state fallback result slot %d: %d", slot, fallbackOk ? 1 : 0);
        return fallbackOk;
    }

    const size_t written = fwrite(stateData.data(), 1, stateSize, fp);
    fclose(fp);
    ok = written == stateSize;
    if (!ok) {
        LOGE("State file write failed, trying mCoreSaveState fallback");
        ok = mCoreSaveState(m_core, slot, 0);
    }
    LOGD("Save state result slot %d: %d", slot, ok ? 1 : 0);
    return ok;
}

bool JboyCore::loadState(int slot) {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || !m_romLoaded || slot < 0) return false;
    LOGD("Loading state from slot: %d", slot);

    if (!m_core->stateSize || !m_core->loadState) {
        LOGE("Core load callbacks unavailable, trying mCoreLoadState fallback");
        const bool fallbackOk = mCoreLoadState(m_core, slot, 0);
        if (fallbackOk) {
            m_coreReady = true;
        }
        return fallbackOk;
    }

    const size_t stateSize = m_core->stateSize(m_core);
    if (!stateSize) {
        LOGE("Invalid state size: 0");
        const bool fallbackOk = mCoreLoadState(m_core, slot, 0);
        if (fallbackOk) {
            m_coreReady = true;
        }
        return fallbackOk;
    }

    const std::string statePath = getStatePath(slot);
    LOGD("Load state path: %s", statePath.c_str());
    FILE* fp = fopen(statePath.c_str(), "rb");
    if (!fp) {
        LOGE("Failed to fopen state file for reading: %s", statePath.c_str());
        const bool fallbackOk = mCoreLoadState(m_core, slot, 0);
        if (fallbackOk) {
            m_coreReady = true;
        }
        return fallbackOk;
    }

    std::vector<uint8_t> stateData(stateSize);
    const size_t read = fread(stateData.data(), 1, stateSize, fp);
    fclose(fp);
    if (read != stateSize) {
        LOGE("State file size mismatch. Read=%zu expected=%zu", read, stateSize);
        const bool fallbackOk = mCoreLoadState(m_core, slot, 0);
        if (fallbackOk) {
            m_coreReady = true;
        }
        return fallbackOk;
    }

    bool ok = m_core->loadState(m_core, stateData.data());
    if (!ok) {
        LOGE("Core loadState callback failed, trying mCoreLoadState fallback");
        ok = mCoreLoadState(m_core, slot, 0);
    }
    LOGD("Load state result slot %d: %d", slot, ok ? 1 : 0);
    if (ok) {
        m_coreReady = true;
    }
    return ok;
}

bool JboyCore::hasSaveState(int slot) const {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (!m_core || slot < 0) return false;

    const std::string statePath = getStatePath(slot);
    FILE* fp = fopen(statePath.c_str(), "rb");
    if (fp) {
        fclose(fp);
        return true;
    }

    struct VFile* vf = mCoreGetState(m_core, slot, false);
    if (vf) {
        vf->close(vf);
        return true;
    }
    return false;
}

void JboyCore::pause() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    m_paused = true;
    LOGD("JBOY paused");
}

void JboyCore::resume() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    m_paused = false;
    LOGD("JBOY resumed");
}

void JboyCore::reset() {
    std::lock_guard<std::recursive_mutex> lock(m_coreMutex);
    if (m_romPath.empty()) {
        LOGE("JBOY reset failed: rom path empty");
        return;
    }
    const std::string currentPath = m_romPath;
    if (!loadRom(currentPath.c_str())) {
        LOGE("JBOY reset failed");
        return;
    }
    LOGD("JBOY reset done by reloading ROM");
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeInit(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) delete g_jboyCore;
    g_jboyCore = new JboyCore();
    return g_jboyCore->init() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeLoadRom(JNIEnv* env, jobject thiz, jstring romPath) {
    if (!g_jboyCore) return JNI_FALSE;
    const char* path = env->GetStringUTFChars(romPath, nullptr);
    const bool loaded = g_jboyCore->loadRom(path);
    env->ReleaseStringUTFChars(romPath, path);
    return loaded ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeRunFrame(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) g_jboyCore->runFrame();
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSetInput(JNIEnv* env, jobject thiz, jint buttons) {
    if (g_jboyCore) g_jboyCore->setInput(buttons);
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSetAudioConfig(JNIEnv* env, jobject thiz, jint sampleRate, jint bufferSize) {
    (void) env;
    (void) thiz;
    if (g_jboyCore) {
        g_jboyCore->setAudioConfig(sampleRate, bufferSize);
    }
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSetGameOptions(
    JNIEnv* env,
    jobject thiz,
    jboolean frameSkipEnabled,
    jint frameSkipThrottlePercent,
    jint frameSkipInterval,
    jboolean interframeBlending,
    jstring idleLoopRemoval,
    jboolean gbControllerRumble
) {
    (void) env;
    (void) thiz;
    if (!g_jboyCore) {
        return;
    }

    int idleLoopMode = 0;
    if (idleLoopRemoval) {
        const char* modeChars = env->GetStringUTFChars(idleLoopRemoval, nullptr);
        if (modeChars) {
            if (strcmp(modeChars, "DETECT_AND_REMOVE") == 0) {
                idleLoopMode = 1;
            } else if (strcmp(modeChars, "IGNORE") == 0) {
                idleLoopMode = 2;
            }
            env->ReleaseStringUTFChars(idleLoopRemoval, modeChars);
        }
    }

    g_jboyCore->setGameOptions(
        frameSkipEnabled == JNI_TRUE,
        static_cast<int>(frameSkipThrottlePercent),
        static_cast<int>(frameSkipInterval),
        interframeBlending == JNI_TRUE,
        idleLoopMode,
        gbControllerRumble == JNI_TRUE
    );
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSaveState(JNIEnv* env, jobject thiz, jint slot) {
    if (!g_jboyCore) return JNI_FALSE;
    return g_jboyCore->saveState(slot) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeLoadState(JNIEnv* env, jobject thiz, jint slot) {
    if (!g_jboyCore) return JNI_FALSE;
    return g_jboyCore->loadState(slot) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeHasSaveState(JNIEnv* env, jobject thiz, jint slot) {
    if (!g_jboyCore) return JNI_FALSE;
    return g_jboyCore->hasSaveState(slot) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeCleanup(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) {
        g_jboyCore->cleanup();
        delete g_jboyCore;
        g_jboyCore = nullptr;
    }
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeIsPaused(JNIEnv* env, jobject thiz) {
    if (!g_jboyCore) return JNI_FALSE;
    return g_jboyCore->isPaused() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativePause(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) g_jboyCore->pause();
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeResume(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) g_jboyCore->resume();
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeReset(JNIEnv* env, jobject thiz) {
    if (g_jboyCore) g_jboyCore->reset();
}

JNIEXPORT jstring JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeGetRomTitle(JNIEnv* env, jobject thiz) {
    if (!g_jboyCore) return env->NewStringUTF("");
    return env->NewStringUTF(g_jboyCore->getRomTitle());
}

JNIEXPORT jint JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeGetAudioSampleRate(JNIEnv* env, jobject thiz) {
    (void) env;
    (void) thiz;
    if (!g_jboyCore) {
        return 0;
    }
    return static_cast<jint>(g_jboyCore->getAudioRate());
}

JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeClearCheats(JNIEnv* env, jobject thiz) {
    (void) env;
    (void) thiz;
    if (!g_jboyCore) {
        return;
    }
    g_jboyCore->clearCheats();
}

JNIEXPORT jboolean JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeAddCheatCode(JNIEnv* env, jobject thiz, jstring code) {
    (void) thiz;
    if (!g_jboyCore || !code) {
        return JNI_FALSE;
    }

    const char* raw = env->GetStringUTFChars(code, nullptr);
    if (!raw) {
        return JNI_FALSE;
    }
    const bool ok = g_jboyCore->addCheatCode(raw);
    env->ReleaseStringUTFChars(code, raw);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeGetVideoFrame(JNIEnv* env, jobject thiz) {
    if (!g_jboyCore || !g_jboyCore->isRomLoaded()) {
        return nullptr;
    }
    const uint8_t* frame = g_jboyCore->getVideoBuffer();
    const int size = GBA_SCREEN_WIDTH * GBA_SCREEN_HEIGHT * 2;
    if (!frame) {
        return nullptr;
    }
    jbyteArray out = env->NewByteArray(size);
    if (!out) {
        return nullptr;
    }
    env->SetByteArrayRegion(out, 0, size, reinterpret_cast<const jbyte*>(frame));
    return out;
}

JNIEXPORT jshortArray JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeGetAudioFrame(JNIEnv* env, jobject thiz) {
    if (!g_jboyCore || !g_jboyCore->isRomLoaded()) {
        return nullptr;
    }

    int16_t localBuffer[2048];
    const int count = g_jboyCore->consumeAudioSamples(localBuffer, 2048);
    if (count <= 0) {
        return nullptr;
    }

    jshortArray out = env->NewShortArray(count);
    if (!out) {
        return nullptr;
    }
    env->SetShortArrayRegion(out, 0, count, reinterpret_cast<const jshort*>(localBuffer));
    return out;
}

} // extern "C"
