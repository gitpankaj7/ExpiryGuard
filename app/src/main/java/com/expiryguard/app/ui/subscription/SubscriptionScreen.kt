package com.expiryguard.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.local.UserProfile
import com.expiryguard.app.data.repository.UserRepository
import kotlinx.coroutines.launch

import com.expiryguard.app.PaymentEventBus
import com.expiryguard.app.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SubscriptionViewModel(
    private val userRepository: UserRepository,
    private val authRepository: com.expiryguard.app.data.repository.AuthRepository
) : ViewModel() {
    val userProfile = userRepository.userProfileFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _razorpayPayload = MutableStateFlow<JSONObject?>(null)
    val razorpayPayload: StateFlow<JSONObject?> = _razorpayPayload.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    init {
        viewModelScope.launch {
            PaymentEventBus.paymentResults.collect { result ->
                when (result) {
                    is PaymentResult.Success -> verifyPayment(result.orderId, result.paymentId, result.signature)
                    is PaymentResult.Error -> {
                        _isLoading.value = false
                        _paymentError.value = "Payment failed: ${result.response}"
                    }
                }
            }
        }
    }

    fun startCheckout() {
        viewModelScope.launch {
            _isLoading.value = true
            _paymentError.value = null
            
            val orderData = authRepository.createRazorpayOrder()
            if (orderData != null) {
                val orderId = orderData["id"] as String
                val amount = orderData["amount"] as Int
                val currency = orderData["currency"] as String
                val keyId = orderData["keyId"] as String
                
                val options = JSONObject()
                options.put("key", keyId)
                options.put("amount", amount)
                options.put("currency", currency)
                options.put("name", "ExpiryGuard Premium")
                options.put("description", "1-Month Subscription")
                options.put("order_id", orderId)
                
                _razorpayPayload.value = options
            } else {
                _isLoading.value = false
                _paymentError.value = "Could not create order. Please try again."
            }
        }
    }
    
    fun onCheckoutLaunched() {
        _razorpayPayload.value = null
    }

    private suspend fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        _isLoading.value = true
        val verified = authRepository.verifyRazorpayPayment(orderId, paymentId, signature)
        _isLoading.value = false
        if (verified) {
            // Profile is updated on the server, local snapshot listener will automatically update UI
            _paymentError.value = null
        } else {
            _paymentError.value = "Payment verification failed."
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                SubscriptionViewModel(
                    app.container.userRepository,
                    com.expiryguard.app.data.repository.AuthRepository()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SubscriptionViewModel = viewModel(factory = SubscriptionViewModel.Factory)
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    
    val isLoading by viewModel.isLoading.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()
    val razorpayPayload by viewModel.razorpayPayload.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(razorpayPayload) {
        razorpayPayload?.let { payload ->
            val activity = context as? android.app.Activity
            if (activity != null) {
                val checkout = com.razorpay.Checkout()
                checkout.setKeyID(payload.getString("key"))
                checkout.open(activity, payload)
                viewModel.onCheckoutLaunched()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Subscription") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "ExpiryGuard Premium",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (userProfile?.isSubscribed == true) {
                Text(
                    text = "You are a Premium Member!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    PaddingValues(16.dp)
                    Text(
                        text = "Thank you for your support. All premium features are permanently unlocked.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                val trialDays = userProfile?.getTrialDaysRemaining() ?: 0
                if (trialDays > 0) {
                    Text(
                        text = "You have $trialDays days left in your free trial.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Your free trial has expired.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureRow("Unlimited Product Tracking")
                    FeatureRow("Cloud Sync & Backup")
                    FeatureRow("Smart Barcode Scanning")
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                if (paymentError != null) {
                    Text(
                        text = paymentError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                Button(
                    onClick = { viewModel.startCheckout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Upgrade for ₹49/month", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Cancel anytime. Secure payment via Razorpay.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
