package com.jboy.emulator

import android.app.Application
import android.util.Log
import com.jboy.emulator.core.*
import dagger.hilt.android.HiltAndroidApp

/**
 * JBoy 应用程序类 - 全局初始化管理
 */
@HiltAndroidApp
class JBoyApplication : Application() {

    companion object {
        private const val TAG = "JBoyApplication"
        
        @Volatile
        lateinit var instance: JBoyApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "JBoyApplication starting...")
        
        // 初始化应用
        initializeApp()
        
        Log.d(TAG, "JBoyApplication initialized successfully")
    }
    
    /**
     * 应用初始化
     */
    private fun initializeApp() {
        try {
            // 初始化核心组件
            initializeCoreComponents()
            
            // 加载配置
            loadConfiguration()
            
            // 设置全局异常处理
            setupUncaughtExceptionHandler()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize app: ${e.message}", e)
        }
    }
    
    /**
     * 初始化核心组件
     */
    private fun initializeCoreComponents() {
        // 输入处理器
        InputHandler.getInstance().initDefaultMapping()
        Log.d(TAG, "InputHandler initialized")
        
        // 音频输出
        AudioOutput.getInstance()
        Log.d(TAG, "AudioOutput initialized")
        
        // 视频渲染器
        VideoRenderer.getInstance()
        Log.d(TAG, "VideoRenderer initialized")
        
        // 模拟器核心
        EmulatorCore.getInstance()
        Log.d(TAG, "EmulatorCore initialized")
    }
    
    /**
     * 加载应用配置
     */
    private fun loadConfiguration() {
        val prefs = getSharedPreferences("jboy_settings", MODE_PRIVATE)
        
        // 加载音频设置
        val audioEnabled = prefs.getBoolean("audio_enabled", true)
        val audioVolume = prefs.getFloat("audio_volume", 1.0f)
        AudioOutput.getInstance().setAudioEnabled(audioEnabled)
        AudioOutput.getInstance().setVolume(audioVolume)
        
        // 加载视频设置
        val scaleModeOrdinal = prefs.getInt("video_scale_mode", VideoRenderer.ScaleMode.FIT.ordinal)
        VideoRenderer.getInstance().setScaleMode(
            VideoRenderer.ScaleMode.values()[scaleModeOrdinal]
        )
        
        Log.d(TAG, "Configuration loaded")
    }
    
    /**
     * 保存应用配置
     */
    fun saveConfiguration() {
        val prefs = getSharedPreferences("jboy_settings", MODE_PRIVATE)
        with(prefs.edit()) {
            // 保存音频设置
            putBoolean("audio_enabled", AudioOutput.getInstance().isAudioEnabled())
            putFloat("audio_volume", AudioOutput.getInstance().getVolume())
            
            apply()
        }
        Log.d(TAG, "Configuration saved")
    }
    
    /**
     * 设置全局异常处理
     */
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            
            // 清理资源
            cleanupResources()
            
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 清理资源
     */
    private fun cleanupResources() {
        Log.d(TAG, "Cleaning up resources...")
        
        try {
            // 保存配置
            saveConfiguration()
            
            // 清理核心组件
            EmulatorCore.getInstance().cleanup()
            AudioOutput.getInstance().cleanup()
            VideoRenderer.getInstance().cleanup()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "JBoyApplication terminating...")
        cleanupResources()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
        // 可以在这里释放一些非关键资源
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "Critical memory level: $level")
            }
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_MODERATE -> {
                Log.w(TAG, "Low memory level: $level")
            }
            else -> {
                Log.d(TAG, "Memory trim level: $level")
            }
        }
    }
}
