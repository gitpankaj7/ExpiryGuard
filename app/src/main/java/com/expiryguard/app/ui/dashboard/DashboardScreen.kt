package com.expiryguard.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expiryguard.app.ui.components.ProductCard
import com.expiryguard.app.ui.components.StatsCard
import com.expiryguard.app.ui.theme.GradientBlueEnd
import com.expiryguard.app.ui.theme.GradientBlueStart
import com.expiryguard.app.ui.theme.GradientDarkRedEnd
import com.expiryguard.app.ui.theme.GradientDarkRedStart
import com.expiryguard.app.ui.theme.GradientOrangeEnd
import com.expiryguard.app.ui.theme.GradientOrangeStart
import com.expiryguard.app.ui.theme.GradientRedEnd
import com.expiryguard.app.ui.theme.GradientRedStart
import com.expiryguard.app.ui.product.ProductFilter
import kotlinx.coroutines.delay
import com.expiryguard.app.data.local.UserProfile

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    onNavigateToProducts: (ProductFilter) -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onScanBarcode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // ── Header ──

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(com.expiryguard.app.R.string.dashboard),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (userProfile?.isSubscribed == true) {
                        Surface(
                            onClick = onNavigateToSubscription,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Premium Member",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (userProfile != null) {
                        val days = userProfile.getTrialDaysRemaining()
                        Surface(
                            onClick = onNavigateToSubscription,
                            color = if (days > 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (days > 0) "$days days left in free trial • Upgrade" else "Free trial expired • Upgrade",
                                fontSize = 14.sp,
                                color = if (days > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Track. Alert. Save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        Spacer(Modifier.height(20.dp))

        // ── Stats Grid (2×2) ──

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatsCard(
                        title = "Total Products",
                        value = state.totalProducts.toString(),
                        icon = Icons.Rounded.Inventory2,
                        gradientColors = listOf(GradientBlueStart, GradientBlueEnd),
                        onClick = { onNavigateToProducts(ProductFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Expiring in 7 Days",
                        value = state.expiring7Days.toString(),
                        icon = Icons.Rounded.Warning,
                        gradientColors = listOf(GradientRedStart, GradientRedEnd),
                        onClick = { onNavigateToProducts(ProductFilter.EXPIRING_SOON) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatsCard(
                        title = "Expiring in 30 Days",
                        value = state.expiring30Days.toString(),
                        icon = Icons.Rounded.Timer,
                        gradientColors = listOf(GradientOrangeStart, GradientOrangeEnd),
                        onClick = { onNavigateToProducts(ProductFilter.EXPIRING_SOON) },
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Expired",
                        value = state.expiredProducts.toString(),
                        icon = Icons.Rounded.Error,
                        gradientColors = listOf(GradientDarkRedStart, GradientDarkRedEnd),
                        onClick = { onNavigateToProducts(ProductFilter.EXPIRED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        Spacer(Modifier.height(24.dp))

        // ── Quick Actions ──

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onAddProduct,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Product")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToProducts(ProductFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View All")
                    }
                }
                FilledTonalButton(
                    onClick = onScanBarcode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Barcode")
                }
            }

        Spacer(Modifier.height(28.dp))

        // ── Expiring Soon Section ──

            Column {
                Text(
                    text = stringResource(com.expiryguard.app.R.string.expiring_soon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.recentExpiringProducts.isEmpty()) {
                    Text(
                        text = "All products are safe! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    state.recentExpiringProducts.forEach { product ->
                        ProductCard(
                            product = product,
                            onClick = { onEditProduct(product.id) },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
    }
}
