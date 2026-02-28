package com.jboy.emulator

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.jboy.emulator.core.InputHandler
import com.jboy.emulator.data.settingsDataStore
import com.jboy.emulator.input.HardwareKeyCaptureBus
import com.jboy.emulator.ui.navigation.NavGraph
import com.jboy.emulator.ui.theme.JBoyEmulatorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val coreInputHandler by lazy { InputHandler.getInstance() }
    private val prefLanguage = stringPreferencesKey("language")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedLanguage()
        enableEdgeToEdge()
        setContent {
            JBoyEmulatorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && isExternalControllerEvent(event)) {
            HardwareKeyCaptureBus.emitKeyDown(keyCode)
        }
        if (coreInputHandler.onKeyDown(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (coreInputHandler.onKeyUp(keyCode)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val source = event.source
        val isJoystick = source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (isJoystick && event.action == MotionEvent.ACTION_MOVE) {
            coreInputHandler.onJoystickInput(event)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun isExternalControllerEvent(event: KeyEvent): Boolean {
        if (event.repeatCount != 0) {
            return false
        }

        val source = event.source
        val fromGamepad =
            source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
                source and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD

        return fromGamepad && event.keyCode != KeyEvent.KEYCODE_BACK
    }

    private fun applySavedLanguage() {
        val language = runBlocking {
            applicationContext.settingsDataStore.data
                .map { prefs -> prefs[prefLanguage] ?: "zh-CN" }
                .first()
        }

        val languageTag = when (language) {
            "zh-CN" -> "zh-CN"
            "zh-TW" -> "zh-TW"
            "en" -> "en"
            "ja" -> "ja"
            else -> "zh-CN"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}

@Composable
fun AppPreview() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JBoyEmulatorTheme {
        AppPreview()
    }
}
