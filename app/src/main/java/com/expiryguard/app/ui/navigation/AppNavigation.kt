package com.expiryguard.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.ui.auth.AuthViewModel
import com.expiryguard.app.ui.auth.ForgotPasswordScreen
import com.expiryguard.app.ui.auth.LoginScreen
import com.expiryguard.app.ui.auth.RegisterScreen
import com.expiryguard.app.ui.dashboard.DashboardScreen
import com.expiryguard.app.ui.product.AddEditProductScreen
import com.expiryguard.app.ui.product.ProductFilter
import com.expiryguard.app.ui.product.ProductListScreen
import com.expiryguard.app.ui.scanner.BarcodeScannerScreen
import com.expiryguard.app.ui.scanner.ExpiryOcrScreen
import com.expiryguard.app.ui.settings.SettingsScreen
import com.expiryguard.app.ui.subscription.SubscriptionScreen

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("Dashboard", Icons.Rounded.Dashboard),
    NavItem("Products", Icons.Rounded.Inventory2),
    NavItem("Settings", Icons.Rounded.Settings)
)

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ExpiryGuardApp
    val loggedInUserId by app.container.authRepository.loggedInUserIdFlow.collectAsState(initial = "__LOADING__")
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)

    var authRoute by rememberSaveable { mutableStateOf("login") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddEditProduct by rememberSaveable { mutableStateOf(false) }
    var editProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scannerFromForm by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionScreen by rememberSaveable { mutableStateOf(false) }

    val userProfile by app.container.userRepository.userProfileFlow.collectAsState(initial = null)

    // OCR scanner state
    var showExpiryOcr by rememberSaveable { mutableStateOf(false) }
    var scannedExpiryDate by rememberSaveable { mutableStateOf<Long?>(null) }

    // Filter state for navigating from Dashboard to Product List
    var requestedFilter by rememberSaveable { mutableStateOf(ProductFilter.ALL) }
    var filterRequestKey by rememberSaveable { mutableIntStateOf(0) }

    // Key to force fresh ViewModel for each Add/Edit session
    var addEditKey by rememberSaveable { mutableIntStateOf(0) }

    // Wait for the initial DataStore emission before showing auth vs main
    if (loggedInUserId == "__LOADING__") {
        return
    }

    if (loggedInUserId == null) {
        when (authRoute) {
            "login" -> LoginScreen(
                onNavigateToRegister = {
                    authViewModel.clearError()
                    authRoute = "register"
                },
                onNavigateToForgotPassword = {
                    authViewModel.clearError()
                    authRoute = "forgot"
                }
            )
            "register" -> RegisterScreen(
                onNavigateToLogin = {
                    authViewModel.clearError()
                    authRoute = "login"
                }
            )
            "forgot" -> ForgotPasswordScreen(
                onNavigateBack = {
                    authViewModel.clearError()
                    authRoute = "login"
                }
            )
        }
        return
    }

    // Full-screen Expiry OCR scanner
    if (showExpiryOcr) {
        ExpiryOcrScreen(
            onExpiryDetected = { epochMillis ->
                scannedExpiryDate = epochMillis
                showExpiryOcr = false
            },
            onNavigateBack = {
                showExpiryOcr = false
            }
        )
        return
    }

    // Full-screen Barcode Scanner overlay
    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { barcode ->
                scannedBarcode = barcode
                showScanner = false
                if (!scannerFromForm) {
                    addEditKey++  // Fresh ViewModel for new product via scanner
                    showAddEditProduct = true
                }
            },
            onNavigateBack = {
                showScanner = false
                scannerFromForm = false
            }
        )
        return
    }

    // Full-screen Subscription Screen
    if (showSubscriptionScreen) {
        SubscriptionScreen(
            onNavigateBack = { showSubscriptionScreen = false }
        )
        return
    }

    // Full-screen overlay for Add/Edit — keyed to force fresh ViewModel
    if (showAddEditProduct || editProductId != null) {
        key(addEditKey, editProductId) {
            AddEditProductScreen(
                productId = editProductId,
                scannedBarcode = scannedBarcode,
                scannedExpiryDate = scannedExpiryDate,
                screenKey = addEditKey,
                onNavigateBack = {
                    showAddEditProduct = false
                    editProductId = null
                    scannedBarcode = null
                    scannerFromForm = false
                    scannedExpiryDate = null
                },
                onOpenScanner = {
                    scannerFromForm = true
                    showScanner = true
                },
                onOpenExpiryOcr = {
                    showExpiryOcr = true
                }
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn()).togetherWith(fadeOut())
            },
            label = "tabNavigation"
        ) { tab ->
            when (tab) {
                0 -> DashboardScreen(
                    userProfile = userProfile,
                    onNavigateToProducts = { filter -> 
                        requestedFilter = filter
                        filterRequestKey++
                        selectedTab = 1 
                    },
                    onNavigateToSubscription = {
                        showSubscriptionScreen = true
                    },
                    onAddProduct = {
                        if (userProfile?.isTrialExpired() == true && userProfile?.isSubscribed == false) {
                            showSubscriptionScreen = true
                        } else {
                            addEditKey++  // Fresh ViewModel every time
                            showAddEditProduct = true
                        }
                    },
                    onEditProduct = { id ->
                        addEditKey++
                        editProductId = id
                    },
                    onScanBarcode = {
                        scannerFromForm = false
                        showScanner = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> ProductListScreen(
                    userProfile = userProfile,
                    requestedFilter = requestedFilter,
                    filterRequestKey = filterRequestKey,
                    onAddProduct = {
                        if (userProfile?.isTrialExpired() == true && userProfile?.isSubscribed == false) {
                            showSubscriptionScreen = true
                        } else {
                            addEditKey++
                            showAddEditProduct = true
                        }
                    },
                    onEditProduct = { id ->
                        addEditKey++
                        editProductId = id
                    },
                    onScanBarcode = {
                        scannerFromForm = false
                        showScanner = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> SettingsScreen(
                    userProfile = userProfile,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onLogout = { authViewModel.logout() },
                    onNavigateToSubscription = {
                        showSubscriptionScreen = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
