#include <jni.h>
#include <android/log.h>
#include "emulator_core.h"

#define LOG_TAG "JBOY_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static EmulatorCore* s_emulator = nullptr;

extern "C" {
    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeInit(JNIEnv* env, jobject thiz) {
        LOGD("Native init called");
        if (s_emulator == nullptr) {
            s_emulator = new EmulatorCore();
            s_emulator->init();
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeLoadRom(JNIEnv* env, jobject thiz, jstring romPath) {
        LOGD("Native load ROM called");
        if (s_emulator != nullptr) {
            const char* path = env->GetStringUTFChars(romPath, nullptr);
            s_emulator->loadRom(path);
            env->ReleaseStringUTFChars(romPath, path);
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeRunFrame(JNIEnv* env, jobject thiz) {
        if (s_emulator != nullptr) {
            s_emulator->runFrame();
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSetInput(JNIEnv* env, jobject thiz, jint buttons) {
        if (s_emulator != nullptr) {
            s_emulator->setInput(buttons);
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeSaveState(JNIEnv* env, jobject thiz, jint slot) {
        LOGD("Native save state called, slot: %d", slot);
        if (s_emulator != nullptr) {
            s_emulator->saveState(slot);
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeLoadState(JNIEnv* env, jobject thiz, jint slot) {
        LOGD("Native load state called, slot: %d", slot);
        if (s_emulator != nullptr) {
            s_emulator->loadState(slot);
        }
    }

    JNIEXPORT void JNICALL Java_com_jboy_emulator_core_EmulatorCore_nativeCleanup(JNIEnv* env, jobject thiz) {
        LOGD("Native cleanup called");
        if (s_emulator != nullptr) {
            s_emulator->cleanup();
            delete s_emulator;
            s_emulator = nullptr;
        }
    }
}
