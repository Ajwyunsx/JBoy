#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>
#include <cstring>
#include <cstdint>

#define LOG_TAG "JBOY_Audio"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// GB音频参数
const int GB_AUDIO_SAMPLE_RATE = 44100;
const int GB_AUDIO_CHANNELS = 2;
const int GB_AUDIO_BUFFER_SIZE = 2048;

class AudioOutput {
public:
    AudioOutput();
    ~AudioOutput();
    
    bool initialize();
    void shutdown();
    void play();
    void pause();
    void writeSamples(const int16_t* samples, int count);
    bool isInitialized() const { return m_initialized; }
    
private:
    static void bufferQueueCallback(SLAndroidSimpleBufferQueueItf bq, void* context);
    void processBuffer();
    
    // OpenSL ES 对象
    SLObjectItf m_engineObject = nullptr;
    SLEngineItf m_engineEngine = nullptr;
    SLObjectItf m_outputMixObject = nullptr;
    SLObjectItf m_playerObject = nullptr;
    SLPlayItf m_playerPlay = nullptr;
    SLAndroidSimpleBufferQueueItf m_playerBufferQueue = nullptr;
    SLVolumeItf m_playerVolume = nullptr;
    
    // 音频缓冲区 (双缓冲)
    int16_t m_buffers[2][GB_AUDIO_BUFFER_SIZE];
    int m_currentBuffer = 0;
    bool m_initialized = false;
    bool m_playing = false;
};

AudioOutput::AudioOutput() {
    // 初始化缓冲区为静音
    std::memset(m_buffers, 0, sizeof(m_buffers));
}

AudioOutput::~AudioOutput() {
    shutdown();
}

bool AudioOutput::initialize() {
    LOGD("Initializing audio output");
    
    SLresult result;
    
    // 创建引擎对象
    result = slCreateEngine(&m_engineObject, 0, nullptr, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create engine");
        return false;
    }
    
    // 实例化引擎
    result = (*m_engineObject)->Realize(m_engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize engine");
        return false;
    }
    
    // 获取引擎接口
    result = (*m_engineObject)->GetInterface(m_engineObject, SL_IID_ENGINE, &m_engineEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get engine interface");
        return false;
    }
    
    // 创建输出混音器
    result = (*m_engineEngine)->CreateOutputMix(m_engineEngine, &m_outputMixObject, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create output mix");
        return false;
    }
    
    // 实例化混音器
    result = (*m_outputMixObject)->Realize(m_outputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize output mix");
        return false;
    }
    
    // 配置音频源
    SLDataLocator_AndroidSimpleBufferQueue locatorBufferQueue;
    locatorBufferQueue.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
    locatorBufferQueue.numBuffers = 2;
    
    SLDataFormat_PCM formatPCM;
    formatPCM.formatType = SL_DATAFORMAT_PCM;
    formatPCM.numChannels = GB_AUDIO_CHANNELS;
    formatPCM.samplesPerSec = SL_SAMPLINGRATE_44_1;
    formatPCM.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    formatPCM.containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
    formatPCM.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    formatPCM.endianness = SL_BYTEORDER_LITTLEENDIAN;
    
    SLDataSource audioSrc;
    audioSrc.pLocator = &locatorBufferQueue;
    audioSrc.pFormat = &formatPCM;
    
    // 配置音频接收器
    SLDataLocator_OutputMix locatorOutputMix;
    locatorOutputMix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locatorOutputMix.outputMix = m_outputMixObject;
    
    SLDataSink audioSnk;
    audioSnk.pLocator = &locatorOutputMix;
    audioSnk.pFormat = nullptr;
    
    // 创建音频播放器
    const SLInterfaceID ids[] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean req[] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    
    result = (*m_engineEngine)->CreateAudioPlayer(m_engineEngine, &m_playerObject, 
                                                   &audioSrc, &audioSnk, 2, ids, req);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio player");
        return false;
    }
    
    // 实例化音频播放器
    result = (*m_playerObject)->Realize(m_playerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize audio player");
        return false;
    }
    
    // 获取播放接口
    result = (*m_playerObject)->GetInterface(m_playerObject, SL_IID_PLAY, &m_playerPlay);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get play interface");
        return false;
    }
    
    // 获取缓冲区队列接口
    result = (*m_playerObject)->GetInterface(m_playerObject, SL_IID_BUFFERQUEUE, &m_playerBufferQueue);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get buffer queue interface");
        return false;
    }
    
    // 注册缓冲区回调
    result = (*m_playerBufferQueue)->RegisterCallback(m_playerBufferQueue, bufferQueueCallback, this);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to register buffer callback");
        return false;
    }
    
    // 获取音量接口
    result = (*m_playerObject)->GetInterface(m_playerObject, SL_IID_VOLUME, &m_playerVolume);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get volume interface");
        return false;
    }
    
    // 设置音量 (0-100)
    (*m_playerVolume)->SetVolumeLevel(m_playerVolume, 0); // 0 = 最大音量
    
    m_initialized = true;
    LOGD("Audio output initialized successfully");
    return true;
}

void AudioOutput::shutdown() {
    LOGD("Shutting down audio output");
    
    if (m_playerObject != nullptr) {
        (*m_playerObject)->Destroy(m_playerObject);
        m_playerObject = nullptr;
        m_playerPlay = nullptr;
        m_playerBufferQueue = nullptr;
        m_playerVolume = nullptr;
    }
    
    if (m_outputMixObject != nullptr) {
        (*m_outputMixObject)->Destroy(m_outputMixObject);
        m_outputMixObject = nullptr;
    }
    
    if (m_engineObject != nullptr) {
        (*m_engineObject)->Destroy(m_engineObject);
        m_engineObject = nullptr;
        m_engineEngine = nullptr;
    }
    
    m_initialized = false;
    m_playing = false;
}

void AudioOutput::play() {
    if (!m_initialized || m_playing) {
        return;
    }
    
    LOGD("Starting audio playback");
    
    // 预填充缓冲区
    m_currentBuffer = 0;
    (*m_playerBufferQueue)->Enqueue(m_playerBufferQueue, m_buffers[0], GB_AUDIO_BUFFER_SIZE * sizeof(int16_t));
    (*m_playerBufferQueue)->Enqueue(m_playerBufferQueue, m_buffers[1], GB_AUDIO_BUFFER_SIZE * sizeof(int16_t));
    
    // 开始播放
    (*m_playerPlay)->SetPlayState(m_playerPlay, SL_PLAYSTATE_PLAYING);
    m_playing = true;
}

void AudioOutput::pause() {
    if (!m_initialized || !m_playing) {
        return;
    }
    
    LOGD("Pausing audio playback");
    (*m_playerPlay)->SetPlayState(m_playerPlay, SL_PLAYSTATE_PAUSED);
    m_playing = false;
}

void AudioOutput::writeSamples(const int16_t* samples, int count) {
    // 这里可以实现一个环形缓冲区来存储音频样本
    // 当缓冲区足够时，通过回调填充到OpenSL ES
    // 简化实现：直接忽略，依赖回调生成音频
}

void AudioOutput::bufferQueueCallback(SLAndroidSimpleBufferQueueItf bq, void* context) {
    AudioOutput* audio = static_cast<AudioOutput*>(context);
    audio->processBuffer();
}

void AudioOutput::processBuffer() {
    // 生成音频数据（这里应该从模拟器核心获取实际的音频数据）
    // 简化实现：填充静音
    int16_t* buffer = m_buffers[m_currentBuffer];
    
    // TODO: 从GB音频核心填充实际音频数据
    // 现在填充静音
    std::memset(buffer, 0, GB_AUDIO_BUFFER_SIZE * sizeof(int16_t));
    
    // 将填充好的缓冲区入队
    SLresult result = (*m_playerBufferQueue)->Enqueue(m_playerBufferQueue, buffer, 
                                                       GB_AUDIO_BUFFER_SIZE * sizeof(int16_t));
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to enqueue buffer");
    }
    
    // 切换到下一个缓冲区
    m_currentBuffer = (m_currentBuffer + 1) % 2;
}
