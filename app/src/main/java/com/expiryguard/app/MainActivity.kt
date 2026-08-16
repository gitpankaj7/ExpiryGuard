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

import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// Global event bus for payment results
object PaymentEventBus {
    val paymentResults = MutableSharedFlow<PaymentResult>()
}

sealed class PaymentResult {
    data class Success(val paymentId: String, val orderId: String, val signature: String) : PaymentResult()
    data class Error(val code: Int, val response: String) : PaymentResult()
}

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpiryGuardApp

        setContent {
            val isDarkMode by app.container.userPreferences.isDarkMode.collectAsState(initial = false)
            val language by app.container.userPreferences.language.collectAsState(initial = "en")
            val composeScope = rememberCoroutineScope()

            val context = androidx.compose.ui.platform.LocalContext.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            
            val locale = java.util.Locale(language)
            val config = android.content.res.Configuration(configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides config
            ) {
                ExpiryGuardTheme(darkTheme = isDarkMode) {
                    AppNavigation(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { enabled ->
                            composeScope.launch {
                                app.container.userPreferences.setDarkMode(enabled)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onPaymentSuccess(paymentId: String, paymentData: PaymentData) {
        scope.launch {
            PaymentEventBus.paymentResults.emit(
                PaymentResult.Success(
                    paymentId = paymentData.paymentId,
                    orderId = paymentData.orderId,
                    signature = paymentData.signature
                )
            )
        }
    }

    override fun onPaymentError(code: Int, response: String, paymentData: PaymentData?) {
        scope.launch {
            PaymentEventBus.paymentResults.emit(
                PaymentResult.Error(code, response)
            )
        }
    }
}
