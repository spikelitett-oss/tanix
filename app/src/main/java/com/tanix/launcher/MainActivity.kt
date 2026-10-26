package com.tanix.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tanix.launcher.ui.TanixTheme
import com.tanix.launcher.ui.animMs
import com.tanix.launcher.ui.screens.HomeScreen
import com.tanix.launcher.ui.screens.Onboarding
import com.tanix.launcher.ui.screens.SettingsScreen

enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Повторное нажатие Home возвращает на главный экран
        addOnNewIntentListener { vm.onHomePressed() }
        setContent {
            val s = vm.s
            var screen by remember { mutableStateOf(Screen.HOME) }
            LaunchedEffect(vm.homeSignal) { screen = Screen.HOME }
            TanixTheme(s) {
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
                val ms = animMs(320)
                val target = if (!s.onboarded) 0 else if (screen == Screen.HOME) 1 else 2
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Переходы между экранами: плавное проявление + лёгкое масштабирование
                    AnimatedContent(
                        targetState = target,
                        transitionSpec = {
                            (fadeIn(tween(ms, delayMillis = ms / 4)) + scaleIn(tween(ms), initialScale = 0.94f)) togetherWith
                                (fadeOut(tween(ms / 2)) + scaleOut(tween(ms), targetScale = 1.04f))
                        },
                        label = "screens"
                    ) { t ->
                        when (t) {
                            0 -> Onboarding(vm)
                            1 -> HomeScreen(vm) { screen = Screen.SETTINGS }
                            else -> SettingsScreen(vm) { screen = Screen.HOME }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
    }
}
