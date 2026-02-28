package com.jboy.emulator.core

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * 输入处理类 - 处理游戏手柄和键盘输入
 */
class InputHandler private constructor() {

    companion object {
        private const val TAG = "InputHandler"
        
        @Volatile
        private var instance: InputHandler? = null
        
        fun getInstance(): InputHandler {
            return instance ?: synchronized(this) {
                instance ?: InputHandler().also { instance = it }
            }
        }
    }
    
    // 当前按键状态
    private var currentKeyMask: Int = 0
    private val keyMaskLock = Object()
    
    // 按键映射配置
    private val keyMapping = mutableMapOf<Int, Int>()
    
    // 输入回调接口
    interface InputCallback {
        fun onKeyStateChanged(keyMask: Int)
        fun onKeyPressed(key: Int)
        fun onKeyReleased(key: Int)
    }
    
    private var inputCallback: InputCallback? = null
    
    /**
     * 初始化默认按键映射
     */
    fun initDefaultMapping() {
        // 键盘默认映射
        keyMapping[KeyEvent.KEYCODE_DPAD_UP] = InputKeys.UP
        keyMapping[KeyEvent.KEYCODE_DPAD_DOWN] = InputKeys.DOWN
        keyMapping[KeyEvent.KEYCODE_DPAD_LEFT] = InputKeys.LEFT
        keyMapping[KeyEvent.KEYCODE_DPAD_RIGHT] = InputKeys.RIGHT
        keyMapping[KeyEvent.KEYCODE_BUTTON_A] = InputKeys.A
        keyMapping[KeyEvent.KEYCODE_BUTTON_B] = InputKeys.B
        keyMapping[KeyEvent.KEYCODE_BUTTON_X] = InputKeys.A
        keyMapping[KeyEvent.KEYCODE_BUTTON_Y] = InputKeys.B
        keyMapping[KeyEvent.KEYCODE_BUTTON_L1] = InputKeys.L
        keyMapping[KeyEvent.KEYCODE_BUTTON_R1] = InputKeys.R
        keyMapping[KeyEvent.KEYCODE_BUTTON_SELECT] = InputKeys.SELECT
        keyMapping[KeyEvent.KEYCODE_BUTTON_START] = InputKeys.START
        keyMapping[KeyEvent.KEYCODE_ENTER] = InputKeys.START
        keyMapping[KeyEvent.KEYCODE_SPACE] = InputKeys.SELECT
        
        Log.d(TAG, "Default key mapping initialized")
    }
    
    /**
     * 设置自定义按键映射
     */
    fun setKeyMapping(androidKeyCode: Int, inputKey: Int) {
        keyMapping[androidKeyCode] = inputKey
        Log.d(TAG, "Key mapping set: $androidKeyCode -> $inputKey")
    }
    
    /**
     * 移除按键映射
     */
    fun removeKeyMapping(androidKeyCode: Int) {
        keyMapping.remove(androidKeyCode)
    }
    
    /**
     * 清除所有按键映射
     */
    fun clearKeyMapping() {
        keyMapping.clear()
    }
    
    /**
     * 处理按键按下事件
     */
    fun onKeyDown(keyCode: Int): Boolean {
        val inputKey = keyMapping[keyCode] ?: return false
        
        synchronized(keyMaskLock) {
            if (currentKeyMask and inputKey == 0) {
                currentKeyMask = currentKeyMask or inputKey
                inputCallback?.onKeyPressed(inputKey)
                inputCallback?.onKeyStateChanged(currentKeyMask)
                updateEmulatorInput()
            }
        }
        
        return true
    }
    
    /**
     * 处理按键抬起事件
     */
    fun onKeyUp(keyCode: Int): Boolean {
        val inputKey = keyMapping[keyCode] ?: return false
        
        synchronized(keyMaskLock) {
            if (currentKeyMask and inputKey != 0) {
                currentKeyMask = currentKeyMask and inputKey.inv()
                inputCallback?.onKeyReleased(inputKey)
                inputCallback?.onKeyStateChanged(currentKeyMask)
                updateEmulatorInput()
            }
        }
        
        return true
    }
    
    /**
     * 处理触摸事件（虚拟按键）
     */
    fun onTouchKey(key: Int, isPressed: Boolean) {
        synchronized(keyMaskLock) {
            if (isPressed) {
                if (currentKeyMask and key == 0) {
                    currentKeyMask = currentKeyMask or key
                    inputCallback?.onKeyPressed(key)
                }
            } else {
                if (currentKeyMask and key != 0) {
                    currentKeyMask = currentKeyMask and key.inv()
                    inputCallback?.onKeyReleased(key)
                }
            }
            inputCallback?.onKeyStateChanged(currentKeyMask)
            updateEmulatorInput()
        }
    }
    
    /**
     * 处理摇杆/方向键输入
     */
    fun onJoystickInput(event: MotionEvent) {
        // 处理X轴
        val xAxis = event.getAxisValue(MotionEvent.AXIS_X)
        when {
            xAxis < -0.5f -> simulateKey(InputKeys.LEFT, true)
            xAxis > 0.5f -> simulateKey(InputKeys.RIGHT, true)
            else -> {
                simulateKey(InputKeys.LEFT, false)
                simulateKey(InputKeys.RIGHT, false)
            }
        }
        
        // 处理Y轴
        val yAxis = event.getAxisValue(MotionEvent.AXIS_Y)
        when {
            yAxis < -0.5f -> simulateKey(InputKeys.UP, true)
            yAxis > 0.5f -> simulateKey(InputKeys.DOWN, true)
            else -> {
                simulateKey(InputKeys.UP, false)
                simulateKey(InputKeys.DOWN, false)
            }
        }
    }
    
    /**
     * 模拟按键
     */
    private fun simulateKey(key: Int, isPressed: Boolean) {
        synchronized(keyMaskLock) {
            if (isPressed) {
                if (currentKeyMask and key == 0) {
                    currentKeyMask = currentKeyMask or key
                    inputCallback?.onKeyPressed(key)
                }
            } else {
                if (currentKeyMask and key != 0) {
                    currentKeyMask = currentKeyMask and key.inv()
                    inputCallback?.onKeyReleased(key)
                }
            }
            inputCallback?.onKeyStateChanged(currentKeyMask)
            updateEmulatorInput()
        }
    }
    
    /**
     * 更新模拟器输入状态
     */
    private fun updateEmulatorInput() {
        EmulatorCore.getInstance().setInputState(currentKeyMask)
    }
    
    /**
     * 获取当前按键掩码
     */
    fun getCurrentKeyMask(): Int {
        return synchronized(keyMaskLock) {
            currentKeyMask
        }
    }
    
    /**
     * 重置所有按键状态
     */
    fun resetKeys() {
        synchronized(keyMaskLock) {
            currentKeyMask = 0
            inputCallback?.onKeyStateChanged(0)
            updateEmulatorInput()
        }
        Log.d(TAG, "All keys reset")
    }
    
    /**
     * 检查特定按键是否按下
     */
    fun isKeyPressed(key: Int): Boolean {
        return synchronized(keyMaskLock) {
            currentKeyMask and key != 0
        }
    }
    
    /**
     * 设置输入回调
     */
    fun setInputCallback(callback: InputCallback?) {
        this.inputCallback = callback
    }
    
    /**
     * 获取按键名称（用于调试和显示）
     */
    fun getKeyName(key: Int): String {
        return when (key) {
            InputKeys.A -> "A"
            InputKeys.B -> "B"
            InputKeys.SELECT -> "SELECT"
            InputKeys.START -> "START"
            InputKeys.RIGHT -> "RIGHT"
            InputKeys.LEFT -> "LEFT"
            InputKeys.UP -> "UP"
            InputKeys.DOWN -> "DOWN"
            InputKeys.R -> "R"
            InputKeys.L -> "L"
            else -> "UNKNOWN"
        }
    }
}

/**
 * 输入按键定义 - 与GBA硬件按键对应
 */
object InputKeys {
    const val A = 0x01
    const val B = 0x02
    const val SELECT = 0x04
    const val START = 0x08
    const val RIGHT = 0x10
    const val LEFT = 0x20
    const val UP = 0x40
    const val DOWN = 0x80
    const val R = 0x100
    const val L = 0x200
}
