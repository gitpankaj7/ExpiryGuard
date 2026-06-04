package com.expiryguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.expiryguard.app.ui.navigation.AppNavigation
import com.expiryguard.app.ui.theme.ExpiryGuardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpiryGuardApp

        setContent {
            val isDarkMode by app.container.userPreferences.isDarkMode.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            ExpiryGuardTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { enabled ->
                        scope.launch {
                            app.container.userPreferences.setDarkMode(enabled)
                        }
                    }
                )
            }
        }
    }
}
