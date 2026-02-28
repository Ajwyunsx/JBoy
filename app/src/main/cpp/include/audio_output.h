#ifndef AUDIO_OUTPUT_H
#define AUDIO_OUTPUT_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <cstdint>

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
    bool isInitialized() const;
    
private:
    static void bufferQueueCallback(SLAndroidSimpleBufferQueueItf bq, void* context);
    void processBuffer();
    
    SLObjectItf m_engineObject = nullptr;
    SLEngineItf m_engineEngine = nullptr;
    SLObjectItf m_outputMixObject = nullptr;
    SLObjectItf m_playerObject = nullptr;
    SLPlayItf m_playerPlay = nullptr;
    SLAndroidSimpleBufferQueueItf m_playerBufferQueue = nullptr;
    SLVolumeItf m_playerVolume = nullptr;
    
    int16_t m_buffers[2][GB_AUDIO_BUFFER_SIZE];
    int m_currentBuffer = 0;
    bool m_initialized = false;
    bool m_playing = false;
};

#endif // AUDIO_OUTPUT_H
